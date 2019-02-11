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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;

/**
 * Represents a node in a RDkNN-Tree.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @navhas - contains - RdKNNEntry
 */
public class RdKNNNode extends AbstractRStarTreeNode<RdKNNNode, RdKNNEntry> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public RdKNNNode() {
    // empty constructor
  }

  /**
   * Creates a new RdKNNNode object.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public RdKNNNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf);
  }

  /**
   * Computes and returns the aggregated knn distance of this node
   * 
   * @return the aggregated knn distance of this node
   */
  protected double kNNDistance() {
    double result = getEntry(0).getKnnDistance();
    for(int i = 1; i < getNumEntries(); i++) {
      double knnDistance = getEntry(i).getKnnDistance();
      result = (result < knnDistance) ? knnDistance : result;
    }
    return result;
  }

  @Override
  public boolean adjustEntry(RdKNNEntry entry) {
    boolean changed = super.adjustEntry(entry);
    entry.setKnnDistance(kNNDistance());
    return changed;
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly
   * set. Subclasses may need to overwrite this method.
   * 
   * @param parent the parent holding the entry representing this node
   * @param index the index of the entry in the parents child array
   */
  @Override
  protected void integrityCheckParameters(RdKNNNode parent, int index) {
    super.integrityCheckParameters(parent, index);
    // test if knn distance is correctly set
    RdKNNEntry entry = parent.getEntry(index);
    double knnDistance = kNNDistance();
    if(entry.getKnnDistance() != knnDistance) {
      double soll = knnDistance;
      double ist = entry.getKnnDistance();
      throw new RuntimeException("Wrong knnDistance in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }
}