package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNList;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Joins in a given spatial database to each object its k-nearest neighbors.
 * This algorithm only supports spatial databases based on a spatial index
 * structure.
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <N> the type of node used in the spatial index structure
 * @param <E> the type of entry used in the spatial node
 */
@Title("K-Nearest Neighbor Join")
@Description("Algorithm to find the k-nearest neighbors of each object in a spatial database")
public class KNNJoin<V extends NumberVector<V, ?>, D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractDistanceBasedAlgorithm<V, D, DataStore<KNNList<D>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KNNJoin.class);

  /**
   * Parameter that specifies the k-nearest neighbors to be assigned, must be an
   * integer greater than 0. Default value: 1.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knnjoin.k", "Specifies the k-nearest neighbors to be assigned.");

  /**
   * Association ID for KNNLists.
   */
  public static final AssociationID<KNNList<?>> KNNLIST = AssociationID.getOrCreateAssociationIDGenerics("KNNS", KNNList.class);

  /**
   * The k parameter
   */
  int k;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param k k parameter
   */
  public KNNJoin(DistanceFunction<? super V, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Joins in the given spatial database to each object its k-nearest neighbors.
   * 
   * @throws IllegalStateException if the specified database is not an instance
   *         of {@link SpatialIndexDatabase} or the specified distance function
   *         is not an instance of {@link SpatialPrimitiveDistanceFunction}.
   */
  @Override
  @SuppressWarnings("unchecked")
  protected DataStore<KNNList<D>> runInTime(Database<V> database) throws IllegalStateException {
    if(!(getDistanceFunction() instanceof SpatialPrimitiveDistanceFunction)) {
      throw new IllegalStateException("Distance Function must be an instance of " + SpatialPrimitiveDistanceFunction.class.getName());
    }
    Collection<SpatialIndex<V, N, E>> indexes = ResultUtil.filterResults(database, SpatialIndex.class);
    if(indexes.size() != 1) {
      throw new AbortException("KNNJoin found " + indexes.size() + " spatial indexes, expected exactly one.");
    }
    SpatialIndex<V, N, E> index = indexes.iterator().next();
    SpatialPrimitiveDistanceFunction<V, D> distFunction = (SpatialPrimitiveDistanceFunction<V, D>) getDistanceFunction();
    DistanceQuery<V, D> distq = getDistanceFunction().instantiate(database);

    DBIDs ids = database.getIDs();

    WritableDataStore<KNNHeap<D>> knnHeaps = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, KNNHeap.class);

    try {
      // data pages of s
      List<E> ps_candidates = index.getLeaves();
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress(this.getClass().getName(), database.size(), logger) : null;
      IndefiniteProgress pageprog = logger.isVerbose() ? new IndefiniteProgress("Number of processed data pages", logger) : null;
      if(logger.isDebugging()) {
        logger.debugFine("# ps = " + ps_candidates.size());
      }
      // data pages of r
      List<E> pr_candidates = new ArrayList<E>(ps_candidates);
      if(logger.isDebugging()) {
        logger.debugFine("# pr = " + pr_candidates.size());
      }
      int processed = 0;
      int processedPages = 0;
      boolean up = true;
      for(E pr_entry : pr_candidates) {
        HyperBoundingBox pr_mbr = pr_entry.getMBR();
        N pr = index.getNode(pr_entry);
        D pr_knn_distance = distq.infiniteDistance();
        if(logger.isDebugging()) {
          logger.debugFine(" ------ PR = " + pr);
        }
        // create for each data object a knn list
        for(int j = 0; j < pr.getNumEntries(); j++) {
          knnHeaps.put(((LeafEntry) pr.getEntry(j)).getDBID(), new KNNHeap<D>(k, distq.infiniteDistance()));
        }

        if(up) {
          for(E ps_entry : ps_candidates) {
            HyperBoundingBox ps_mbr = ps_entry.getMBR();
            D distance = distFunction.distance(pr_mbr, ps_mbr);

            if(distance.compareTo(pr_knn_distance) <= 0) {
              N ps = index.getNode(ps_entry);
              pr_knn_distance = processDataPages(distq, pr, ps, knnHeaps, pr_knn_distance);
            }
          }
          up = false;
        }

        else {
          for(int s = ps_candidates.size() - 1; s >= 0; s--) {
            E ps_entry = ps_candidates.get(s);
            HyperBoundingBox ps_mbr = ps_entry.getMBR();
            D distance = distFunction.distance(pr_mbr, ps_mbr);

            if(distance.compareTo(pr_knn_distance) <= 0) {
              N ps = index.getNode(ps_entry);
              pr_knn_distance = processDataPages(distq, pr, ps, knnHeaps, pr_knn_distance);
            }
          }
          up = true;
        }

        processed += pr.getNumEntries();

        if(progress != null && pageprog != null) {
          progress.setProcessed(processed, logger);
          pageprog.setProcessed(processedPages++, logger);
        }
      }
      if(pageprog != null) {
        pageprog.setCompleted(logger);
      }
      WritableDataStore<KNNList<D>> knnLists = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_STATIC, KNNList.class);
      for(DBID id : ids) {
        knnLists.put(id, knnHeaps.get(id).toKNNList());
      }
      return knnLists;
    }

    catch(Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Processes the two data pages pr and ps and determines the k-nearest
   * neighbors of pr in ps.
   * 
   * @param distQ the distance to use
   * @param pr the first data page
   * @param ps the second data page
   * @param knnLists the knn lists for each data object
   * @param pr_knn_distance the current knn distance of data page pr
   * @return the k-nearest neighbor distance of pr in ps
   */
  private D processDataPages(DistanceQuery<V, D> distQ, N pr, N ps, WritableDataStore<KNNHeap<D>> knnLists, D pr_knn_distance) {
    // noinspection unchecked
    boolean infinite = pr_knn_distance.isInfiniteDistance();
    for(int i = 0; i < pr.getNumEntries(); i++) {
      DBID r_id = ((LeafEntry) pr.getEntry(i)).getDBID();
      KNNHeap<D> knnList = knnLists.get(r_id);

      for(int j = 0; j < ps.getNumEntries(); j++) {
        DBID s_id = ((LeafEntry) ps.getEntry(j)).getDBID();

        D distance = distQ.distance(r_id, s_id);
        if(knnList.add(new DistanceResultPair<D>(distance, s_id))) {
          // set kNN distance of r
          if(infinite) {
            pr_knn_distance = knnList.getMaximumDistance();
          }
          pr_knn_distance = DistanceUtil.max(knnList.getMaximumDistance(), pr_knn_distance);
        }
      }
    }
    return pr_knn_distance;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <V extends NumberVector<V, ?>, D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> KNNJoin<V, D, N, E> parameterize(Parameterization config) {
    int k = getParameterK(config);
    DistanceFunction<V, D> distanceFunction = getParameterDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    return new KNNJoin<V, D, N, E>(distanceFunction, k);
  }

  /**
   * Get the k parameter for the knn query
   * 
   * @param config Parameterization
   * @return k parameter
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID, new GreaterConstraint(0), 1);
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }
}