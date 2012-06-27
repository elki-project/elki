package de.lmu.ifi.dbs.elki.evaluation.roc;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Iterator;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.PairInterface;

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
 * @apiviz.uses SimpleAdapter
 * @apiviz.uses DistanceResultAdapter
 * @apiviz.uses OutlierScoreAdapter
 */
// TODO: add lazy Iterator<> based results that do not require full
// materialization
public class ROC {
  /**
   * Compute a ROC curve given a set of positive IDs and a sorted list of
   * (comparable, ID)s, where the comparable object is used to decided when two
   * objects are interchangeable.
   * 
   * @param <C> Reference type
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei List of neighbors along with some comparable object to detect
   *        'same positions'.
   * @return area under curve
   */
  public static <C extends Comparable<? super C>, T> XYCurve materializeROC(int size, Set<? super T> ids, Iterator<? extends PairInterface<C, T>> nei) {
    final int postot = ids.size(), negtot = size - postot;
    int poscnt = 0, negcnt = 0;
    XYCurve curve = new XYCurve("True Negative Rate", "True Positive Rate", postot + 2);

    // start in bottom left
    curve.add(0.0, 0.0);

    C prevval = null;
    while(nei.hasNext()) {
      // Analyze next point
      PairInterface<C, T> cur = nei.next();
      // positive or negative match?
      if(ids.contains(cur.getSecond())) {
        poscnt += 1;
      }
      else {
        negcnt += 1;
      }
      // defer calculation for ties
      if((prevval != null) && (prevval.compareTo(cur.getFirst()) == 0)) {
        continue;
      }
      // Add a new point.
      curve.addAndSimplify(negcnt / (double) negtot, poscnt / (double) postot);
      prevval = cur.getFirst();
    }
    // Ensure we end up in the top right corner.
    // Simplification will skip this if we already were.
    curve.addAndSimplify(1.0, 1.0);
    return curve;
  }

  /**
   * Compute a ROC curve given a set of positive IDs and a sorted list of
   * (comparable, ID)s, where the comparable object is used to decided when two
   * objects are interchangeable.
   * 
   * @param <C> Reference type
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei List of neighbors along with some comparable object to detect
   *        'same positions'.
   * @return area under curve
   */
  public static <C extends Comparable<? super C>> XYCurve materializeROC(int size, SetDBIDs ids, Iterator<? extends PairInterface<C, DBID>> nei) {
    final int postot = ids.size(), negtot = size - postot;
    int poscnt = 0, negcnt = 0;
    XYCurve curve = new XYCurve("True Negative Rate", "True Positive Rate", postot + 2);

    // start in bottom left
    curve.add(0.0, 0.0);

    C prevval = null;
    while(nei.hasNext()) {
      // Rates at *previous* data point. Because of tie handling strategy!
      final double trueneg = negcnt / (double) negtot;
      final double truepos = poscnt / (double) postot;
      // Analyze next point
      PairInterface<C, DBID> cur = nei.next();
      // positive or negative match?
      if(ids.contains(cur.getSecond())) {
        poscnt += 1;
      }
      else {
        negcnt += 1;
      }
      // defer calculation for ties
      if((prevval != null) && (prevval.compareTo(cur.getFirst()) == 0)) {
        continue;
      }
      // Add point for *previous* result (since we are no longer tied with it)
      curve.addAndSimplify(trueneg, truepos);
      prevval = cur.getFirst();
    }
    // Ensure we end up in the top right corner.
    // Simplification will skip this if we already were.
    curve.addAndSimplify(1.0, 1.0);
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
   */
  public static class SimpleAdapter implements Iterator<DBIDPair> {
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
    public boolean hasNext() {
      return this.iter.valid();
    }

    @Override
    public DBIDPair next() {
      DBID id = this.iter.getDBID();
      this.iter.advance();
      return DBIDUtil.newPair(id, id);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
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
   * @param <D> Distance type
   */
  public static class DistanceResultAdapter<D extends Distance<D>> implements Iterator<Pair<D, DBID>> {
    /**
     * Original Iterator
     */
    private Iterator<? extends DistanceResultPair<D>> iter;

    /**
     * Constructor
     * 
     * @param iter Iterator for distance results
     */
    public DistanceResultAdapter(Iterator<? extends DistanceResultPair<D>> iter) {
      super();
      this.iter = iter;
    }

    @Override
    public boolean hasNext() {
      return this.iter.hasNext();
    }

    @Override
    public Pair<D, DBID> next() {
      DistanceResultPair<D> d = this.iter.next();
      return new Pair<D, DBID>(d.getDistance(), d.getDBID());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
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
   */
  public static class OutlierScoreAdapter implements Iterator<DoubleObjPair<DBID>> {
    /**
     * Original Iterator
     */
    private DBIDIter iter;

    /**
     * Outlier score
     */
    private Relation<Double> scores;

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
    public boolean hasNext() {
      return this.iter.valid();
    }

    @Override
    public DoubleObjPair<DBID> next() {
      DBID id = this.iter.getDBID();
      iter.advance();
      return new DoubleObjPair<DBID>(scores.get(id), id);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
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
  public static <D extends Distance<D>> double computeROCAUCDistanceResult(int size, Cluster<?> clus, Iterable<? extends DistanceResultPair<D>> nei) {
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
  public static <D extends Distance<D>> double computeROCAUCDistanceResult(int size, DBIDs ids, Iterable<? extends DistanceResultPair<D>> nei) {
    // TODO: do not materialize the ROC, but introduce an iterator interface
    XYCurve roc = materializeROC(size, DBIDUtil.ensureSet(ids), new DistanceResultAdapter<D>(nei.iterator()));
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
    XYCurve roc = materializeROC(size, DBIDUtil.ensureSet(ids), new SimpleAdapter(nei.iter()));
    return XYCurve.areaUnderCurve(roc);
  }
}