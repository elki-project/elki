package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.ExtractFlatClusteringFromHierarchy;
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
 * Linear memory implementation of HDBSCAN clustering.
 *
 * By not building a distance matrix, we can reduce memory usage to linear
 * memory only; but at the cost of roughly double the runtime (unless using
 * indexes) as we first need to compute all kNN distances (for core sizes), then
 * recompute distances when building the spanning tree.
 *
 * This implementation follows the HDBSCAN publication more closely than
 * {@link SLINKHDBSCANLinearMemory}, by computing the minimum spanning tree
 * using Prim's algorithm (instead of SLINK; although the two are remarkably
 * similar). In order to produce the preferred internal format of hierarchical
 * clusterings (the compact pointer representation introduced in {@link SLINK})
 * we have to perform a postprocessing conversion.
 *
 * This implementation does <em>not</em> include the cluster extraction
 * discussed as Step 4. This functionality should however already be provided by
 * {@link ExtractFlatClusteringFromHierarchy}. For this reason, we also do
 * <em>not include self-edges</em>.
 *
 * Reference:
 * <p>
 * R. J. G. B. Campello, D. Moulavi, and J. Sander<br />
 * Density-Based Clustering Based on Hierarchical Density Estimates<br />
 * Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining,
 * PAKDD
 * </p>
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 *
 * @apiviz.has PointerDensityHierarchyRepresentationResult
 */
@Title("HDBSCAN: Hierarchical Density-Based Spatial Clustering of Applications with Noise")
@Description("Density-Based Clustering Based on Hierarchical Density Estimates")
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, and J. Sander", //
title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
booktitle = "Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining, PAKDD", //
url = "http://dx.doi.org/10.1007/978-3-642-37456-2_14")
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
  public PointerDensityHierarchyRepresentationResult run(Database db, Relation<O> relation) {
    final DistanceQuery<O> distQ = db.getDistanceQuery(relation, getDistanceFunction());
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
    PrimsMinimumSpanningTree.processDense(ids,//
        new HDBSCANAdapter(ids, coredists, distQ), //
        new HeapMSTCollector(heap, mprog, LOG));
    LOG.ensureCompleted(mprog);
    // Storage for pointer representation:
    WritableDBIDDataStore pi = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore lambda = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    convertToPointerRepresentation(ids, heap, pi, lambda);

    return new PointerDensityHierarchyRepresentationResult(ids, pi, lambda, coredists);
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
