package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

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


import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 *
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LOF}.
 *
 * TODO correct handling of datastore events
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @apiviz.uses SpatialIndexTree
 *
 * @param <N> the type of spatial nodes in the spatial index
 * @param <E> the type of spatial entries in the spatial index
 */
@Title("Spatial Approximation Materialize kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database using a spatial approximation.")
@Alias("de.lmu.ifi.dbs.elki.preprocessing.SpatialApproximationMaterializeKNNPreprocessor")
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
  public SpatialApproximationMaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O> distanceFunction, int k) {
    super(relation, distanceFunction, k);
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction);

    Collection<SpatialIndexTree<N, E>> indexes = ResultUtil.filterResults(relation.getHierarchy(), relation, SpatialIndexTree.class);
    if(indexes.size() != 1) {
      throw new AbortException(SpatialApproximationMaterializeKNNPreprocessor.class.getSimpleName() + " found " + indexes.size() + " spatial indexes, expected exactly one.");
    }
    SpatialIndexTree<N, E> index = indexes.iterator().next();

    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNList.class);
    MeanVariance pagesize = new MeanVariance();
    MeanVariance ksize = new MeanVariance();
    if(getLogger().isVerbose()) {
      getLogger().verbose("Approximating nearest neighbor lists to database objects");
    }

    List<E> leaves = index.getLeaves();
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Processing leaf nodes", leaves.size(), getLogger()) : null;
    for(E leaf : leaves) {
      N node = index.getNode(leaf);
      int size = node.getNumEntries();
      pagesize.put(size);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest("NumEntires = " + size);
      }
      // Collect the ids in this node.
      ArrayModifiableDBIDs ids = DBIDUtil.newArray(size);
      for(int i = 0; i < size; i++) {
        ids.add(((LeafEntry) node.getEntry(i)).getDBID());
      }
      TObjectDoubleHashMap<DBIDPair> cache = new TObjectDoubleHashMap<>((size * size * 3) >> 3, Constants.DEFAULT_LOAD_FACTOR, Double.NaN);
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        KNNHeap kNN = DBIDUtil.newHeap(k);
        for(DBIDIter id2 = ids.iter(); id2.valid(); id2.advance()) {
          DBIDPair key = DBIDUtil.newPair(id, id2);
          double d = cache.remove(key);
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
      if(getLogger().isDebugging()) {
        if(cache.size() > 0) {
          getLogger().warning("Cache should be empty after each run, but still has " + cache.size() + " elements.");
        }
      }
      getLogger().incrementProcessed(progress);
    }
    getLogger().ensureCompleted(progress);
    if(getLogger().isVerbose()) {
      getLogger().verbose("Average page size = " + pagesize.getMean() + " +- " + pagesize.getSampleStddev());
      getLogger().verbose("On average, " + ksize.getMean() + " +- " + ksize.getSampleStddev() + " neighbors returned.");
    }
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
   * @apiviz.stereotype factory
   * @apiviz.uses SpatialApproximationMaterializeKNNPreprocessor oneway - -
   *              «create»
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
    public Factory(int k, DistanceFunction<? super NumberVector> distanceFunction) {
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
     *
     * @apiviz.exclude
     */
    public static class Parameterizer<N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<NumberVector> {
      @Override
      protected Factory<N, E> makeInstance() {
        return new Factory<>(k, distanceFunction);
      }
    }
  }
}
