package de.lmu.ifi.dbs.elki.evaluation.roc;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
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

    while(iter.valid()) {
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++poscnt;
        }
        else {
          ++negcnt;
        }
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
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
   * Compute the area under the ROC curve given a set of positive IDs and a
   * sorted list of (comparable, ID)s, where the comparable object is used to
   * decided when two objects are interchangeable.
   * 
   * @param <I> Iterator type
   * @param predicate Predicate to test for positive objects
   * @param iter Iterator over results, with ties.
   * @return area under curve
   */
  public static <I extends ScoreIter> double computeROCAUC(Predicate<? super I> predicate, I iter) {
    int poscnt = 0, negcnt = 0, pospre = 0, negpre = 0;
    double acc = 0.;
    while(iter.valid()) {
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++poscnt;
        }
        else {
          ++negcnt;
        }
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
      if(negcnt > negpre) {
        acc += (poscnt + pospre) * .5 * (negcnt - negpre);
        negpre = negcnt;
      }
      pospre = poscnt;
    }
    acc /= negcnt * (long) poscnt;
    return acc == acc ? acc : 0.5; /* Detect NaN */
  }

  /**
   * Compute a ROC curves Area-under-curve for cluster and a ranking.
   * 
   * @param clus Cluster object
   * @param nei Query result
   * @return area under curve
   */
  public static double computeROCAUC(Cluster<?> clus, DoubleDBIDList nei) {
    return computeROCAUC(new DBIDsTest(DBIDUtil.ensureSet(clus.getIDs())), new DistanceResultAdapter(nei.iter()));
  }

  /**
   * Compute a ROC curves Area-under-curve for a set of DBIDs and a ranking.
   * 
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei Query Result
   * @return area under curve
   */
  public static double computeROCAUC(DBIDs ids, DoubleDBIDList nei) {
    return computeROCAUC(new DBIDsTest(DBIDUtil.ensureSet(ids)), new DistanceResultAdapter(nei.iter()));
  }

  /**
   * Compute a ROC curves Area-under-curve for a set of outliers and an outlier
   * scoring.
   * 
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param outlier Outlier result
   * @return area under curve
   */
  public static double computeROCAUC(DBIDs ids, OutlierResult outlier) {
    return computeROCAUC(new DBIDsTest(DBIDUtil.ensureSet(ids)), new OutlierScoreAdapter(outlier));
  }

  /**
   * Compute the average precision given a set of positive IDs and a sorted list
   * of (comparable, ID)s, where the comparable object is used to decided when
   * two objects are interchangeable.
   * 
   * @param <I> Iterator type
   * @param predicate Predicate to test for positive objects
   * @param iter Iterator over results, with ties.
   * @return average precision
   */
  public static <I extends ScoreIter> double computeAveragePrecision(Predicate<? super I> predicate, I iter) {
    int poscnt = 0, negcnt = 0, pospre = 0;
    double acc = 0.;
    while(iter.valid()) {
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++poscnt;
        }
        else {
          ++negcnt;
        }
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
      // Add a new point.
      if(poscnt > pospre) {
        acc += (poscnt / (double) (poscnt + negcnt)) * (poscnt - pospre);
        pospre = poscnt;
      }
    }
    return (poscnt > 0) ? acc / poscnt : 0.;
  }

  /**
   * Compute the average precision for a set of DBIDs and a ranking.
   * 
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei Query Result
   * @return average precision
   */
  public static double computeAveragePrecision(DBIDs ids, DoubleDBIDList nei) {
    return computeAveragePrecision(new DBIDsTest(DBIDUtil.ensureSet(ids)), new DistanceResultAdapter(nei.iter()));
  }

  /**
   * Compute the average precision for a set of outliers and an outlier scoring.
   * 
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param outlier Outlier result
   * @return average precision
   */
  public static double computeAveragePrecision(DBIDs ids, OutlierResult outlier) {
    return computeAveragePrecision(new DBIDsTest(DBIDUtil.ensureSet(ids)), new OutlierScoreAdapter(outlier));
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
  public static class SimpleAdapter implements ScoreIter, DBIDRefIter {
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
    public SimpleAdapter advance() {
      iter.advance();
      return this;
    }

    @Override
    public boolean tiedToPrevious() {
      return false; // No information.
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

    @Override
    public DBIDRef getRef() {
      return iter;
    }
  }

  /**
   * This adapter is used to process a list of (double, DBID) objects. The list
   * <em>must</em> be sorted appropriately, the score is only used to detect
   * ties.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf DoubleDBIDListIter
   */
  public static class DistanceResultAdapter implements ScoreIter, DBIDRefIter {
    /**
     * Original Iterator
     */
    protected DoubleDBIDListIter iter;

    /**
     * Distance of previous.
     */
    protected double prevDist = Double.NaN;

    /**
     * Constructor
     * 
     * @param iter Iterator for distance results
     */
    public DistanceResultAdapter(DoubleDBIDListIter iter) {
      super();
      this.iter = iter;
    }

    @Override
    public boolean valid() {
      return iter.valid();
    }

    @Override
    public DistanceResultAdapter advance() {
      prevDist = iter.doubleValue();
      iter.advance();
      return this;
    }

    @Override
    public DBIDRef getRef() {
      return iter;
    }

    @Override
    public boolean tiedToPrevious() {
      return iter.doubleValue() == prevDist;
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
   * This adapter is used to process a list of (double, DBID) objects, but
   * allows skipping one object in the ranking. The list <em>must</em> be sorted
   * appropriately, the score is only used to detect ties.
   * 
   * @author Erich Schubert
   */
  public static class FilteredDistanceResultAdapter extends DistanceResultAdapter {
    /**
     * DBID to skip (usually: query object).
     */
    DBIDRef skip;

    /**
     * Constructor
     * 
     * @param iter Iterator for distance results
     * @param skip DBID to skip (reference must remain stable!)
     */
    public FilteredDistanceResultAdapter(DoubleDBIDListIter iter, DBIDRef skip) {
      super(iter);
      this.skip = skip;
      if(iter.valid() && DBIDUtil.equal(iter, skip)) {
        iter.advance();
      }
    }

    @Override
    public DistanceResultAdapter advance() {
      super.advance();
      if(iter.valid() && DBIDUtil.equal(iter, skip)) {
        iter.advance();
      }
      return this;
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
  public static class OutlierScoreAdapter implements ScoreIter, DBIDRefIter {
    /**
     * Original iterator.
     */
    private DBIDIter iter;

    /**
     * Outlier score.
     */
    private DoubleRelation scores;

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
    public OutlierScoreAdapter advance() {
      prev = scores.doubleValue(iter);
      iter.advance();
      return this;
    }

    @Override
    public boolean tiedToPrevious() {
      return scores.doubleValue(iter) == prev;
    }

    @Override
    public DBIDRef getRef() {
      return iter;
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
    private NumberVector vec;

    /**
     * Current position.
     */
    int pos = 0;

    /**
     * Constructor.
     * 
     * @param vec Vector to iterate over.
     */
    public DecreasingVectorIter(NumberVector vec) {
      this.vec = vec;
      final int dim = vec.getDimensionality();
      this.sort = new int[dim];
      for(int d = 0; d < dim; d++) {
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
      return pos < vec.getDimensionality() && pos >= 0;
    }

    @Override
    public DecreasingVectorIter advance() {
      ++pos;
      return this;
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
    public DecreasingVectorIter advance(int count) {
      pos += count;
      return this;
    }

    @Override
    public DecreasingVectorIter retract() {
      pos--;
      return this;
    }

    @Override
    public DecreasingVectorIter seek(int off) {
      pos = off;
      return this;
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
    private NumberVector vec;

    /**
     * Current position.
     */
    int pos = 0;

    /**
     * Constructor.
     * 
     * @param vec Vector to iterate over.
     */
    public IncreasingVectorIter(NumberVector vec) {
      this.vec = vec;
      final int dim = vec.getDimensionality();
      this.sort = new int[dim];
      for(int d = 0; d < dim; d++) {
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
    public IncreasingVectorIter advance() {
      ++pos;
      return this;
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
    public IncreasingVectorIter advance(int count) {
      pos += count;
      return this;
    }

    @Override
    public IncreasingVectorIter retract() {
      pos--;
      return this;
    }

    @Override
    public IncreasingVectorIter seek(int off) {
      pos = off;
      return this;
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
    public VectorNonZero(NumberVector vec) {
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
    NumberVector vec;

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
    public VectorOverThreshold(NumberVector vec, double threshold) {
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
    NumberVector vec;

    /**
     * Constructor.
     * 
     * @param vec Reference vector.
     */
    public VectorZero(NumberVector vec) {
      this.vec = vec;
    }

    @Override
    public boolean test(IncreasingVectorIter o) {
      return Math.abs(vec.doubleValue(o.dim())) < Double.MIN_NORMAL;
    }
  }

  /**
   * A score iterator wrapping a DBIDRef object.
   * 
   * @author Erich Schubert
   */
  public static interface DBIDRefIter {
    /**
     * Get the current DBIDRef.
     * 
     * @return DBID reference
     */
    DBIDRef getRef();
  }

  /**
   * Test predicate using a DBID set as positive elements.
   * 
   * @apiviz.composedOf DBIDs
   *
   * @author Erich Schubert
   */
  public static class DBIDsTest implements Predicate<DBIDRefIter> {
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
    public boolean test(DBIDRefIter o) {
      return set.contains(o.getRef());
    }
  }
}
