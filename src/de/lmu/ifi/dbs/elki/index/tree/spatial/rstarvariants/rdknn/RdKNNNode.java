package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;

/**
 * Represents a node in a RDkNN-Tree.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has RdKNNEntry oneway - - contains
 * 
 * @param <D> Distance type
 */
public class RdKNNNode<D extends NumberDistance<D, ?>> extends AbstractRStarTreeNode<RdKNNNode<D>, RdKNNEntry<D>> {
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
    super(capacity, isLeaf, RdKNNEntry.class);
  }

  /**
   * Computes and returns the aggregated knn distance of this node
   * 
   * @return the aggregated knn distance of this node
   */
  protected D kNNDistance() {
    D result = getEntry(0).getKnnDistance();
    for(int i = 1; i < getNumEntries(); i++) {
      D knnDistance = getEntry(i).getKnnDistance();
      result = DistanceUtil.max(result, knnDistance);
    }
    return result;
  }

  @Override
  public void adjustEntry(RdKNNEntry<D> entry) {
    super.adjustEntry(entry);
    entry.setKnnDistance(kNNDistance());
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly
   * set. Subclasses may need to overwrite this method.
   * 
   * @param parent the parent holding the entry representing this node
   * @param index the index of the entry in the parents child array
   */
  @Override
  protected void integrityCheckParameters(RdKNNNode<D> parent, int index) {
    super.integrityCheckParameters(parent, index);
    // test if knn distance is correctly set
    RdKNNEntry<D> entry = parent.getEntry(index);
    D knnDistance = kNNDistance();
    if(!entry.getKnnDistance().equals(knnDistance)) {
      String soll = knnDistance.toString();
      String ist = entry.getKnnDistance().toString();
      throw new RuntimeException("Wrong knnDistance in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }
}