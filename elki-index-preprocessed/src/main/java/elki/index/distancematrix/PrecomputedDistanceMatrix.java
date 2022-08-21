/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
import elki.database.query.PrioritySearcher;
import elki.database.query.distance.DatabaseDistanceQuery;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.*;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Distance matrix, for precomputing similarity for a small data set.
 * <p>
 * This class uses a linear memory layout (not a ragged array), and assumes
 * symmetry as well as strictness. This way, it only stores the upper triangle
 * matrix with double precision. It has to store (n-1) * (n-2) distance values
 * in memory, requiring 8 * (n-1) * (n-2) bytes. Since Java has a size limit of
 * arrays of 31 bits (signed integer), we can store at most \(2^{16}\) objects
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
public class PrecomputedDistanceMatrix<O> implements DistanceIndex<O>, RangeIndex<O>, KNNIndex<O>, DistancePriorityIndex<O> {
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
  protected final Distance<? super O> distance;

  /**
   * Distance matrix.
   */
  private double[] matrix = null;

  /**
   * DBID range.
   */
  private DBIDRange ids;

  /**
   * Constructor.
   *
   * @param relation Data relation
   * @param range DBID range
   * @param distance Distance function
   */
  public PrecomputedDistanceMatrix(Relation<O> relation, DBIDRange range, Distance<? super O> distance) {
    super();
    this.refrelation = new WeakReference<>(relation);
    this.ids = range;
    this.distance = distance;

    if(!distance.isSymmetric()) {
      throw new AbortException("Distance matrixes currently only support symmetric distance functions (Patches welcome).");
    }
  }

  @Override
  public void initialize() {
    if(ids.size() > 65536) {
      throw new AbortException("Distance matrixes currently have a limit of 65536 objects (~16 GB). After this, the array size exceeds the Java integer range, and a different data structure needs to be used.");
    }
    DistanceQuery<O> distanceQuery = distance.instantiate(refrelation.get());

    final int msize = triangleSize(ids.size());
    matrix = new double[msize];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();

    Duration timer = LOG.newDuration(getClass().getName() + ".precomputation-time").begin();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Precomputing distance matrix", msize, LOG) : null;
    int pos = 0;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      // y < x -- must match {@link #getOffset}!
      for(iy.seek(0); iy.getOffset() < ix.getOffset(); iy.advance()) {
        matrix[pos++] = distanceQuery.distance(ix, iy);
      }
      if(prog != null) {
        prog.setProcessed(prog.getProcessed() + ix.getOffset(), LOG);
      }
    }
    LOG.ensureCompleted(prog);
    LOG.statistics(timer.end());
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
  public DistanceQuery<O> getDistanceQuery(Distance<? super O> distanceFunction) {
    return this.distance.equals(distanceFunction) ? new PrecomputedDistanceQuery() : null;
  }

