package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash;

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

import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

/**
 * Provides a unique interval represented by its id, a hyper bounding box
 * representing the alpha intervals, an interval of the corresponding distance,
 * and a set of objects ids associated with this interval.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash.CASHIntervalSplit
 */
public class CASHInterval extends HyperBoundingBox implements Comparable<CASHInterval> {
  /**
   * Serial version number.
   */
  private static final long serialVersionUID = 1;

  /**
   * Used for id assignment.
   */
  private static int ID = 0;

  /**
   * Holds the unique id of this interval.
   */
  private final Integer intervalID;

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
  private ModifiableDBIDs ids;

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
  public CASHInterval(double[] min, double[] max, CASHIntervalSplit split, ModifiableDBIDs ids, int maxSplitDimension, int level, double d_min, double d_max) {
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
  public ModifiableDBIDs getIDs() {
    return ids;
  }

  /**
   * Removes the specified ids from this interval.
   * 
   * @param ids the set of ids to be removed
   */
  public void removeIDs(DBIDs ids) {
    this.ids.removeDBIDs(ids);
  }

  /**
   * Returns the number of objects associated with this interval.
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
  @Override
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

    if(other.intervalID.compareTo(this.intervalID) < 0) {
      return -1;
    }
    else {
      return 1;
    }
  }

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

  @Override
  public int hashCode() {
    return intervalID.hashCode();
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

    int splitDim = isSplit(dim) ? 0 : maxSplitDimension + 1;
    double splitPoint = getMin(splitDim) + (getMax(splitDim) - getMin(splitDim)) / 2;

    // left and right child
    for(int i = 0; i < 2; i++) {
      double[] min = SpatialUtil.getMin(this);
      double[] max = SpatialUtil.getMax(this);

      // right child
      if(i == 0) {
        min[splitDim] = splitPoint;
      }
      // left child
      else {
        max[splitDim] = splitPoint;
      }

      ModifiableDBIDs childIDs = split.determineIDs(getIDs(), new HyperBoundingBox(min, max), d_min, d_max);
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