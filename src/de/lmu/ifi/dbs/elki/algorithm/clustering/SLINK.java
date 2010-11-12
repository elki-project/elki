package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.Collections;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.TreeResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
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
public class SLINK<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SLINK.class);
  
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
   * Constructor.
   * 
   * @param distanceFunction Distance function
   */
  public SLINK(DistanceFunction<? super O, D> distanceFunction) {
    super(distanceFunction);
  }

  /**
   * Performs the SLINK algorithm on the given database.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected Result runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = getDistanceFunction().instantiate(database);
    Class<D> distCls = (Class<D>) getDistanceFunction().getDistanceFactory().getClass();
    WritableRecordStore store = DataStoreUtil.makeRecordStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, DBID.class, distCls);
    pi = store.getStorage(0, DBID.class);
    lambda = store.getStorage(1, distCls);
    m = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, distCls);
    try {
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Clustering", database.size(), logger) : null;

      // sort the db objects according to their ids
      // TODO: is this cheap or expensive?
      ArrayModifiableDBIDs ids = DBIDUtil.newArray(database.getIDs());
      Collections.sort(ids);

      ModifiableDBIDs processedIDs = DBIDUtil.newHashSet(ids.size());
      // apply the algorithm
      int cnt = 0;
      for(DBID id : ids) {
        step1(id);
        step2(id, processedIDs, distFunc);
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

    TreeResult result = new TreeResult("SLINK order", "slink-order");
    result.addPrimaryResult(new AnnotationFromDataStore<DBID>("SLINK pi", "slink-order", SLINK_PI, pi));
    result.addPrimaryResult(new AnnotationFromDataStore<Distance<?>>("SLINK lambda", "slink-order", SLINK_LAMBDA, lambda));
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
    lambda.put(newID, getDistanceFunction().getDistanceFactory().infiniteDistance());
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   * 
   * @param newID the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   * @param distFunc Distance function to use
   */
  private void step2(DBID newID, ModifiableDBIDs processedIDs, DistanceQuery<O, D> distFunc) {
    // M(i) = dist(i, n+1)
    for(DBID id : processedIDs) {
      D distance = distFunc.distance(newID, id);
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

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return Clustering Algorithm
   */
  public static <O extends DatabaseObject, D extends Distance<D>> SLINK<O, D> parameterize(Parameterization config) {
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    return new SLINK<O, D>(distanceFunction);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}