  @Override
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return null; // not possible
  }

  @Override
  public KNNSearcher<DBIDRef> kNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return this.distance.equals(distanceQuery.getDistance()) ? new PrecomputedKNNQuery() : null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return null; // not possible
  }

  @Override
  public RangeSearcher<DBIDRef> rangeByDBID(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return this.distance.equals(distanceQuery.getDistance()) ? new PrecomputedRangeQuery() : null;
  }

  @Override
  public PrioritySearcher<O> priorityByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return null; // not possible
  }

  @Override
  public PrioritySearcher<DBIDRef> priorityByDBID(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return this.distance.equals(distanceQuery.getDistance()) ? new PrecomputedDistancePrioritySearcher() : null;
  }

  /**
   * Distance query using the precomputed matrix.
   *
   * @author Erich Schubert
   */
  public class PrecomputedDistanceQuery implements DatabaseDistanceQuery<O> {
    @Override
    public double distance(DBIDRef id1, DBIDRef id2) {
      final int x = ids.getOffset(id1), y = ids.getOffset(id2);
      return (x != y) ? matrix[getOffset(x, y)] : 0.;
    }

    @Override
    public Distance<? super O> getDistance() {
      return distance;
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
  public class PrecomputedRangeQuery implements RangeSearcher<DBIDRef> {
    /**
     * Iterator for mapping.
     */
    DBIDArrayIter it = ids.iter();

    @Override
    public ModifiableDoubleDBIDList getRange(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
      result.add(0., id);
      final int x = ids.getOffset(id);
      // Case y < x: triangleSize(x) + y
      int pos = triangleSize(x);
      for(int y = 0; y < x; y++, pos++) {
        final double dist = matrix[pos];
        if(dist <= range) {
          result.add(dist, it.seek(y));
        }
      }
      assert (pos == triangleSize(x + 1));
      // Case y > x: triangleSize(y) + x
      pos = triangleSize(x + 1) + x;
      for(int y = x + 1, size = ids.size(); y < size; pos += y++) {
        final double dist = matrix[pos];
        if(dist <= range) {
          result.add(dist, it.seek(y));
        }
      }
      return result;
    }
  }

  /**
   * kNN query using the distance matrix.
   *
   * @author Erich Schubert
   */
  public class PrecomputedKNNQuery implements KNNSearcher<DBIDRef> {
    /**
     * Iterator for mapping.
     */
    DBIDArrayIter it = ids.iter();

    @Override
    public KNNList getKNN(DBIDRef id, int k) {
      KNNHeap heap = DBIDUtil.newHeap(k);
      heap.insert(0., id);
      double max = Double.POSITIVE_INFINITY;
      final int x = ids.getOffset(id);
      // Case y < x: triangleSize(x) + y
      int pos = triangleSize(x);
      for(int y = 0; y < x; y++, pos++) {
        final double dist = matrix[pos];
        max = dist <= max ? heap.insert(dist, it.seek(y)) : max;
      }
      assert (pos == triangleSize(x + 1));
      // Case y > x: triangleSize(y) + x
      pos = triangleSize(x + 1) + x;
      for(int y = x + 1, size = ids.size(); y < size; pos += y++) {
        final double dist = matrix[pos];
        max = dist <= max ? heap.insert(dist, it.seek(y)) : max;
      }
      return heap.toKNNList();
    }
  }

  /**
   * Range query using the distance matrix.
   *
   * @author Erich Schubert
   */
  public class PrecomputedDistancePrioritySearcher implements PrioritySearcher<DBIDRef>, QuickSelect.Adapter<PrecomputedDistancePrioritySearcher> {
    /**
     * Iterator for mapping.
     */
    DBIDArrayIter it = ids.iter();

    /**
     * Current position
     */
    int off;

    /**
     * Sorting position
     */
    int sorted;

    /**
     * "side effect" sorting positions
     */
    int lbsorted, upsorted;

    /**
     * Query threshold
     */
    double threshold;

    /**
     * Object indexes
     */
    int[] idx = new int[ids.size()];

    /**
     * Sorted distances
     */
    double[] dists = new double[ids.size()];

    @Override
    public PrioritySearcher<DBIDRef> search(DBIDRef query) {
      off = 0;
      threshold = Double.POSITIVE_INFINITY;
      int x = ids.getOffset(query);
      int pos = triangleSize(x);
      // Initialize ids:
      idx[0] = x;
      for(int y = 0; y < x; y++) {
        idx[y + 1] = y;
      }
      for(int y = x + 1, size = dists.length; y < size; y++) {
        idx[y] = y;
      }
      // Initialize distances:
      dists[0] = 0;
      System.arraycopy(matrix, pos, dists, 1, x);
      pos = triangleSize(x + 1) + x;
      for(int y = x + 1, size = dists.length; y < size; pos += y++) {
        dists[y] = matrix[pos];
      }
      sorted = 1;
      return this;
    }

    /**
     * Partially sort the data.
     * 
     * @param target Target
     */
    private void partialSort(int target) {
      if(sorted < target) {
        lbsorted = target - 1;
        upsorted = target;
        // FIXME: improve this by merging QuickSelect and QuickSort into a
        // full-blown PartialQuickSort that can guarantee any subset to be
        // sorted correctly.
        QuickSelect.quickSelect(this, this, sorted, dists.length, target - 1);
        // As side effect, [lbsorted:upsorted[ was sorted by quickSelect.
        if(sorted < lbsorted) { // Unsorted parts below target:
          DoubleIntegerArrayQuickSort.sort(dists, idx, sorted, lbsorted);
        }
        sorted = upsorted;
      }
    }

    @Override
    public PrioritySearcher<DBIDRef> advance() {
      if(++off >= sorted) {
        partialSort(Math.min(sorted == 1 ? 10 : sorted + (sorted >>> 1), dists.length));
      }
      return this;
    }

    @Override
    public boolean valid() {
      return off < ids.size() && dists[off] <= threshold;
    }

    @Override
    public PrioritySearcher<DBIDRef> decreaseCutoff(double threshold) {
      this.threshold = threshold;
      return this;
    }

    @Override
    public int internalGetIndex() {
      return it.seek(idx[off]).internalGetIndex();
    }

    @Override
    public double computeExactDistance() {
      return dists[off];
    }

    @Override
    public double getApproximateDistance() {
      return dists[off];
    }

    @Override
    public double getApproximateAccuracy() {
      return 0;
    }

    @Override
    public double getLowerBound() {
      return dists[off];
    }

    @Override
    public double getUpperBound() {
      return dists[off];
    }

    @Override
    public double allLowerBound() {
      return off < idx.length ? 0 : Double.POSITIVE_INFINITY;
    }

    @Override
    public int compare(PrecomputedDistanceMatrix<O>.PrecomputedDistancePrioritySearcher data, int i, int j) {
      return Double.compare(dists[i], dists[j]);
    }

    @Override
    public void swap(PrecomputedDistanceMatrix<O>.PrecomputedDistancePrioritySearcher data, int i, int j) {
      final int tmp = idx[i];
      idx[i] = idx[j];
      idx[j] = tmp;
      final double tmp2 = dists[i];
      dists[i] = dists[j];
      dists[j] = tmp2;
    }

    @Override
    public void isSorted(PrecomputedDistanceMatrix<O>.PrecomputedDistancePrioritySearcher data, int begin, int end) {
      // This is a pretty obscure logic. See FIXME above to use a full-blown
      // PartialQuickSort instead of this callback hack.
      lbsorted = begin < lbsorted && end >= lbsorted ? begin : lbsorted;
      upsorted = begin <= upsorted && end > upsorted ? end : upsorted;
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
    protected final Distance<? super O> distance;

    /**
     * Constructor.
     *
     * @param distance Distance function
     */
    public Factory(Distance<? super O> distance) {
      super();
      this.distance = distance;
    }

    @Override
    public PrecomputedDistanceMatrix<O> instantiate(Relation<O> relation) {
      DBIDs rids = relation.getDBIDs();
      if(!(rids instanceof DBIDRange)) {
        throw new AbortException("Distance matrixes are currently only supported for DBID ranges (as used by static databases; not on modifiable databases) for performance reasons (Patches welcome).");
      }
      return new PrecomputedDistanceMatrix<>(relation, (DBIDRange) rids, distance);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distance.getInputTypeRestriction();
    }

    /**
     * Parameterizer.
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
