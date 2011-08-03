package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Encapsulates the required parameters for a topological split of a R*-Tree.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has Split
 * @apiviz.uses SpatialComparator
 */
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public class TopologicalSplitter implements SplitStrategy<SpatialEntry> {
  /**
   * constructor.
   */
  public TopologicalSplitter() {
    // Nothing to do.
  }

  @Override
  public <E extends SpatialEntry> Pair<List<E>, List<E>> split(List<E> entries, int minEntries) {
    Split<E> split = new Split<E>();
    split.chooseSplitAxis(entries, minEntries);
    split.chooseSplitPoint(minEntries);
    int splitpoint = split.getSplitPoint();
    List<E> sorted = split.getBestSorting();

    return new Pair<List<E>, List<E>>(sorted.subList(0, splitpoint), sorted.subList(splitpoint, sorted.size()));
  }

  /**
   * Internal data for an actual split.
   * 
   * @author Erich Schubert
   * 
   * @param <E> Actual entry type
   */
  private class Split<E extends SpatialEntry> {
    /**
     * The split axis.
     */
    int splitAxis = 0;

    /**
     * The index of the split point.
     */
    int splitPoint = -1;

    /**
     * Indicates whether the sorting according to maximal or to minimal value
     * has been used for choosing the split axis and split point.
     */
    int bestSorting;

    /**
     * The entries sorted according to their max values of their MBRs.
     */
    List<E> maxSorting;

    /**
     * The entries sorted according to their min values of their MBRs.
     */
    List<E> minSorting;

    /**
     * Constructor.
     */
    public Split() {
      // Initialized by calling chooseSplitAxis.
    }

    /**
     * Chooses a split axis.
     * 
     * @param entries the entries to be split
     * @param minEntries number of minimum entries in the node to be split
     */
    void chooseSplitAxis(List<E> entries, int minEntries) {
      int dim = entries.get(0).getDimensionality();

      maxSorting = new ArrayList<E>(entries);
      minSorting = new ArrayList<E>(entries);

      // best value for the surface
      double minSurface = Double.MAX_VALUE;
      // comparator used by sort method

      for(int i = 1; i <= dim; i++) {
        double sumOfAllMargins = 0;
        // sort the entries according to their minimal and according to their
        // maximal value
        final SpatialComparator compMin = new SpatialComparator(i, SpatialComparator.MIN);
        Collections.sort(minSorting, compMin);
        final SpatialComparator compMax = new SpatialComparator(i, SpatialComparator.MAX);
        Collections.sort(maxSorting, compMax);

        SpatialComparable mbr_min_left = minSorting.get(0);
        SpatialComparable mbr_min_right = minSorting.get(minSorting.size() - 1);
        SpatialComparable mbr_max_left = maxSorting.get(0);
        SpatialComparable mbr_max_right = maxSorting.get(maxSorting.size() - 1);

        for(int k = 1; k < entries.size() - minEntries; k++) {
          mbr_min_left = SpatialUtil.union(mbr_min_left, minSorting.get(k));
          mbr_min_right = SpatialUtil.union(mbr_min_right, minSorting.get(minSorting.size() - 1 - k));
          mbr_max_left = SpatialUtil.union(mbr_max_left, maxSorting.get(k));
          mbr_max_right = SpatialUtil.union(mbr_max_right, maxSorting.get(maxSorting.size() - 1 - k));
          if(k >= minEntries - 1) {
            // Yes, build the sum. This value is solely used for finding the
            // split axis!
            // Compare with the original paper, "sum of all margin-values".
            // Note that mbr_min_left and mbr_max_left do not add up to a
            // complete split, but when the sum is complete, it will also
            // include their proper counterpart.
            sumOfAllMargins += SpatialUtil.perimeter(mbr_min_left) + SpatialUtil.perimeter(mbr_min_right) + SpatialUtil.perimeter(mbr_max_left) + SpatialUtil.perimeter(mbr_max_right);
          }
        }
        if(sumOfAllMargins < minSurface) {
          splitAxis = i;
          minSurface = sumOfAllMargins;
        }
      }
    }

    /**
     * Chooses a split axis.
     * 
     * @param minEntries number of minimum entries in the node to be split
     */
    void chooseSplitPoint(int minEntries) {
      // numEntries
      int numEntries = maxSorting.size();
      // sort upper and lower in the right dimension
      final SpatialComparator compMin = new SpatialComparator(splitAxis, SpatialComparator.MIN);
      Collections.sort(minSorting, compMin);
      final SpatialComparator compMax = new SpatialComparator(splitAxis, SpatialComparator.MAX);
      Collections.sort(maxSorting, compMax);

      // the split point (first set to minimum entries in the node)
      splitPoint = minEntries;
      // best value for the overlap
      double minOverlap = Double.MAX_VALUE;
      // the volume of mbr1 and mbr2
      double volume = 0.0;
      // indicates whether the sorting according to maximal or to minimal value
      // is
      // best for the split axis
      bestSorting = -1;

      for(int i = 0; i <= numEntries - 2 * minEntries; i++) {
        // test the sorting with respect to the minimal values
        HyperBoundingBox mbr1 = mbr(minSorting, 0, minEntries + i);
        HyperBoundingBox mbr2 = mbr(minSorting, minEntries + i, numEntries);
        double currentOverlap = SpatialUtil.relativeOverlap(mbr1, mbr2);
        double vol1 = SpatialUtil.volume(mbr1);
        double vol2 = SpatialUtil.volume(mbr2);
        if(currentOverlap < minOverlap || (currentOverlap == minOverlap && (vol1 + vol2) < volume)) {
          minOverlap = currentOverlap;
          splitPoint = minEntries + i;
          bestSorting = SpatialComparator.MIN;
          volume = vol1 + vol2;
        }
        // test the sorting with respect to the maximal values
        mbr1 = mbr(maxSorting, 0, minEntries + i);
        mbr2 = mbr(maxSorting, minEntries + i, numEntries);
        currentOverlap = SpatialUtil.relativeOverlap(mbr1, mbr2);
        vol1 = SpatialUtil.volume(mbr1);
        vol2 = SpatialUtil.volume(mbr2);
        if(currentOverlap < minOverlap || (currentOverlap == minOverlap && (vol1 + vol2) < volume)) {
          minOverlap = currentOverlap;
          splitPoint = minEntries + i;
          bestSorting = SpatialComparator.MAX;
          volume = vol1 + vol2;
        }
      }
    }

    /**
     * Computes and returns the mbr of the specified nodes, only the nodes
     * between from and to index are considered.
     * 
     * @param entries the array of nodes
     * @param from the start index
     * @param to the end index
     * @return the mbr of the specified nodes
     */
    private HyperBoundingBox mbr(final List<E> entries, final int from, final int to) {
      double[] min = new double[entries.get(from).getDimensionality()];
      double[] max = new double[entries.get(from).getDimensionality()];

      for(int d = 1; d <= min.length; d++) {
        min[d - 1] = entries.get(from).getMin(d);
        max[d - 1] = entries.get(from).getMax(d);
      }

      for(int i = from + 1; i < to; i++) {
        SpatialComparable currMBR = entries.get(i);
        for(int d = 1; d <= min.length; d++) {
          if(min[d - 1] > currMBR.getMin(d)) {
            min[d - 1] = currMBR.getMin(d);
          }
          if(max[d - 1] < currMBR.getMax(d)) {
            max[d - 1] = currMBR.getMax(d);
          }
        }
      }
      return new HyperBoundingBox(min, max);
    }

    /**
     * Returns the split point.
     * 
     * @return the split point
     */
    public int getSplitPoint() {
      return splitPoint;
    }

    /**
     * Returns whether the sorting according to maximal or to minimal value has
     * been used for choosing the split axis and split point.
     * 
     * @return The sorting to use
     */
    public List<E> getBestSorting() {
      if(bestSorting == SpatialComparator.MIN) {
        return minSorting;
      }
      if(bestSorting == SpatialComparator.MAX) {
        return maxSorting;
      }
      else {
        throw new IllegalStateException("split.bestSort is undefined: " + bestSorting);
      }
    }
  }
}
