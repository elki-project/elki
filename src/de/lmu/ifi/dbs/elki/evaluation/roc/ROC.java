package de.lmu.ifi.dbs.elki.evaluation.roc;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerComparator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayIter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter;

/**
 * Compute ROC (Receiver Operating Characteristics) curves.
 * 
 * A ROC curve compares the true positive rate (y-axis) and false positive rate
 * (x-axis).
 * 
 * It was first used in radio signal detection, but has since found widespread
 * use in information retrieval, in particular for evaluating binary
 * classification problems.
 * 
 * ROC curves are particularly useful to evaluate a ranking of objects with
 * respect to a binary classification problem: a random sampling will
 * approximately achieve a ROC value of 0.5, while a perfect separation will
 * achieve 1.0 (all positives first) or 0.0 (all negatives first). In most use
 * cases, a score significantly below 0.5 indicates that the algorithm result
 * has been used the wrong way, and should be used backwards.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf ScoreIter
 * @apiviz.composedOf Predicate
 * @apiviz.has XYCurve
 */
public class ROC {
  /**
   * Iterator for comparing scores.
   * 
   * @author Erich Schubert
   */
  public static interface ScoreIter extends Iter {
    /**
     * Test whether the score is the same as the previous objects score.
     * 
     * When there is no previous result, implementations should return false!
     * 
     * @return Boolean
     */
    boolean tiedToPrevious();
  }

  /**
   * Predicate to test whether an object is a true positive or false positive.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Data type
   */
  public static interface Predicate<T> {
    boolean test(T o);
  }

  /**
   * Compute a ROC curve given a set of positive IDs and a sorted list of
   * (comparable, ID)s, where the comparable object is used to decided when two
   * objects are interchangeable.
   * 
   * @param <I> Iterator type
   * @param predicate Predicate to test for positive objects
   * @param iter Iterator over results, with ties.
   * @return area under curve
   */
  public static <I extends ScoreIter> XYCurve materializeROC(Predicate<? super I> predicate, I iter) {
    int poscnt = 0, negcnt = 0;
    XYCurve curve = new XYCurve("False Positive Rate", "True Positive Rate");

    // start in bottom left
    curve.add(0.0, 0.0);

    while (iter.valid()) {
      // positive or negative match?
      do {
        if (predicate.test(iter)) {
          ++poscnt;
        } else {
          ++negcnt;
        }
        iter.advance();
      } // Loop while tied:
      while (iter.valid() && iter.tiedToPrevious());
      // Add a new point.
      curve.addAndSimplify(negcnt, poscnt);
    }
    // Ensure we end up in the top right corner.
    // Simplification will skip this if we already were.
    curve.addAndSimplify(negcnt, poscnt);
    curve.rescale(1. / negcnt, 1. / poscnt);
    return curve;
  }

  /**
   * This adapter can be used for an arbitrary collection of Integers, and uses
   * that id1.compareTo(id2) != 0 for id1 != id2 to satisfy the comparability.
   * 
   * Note that of course, no id should occur more than once.
   * 
   * The ROC values would be incorrect then anyway!
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf DBIDIter
   */
  public static class SimpleAdapter implements ScoreIter, DBIDRef {
    /**
     * Original Iterator
     */
    private DBIDIter iter;

    /**
     * Constructor
     * 
     * @param iter Iterator for object IDs
     */
    public SimpleAdapter(DBIDIter iter) {
      super();
      this.iter = iter;
    }

    @Override
    public boolean valid() {
      return iter.valid();
    }

    @Override
    public void advance() {
      iter.advance();
    }

    @Override
    public boolean tiedToPrevious() {
      return false; // No information.
    }

    @Override
    public int internalGetIndex() {
      return iter.internalGetIndex();
    }

