package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialObject;

import java.util.Arrays;

/**
 * Encapsulates the required parameters for a bulk split of a RTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class BulkSplitDescription {
  /**
   * The potential split axis.
   */
  int splitAxis = -1;

  /**
   * the potential split point:
   */
  int splitPoint = -1;

  /**
   * Chooses a split axis.
   *
   * @param objects the spatial objects to be split
   */
  public void chooseBulkSplitAxis(SpatialObject[] objects) {
    int dimension = objects[0].getDimensionality();

    // maximum and minimum value for the extension
    double[] maxExtension = new double[dimension];
    double[] minExtension = new double[dimension];
    Arrays.fill(minExtension, Double.MAX_VALUE);

    // compute min and max value in each dimension
    for (int i = 0; i < objects.length; i++) {
      MBR mbr = objects[i].mbr();
      for (int d = 0; d < dimension; d++) {
        double min, max;
        min = mbr.getMin(d);
        max = mbr.getMax(d);

        if (maxExtension[d] < max)
          maxExtension[d] = max;

        if (minExtension[d] > min)
          minExtension[d] = min;
      }
    }

    // set split axis to dim with maximal extension
    double max = 0;
    for (int d = 0; d < dimension; d++) {
      double currentExtension = maxExtension[d] - minExtension[d];
      if (max < currentExtension) {
        max = currentExtension;
        splitAxis = d;
      }
    }
  }

  /**
   * Chooses a split point.
   *
   * @param numEntries the number of entries to be split
   * @param minEntries the number of minimum entries in the node to be split
   * @param maxEntries number of maximum entries in the node to be split
   * @return the split point
   */
  public int chooseBulkSplitPoint(int numEntries, int minEntries, int maxEntries) {
    int splitPoint = -1;

    if (numEntries < minEntries) {
      splitPoint = -1;
    }

    else if (numEntries <= maxEntries) {
      splitPoint = numEntries;
    }

    else if (numEntries < maxEntries + minEntries) {
      splitPoint = (numEntries - minEntries);
    }

    else {
      splitPoint = maxEntries;
    }

    return splitPoint;
  }
}
