package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
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
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HierarchyHashmapList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.ModifiableHierarchy;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

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
   * The minimum number of clusters to extract
   */
  public static final OptionID SLINK_MINCLUSTERS_ID = OptionID.getOrCreateOptionID("slink.minclusters", "The maximum number of clusters to extract.");

  /**
   * The values of the function Pi of the pointer representation.
   */
  private WritableDataStore<DBID> pi;

  /**
   * The values of the function Lambda of the pointer representation.
   */
  WritableDataStore<D> lambda;

  /**
   * The values of the helper function m to determine the pointer
   * representation.
   */
  private WritableDataStore<D> m;

  /**
   * Minimum number of clusters to extract
   */
  private Integer minclusters;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param maxclusters Maximum clusters to extract. Can be null
   */
  public SLINK(DistanceFunction<? super O, D> distanceFunction, Integer maxclusters) {
    super(distanceFunction);
    this.minclusters = maxclusters;
  }

  /**
   * Performs the SLINK algorithm on the given database.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected Result runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(getDistanceFunction());
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
      for(DBID id : ids) {
        step1(id);
        step2(id, processedIDs, distFunc);
        step3(id, processedIDs);
        step4(id, processedIDs);

        processedIDs.add(id);

        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
      if(progress != null) {
        progress.ensureCompleted(logger);
      }
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }

    // Build clusters identified by their target object
    BasicResult result = null;
    if(minclusters != null) {
      result = extractClusters(database, pi, lambda, minclusters);
    }
    // Fallback
    if(result == null) {
      result = new BasicResult("SLINK Clustering", "slink");
    }
    result.addChildResult(new AnnotationFromDataStore<DBID>("SLINK pi", "slink-order", SLINK_PI, pi));
    result.addChildResult(new AnnotationFromDataStore<Distance<?>>("SLINK lambda", "slink-order", SLINK_LAMBDA, lambda));
    result.addChildResult(new OrderingFromDataStore<D>("SLINK order", "slink-order", lambda));
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
   * Extract all clusters from the pi-lambda-representation.
   * 
   * @param database Database
   * @param pi Pi store
   * @param lambda Lambda store
   * @param minclusters Minimum number of clusters to extract
   * 
   * @return Hierarchical clustering
   */
  private Clustering<Model> extractClusters(Database<O> database, final DataStore<DBID> pi, final DataStore<D> lambda, int minclusters) {
    // extract a hierarchical clustering
    ArrayModifiableDBIDs order = DBIDUtil.newArray(database.getIDs());
    // sort by lambda
    Collections.sort(order, new CompareByLambda<D>(lambda));
    D curdist = null;

    D stopdist = null;
    if(minclusters < database.size()) {
      stopdist = lambda.get(order.get(database.size() - minclusters));
    }

    ModifiableHierarchy<Cluster<Model>> hier = new HierarchyHashmapList<Cluster<Model>>();
    Map<DBID, Cluster<Model>> clusters = new HashMap<DBID, Cluster<Model>>();
    Map<DBID, ModifiableDBIDs> cids = new HashMap<DBID, ModifiableDBIDs>();

    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Extracting clusters", database.size(), logger) : null;

    for(DBID cur : order) {
      DBID dest = pi.get(cur);
      D l = lambda.get(cur);
      // logger.debugFine("DBID " + cur.toString() + " dist: " + l.toString());
      if(stopdist != null && stopdist.compareTo(l) > 0) {
        ModifiableDBIDs curset = cids.remove(cur);
        ModifiableDBIDs destset = cids.get(dest);
        if(destset == null) {
          if(curset != null) {
            destset = curset;
          }
          else {
            destset = DBIDUtil.newHashSet();
            destset.add(cur);
          }
          destset.add(dest);
          cids.put(dest, destset);
        }
        else {
          if(curset != null) {
            destset.addDBIDs(curset);
          }
          else {
            destset.add(cur);
          }
        }
        curdist = l;
      }
      else {
        if(curdist == null || l.compareTo(curdist) > 0) {
          // New distance level reached. Post-process the current objects
          for(Entry<DBID, ModifiableDBIDs> ent : cids.entrySet()) {
            DBID key = ent.getKey();
            ModifiableDBIDs clusids = ent.getValue();
            // Make a new cluster
            String cname = "Cluster_" + key.toString() + "_" + curdist.toString();
            Cluster<Model> cluster = new Cluster<Model>(cname, clusids, ClusterModel.CLUSTER, hier);
            // Collect child clusters and clean up the cluster ids, keeping only
            // "new" objects.
            Iterator<DBID> iter = clusids.iterator();
            while(iter.hasNext()) {
              DBID child = iter.next();
              Cluster<Model> chiclus = clusters.get(child);
              if(chiclus != null) {
                hier.add(cluster, chiclus);
                clusters.remove(child);
                iter.remove();
              }
            }
            clusters.put(key, cluster);
          }
          if(logger.isDebuggingFine()) {
            StringBuffer buf = new StringBuffer();
            buf.append("Number of clusters at depth ");
            buf.append((curdist != null ? curdist.toString() : "null"));
            buf.append(": ").append(clusters.size()).append(" ");
            buf.append("last-objects:");
            for(DBID id : clusters.keySet()) {
              buf.append(" ").append(id.toString());
            }
            logger.debugFine(buf.toString());
          }
          cids.clear();
          curdist = l;
        }
        // Add the current object to the destinations cluster
        {
          ModifiableDBIDs destset = cids.get(dest);
          if(destset == null) {
            destset = DBIDUtil.newHashSet();
            cids.put(dest, destset);
            destset.add(dest);
          }
          destset.add(cur);
        }
      }
      // Decrement counter
      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }
    // There should be one cluster remaining at infinite distance...
    if(clusters.size() != 1) {
      logger.warning("Single-link is expected to have a single cluster at the top level!");
      return null;
    }
    final Clustering<Model> clustering = new Clustering<Model>("Single-Link-Clustering", "slink-clustering");
    // FIXME: validate this is works correctly for a single-object dataset!
    for(Cluster<Model> cluster : clusters.values()) {
      clustering.addCluster(cluster);
    }
    return clustering;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return Clustering Algorithm
   */
  public static <O extends DatabaseObject, D extends Distance<D>> SLINK<O, D> parameterize(Parameterization config) {
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    final IntParameter minclustersparam = new IntParameter(SLINK_MINCLUSTERS_ID, new GreaterEqualConstraint(1), true);
    Integer maxclusters = null;
    if(config.grab(minclustersparam)) {
      maxclusters = minclustersparam.getValue();
    }
    if(config.hasErrors()) {
      return null;
    }
    return new SLINK<O, D>(distanceFunction, maxclusters);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Order a DBID collection by the lambda value.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <D> Distance type
   */
  private static final class CompareByLambda<D extends Distance<D>> implements Comparator<DBID> {
    /**
     * Lambda storage
     */
    private final DataStore<D> lambda;

    /**
     * Constructor.
     * 
     * @param lambda Lambda storage
     */
    protected CompareByLambda(DataStore<D> lambda) {
      this.lambda = lambda;
    }

    @Override
    public int compare(DBID id1, DBID id2) {
      D k1 = lambda.get(id1);
      D k2 = lambda.get(id2);
      assert (k1 != null);
      assert (k2 != null);
      return k1.compareTo(k2);
    }
  }
}