    @Deprecated
    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Deprecated
    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }
  }

  /**
   * This adapter can be used for an arbitrary collection of Integers, and uses
   * that id1.compareTo(id2) != 0 for id1 != id2 to satisfy the comparability.
   * 
   * Note that of course, no id should occur more than once.
   * 
   * The ROC values would be incorrect then anyway!
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf DistanceDBIDListIter
   * 
   * @param <D> Distance type
   */
  public static class DistanceResultAdapter<D extends Distance<D>> implements ScoreIter, DBIDRef {
    /**
     * Original Iterator
     */
    private DistanceDBIDListIter<D> iter;

    /**
     * Distance of previous.
     */
    private D prevDist = null;

    /**
     * Constructor
     * 
     * @param iter Iterator for distance results
     */
    public DistanceResultAdapter(DistanceDBIDListIter<D> iter) {
      super();
      this.iter = iter;
    }

    @Override
    public boolean valid() {
      return iter.valid();
    }

    @Override
    public void advance() {
      prevDist = iter.getDistance();
      iter.advance();
    }

    @Override
    public int internalGetIndex() {
      return iter.internalGetIndex();
    }

    @Override
    public boolean tiedToPrevious() {
      return iter.getDistance().equals(prevDist);
    }

    @Deprecated
    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Deprecated
    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }
  }

  /**
   * This adapter can be used for an arbitrary collection of Integers, and uses
   * that id1.compareTo(id2) != 0 for id1 != id2 to satisfy the comparability.
   * 
   * Note that of course, no id should occur more than once.
   * 
   * The ROC values would be incorrect then anyway!
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf OutlierResult
   */
  public static class OutlierScoreAdapter implements ScoreIter, DBIDRef {
    /**
     * Original iterator.
     */
    private DBIDIter iter;

    /**
     * Outlier score.
     */
    private Relation<Double> scores;

    /**
     * Previous value.
     */
    double prev = Double.NaN;

    /**
     * Constructor.
     * 
     * @param o Result
     */
    public OutlierScoreAdapter(OutlierResult o) {
      super();
      this.iter = o.getOrdering().iter(o.getScores().getDBIDs()).iter();
      this.scores = o.getScores();
    }

    @Override
    public boolean valid() {
      return iter.valid();
    }

    @Override
    public void advance() {
      prev = scores.get(iter);
      iter.advance();
    }

    @Override
    public boolean tiedToPrevious() {
      return scores.get(iter) == prev;
    }

    @Override
    public int internalGetIndex() {
      return iter.internalGetIndex();
    }

    @Deprecated
    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Deprecated
    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }
  }

  /**
   * Class to iterate over a number vector in decreasing order.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf NumberVector
   */
  public static class DecreasingVectorIter implements ScoreIter, IntegerComparator, ArrayIter {
    /**
     * Order of dimensions.
     */
    private int[] sort;

    /**
     * Data vector.
     */
    private NumberVector<?> vec;

    /**
     * Current position.
     */
    int pos = 0;

    /**
     * Constructor.
     * 
     * @param vec Vector to iterate over.
     */
    public DecreasingVectorIter(NumberVector<?> vec) {
      this.vec = vec;
      final int dim = vec.getDimensionality();
      this.sort = new int[dim];
      for (int d = 0; d < dim; d++) {
        sort[d] = d;
      }
      IntegerArrayQuickSort.sort(sort, this);
    }

    @Override
    public int compare(int x, int y) {
      return Double.compare(vec.doubleValue(y), vec.doubleValue(x));
    }

    public int dim() {
      return sort[pos];
    }

    @Override
    public boolean valid() {
      return pos < vec.getDimensionality();
    }

    @Override
    public void advance() {
      ++pos;
    }

    @Override
    public boolean tiedToPrevious() {
      return pos > 0 && Double.compare(vec.doubleValue(sort[pos]), vec.doubleValue(sort[pos - 1])) == 0;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public void advance(int count) {
      pos += count;
    }

    @Override
    public void retract() {
      pos--;
    }

    @Override
    public void seek(int off) {
      pos = off;
    }
  }

  /**
   * Class to iterate over a number vector in decreasing order.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf NumberVector
   */
  public static class IncreasingVectorIter implements ScoreIter, IntegerComparator, ArrayIter {
    /**
     * Order of dimensions.
     */
    private int[] sort;

    /**
     * Data vector.
     */
    private NumberVector<?> vec;

    /**
     * Current position.
     */
    int pos = 0;

    /**
     * Constructor.
     * 
     * @param vec Vector to iterate over.
     */
    public IncreasingVectorIter(NumberVector<?> vec) {
      this.vec = vec;
      final int dim = vec.getDimensionality();
      this.sort = new int[dim];
      for (int d = 0; d < dim; d++) {
        sort[d] = d;
      }
      IntegerArrayQuickSort.sort(sort, this);
    }

    @Override
    public int compare(int x, int y) {
      return Double.compare(vec.doubleValue(x), vec.doubleValue(y));
    }

    public int dim() {
      return sort[pos];
    }

    @Override
    public boolean valid() {
      return pos < vec.getDimensionality();
    }

    @Override
    public void advance() {
      ++pos;
    }

    @Override
    public boolean tiedToPrevious() {
      return pos > 0 && Double.compare(vec.doubleValue(sort[pos]), vec.doubleValue(sort[pos - 1])) == 0;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public void advance(int count) {
      pos += count;
    }

    @Override
    public void retract() {
      pos--;
    }

    @Override
    public void seek(int off) {
      pos = off;
    }
  }

  /**
   * Class that uses a NumberVector as reference, and considers all non-zero
   * values as positive entries.
   * 
   * @apiviz.composedOf NumberVector
   * 
   * @author Erich Schubert
   */
  public static class VectorNonZero extends VectorOverThreshold {
    /**
     * Constructor.
     * 
     * @param vec Reference vector.
     */
    public VectorNonZero(NumberVector<?> vec) {
      super(vec, 0.);
    }
  }
  
  /**
   * Class that uses a NumberVector as reference, and considers all non-zero
   * values as positive entries.
   * 
   * @apiviz.composedOf NumberVector
   * 
   * @author Erich Schubert
   */
  public static class VectorOverThreshold implements Predicate<DecreasingVectorIter> {
    /**
     * Vector to use as reference
     */
    NumberVector<?> vec;

    /**
     * Threshold
     */
    double threshold;

    /**
     * Constructor.
     * 
     * @param vec Reference vector.
     * @param threshold Threshold value.
     */
    public VectorOverThreshold(NumberVector<?> vec, double threshold) {
      super();
      this.vec = vec;
      this.threshold = threshold;
    }

    @Override
    public boolean test(DecreasingVectorIter o) {
      return Math.abs(vec.doubleValue(o.dim())) > threshold;
    }
  }

  /**
   * Class that uses a NumberVector as reference, and considers all zero values
   * as positive entries.
   * 
   * @apiviz.composedOf NumberVector
   * 
   * @author Erich Schubert
   */
  public static class VectorZero implements Predicate<IncreasingVectorIter> {
    /**
     * Vector to use as reference
     */
    NumberVector<?> vec;

    /**
     * Constructor.
     * 
     * @param vec Reference vector.
     */
    public VectorZero(NumberVector<?> vec) {
      this.vec = vec;
    }

    @Override
    public boolean test(IncreasingVectorIter o) {
      return Math.abs(vec.doubleValue(o.dim())) < Double.MIN_NORMAL;
    }
  }

  /**
   * Test predicate using a DBID set as positive elements.
   * 
   * @apiviz.composedOf DBIDs
   * 
   * @author Erich Schubert
   */
  public static class DBIDsTest implements Predicate<DBIDRef> {
    /**
     * DBID set.
     */
    private DBIDs set;

    /**
     * Constructor.
     * 
     * @param set Set of positive objects
     */
    public DBIDsTest(DBIDs set) {
      this.set = set;
    }

    @Override
    public boolean test(DBIDRef o) {
      return set.contains(o);
    }
  }

  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param <D> Distance type
   * @param size Database size
   * @param clus Cluster object
   * @param nei Query result
   * @return area under curve
   */
  public static <D extends Distance<D>> double computeROCAUCDistanceResult(int size, Cluster<?> clus, DistanceDBIDList<D> nei) {
    // TODO: ensure the collection has efficient "contains".
    return ROC.computeROCAUCDistanceResult(size, clus.getIDs(), nei);
  }

  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param <D> Distance type
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei Query Result
   * @return area under curve
   */
  public static <D extends Distance<D>> double computeROCAUCDistanceResult(int size, DBIDs ids, DistanceDBIDList<D> nei) {
    // TODO: do not materialize the ROC, but introduce an iterator interface
    XYCurve roc = materializeROC(new DBIDsTest(DBIDUtil.ensureSet(ids)), new DistanceResultAdapter<>(nei.iter()));
    return XYCurve.areaUnderCurve(roc);
  }

  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei Query Result
   * @return area under curve
   */
  public static double computeROCAUCSimple(int size, DBIDs ids, DBIDs nei) {
    // TODO: do not materialize the ROC, but introduce an iterator interface
    XYCurve roc = materializeROC(new DBIDsTest(DBIDUtil.ensureSet(ids)), new SimpleAdapter(nei.iter()));
    return XYCurve.areaUnderCurve(roc);
  }
}
