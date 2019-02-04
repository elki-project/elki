/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Provides a unique interval represented by its id, a hyper bounding box
 * representing the alpha intervals, an interval of the corresponding distance,
 * and a set of objects ids associated with this interval.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @has - - - CASHIntervalSplit
 */
public class CASHInterval extends HyperBoundingBox implements Comparable<CASHInterval> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CASHInterval.class);

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
   * @param ids2 the set of ids to be removed
   */
  public void removeIDs(DBIDs ids2) {
    this.ids.removeDBIDs(ids2);
  }

  /**
   * Returns the number of objects associated with this interval.
   * 
   * @return the number of objects associated with this interval
   */
  public int numObjects() {
    return ids.size();
  }

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

  @Override
  public int compareTo(CASHInterval other) {
    if(this.equals(other)) {
      return 0;
    }
    int c = Integer.compare(this.priority(), other.priority());
    c = c == 0 ? Integer.compare(this.level, other.level) : c;
    c = c == 0 ? Integer.compare(this.maxSplitDimension, other.maxSplitDimension) : c;
    return c == 0 ? Integer.compare(other.intervalID, this.intervalID) : c;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || (o != null && getClass() == o.getClass() //
        && this.intervalID == ((CASHInterval) o).intervalID);
  }

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

    final boolean issplit = (maxSplitDimension >= (getDimensionality() - 1));
    final int childLevel = issplit ? level + 1 : level;
    final int splitDim = issplit ? 0 : maxSplitDimension + 1;
    final double splitPoint = getMin(splitDim) + (getMax(splitDim) - getMin(splitDim)) * .5;

    // left and right child
    for(int i = 0; i < 2; i++) {
      double[] min = SpatialUtil.getMin(this); // clone
      double[] max = SpatialUtil.getMax(this); // clone

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

    if(LOG.isDebuggingFine()) {
      StringBuilder msg = new StringBuilder();
      msg.append("Child level ").append(childLevel).append(",  split Dim   ").append(splitDim);
      if(leftChild != null) {
        msg.append("\nleft   ").append(leftChild);
      }
      if(rightChild != null) {
        msg.append("\nright   ").append(rightChild);
      }
      LOG.fine(msg.toString());
    }
  }
}
