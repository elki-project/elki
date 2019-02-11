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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;

/**
 * Represents a node in a DeLiClu-Tree.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @navhas - contains - DeLiCluEntry
 */
public class DeLiCluNode extends AbstractRStarTreeNode<DeLiCluNode, DeLiCluEntry> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public DeLiCluNode() {
    // empty constructor
  }

  /**
   * Creates a new DeLiCluNode with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public DeLiCluNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf);
  }

  /**
   * Returns true, if the children of this node (or their child nodes) contain
   * handled data objects.
   * 
   * @return true, if the children of this node (or their child nodes) contain
   *         handled data objects
   */
  public boolean hasHandled() {
    for(int i = 0; i < getNumEntries(); i++) {
      boolean handled = getEntry(i).hasHandled();
      if(handled) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true, if the children of this node (or their child nodes) contain
   * unhandled data objects.
   * 
   * @return true, if the children of this node (or their child nodes) contain
   *         unhandled data objects
   */
  public boolean hasUnhandled() {
    for(int i = 0; i < getNumEntries(); i++) {
      boolean handled = getEntry(i).hasUnhandled();
      if(handled) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean adjustEntry(DeLiCluEntry entry) {
    boolean changed = super.adjustEntry(entry);
    // adjust hasHandled and hasUnhandled flag
    boolean hasHandled = hasHandled();
    boolean hasUnhandled = hasUnhandled();
    entry.setHasHandled(hasHandled);
    entry.setHasUnhandled(hasUnhandled);
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
  protected void integrityCheckParameters(DeLiCluNode parent, int index) {
    super.integrityCheckParameters(parent, index);
    // test if hasHandled and hasUnhandled flag are correctly set
    DeLiCluEntry entry = parent.getEntry(index);
    boolean hasHandled = hasHandled();
    boolean hasUnhandled = hasUnhandled();
    if(entry.hasHandled() != hasHandled) {
      String soll = Boolean.toString(hasHandled);
      String ist = Boolean.toString(entry.hasHandled());
      throw new RuntimeException("Wrong hasHandled in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
    if(entry.hasUnhandled() != hasUnhandled) {
      String soll = Boolean.toString(hasUnhandled);
      String ist = Boolean.toString(entry.hasUnhandled());
      throw new RuntimeException("Wrong hasUnhandled in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }
}