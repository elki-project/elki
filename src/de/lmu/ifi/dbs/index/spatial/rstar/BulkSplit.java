package de.lmu.ifi.dbs.index.spatial.rstar;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.spatial.MBR;

import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates the required parameters for a bulk split of an R*-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class BulkSplit<O extends NumberVector> {
  /**
   * The split axis.
   */
  int splitAxis = -1;

  /**
   * The split point.
   */
  int splitPoint = -1;

  /**
   * Creates a new BulkSplit object.
   *
   * @param objects    the data objects to be splitted
   * @param minEntries the minimum number of entries in a node
   * @param maxEntries the maximum number of entries in a node
   */
  public BulkSplit(List<O> objects, int minEntries, int maxEntries) {
    chooseMaximalExtendedSplitAxisForObjects(objects);
    chooseBulkSplitPoint(objects.size(), minEntries, maxEntries);
  }

  /**
   * Creates a new BulkSplit object.
   *
   * @param nodes      the nodes to be splitted
   * @param minEntries the minimum number of entries in a node
   * @param maxEntries the maximum number of entries in a node
   */
  public BulkSplit(RTreeNode[] nodes, int minEntries, int maxEntries) {
    chooseMaximalExtendedSplitAxisForNodes(nodes);
    chooseBulkSplitPoint(nodes.length, minEntries, maxEntries);
  }

  /**
   * Computes the best split axis. The best split axis
   * is the split axes with the maximal extension.
   *
   * @param objects the data objects to be splitted
   */
  private void chooseMaximalExtendedSplitAxisForObjects(List<O> objects) {
    int dimension = objects.get(0).getDimensionality();

    // maximum and minimum value for the extension
    double[] maxExtension = new double[dimension];
    double[] minExtension = new double[dimension];
    Arrays.fill(minExtension, Double.MAX_VALUE);

    // compute min and max value in each dimension
    for (O object : objects) {
      for (int d = 1; d <= dimension; d++) {
        double value = object.getValue(d).doubleValue();

        if (maxExtension[d - 1] < value)
          maxExtension[d - 1] = value;

        if (minExtension[d - 1] > value)
          minExtension[d - 1] = value;
      }
    }

    // set split axis to dim with maximal extension
    double max = 0;
    for (int d = 1; d <= dimension; d++) {
      double currentExtension = maxExtension[d - 1] - minExtension[d - 1];
      if (max < currentExtension) {
        max = currentExtension;
        splitAxis = d;
      }
    }
  }

  /**
   * Computes the best split axis. The best split axis
   * is the split axes with the maximal extension.
   *
   * @param nodes the spatial objects to be split
   * @return the best split axis
   */
  private int chooseMaximalExtendedSplitAxisForNodes(RTreeNode[] nodes) {
    int dimension = nodes[0].getDimensionality();

    // maximum and minimum value for the extension
    double[] maxExtension = new double[dimension];
    double[] minExtension = new double[dimension];
    Arrays.fill(minExtension, Double.MAX_VALUE);

    // compute min and max value in each dimension
    for (RTreeNode node : nodes) {
      MBR mbr = node.mbr();
      for (int d = 1; d <= dimension; d++) {
        double min, max;
        min = mbr.getMin(d);
        max = mbr.getMax(d);

        if (maxExtension[d - 1] < max)
          maxExtension[d - 1] = max;

        if (minExtension[d - 1] > min)
          minExtension[d - 1] = min;
      }
    }

    // set split axis to dim with maximal extension
    double max = 0;
    for (int d = 1; d <= dimension; d++) {
      double currentExtension = maxExtension[d - 1] - minExtension[d - 1];
      if (max < currentExtension) {
        max = currentExtension;
        splitAxis = d;
      }
    }
    return splitAxis;
  }

  /**
   * Computes the best split point.
   *
   * @param numEntries the number of entries to be split
   * @param minEntries the number of minimum entries in the node to be split
   * @param maxEntries number of maximum entries in the node to be split
   */
  public void chooseBulkSplitPoint(int numEntries, int minEntries,
                                   int maxEntries) {
    if (numEntries < minEntries) {
      throw new IllegalArgumentException("numEntries < minEntries!");
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
  }
}
