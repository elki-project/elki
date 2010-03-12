package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash;

import java.util.Set;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.utilities.Identifiable;

/**
 * Provides a unique interval represented by its id, a hyper bounding box
 * representing the alpha intervals, an interval of the corresponding distance,
 * and a set of objects ids associated with this interval.
 * 
 * @author Elke Achtert
 */
public class CASHInterval extends HyperBoundingBox implements Identifiable {
  /**
   * Serial version number
   */
  private static final long serialVersionUID = 1;

  /**
   * Used for id assignment.
   */
  private static int ID = 0;

  /**
   * Holds the unique id of this interval.
   */
  private final int intervalID;

  /**
   * The level of this interval, 0 indicates the root level.
   */
  private int level;

  /**
   * The minimum distance value.
   */
  private double d_min;

  /**
   * The maximum distance value.
   */
  private double d_max;

  /**
   * Holds the ids of the objects associated with this interval.
   */
  private Set<Integer> ids;

  /**
   * Holds the maximum dimension which has already been split.
   */
  private int maxSplitDimension;

  /**
   * Holds the left child.
   */
  private CASHInterval leftChild;

  /**
   * Holds the right child.
   */
  private CASHInterval rightChild;

  /**
   * The object to perform interval splitting.
   */
  private CASHIntervalSplit split;

  /**
   * Empty constructor for Externalizable interface.
   */
  public CASHInterval() {
    super();
    this.intervalID = ++ID;
  }

  /**
   * Provides a unique interval represented by its id, a hyper bounding box and
   * a set of objects ids associated with this interval.
   * 
   * @param min the coordinates of the minimum hyper point
   * @param max the coordinates of the maximum hyper point
   * @param split the object to perform interval splitting
   * @param ids the ids of the objects associated with this interval
   * @param maxSplitDimension the maximum dimension which has already been split
   * @param level the level of this interval, 0 indicates the root level
   * @param d_min the minimum distance value
   * @param d_max the maximum distance value
   */
  public CASHInterval(double[] min, double[] max, CASHIntervalSplit split, Set<Integer> ids, int maxSplitDimension, int level, double d_min, double d_max) {
    super(min, max);
    // this.debug = true;
    this.intervalID = ++ID;
    this.split = split;
    this.ids = ids;
    this.maxSplitDimension = maxSplitDimension;
    this.level = level;
    this.d_min = d_min;
    this.d_max = d_max;
  }

  /**
   * Returns the set of ids of the objects associated with this interval.
   * 
   * @return the set of ids of the objects associated with this interval
   */
  public Set<Integer> getIDs() {
    return ids;
  }

  /**
   * Removes the specified ids from this interval.
   * 
   * @param ids the set of ids to be removed
   */
  public void removeIDs(Set<Integer> ids) {
    this.ids.removeAll(ids);
  }

  /**
   * Returns the number of objects associated with this interval
   * 
   * @return the number of objects associated with this interval
   */
  public int numObjects() {
    return ids.size();
  }

  /**
   * Returns true if this interval has already been split in the specified
   * dimension.
   * 
   * @param d the dimension to be tested
   * @return true if this interval has already been split in the specified
   *         dimension
   */
  public boolean isSplit(int d) {
    return maxSplitDimension >= d;
  }

  /**
   * Returns a String representation of the HyperBoundingBox.
   * 
   * @return String
   */
  @Override
  public String toString() {
    return super.toString() + ", ids: " + ids.size() + ", d_min: " + d_min + ", d_max " + d_max;
  }

  /**
   * Returns the priority of this interval (used as key in the heap).
   * 
   * @return the priority of this interval (used as key in the heap)
   */
  public int priority() {
    // return numObjects() * (maxSplitDimension + 1);
    return numObjects();
    // return numObjects() * (level + 1);
  }

  /**
   * Returns the maximum split dimension.
   * 
   * @return the maximum split dimension
   */
  public int getMaxSplitDimension() {
    return maxSplitDimension;
  }

  /**
   * Returns the level of this interval.
   * 
   * @return the level of this interval
   */
  public int getLevel() {
    return level;
  }

  /**
   * Returns the left child of this interval.
   * 
   * @return the left child of this interval
   */
  public CASHInterval getLeftChild() {
    return leftChild;
  }

  /**
   * Returns the right child of this interval.
   * 
   * @return the right child of this interval
   */
  public CASHInterval getRightChild() {
    return rightChild;
  }

  /**
   * Returns the unique id of this interval.
   * 
   * @return the unique id of this interval
   */
  public Integer getID() {
    return intervalID;
  }

  /**
   * Returns the minimum distance value.
   * 
   * @return the minimum distance value
   */
  public double getD_min() {
    return d_min;
  }

  /**
   * Returns the maximum distance value.
   * 
   * @return the maximum distance value
   */
  public double getD_max() {
    return d_max;
  }

  /**
   * Compares this object with the specified object for order. Returns a
   * negative integer, zero, or a positive integer as this object is less than,
   * equal to, or greater than the specified object.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareTo(CASHInterval other) {
    if(this.equals(other)) {
      return 0;
    }

    if(this.priority() < other.priority()) {
      return -1;
    }
    if(this.priority() > other.priority()) {
      return 1;
    }

    if(this.level < other.level) {
      return -1;
    }
    if(this.level > other.level) {
      return 1;
    }

    if(this.maxSplitDimension < other.maxSplitDimension) {
      return -1;
    }
    if(this.maxSplitDimension > other.maxSplitDimension) {
      return 1;
    }

    if(this.intervalID < other.intervalID) {
      return -1;
    }
    else {
      return 1;
    }
  }

  /**
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    final CASHInterval interval = (CASHInterval) o;
    if(intervalID != interval.intervalID) {
      return false;
    }
    return super.equals(o);
  }

  /**
   * Returns the unique id of this interval as hash code.
   */
  @Override
  public int hashCode() {
    return intervalID;
  }

  /**
   * Returns true if this interval has children.
   * 
   * @return if this interval has children
   */
  public boolean hasChildren() {
    return leftChild != null || rightChild != null;
  }

  /**
   * Splits this interval into 2 children.
   */
  public void split() {
    if(hasChildren()) {
      return;
    }

    int dim = getDimensionality();
    int childLevel = isSplit(dim) ? level + 1 : level;

    int splitDim = isSplit(dim) ? 1 : maxSplitDimension + 1;
    double splitPoint = getMin(splitDim) + (getMax(splitDim) - getMin(splitDim)) / 2;

    // left and right child
    for(int i = 0; i < 2; i++) {
      double[] min = getMin();
      double[] max = getMax();

      // right child
      if(i == 0) {
        min[splitDim - 1] = splitPoint;
      }
      // left child
      else {
        max[splitDim - 1] = splitPoint;
      }

      Set<Integer> childIDs = split.determineIDs(getIDs(), new HyperBoundingBox(min, max), d_min, d_max);
      if(childIDs != null) {
        // right child
        if(i == 0) {
          rightChild = new CASHInterval(min, max, split, childIDs, splitDim, childLevel, d_min, d_max);
        }
        // left child
        else {
          leftChild = new CASHInterval(min, max, split, childIDs, splitDim, childLevel, d_min, d_max);
        }
      }
    }

    if(LoggingConfiguration.DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\nchild level ").append(childLevel).append(",  split Dim   ").append(splitDim);
      if(leftChild != null) {
        msg.append("\nleft   ").append(leftChild);
      }
      if(rightChild != null) {
        msg.append("\nright   ").append(rightChild);
      }
      Logger.getLogger(this.getClass().getName()).fine(msg.toString());
    }
  }
}