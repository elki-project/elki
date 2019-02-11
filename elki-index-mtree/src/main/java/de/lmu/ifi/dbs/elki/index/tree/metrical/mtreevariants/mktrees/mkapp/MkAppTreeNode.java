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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkapp;

import java.util.Arrays;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Represents a node in an MkApp-Tree.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @navhas - contains - MkAppEntry
 * 
 * @param <O> object type
 */
class MkAppTreeNode<O> extends AbstractMTreeNode<O, MkAppTreeNode<O>, MkAppEntry> {
  private static final long serialVersionUID = 2;

  /**
   * Empty constructor for Externalizable interface.
   */
  public MkAppTreeNode() {
    // empty constructor
  }

  /**
   * Creates a MkAppTreeNode object.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public MkAppTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf);
  }

  /**
   * Determines and returns the polynomial approximation for the knn distances
   * of this node as the maximum of the polynomial approximations of all
   * entries.
   * 
   * @return the conservative approximation for the knn distances
   */
  protected PolynomialApproximation knnDistanceApproximation() {
    int p_max = 0;
    double[] b = null;
    for(int i = 0; i < getNumEntries(); i++) {
      MkAppEntry entry = getEntry(i);
      PolynomialApproximation approximation = entry.getKnnDistanceApproximation();
      if(b == null) {
        p_max = approximation.getPolynomialOrder();
        b = new double[p_max];
      }
      for(int p = 0; p < p_max; p++) {
        b[p] += approximation.getB(p);
      }
    }

    for(int p = 0; p < p_max; p++) {
      b[p] /= p_max;
    }

    if(LoggingConfiguration.DEBUG) {
      StringBuilder msg = new StringBuilder();
      msg.append("b " + FormatUtil.format(b, FormatUtil.NF4));
      Logger.getLogger(this.getClass().getName()).fine(msg.toString());
    }

    return new PolynomialApproximation(b);
  }

  /**
   * Adjusts the parameters of the entry representing this node.
   * 
   * @param entry the entry representing this node
   * @param routingObjectID the id of the (new) routing object of this node
   * @param parentDistance the distance from the routing object of this node to
   *        the routing object of the parent node
   * @param mTree the M-Tree object holding this node
   */
  @Override
  public boolean adjustEntry(MkAppEntry entry, DBID routingObjectID, double parentDistance, AbstractMTree<O, MkAppTreeNode<O>, MkAppEntry, ?> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // entry.setKnnDistanceApproximation(knnDistanceApproximation());
    return true; // TODO: improve
  }

  @Override
  protected void integrityCheckParameters(MkAppEntry parentEntry, MkAppTreeNode<O> parent, int index, AbstractMTree<O, MkAppTreeNode<O>, MkAppEntry, ?> mTree) {
    super.integrityCheckParameters(parentEntry, parent, index, mTree);

    MkAppEntry entry = parent.getEntry(index);
    PolynomialApproximation approximation_soll = knnDistanceApproximation();
    PolynomialApproximation approximation_ist = entry.getKnnDistanceApproximation();

    if(!Arrays.equals(approximation_ist.getCoefficients(), approximation_soll.getCoefficients())) {
      String soll = approximation_soll.toString();
      String ist = entry.getKnnDistanceApproximation().toString();
      throw new RuntimeException("Wrong polynomial approximation in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);

    }
  }
}