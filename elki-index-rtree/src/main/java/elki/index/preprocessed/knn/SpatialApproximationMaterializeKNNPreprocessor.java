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
package elki.index.preprocessed.knn;

import java.util.List;

import elki.data.NumberVector;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.tree.LeafEntry;
import elki.index.tree.spatial.SpatialEntry;
import elki.index.tree.spatial.SpatialIndexTree;
import elki.index.tree.spatial.SpatialNode;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MeanVariance;
import elki.result.Metadata;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 *
 * Used for example by {@link elki.outlier.lof.LOF}.
 *
 * TODO correct handling of datastore events
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @assoc - - - SpatialIndexTree
 *
 * @param <N> the type of spatial nodes in the spatial index
 * @param <E> the type of spatial entries in the spatial index
 */
@Title("Spatial Approximation Materialize kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database using a spatial approximation.")
public class SpatialApproximationMaterializeKNNPreprocessor<O extends NumberVector, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractMaterializeKNNPreprocessor<O> {
  /**
   * Logger to use
   */
  private static final Logging LOG = Logging.getLogger(SpatialApproximationMaterializeKNNPreprocessor.class);

  /**
   * Constructor
   *
   * @param relation Relation to preprocess
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  public SpatialApproximationMaterializeKNNPreprocessor(Relation<O> relation, Distance<? super O> distanceFunction, int k) {
    super(relation, distanceFunction, k);
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction);
    SpatialIndexTree<N, E> index = getSpatialIndex(relation);

    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNList.class);
    MeanVariance pagesize = new MeanVariance();
    MeanVariance ksize = new MeanVariance();
    final Logging log = getLogger();
    if(log.isVerbose()) {
      log.verbose("Approximating nearest neighbor lists to database objects");
    }

    List<E> leaves = index.getLeaves();
    FiniteProgress progress = log.isVerbose() ? new FiniteProgress("Processing leaf nodes", leaves.size(), log) : null;
    for(E leaf : leaves) {
      N node = index.getNode(leaf);
      int size = node.getNumEntries();
      pagesize.put(size);
      if(log.isDebuggingFinest()) {
        log.debugFinest("NumEntires = " + size);
      }
      // Collect the ids in this node.
      ArrayModifiableDBIDs ids = DBIDUtil.newArray(size);
      for(int i = 0; i < size; i++) {
        ids.add(((LeafEntry) node.getEntry(i)).getDBID());
      }
      Object2DoubleOpenHashMap<DBIDPair> cache = new Object2DoubleOpenHashMap<>((size * size * 3) >> 3);
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        KNNHeap kNN = DBIDUtil.newHeap(k);
        for(DBIDIter id2 = ids.iter(); id2.valid(); id2.advance()) {
          DBIDPair key = DBIDUtil.newPair(id, id2);
          double d = cache.removeDouble(key);
          if(d == d) { // Not NaN
            // consume the previous result.
            kNN.insert(d, id2);
          }
          else {
            // compute new and store the previous result.
            d = distanceQuery.distance(id, id2);
            kNN.insert(d, id2);
            // put it into the cache, but with the keys reversed
            key = DBIDUtil.newPair(id2, id);
            cache.put(key, d);
          }
        }
        ksize.put(kNN.size());
        storage.put(id, kNN.toKNNList());
      }
      if(log.isDebugging() && cache.size() > 0) {
        log.warning("Cache should be empty after each run, but still has " + cache.size() + " elements.");
      }
      log.incrementProcessed(progress);
    }
    log.ensureCompleted(progress);
    if(log.isVerbose()) {
      log.verbose("Average page size = " + pagesize.getMean() + " +- " + pagesize.getSampleStddev());
      log.verbose("On average, " + ksize.getMean() + " +- " + ksize.getSampleStddev() + " neighbors returned.");
    }
  }

  protected SpatialIndexTree<N, E> getSpatialIndex(Relation<O> relation) {
    SpatialIndexTree<N, E> ret = null;
    for(It<SpatialIndexTree<N, E>> iter = Metadata.hierarchyOf(relation).iterDescendants().filter(SpatialIndexTree.class); iter.valid(); iter.advance()) {
      if(ret != null) {
        throw new IllegalStateException("More than one spatial index found - this is not supported!");
      }
      // FIXME: check we got the right the representation
      ret = (SpatialIndexTree<N, E>) iter.get();
    }
    if(ret == null) {
      throw new IllegalStateException("No spatial index found!");
    }
    return ret;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public String getLongName() {
    return "Spatial Index Approximative kNN";
  }

  @Override
  public String getShortName() {
    return "spatial-approximate-knn";
  }

  @Override
  public void logStatistics() {
    // No statistics to log.
  }

  /**
   * The actual preprocessor instance.
   *
   * @author Erich Schubert
   *
   * @stereotype factory
   * @navassoc - creates - SpatialApproximationMaterializeKNNPreprocessor
   *
   * @param <N> the type of spatial nodes in the spatial index
   * @param <E> the type of spatial entries in the spatial index
   */
  public static class Factory<N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractMaterializeKNNPreprocessor.Factory<NumberVector> {
    /**
     * Constructor.
     *
     * @param k k
     * @param distanceFunction distance function
     */
    public Factory(int k, Distance<? super NumberVector> distanceFunction) {
      super(k, distanceFunction);
    }

    @Override
    public SpatialApproximationMaterializeKNNPreprocessor<NumberVector, N, E> instantiate(Relation<NumberVector> relation) {
      SpatialApproximationMaterializeKNNPreprocessor<NumberVector, N, E> instance = new SpatialApproximationMaterializeKNNPreprocessor<>(relation, distanceFunction, k);
      return instance;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractMaterializeKNNPreprocessor.Factory.Par<NumberVector> {
      @Override
      public Factory<N, E> make() {
        return new Factory<>(k, distanceFunction);
      }
    }
  }
}
