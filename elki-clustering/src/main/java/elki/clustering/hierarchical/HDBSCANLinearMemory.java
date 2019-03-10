/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.hierarchical;

import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDBIDDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDUtil;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.geometry.PrimsMinimumSpanningTree;
import elki.utilities.datastructures.heap.DoubleLongHeap;
import elki.utilities.datastructures.heap.DoubleLongMinHeap;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;

/**
 * Linear memory implementation of HDBSCAN clustering.
 * <p>
 * By not building a distance matrix, we can reduce memory usage to linear
 * memory only; but at the cost of roughly double the runtime (unless using
 * indexes) as we first need to compute all kNN distances (for core sizes), then
 * recompute distances when building the spanning tree.
 * <p>
 * This implementation follows the HDBSCAN publication more closely than
 * {@link SLINKHDBSCANLinearMemory}, by computing the minimum spanning tree
 * using Prim's algorithm (instead of SLINK; although the two are remarkably
 * similar). In order to produce the preferred internal format of hierarchical
 * clusterings (the compact pointer representation introduced in {@link SLINK})
 * we have to perform a postprocessing conversion.
 * <p>
 * This implementation does <em>not</em> include the cluster extraction
 * discussed as Step 4, which is provided in a separate step. For this reason,
 * we also do <em>not include self-edges</em>.
 * <p>
 * Reference:
 * <p>
 * R. J. G. B. Campello, D. Moulavi, J. Sander<br>
 * Density-Based Clustering Based on Hierarchical Density Estimates<br>
 * Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - PointerDensityHierarchyRepresentationResult
 *
 * @param <O> Object type
 */
@Title("HDBSCAN: Hierarchical Density-Based Spatial Clustering of Applications with Noise")
@Description("Density-Based Clustering Based on Hierarchical Density Estimates")
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, J. Sander", //
    title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
    booktitle = "Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)", //
    url = "https://doi.org/10.1007/978-3-642-37456-2_14", //
    bibkey = "DBLP:conf/pakdd/CampelloMS13")
public class HDBSCANLinearMemory<O> extends AbstractHDBSCAN<O, PointerDensityHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
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
  public HDBSCANLinearMemory(Distance<? super O> distanceFunction, int minPts) {
    super(distanceFunction, minPts);
  }

  /**
   * Run the algorithm
   *
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerDensityHierarchyRepresentationResult run(Database db, Relation<O> relation) {
    final DistanceQuery<O> distQ = db.getDistanceQuery(relation, getDistance());
    final KNNQuery<O> knnQ = db.getKNNQuery(distQ, minPts);
    // We need array addressing later.
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());

    // 1. Compute the core distances
    // minPts + 1: ignore query point.
    final WritableDoubleDataStore coredists = computeCoreDists(ids, knnQ, minPts);

    final int numedges = ids.size() - 1;
    DoubleLongHeap heap = new DoubleLongMinHeap(numedges);
    // 2. Build spanning tree.
    FiniteProgress mprog = LOG.isVerbose() ? new FiniteProgress("Computing minimum spanning tree (n-1 edges)", numedges, LOG) : null;
    PrimsMinimumSpanningTree.processDense(ids, //
        new HDBSCANAdapter(ids, coredists, distQ), //
        new HeapMSTCollector(heap, mprog, LOG));
    LOG.ensureCompleted(mprog);
    // Storage for pointer representation:
    WritableDBIDDataStore pi = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore lambda = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    convertToPointerRepresentation(ids, heap, pi, lambda);

    return new PointerDensityHierarchyRepresentationResult(ids, pi, lambda, distQ.getDistance().isSquared(), coredists);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
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
   * @hidden
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
