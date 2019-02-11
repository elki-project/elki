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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.AbstractNode;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.InconsistentDataException;

/**
 * Abstract super class for nodes in M-Tree variants.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @navhas - contains - MTreeEntry
 * 
 * @param <O> the type of DatabaseObject to be stored in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 * @param <E> the type of MetricalEntry used in the M-Tree
 */
public abstract class AbstractMTreeNode<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry> extends AbstractNode<E> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public AbstractMTreeNode() {
    // empty constructor
  }

  /**
   * Creates a new MTreeNode with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public AbstractMTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf);
  }

  /**
   * Adjusts the parameters of the entry representing this node (e.g. after
   * insertion of new objects). Subclasses may need to overwrite this method.
   * 
   * @param entry the entry representing this node
   * @param routingObjectID the id of the (new) routing object of this node
   * @param parentDistance the distance from the routing object of this node to
   *        the routing object of the parent node
   * @param mTree the M-Tree object holding this node
   * @return {@code true} if adjustment of parent is needed
   */
  public boolean adjustEntry(E entry, DBID routingObjectID, double parentDistance, AbstractMTree<O, N, E, ?> mTree) {
    boolean changed = entry.setRoutingObjectID(routingObjectID);
    changed |= entry.setParentDistance(parentDistance);
    changed |= entry.setCoveringRadius(coveringRadiusFromEntries(routingObjectID, mTree));
    return changed;
  }

  /**
   * Determines and returns the covering radius of this node.
   * 
   * @param routingObjectID the object id of the routing object of this node
   * @param mTree the M-Tree
   * @return the covering radius of this node
   */
  public double coveringRadiusFromEntries(DBID routingObjectID, AbstractMTree<O, N, E, ?> mTree) {
    double coveringRadius = 0.;
    for(int i = 0; i < getNumEntries(); i++) {
      E entry = getEntry(i);
      final double cover = entry.getParentDistance() + entry.getCoveringRadius();
      coveringRadius = coveringRadius < cover ? cover : coveringRadius;
    }
    return coveringRadius;
  }

  /**
   * Tests this node (for debugging purposes).
   * 
   * @param mTree the M-Tree holding this node
   * @param entry the entry representing this node
   */
  @SuppressWarnings("unchecked")
  public final void integrityCheck(AbstractMTree<O, N, E, ?> mTree, E entry) {
    // leaf node
    if(isLeaf()) {
      for(int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);
        if(i < getNumEntries() && e == null) {
          throw new InconsistentDataException("i < numEntries && entry == null");
        }
        if(i >= getNumEntries() && e != null) {
          throw new InconsistentDataException("i >= numEntries && entry != null");
        }
      }
    }
    // dir node
    else {
      N tmp = mTree.getNode(getEntry(0));
      boolean childIsLeaf = tmp.isLeaf();

      for(int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);

        if(i < getNumEntries() && e == null) {
          throw new InconsistentDataException("i < numEntries && entry == null");
        }

        if(i >= getNumEntries() && e != null) {
          throw new InconsistentDataException("i >= numEntries && entry != null");
        }

        if(e != null) {
          N node = mTree.getNode(e);

          if(childIsLeaf && !node.isLeaf()) {
            for(int k = 0; k < getNumEntries(); k++) {
              mTree.getNode(getEntry(k));
            }

            throw new InconsistentDataException("Wrong Child in " + this + " at " + i);
          }

          if(!childIsLeaf && node.isLeaf()) {
            throw new InconsistentDataException("Wrong Child: child id no leaf, but node is leaf!");
          }

          // noinspection unchecked
          node.integrityCheckParameters(entry, (N) this, i, mTree);
          node.integrityCheck(mTree, e);
        }
      }

      if(LoggingConfiguration.DEBUG) {
        Logger.getLogger(this.getClass().getName()).fine("DirNode " + getPageID() + " ok!");
      }
    }
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly
   * set. Subclasses may need to overwrite this method.
   * 
   * @param parentEntry the entry representing the parent
   * @param parent the parent holding the entry representing this node
   * @param index the index of the entry in the parents child arry
   * @param mTree the M-Tree holding this node
   */
  protected void integrityCheckParameters(E parentEntry, N parent, int index, AbstractMTree<O, N, E, ?> mTree) {
    // test if parent distance is correctly set
    E entry = parent.getEntry(index);
    double parentDistance = mTree.distance(entry.getRoutingObjectID(), parentEntry.getRoutingObjectID());
    if(Math.abs(entry.getParentDistance() - parentDistance) > 1E-10) {
      throw new InconsistentDataException("Wrong parent distance in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + parentDistance + ",\n ist: " + entry.getParentDistance());
    }

    // test if covering radius is correctly set
    double mincover = parentDistance + entry.getCoveringRadius();
    if(parentEntry.getCoveringRadius() < mincover && //
        Math.abs(parentDistance - entry.getCoveringRadius()) > 1e-10) {
      throw new InconsistentDataException("pcr < pd + cr \n" + parentEntry.getCoveringRadius() + " < " + parentDistance + " + " + entry.getCoveringRadius() + "in node " + parent.getPageID() + " at index " + index + " (child " + entry + "):\n" + "dist(" + entry.getRoutingObjectID() + " - " + parentEntry.getRoutingObjectID() + ")" + " >  cr(" + entry + ")");
    }
  }
}
