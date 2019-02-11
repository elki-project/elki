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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import net.jafama.FastMath;

/**
 * Represents a node in an MkCop-Tree.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @navhas - contains - MkCoPEntry
 * 
 * @param <O> object type
 */
class MkCoPTreeNode<O> extends AbstractMTreeNode<O, MkCoPTreeNode<O>, MkCoPEntry> {
  /**
   * Serial version UID
   */
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public MkCoPTreeNode() {
    // empty constructor
  }

  /**
   * Creates a MkCoPTreeNode object.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public MkCoPTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf);
  }

  /**
   * Determines and returns the conservative approximation for the knn distances
   * of this node as the maximum of the conservative approximations of all
   * entries.
   * 
   * @param k_max the maximum k parameter
   * @return the conservative approximation for the knn distances
   */
  protected ApproximationLine conservativeKnnDistanceApproximation(int k_max) {
    // determine k_0, y_1, y_kmax
    int k_0 = k_max;
    double y_1 = Double.NEGATIVE_INFINITY;
    double y_kmax = Double.NEGATIVE_INFINITY;

    for(int i = 0; i < getNumEntries(); i++) {
      MkCoPEntry entry = getEntry(i);
      ApproximationLine approx = entry.getConservativeKnnDistanceApproximation();
      k_0 = Math.min(approx.getK_0(), k_0);
    }

    for(int i = 0; i < getNumEntries(); i++) {
      MkCoPEntry entry = getEntry(i);
      ApproximationLine approx = entry.getConservativeKnnDistanceApproximation();
      double entry_y_1 = approx.getValueAt(k_0);
      double entry_y_kmax = approx.getValueAt(k_max);
      if(!Double.isInfinite(entry_y_1)) {
        y_1 = Math.max(entry_y_1, y_1);
      }

      if(!Double.isInfinite(entry_y_kmax)) {
        y_kmax = Math.max(entry_y_kmax, y_kmax);
      }
    }

    // determine m and t
    double m = (y_kmax - y_1) / (FastMath.log(k_max) - FastMath.log(k_0));
    double t = y_1 - m * FastMath.log(k_0);

    return new ApproximationLine(k_0, m, t);
  }

  /**
   * Determines and returns the progressive approximation for the knn distances
   * of this node as the maximum of the progressive approximations of all
   * entries.
   * 
   * @param k_max the maximum k parameter
   * @return the conservative approximation for the knn distances
   */
  protected ApproximationLine progressiveKnnDistanceApproximation(int k_max) {
    if(!isLeaf()) {
      throw new UnsupportedOperationException("Progressive KNN-distance approximation " + "is only vailable in leaf nodes!");
    }

    // determine k_0, y_1, y_kmax
    int k_0 = 0;
    double y_1 = Double.POSITIVE_INFINITY;
    double y_kmax = Double.POSITIVE_INFINITY;

    for(int i = 0; i < getNumEntries(); i++) {
      MkCoPLeafEntry entry = (MkCoPLeafEntry) getEntry(i);
      ApproximationLine approx = entry.getProgressiveKnnDistanceApproximation();
      k_0 = Math.max(approx.getK_0(), k_0);
    }

    for(int i = 0; i < getNumEntries(); i++) {
      MkCoPLeafEntry entry = (MkCoPLeafEntry) getEntry(i);
      ApproximationLine approx = entry.getProgressiveKnnDistanceApproximation();
      y_1 = Math.min(approx.getValueAt(k_0), y_1);
      y_kmax = Math.min(approx.getValueAt(k_max), y_kmax);
    }

    // determine m and t
    double m = (y_kmax - y_1) / (FastMath.log(k_max) - FastMath.log(k_0));
    double t = y_1 - m * FastMath.log(k_0);

    return new ApproximationLine(k_0, m, t);
  }

  @Override
  public boolean adjustEntry(MkCoPEntry entry, DBID routingObjectID, double parentDistance, AbstractMTree<O, MkCoPTreeNode<O>, MkCoPEntry, ?> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // adjust conservative distance approximation
    // int k_max = ((MkCoPTree<O,D>) mTree).getK_max();
    // entry.setConservativeKnnDistanceApproximation(conservativeKnnDistanceApproximation(k_max));
    return true; // TODO: improve
  }

  @Override
  protected void integrityCheckParameters(MkCoPEntry parentEntry, MkCoPTreeNode<O> parent, int index, AbstractMTree<O, MkCoPTreeNode<O>, MkCoPEntry, ?> mTree) {
    super.integrityCheckParameters(parentEntry, parent, index, mTree);
    // test conservative approximation
    MkCoPEntry entry = parent.getEntry(index);
    int k_max = ((MkCoPTree<O>) mTree).getKmax();
    ApproximationLine approx = conservativeKnnDistanceApproximation(k_max);
    if(!entry.getConservativeKnnDistanceApproximation().equals(approx)) {
      String soll = approx.toString();
      String ist = entry.getConservativeKnnDistanceApproximation().toString();
      throw new RuntimeException("Wrong conservative approximation in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }
}
