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
package de.lmu.ifi.dbs.elki.index.invertedlist;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ArcCosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import net.jafama.FastMath;

/**
 * Simple index using inverted lists, for cosine distance only.
 * <p>
 * TODO: support additional distances.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - ArcCosineKNNQuery
 * @has - - - ArcCosineRangeQuery
 * @has - - - CosineKNNQuery
 * @has - - - CosineRangeQuery
 *
 * @param <V> Vector type
 */
public class InMemoryInvertedIndex<V extends NumberVector> extends AbstractIndex<V> implements KNNIndex<V>, RangeIndex<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(InMemoryInvertedIndex.class);

  /**
   * Inverted index.
   */
  ArrayList<ModifiableDoubleDBIDList> index;

  /**
   * Length storage.
   */
  WritableDoubleDataStore length;

  /**
   * Constructor.
   * 
   * @param relation Data.
   */
  public InMemoryInvertedIndex(Relation<V> relation) {
    super(relation);
  }

  @Override
  public void initialize() {
    if(index != null) {
      LOG.warning("Index was already initialized!");
    }
    index = new ArrayList<>();
    length = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      V obj = relation.get(iter);
      if(obj instanceof SparseNumberVector) {
        indexSparse(iter, (SparseNumberVector) obj);
      }
      else {
        indexDense(iter, obj);
      }
    }
    // Sort indexes
    long count = 0L;
    for(ModifiableDoubleDBIDList column : index) {
      column.sort();
      count += column.size();
    }
    double sparsity = count / (index.size() * (double) relation.size());
    if(sparsity > .2) {
      LOG.warning("Inverted list indexes only perform well for very sparse data. Your data set has a sparsity of " + sparsity);
    }
  }

  /**
   * Index a single (sparse) instance.
   * 
   * @param ref Object reference
   * @param obj Object to index.
   */
  private void indexSparse(DBIDRef ref, SparseNumberVector obj) {
    double len = 0.;
    for(int iter = obj.iter(); obj.iterValid(iter); iter = obj.iterAdvance(iter)) {
      final int dim = obj.iterDim(iter);
      final double val = obj.iterDoubleValue(iter);
      if(val == 0. || val != val) {
        continue;
      }
      len += val * val;
      getOrCreateColumn(dim).add(val, ref);
    }
    length.put(ref, len);
  }

  /**
   * Index a single (dense) instance.
   * 
   * @param ref Object reference
   * @param obj Object to index.
   */
  private void indexDense(DBIDRef ref, V obj) {
    double len = 0.;
    for(int dim = 0, max = obj.getDimensionality(); dim < max; dim++) {
      final double val = obj.doubleValue(dim);
      if(val == 0. || val != val) {
        continue;
      }
      len += val * val;
      getOrCreateColumn(dim).add(val, ref);
    }
    length.put(ref, FastMath.sqrt(len));
  }

  /**
   * Get (or create) a column.
   * 
   * @param dim Dimension
   * @return Column
   */
  private ModifiableDoubleDBIDList getOrCreateColumn(int dim) {
    while(dim >= index.size()) {
      index.add(DBIDUtil.newDistanceDBIDList());
    }
    return index.get(dim);
  }

  /**
   * Query the most similar objects, sparse version.
   * 
   * @param obj Query object
   * @param scores Score storage
   * @param cands Non-zero objects set
   * @return Result
   */
  private double naiveQuerySparse(SparseNumberVector obj, WritableDoubleDataStore scores, HashSetModifiableDBIDs cands) {
    double len = 0.; // Length of query object, for final normalization
    for(int iter = obj.iter(); obj.iterValid(iter); iter = obj.iterAdvance(iter)) {
      final int dim = obj.iterDim(iter);
      final double val = obj.iterDoubleValue(iter);
      if(val == 0. || val != val) {
        continue;
      }
      len += val * val;
      // No matching documents in index:
      if(dim >= index.size()) {
        continue;
      }
      ModifiableDoubleDBIDList column = index.get(dim);
      for(DoubleDBIDListIter n = column.iter(); n.valid(); n.advance()) {
        scores.increment(n, n.doubleValue() * val);
        cands.add(n);
      }
    }
    return FastMath.sqrt(len);
  }

  /**
   * Query the most similar objects, dense version.
   * 
   * @param obj Query object
   * @param scores Score storage
   * @param cands Non-zero objects set
   * @return Result
   */
  private double naiveQueryDense(NumberVector obj, WritableDoubleDataStore scores, HashSetModifiableDBIDs cands) {
    double len = 0.; // Length of query object, for final normalization
    for(int dim = 0, max = obj.getDimensionality(); dim < max; dim++) {
      final double val = obj.doubleValue(dim);
      if(val == 0. || val != val) {
        continue;
      }
      len += val * val;
      // No matching documents in index:
      if(dim >= index.size()) {
        continue;
      }
      ModifiableDoubleDBIDList column = index.get(dim);
      for(DoubleDBIDListIter n = column.iter(); n.valid(); n.advance()) {
        scores.increment(n, n.doubleValue() * val);
        cands.add(n);
      }
    }
    return FastMath.sqrt(len);
  }

  /**
   * Query the most similar objects, abstract version.
   * 
   * @param obj Query object
   * @param scores Score storage (must be initialized with zeros!)
   * @param cands Non-zero objects set (must be empty)
   * @return Result
   */
  private double naiveQuery(V obj, WritableDoubleDataStore scores, HashSetModifiableDBIDs cands) {
    if(obj instanceof SparseNumberVector) {
      return naiveQuerySparse((SparseNumberVector) obj, scores, cands);
    }
    else {
      return naiveQueryDense(obj, scores, cands);
    }
  }

  @Override
  public void logStatistics() {
    long count = 0L;
    for(ModifiableDoubleDBIDList column : index) {
      count += column.size();
    }
    double sparsity = count / (index.size() * (double) relation.size());
    LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".sparsity", sparsity));
  }

  @Override
  public KNNQuery<V> getKNNQuery(DistanceQuery<V> distanceQuery, Object... hints) {
    DistanceFunction<? super V> df = distanceQuery.getDistanceFunction();
    if(df instanceof CosineDistanceFunction) {
      return new CosineKNNQuery(distanceQuery);
    }
    if(df instanceof ArcCosineDistanceFunction) {
      return new ArcCosineKNNQuery(distanceQuery);
    }
    return null;
  }

  @Override
  public RangeQuery<V> getRangeQuery(DistanceQuery<V> distanceQuery, Object... hints) {
    DistanceFunction<? super V> df = distanceQuery.getDistanceFunction();
    if(df instanceof CosineDistanceFunction) {
      return new CosineRangeQuery(distanceQuery);
    }
    if(df instanceof ArcCosineDistanceFunction) {
      return new ArcCosineRangeQuery(distanceQuery);
    }
    return null;
  }

  @Override
  public String getLongName() {
    return "Inverted lists index";
  }

  @Override
  public String getShortName() {
    return "inverted-lists";
  }

  /**
   * kNN query object, for cosine distance.
   * 
   * @author Erich Schubert
   */
  protected class CosineKNNQuery extends AbstractDistanceKNNQuery<V> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     */
    public CosineKNNQuery(DistanceQuery<V> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public KNNList getKNNForObject(V obj, int k) {
      HashSetModifiableDBIDs cands = DBIDUtil.newHashSet();
      WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(cands, //
          DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
      double len = naiveQuery(obj, scores, cands);
      // TODO: delay the division by len!
      KNNHeap heap = DBIDUtil.newHeap(k);
      for(DBIDIter n = cands.iter(); n.valid(); n.advance()) {
        double dist = 1. - scores.doubleValue(n) / (length.doubleValue(n) * len);
        if(heap.getKNNDistance() >= dist) {
          heap.insert(dist, n);
        }
      }

      return heap.toKNNList();
    }
  }

  /**
   * kNN query object, for arc cosine distance.
   * 
   * @author Erich Schubert
   */
  protected class ArcCosineKNNQuery extends AbstractDistanceKNNQuery<V> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     */
    public ArcCosineKNNQuery(DistanceQuery<V> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public KNNList getKNNForObject(V obj, int k) {
      HashSetModifiableDBIDs cands = DBIDUtil.newHashSet();
      WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(cands, //
          DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
      double len = naiveQuery(obj, scores, cands);
      // TODO: delay the division by len and acos!
      KNNHeap heap = DBIDUtil.newHeap(k);
      for(DBIDIter n = cands.iter(); n.valid(); n.advance()) {
        double dist = Math.acos(scores.doubleValue(n) / (length.doubleValue(n) * len));
        if(heap.getKNNDistance() >= dist) {
          heap.insert(dist, n);
        }
      }

      return heap.toKNNList();
    }
  }

  /**
   * kNN query object, for cosine distance.
   * 
   * @author Erich Schubert
   */
  protected class CosineRangeQuery extends AbstractDistanceRangeQuery<V> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     */
    public CosineRangeQuery(DistanceQuery<V> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public void getRangeForObject(V obj, double range, ModifiableDoubleDBIDList result) {
      HashSetModifiableDBIDs cands = DBIDUtil.newHashSet();
      WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(cands, //
          DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
      double len = naiveQuery(obj, scores, cands);
      // dist = 1 - sim/len <-> sim = len * (1-dist)
      double simrange = (1. - range) * len;
      for(DBIDIter n = cands.iter(); n.valid(); n.advance()) {
        double sim = scores.doubleValue(n) / length.doubleValue(n);
        if(sim >= simrange) {
          result.add(1. - sim / len, n);
        }
      }
    }
  }

  /**
   * kNN query object, for cosine distance.
   * 
   * @author Erich Schubert
   */
  protected class ArcCosineRangeQuery extends AbstractDistanceRangeQuery<V> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     */
    public ArcCosineRangeQuery(DistanceQuery<V> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public void getRangeForObject(V obj, double range, ModifiableDoubleDBIDList result) {
      HashSetModifiableDBIDs cands = DBIDUtil.newHashSet();
      WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(cands, //
          DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
      double len = naiveQuery(obj, scores, cands);
      // dist = acos(sim/len) <-> sim = cos(dist)*len
      double simrange = FastMath.cos(range) * len;
      for(DBIDIter n = cands.iter(); n.valid(); n.advance()) {
        double sim = scores.doubleValue(n) / length.doubleValue(n);
        if(sim >= simrange) {
          result.add(Math.acos(sim / len), n);
        }
      }
    }
  }

  /**
   * Index factory
   * 
   * @author Erich Schubert
   * 
   * @has - - - InMemoryInvertedIndex
   * 
   * @param <V> Vector type
   */
  public static class Factory<V extends NumberVector> implements IndexFactory<V> {
    @Override
    public InMemoryInvertedIndex<V> instantiate(Relation<V> relation) {
      return new InMemoryInvertedIndex<>(relation);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
    }

    /**
     * Parameterizer for inverted list index.
     * 
     * @author Erich Schubert
     * 
     * @hidden
     * 
     * @param <V> Vector type
     */
    public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
      @Override
      protected Factory<V> makeInstance() {
        return new Factory<>();
      }
    }
  }
}
