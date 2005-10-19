package de.lmu.ifi.dbs.index.spatial.rstar;

import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialComparator;

import java.util.Arrays;

/**
 * Encapsulates the required parameters for a topological split of a R*-Tree.
 * Also static methods for bulk split are provided.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class Split {
  /**
   * The split axis.
   */
  int splitAxis = 0;

  /**
   * The index of the split point.
   */
  int splitPoint = -1;

  /**
   * Indicates wether the sorting according to maximal or to minmal
   * value is best for the split axis
   */
  int bestSort;

  /**
   * The entries sorted according to their max values of their MBRs.
   */
  Entry[] maxSorting;

  /**
   * The entries sorted according to their min values of their MBRs.
   */
  Entry[] minSorting;

  /**
   * Creates a new BulkSplit object.
   *
   * @param entries    the entries to be split
   * @param minEntries number of minimum entries in the node to be split
   */
  public Split(Entry[] entries, int minEntries) {
    chooseSplitAxis(entries, minEntries);
    chooseSplitPoint(minEntries);
  }

  /**
   * Chooses a split axis.
   *
   * @param entries    the entries to be split
   * @param minEntries number of minimum entries in the node to be split
   */
  private void chooseSplitAxis(Entry[] entries, int minEntries) {
    int dim = entries[0].getMBR().getDimensionality();

    maxSorting = entries.clone();
    minSorting = entries.clone();

    // best value for the surface
    double minSurface = Double.MAX_VALUE;
    // comparator used by sort method
    final SpatialComparator comp = new SpatialComparator();

    for (int i = 1; i <= dim; i++) {
      double currentPerimeter = 0.0;
      // sort the entries according to their minmal and according to their maximal value
      comp.setCompareDimension(i);
      // sort according to minimal value
      comp.setComparisonValue(SpatialComparator.MIN);
      Arrays.sort(minSorting, comp);
      // sort according to maximal value
      comp.setComparisonValue(SpatialComparator.MAX);
      Arrays.sort(maxSorting, comp);

      for (int k = 0; k <= entries.length - 2 * minEntries; k++) {
        MBR mbr1 = mbr(minSorting, 0, minEntries + k);
        MBR mbr2 = mbr(minSorting, minEntries + k, entries.length);
        currentPerimeter += mbr1.perimeter() + mbr2.perimeter();
        mbr1 = mbr(maxSorting, 0, minEntries + k);
        mbr2 = mbr(maxSorting, minEntries + k, entries.length);
        currentPerimeter += mbr1.perimeter() + mbr2.perimeter();
      }

      if (currentPerimeter < minSurface) {
        splitAxis = i;
        minSurface = currentPerimeter;
      }
    }
  }

  /**
   * Chooses a split axis.
   *
   * @param minEntries number of minimum entries in the node to be split
   */
  private void chooseSplitPoint(int minEntries) {
    // numEntries
    int numEntries = maxSorting.length;
    // sort upper and lower in the right dimesnion
    final SpatialComparator comp = new SpatialComparator();
    comp.setCompareDimension(splitAxis);
    comp.setComparisonValue(SpatialComparator.MIN);
    Arrays.sort(minSorting, comp);
    comp.setComparisonValue(SpatialComparator.MAX);
    Arrays.sort(maxSorting, comp);

    // the split point (first set to minimum entries in the node)
    splitPoint = minEntries;
    // best value for the overlap
    double minOverlap = Double.MAX_VALUE;
    // the volume of mbr1 and mbr2
    double volume = 0.0;
    // indicates wether the sorting according to maximal or to minmal value is best for the split axis
    bestSort = -1;

    for (int i = 0; i <= numEntries - 2 * minEntries; i++) {
      // test the sorting with respect to the minimal values
      MBR mbr1 = mbr(minSorting, 0, minEntries + i);
      MBR mbr2 = mbr(minSorting, minEntries + i, numEntries);
      double currentOverlap = mbr1.overlap(mbr2);
      if (currentOverlap < minOverlap || (currentOverlap == minOverlap && (mbr1.volume() + mbr2.volume()) < volume)) {
        minOverlap = currentOverlap;
        splitPoint = minEntries + i;
        bestSort = SpatialComparator.MIN;
        volume = mbr1.volume() + mbr2.volume();
      }
      // test the sorting with respect to the maximal values
      mbr1 = mbr(maxSorting, 0, minEntries + i);
      mbr2 = mbr(maxSorting, minEntries + i, numEntries);
      currentOverlap = mbr1.overlap(mbr2);
      if (currentOverlap < minOverlap || (currentOverlap == minOverlap && (mbr1.volume() + mbr2.volume()) < volume)) {
        minOverlap = currentOverlap;
        splitPoint = minEntries + i;
        bestSort = SpatialComparator.MAX;
        volume = mbr1.volume() + mbr2.volume();
      }
    }
  }

  /**
   * Computes and returns the mbr of the specified nodes, only the nodes
   * between from and to index are considered.
   *
   * @param nodes the array of nodes
   * @param from  the start index
   * @param to    the end index
   * @return the mbr of the specified nodes
   */
  private MBR mbr(final Entry[] nodes, final int from, final int to) {
    double[] min = new double[nodes[from].getMBR().getDimensionality()];
    double[] max = new double[nodes[from].getMBR().getDimensionality()];

    for (int d = 1; d <= min.length; d++) {
      min[d-1] = nodes[from].getMBR().getMin(d);
      max[d-1] = nodes[from].getMBR().getMax(d);
    }

    for (int i = from; i < to; i++) {
      MBR currMBR = nodes[i].getMBR();
      for (int d = 1; d <= min.length; d++) {
        if (min[d-1] > currMBR.getMin(d)) {
          min[d-1] = currMBR.getMin(d);
        }
        if (max[d-1] < currMBR.getMax(d)) {
          max[d-1] = currMBR.getMax(d);
        }
      }
    }
    return new MBR(min, max);
  }

}
