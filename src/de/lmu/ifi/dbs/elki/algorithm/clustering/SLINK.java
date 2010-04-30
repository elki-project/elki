package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Efficient implementation of the Single-Link Algorithm SLINK of R. Sibson.
 * <p>
 * Reference: R. Sibson: SLINK: An optimally efficient algorithm for the
 * single-link cluster method. <br>
 * In: The Computer Journal 16 (1973), No. 1, p. 30-34.
 * </p>
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject the algorithm is applied on
 * @param <D> the type of Distance used
 */
@Title("SLINK: Single Link Clustering")
@Description("Hierarchical clustering algorithm based on single-link connectivity.")
@Reference(authors = "R. Sibson", title = "SLINK: An optimally efficient algorithm for the single-link cluster method", booktitle = "The Computer Journal 16 (1973), No. 1, p. 30-34.", url = "http://dx.doi.org/10.1093/comjnl/16.1.30")
public class SLINK<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, MultiResult> {
  /**
   * Association ID for SLINK pi pointer
   */
  private static final AssociationID<DBID> SLINK_PI = AssociationID.getOrCreateAssociationID("SLINK pi", DBID.class);

  /**
   * Association ID for SLINK lambda value
   */
  private static final AssociationID<Distance<?>> SLINK_LAMBDA = AssociationID.getOrCreateAssociationIDGenerics("SLINK lambda", Distance.class);

  /**
   * The values of the function Pi of the pointer representation.
   */
  private WritableDataStore<DBID> pi;

  /**
   * The values of the function Lambda of the pointer representation.
   */
  private WritableDataStore<D> lambda;

  /**
   * The values of the helper function m to determine the pointer
   * representation.
   */
  private WritableDataStore<D> m;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SLINK(Parameterization config) {
    super(config);
  }

  /**
   * Performs the SLINK algorithm on the given database.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    Class<D> distCls = (Class<D>) getDistanceFunction().getDistanceFactory().getClass();
    WritableRecordStore store = DataStoreUtil.makeRecordStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, DBID.class, distCls);
    pi = store.getStorage(0, DBID.class);
    lambda = store.getStorage(1, distCls);
    m = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, distCls);
    try {
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Clustering", database.size(), logger) : null;
      getDistanceFunction().setDatabase(database);

      // sort the db objects according to their ids
      // TODO: is this cheap or expensive?
      List<DBID> ids = new ArrayList(database.getIDs().asCollection());
      Collections.sort(ids);

      ModifiableDBIDs processedIDs = DBIDUtil.newArray(ids.size());
      // apply the algorithm
      int cnt = 0;
      for(DBID id : ids) {
        step1(id);
        step2(id, processedIDs);
        step3(id, processedIDs);
        step4(id, processedIDs);

        processedIDs.add(id);

        cnt++;
        if(progress != null) {
          progress.setProcessed(cnt, logger);
        }
      }
      if (progress != null) {
        progress.ensureCompleted(logger);
      }
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }

    MultiResult result = new MultiResult();
    result.addResult(new AnnotationFromDataStore<DBID>(SLINK_PI, pi));
    result.addResult(new AnnotationFromDataStore<Distance<?>>(SLINK_LAMBDA, lambda));
    return result;
  }

  /**
   * First step: Initialize P(id) = id, L(id) = infinity.
   * 
   * @param newID the id of the object to be inserted into the pointer
   *        representation
   */
  private void step1(DBID newID) {
    // P(n+1) = n+1:
    pi.put(newID, newID);
    // L(n+1) = infinity
    lambda.put(newID, getDistanceFunction().infiniteDistance());
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   * 
   * @param newID the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   */
  private void step2(DBID newID, ModifiableDBIDs processedIDs) {
    // M(i) = dist(i, n+1)
    for(DBID id : processedIDs) {
      D distance = getDistanceFunction().distance(newID, id);
      m.put(id, distance);
    }
  }

  /**
   * Third step: Determine the values for P and L
   * 
   * @param newID the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   */
  private void step3(DBID newID, ModifiableDBIDs processedIDs) {
    // for i = 1..n
    for(DBID id : processedIDs) {
      D l = lambda.get(id);
      D m = this.m.get(id);
      DBID p = pi.get(id);
      D mp = this.m.get(p);

      // if L(i) >= M(i)
      if(l.compareTo(m) >= 0) {
        D min = mp.compareTo(l) <= 0 ? mp : l;
        // M(P(i)) = min { M(P(i)), L(i) }
        this.m.put(p, min);

        // L(i) = M(i)
        lambda.put(id, m);

        // P(i) = n+1;
        pi.put(id, newID);
      }
      else {
        D min = mp.compareTo(m) <= 0 ? mp : m;
        // M(P(i)) = min { M(P(i)), M(i) }
        this.m.put(p, min);
      }
    }
  }

  /**
   * Fourth step: Actualize the clusters if necessary
   * 
   * @param newID the id of the current object
   * @param processedIDs the already processed ids
   */
  private void step4(DBID newID, ModifiableDBIDs processedIDs) {
    // for i = 1..n
    for(DBID id : processedIDs) {
      if(id.equals(newID)) {
        continue;
      }

      D l = lambda.get(id);
      DBID p = pi.get(id);
      D lp = lambda.get(p);

      // if L(i) >= L(P(i))
      if(l.compareTo(lp) >= 0) {
        // P(i) = n+1
        pi.put(id, newID);
      }
    }
  }
}
