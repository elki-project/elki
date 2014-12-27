package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleLongHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleLongMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Linear memory implementation of HDBSCAN* clustering.
 * 
 * By not building a distance matrix, we can reduce memory usage to linear
 * memory only; but at the cost of roughly double the runtime (unless using
 * indexes) as we first need to compute all kNN distances (for core sizes), then
 * recompute distances when building the spanning tree.
 * 
 * @author Erich Schubert
 */
@Title("HDBSCAN: Hierarchical Density-Based Spatial Clustering of Applications with Noise")
@Description("Density-Based Clustering Based on Hierarchical Density Estimates")
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, and J. Sander", //
title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
booktitle = "Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining, PAKDD", //
url = "http://dx.doi.org/10.1007/978-3-642-37456-2_14")
public class HDBSCANLinearMemory<O> extends AbstractHDBSCAN<O, PointerHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(HDBSCANLinearMemory.class);

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param minPts Minimum number of points for density
   */
  public HDBSCANLinearMemory(DistanceFunction<? super O> distanceFunction, int minPts) {
    super(distanceFunction, minPts);
  }

  /**
   * Run the algorithm
   * 
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerHierarchyRepresentationResult run(Database db, Relation<O> relation) {
    final DistanceQuery<O> distQ = db.getDistanceQuery(relation, getDistanceFunction());
    final KNNQuery<O> knnQ = db.getKNNQuery(distQ, minPts + 1);
    // We need array addressing later.
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());

    // 1. Compute the core distances
    // minPts + 1: ignore query point.
    final WritableDoubleDataStore coredists = computeCoreDists(ids, knnQ, minPts + 1);

    final int numedges = ids.size() - 1;
    DoubleLongHeap heap = new DoubleLongMinHeap(numedges);
    // 2. Build spanning tree.
    FiniteProgress mprog = LOG.isVerbose() ? new FiniteProgress("Computing minimum spanning tree (n-1 edges).", numedges, LOG) : null;
    PrimsMinimumSpanningTree.processDense(ids,//
        new HDBSCANAdapter(ids, coredists, distQ), //
        new HeapMSTCollector(heap, mprog, LOG));
    LOG.ensureCompleted(mprog);
    // Storage for pointer representation:
    WritableDBIDDataStore pi = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore lambda = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    convertToPointerRepresentation(ids, heap, pi, lambda);

    return new PointerHierarchyRepresentationResult(ids, pi, lambda);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractHDBSCAN.Parameterizer<O> {
    @Override
    protected HDBSCANLinearMemory<O> makeInstance() {
      return new HDBSCANLinearMemory<>(distanceFunction, minPts);
    }
  }
}
