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
package elki.index.distancematrix;

import java.lang.ref.WeakReference;

import elki.data.type.TypeInformation;
import elki.database.ids.*;
import elki.database.query.distance.DatabaseDistanceQuery;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.DistanceIndex;
import elki.index.IndexFactory;
import elki.index.KNNIndex;
import elki.index.RangeIndex;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.LongStatistic;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Distance matrix, for precomputing similarity for a small data set.
 * <p>
 * This class uses a linear memory layout (not a ragged array), and assumes
 * symmetry as well as strictness. This way, it only stores the upper triangle
 * matrix with double precision. It has to store (n-1) * (n-2) distance values
 * in memory, requiring 8 * (n-1) * (n-2) bytes. Since Java has a size limit of
 * arrays of 31 bits (signed integer), we can store at most \(2^16\) objects
 * (precisely, 65536 objects) in a single array, which needs about 16 GB of RAM.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - PrecomputedDistanceQuery
 * @has - - - PrecomputedKNNQuery
 * @has - - - PrecomputedRangeQuery
 *
 * @param <O> Object type
 */
public class PrecomputedDistanceMatrix<O> implements DistanceIndex<O>, RangeIndex<O>, KNNIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(PrecomputedDistanceMatrix.class);

  /**
   * Data relation.
   */
  protected final WeakReference<Relation<O>> refrelation;

  /**
   * Nested distance function.
   */
  protected final Distance<? super O> distanceFunction;

  /**
   * Distance matrix.
   */
  private double[] matrix = null;

  /**
   * DBID range.
   */
  private DBIDRange ids;

  /**
   * Size of DBID range.
   */
  private int size;

  /**
   * Constructor.
   *
   * @param relation Data relation
   * @param range DBID range
   * @param distanceFunction Distance function
   */
  public PrecomputedDistanceMatrix(Relation<O> relation, DBIDRange range, Distance<? super O> distanceFunction) {
    super();
    this.refrelation = new WeakReference<>(relation);
    this.ids = range;
    this.distanceFunction = distanceFunction;

    if(!distanceFunction.isSymmetric()) {
      throw new AbortException("Distance matrixes currently only support symmetric distance functions (Patches welcome).");
    }
  }

  @Override
  public void initialize() {
    size = ids.size();
    if(size > 65536) {
      throw new AbortException("Distance matrixes currently have a limit of 65536 objects (~16 GB). After this, the array size exceeds the Java integer range, and a different data structure needs to be used.");
    }
    DistanceQuery<O> distanceQuery = distanceFunction.instantiate(refrelation.get());

    final int msize = triangleSize(size);
    matrix = new double[msize];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Precomputing distance matrix", msize, LOG) : null;
    int pos = 0;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      // y < x -- must match {@link #getOffset}!
      for(iy.seek(0); iy.getOffset() < ix.getOffset(); iy.advance()) {
        matrix[pos] = distanceQuery.distance(ix, iy);
        pos++;
      }
      if(prog != null) {
        prog.setProcessed(prog.getProcessed() + ix.getOffset(), LOG);
      }
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Compute the size of a complete x by x triangle (minus diagonal)
   *
   * @param x Offset
   * @return Size of complete triangle
   */
  protected static int triangleSize(int x) {
    return (x * (x - 1)) >>> 1;
  }

  /**
   * Array offset computation.
   *
   * @param x X parameter
   * @param y Y parameter
   * @return Array offset
   */
  private int getOffset(int x, int y) {
    return (y < x) ? (triangleSize(x) + y) : (triangleSize(y) + x);
  }

  @Override
  public void logStatistics() {
    if(matrix != null) {
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".matrix-size", matrix.length));
    }
  }

  @Override
  public String getLongName() {
    return "Precomputed Distance Matrix";
  }

  @Override
  public String getShortName() {
    return "distance-matrix";
  }

  @Override
  public DistanceQuery<O> getDistanceQuery(Distance<? super O> distanceFunction, Object... hints) {
    if(this.distanceFunction.equals(distanceFunction)) {
      return new PrecomputedDistanceQuery();
    }
    return null;
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(this.distanceFunction.equals(distanceQuery.getDistance())) {
      return new PrecomputedKNNQuery();
    }
    return null;
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(this.distanceFunction.equals(distanceQuery.getDistance())) {
      return new PrecomputedRangeQuery();
    }
    return null;
  }

  /**
   * Distance query using the precomputed matrix.
   *
   * @author Erich Schubert
   */
  private class PrecomputedDistanceQuery implements DatabaseDistanceQuery<O> {
    @Override
    public double distance(DBIDRef id1, DBIDRef id2) {
      final int x = ids.getOffset(id1), y = ids.getOffset(id2);
      return (x != y) ? matrix[getOffset(x, y)] : 0.;
    }

    @Override
    public Distance<? super O> getDistance() {
      return distanceFunction;
    }

    @Override
    public Relation<? extends O> getRelation() {
      return refrelation.get();
    }
  }

  /**
   * Range query using the distance matrix.
   *
   * @author Erich Schubert
   */
  private class PrecomputedRangeQuery implements RangeQuery<O> {
    @Override
    public ModifiableDoubleDBIDList getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
      throw new AbortException("Preprocessor KNN query only supports ID queries.");
    }

    @Override
    public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
      result.add(0., id);
      DBIDArrayIter it = ids.iter();

      final int x = ids.getOffset(id);
      // Case y < x: triangleSize(x) + y
      int pos = triangleSize(x);
      for(int y = 0; y < x; y++) {
        final double dist = matrix[pos];
        if(dist <= range) {
          result.add(dist, it.seek(y));
        }
        pos++;
      }
      assert (pos == triangleSize(x + 1));
      // Case y > x: triangleSize(y) + x
      pos = triangleSize(x + 1) + x;
      for(int y = x + 1; y < size; y++) {
        final double dist = matrix[pos];
        if(dist <= range) {
          result.add(dist, it.seek(y));
        }
        pos += y;
      }
      return result;
    }
  }

  /**
   * kNN query using the distance matrix.
   *
   * @author Erich Schubert
   */
  private class PrecomputedKNNQuery implements KNNQuery<O> {
    @Override
    public KNNList getKNNForDBID(DBIDRef id, int k) {
      KNNHeap heap = DBIDUtil.newHeap(k);
      heap.insert(0., id);
      DBIDArrayIter it = ids.iter();
      double max = Double.POSITIVE_INFINITY;
      final int x = ids.getOffset(id);
      // Case y < x: triangleSize(x) + y
      int pos = triangleSize(x);
      for(int y = 0; y < x; y++) {
        final double dist = matrix[pos];
        if(dist <= max) {
          max = heap.insert(dist, it.seek(y));
        }
        pos++;
      }
      assert (pos == triangleSize(x + 1));
      // Case y > x: triangleSize(y) + x
      pos = triangleSize(x + 1) + x;
      for(int y = x + 1; y < size; y++) {
        final double dist = matrix[pos];
        if(dist <= max) {
          max = heap.insert(dist, it.seek(y));
        }
        pos += y;
      }
      return heap.toKNNList();
    }

    @Override
    public KNNList getKNNForObject(O obj, int k) {
      throw new AbortException("Preprocessor KNN query only supports ID queries.");
    }
  }

  /**
   * Factory for the index.
   *
   * @author Erich Schubert
   *
   * @has - - - PrecomputedDistanceMatrix
   *
   * @param <O> Object type
   */
  public static class Factory<O> implements IndexFactory<O> {
    /**
     * Nested distance function.
     */
    final protected Distance<? super O> distanceFunction;

    /**
     * Constructor.
     *
     * @param distanceFunction Distance function
     */
    public Factory(Distance<? super O> distanceFunction) {
      super();
      this.distanceFunction = distanceFunction;
    }

    @Override
    public PrecomputedDistanceMatrix<O> instantiate(Relation<O> relation) {
      DBIDs rids = relation.getDBIDs();
      if(!(rids instanceof DBIDRange)) {
        throw new AbortException("Distance matrixes are currently only supported for DBID ranges (as used by static databases; not on modifiable databases) for performance reasons (Patches welcome).");
      }
      return new PrecomputedDistanceMatrix<>(relation, (DBIDRange) rids, distanceFunction);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distanceFunction.getInputTypeRestriction();
    }

    /**
     * Par.
     *
     * @author Erich Schubert
     *
     * @hidden
     *
     * @param <O> Object type
     */
    public static class Par<O> implements Parameterizer {
      /**
       * Option parameter for the precomputed distance matrix.
       */
      public static final OptionID DISTANCE_ID = new OptionID("matrix.distance", "Distance function for the precomputed distance matrix.");

      /**
       * Nested distance function.
       */
      protected Distance<? super O> distanceFunction;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<Distance<? super O>>(DISTANCE_ID, Distance.class) //
            .grab(config, x -> distanceFunction = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(distanceFunction);
      }
    }
  }
}
