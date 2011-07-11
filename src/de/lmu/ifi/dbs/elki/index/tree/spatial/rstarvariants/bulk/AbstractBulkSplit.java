package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Encapsulates the required parameters for a bulk split of a spatial index.
 * 
 * @author Elke Achtert
 */
public abstract class AbstractBulkSplit implements BulkSplit, Parameterizable {
  /**
   * Constructor
   */
  public AbstractBulkSplit() {
    // Nothing to do
  }

  /**
   * Computes and returns the best split point.
   * 
   * @param numEntries the number of entries to be split
   * @param minEntries the number of minimum entries in the node to be split
   * @param maxEntries number of maximum entries in the node to be split
   * @return the best split point
   */
  protected int chooseBulkSplitPoint(int numEntries, int minEntries, int maxEntries) {
    if(numEntries < minEntries) {
      throw new IllegalArgumentException("numEntries < minEntries!");
    }

    if(numEntries <= maxEntries) {
      return numEntries;
    }
    else if(numEntries < maxEntries + minEntries) {
      return (numEntries - minEntries);
    }
    else {
      return maxEntries;
    }
  }
}