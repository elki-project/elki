package de.lmu.ifi.dbs.elki.evaluation.roc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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
  public static <C extends Comparable<? super C>> List<Pair<Double, Double>> materializeROC(int size, Collection<Integer> ids, Iterator<Pair<C, Integer>> nei) {
    final double DELTA = 0.1 / (size*size);
    
    int postot = ids.size();
    int negtot = size - postot;
    int poscnt = 0;
    int negcnt = 0;
    ArrayList<Pair<Double, Double>> res = new ArrayList<Pair<Double, Double>>(postot + 2);

    // start in bottom left
    res.add(new Pair<Double, Double>(0.0, 0.0));

    Pair<C, Integer> prev = null;
    while(nei.hasNext()) {
      Pair<C, Integer> cur = nei.next();
      // positive or negative match?
      if(ids.contains(cur.getSecond())) {
        poscnt += 1;
      }
      else {
        negcnt += 1;
      }
      // defer calculation if this points distance equals the previous points
      // distance
      if((prev == null) || (prev.getFirst().compareTo(cur.getFirst()) != 0)) {
        // positive rate - y axis
        double curpos = ((double) poscnt) / postot;
        // negative rate - x axis
        double curneg = ((double) negcnt) / negtot;
        // simplify curve when possible:
        if(res.size() >= 2) {
          Pair<Double, Double> last1 = res.get(res.size() - 2);
          Pair<Double, Double> last2 = res.get(res.size() - 1);
          // vertical simplification
          if((last1.getFirst() == last2.getFirst()) && (last2.getFirst() == curneg)) {
            res.remove(res.size() - 1);
          }
          // horizontal simplification
          else if((last1.getSecond() == last2.getSecond()) && (last2.getSecond() == curpos)) {
            res.remove(res.size() - 1);
          }
          // diagonal simplification
          // TODO: Make a test.
          else if(Math.abs((last2.getFirst() - last1.getFirst()) - (curneg - last2.getFirst())) < DELTA && Math.abs((last2.getSecond() - last1.getSecond()) - (curpos - last2.getSecond())) < DELTA) {
            res.remove(res.size() - 1);
          }
        }
        res.add(new Pair<Double, Double>(curneg, curpos));
      }
      prev = cur;
    }
    // ensure we end up in the top right corner.
    {
      Pair<Double, Double> last = res.get(res.size() - 1);
      if(last.getFirst() < 1.0 || last.getSecond() < 1.0) {
        res.add(new Pair<Double, Double>(1.0, 1.0));
      }
    }
    return res;
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
  public static class SimpleAdapter implements Iterator<Pair<Integer, Integer>> {
    /**
     * Original Iterator
     */
    private Iterator<Integer> iter;

    /**
     * Constructor
     * 
     * @param iter Iterator for object IDs
     */
    public SimpleAdapter(Iterator<Integer> iter) {
      super();
      this.iter = iter;
    }

    @Override
    public boolean hasNext() {
      return this.iter.hasNext();
    }

    @Override
    public Pair<Integer, Integer> next() {
      Integer id = this.iter.next();
      return new Pair<Integer, Integer>(id, id);
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
  public static class DistanceResultAdapter<D extends Distance<D>> implements Iterator<Pair<D, Integer>> {
    /**
     * Original Iterator
     */
    private Iterator<DistanceResultPair<D>> iter;

    /**
     * Constructor
     * 
     * @param iter Iterator for distance results
     */
    public DistanceResultAdapter(Iterator<DistanceResultPair<D>> iter) {
      super();
      this.iter = iter;
    }

    @Override
    public boolean hasNext() {
      return this.iter.hasNext();
    }

    @Override
    public Pair<D, Integer> next() {
      DistanceResultPair<D> d = this.iter.next();
      return new Pair<D, Integer>(d.getDistance(), d.getID());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * compute the Area Under Curve (difference to y axis) for an arbitrary
   * polygon
   * 
   * @param curve Iterable list of points (x,y)
   * @return area und curve
   */
  public static double computeAUC(Iterable<Pair<Double, Double>> curve) {
    double result = 0.0;
    Iterator<Pair<Double, Double>> iter = curve.iterator();
    // it doesn't make sense to speak about the "area under a curve" when there
    // is no curve.
    if(!iter.hasNext()) {
      return Double.NaN;
    }
    // starting point
    Pair<Double, Double> prev = iter.next();
    // check there is at least a second point
    if(!iter.hasNext()) {
      return Double.NaN;
    }
    while(iter.hasNext()) {
      Pair<Double, Double> next = iter.next();
      // width * height at half way.
      double width = next.getFirst() - prev.getFirst();
      double meanheight = (next.getSecond() + prev.getSecond()) / 2;
      result += width * meanheight;
      prev = next;
    }
    return result;
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
  public static <D extends Distance<D>> double computeROCAUCDistanceResult(int size, Cluster<?> clus, List<DistanceResultPair<D>> nei) {
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
  public static <D extends Distance<D>> double computeROCAUCDistanceResult(int size, Collection<Integer> ids, List<DistanceResultPair<D>> nei) {
    // TODO: do not materialize the ROC, but introduce an iterator interface
    List<Pair<Double, Double>> roc = materializeROC(size, ids, new DistanceResultAdapter<D>(nei.iterator()));
    return computeAUC(roc);
  }

  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei Query Result
   * @return area under curve
   */
  public static double computeROCAUCSimple(int size, Collection<Integer> ids, List<Integer> nei) {
    // TODO: do not materialize the ROC, but introduce an iterator interface
    List<Pair<Double, Double>> roc = materializeROC(size, ids, new SimpleAdapter(nei.iterator()));
    return computeAUC(roc);
  }
}
