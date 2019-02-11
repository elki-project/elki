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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.index.tree.AbstractNode;
import de.lmu.ifi.dbs.elki.index.tree.Entry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

/**
 * Abstract superclass for nodes in a R*-Tree.
 *
 * @author Elke Achtert
 * @since 0.1
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class AbstractRStarTreeNode<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends AbstractNode<E> implements SpatialNode<N, E> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public AbstractRStarTreeNode() {
    super();
  }

  /**
   * Creates a new AbstractRStarTreeNode with the specified parameters.
   *
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public AbstractRStarTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf);
  }

  /**
   * Recomputing the MBR is rather expensive.
   *
   * @return MBR
   */
  public ModifiableHyperBoundingBox computeMBR() {
    E firstEntry = getEntry(0);
    if(firstEntry == null) {
      return null;
    }
    // Note: we deliberately get a cloned copy here, since we will modify it.
    ModifiableHyperBoundingBox mbr = new ModifiableHyperBoundingBox(firstEntry);
    for(int i = 1; i < numEntries; i++) {
      mbr.extend(getEntry(i));
    }
    return mbr;
  }

  /**
   * Adjusts the parameters of the entry representing this node.
   *
   * @param entry the entry representing this node
   */
  public boolean adjustEntry(E entry) {
    final SpatialDirectoryEntry se = (SpatialDirectoryEntry) entry;
    final ModifiableHyperBoundingBox mbr = computeMBR();
    boolean changed = false;
    if(se.hasMBR()) {
      final int dim = se.getDimensionality();
      // Test for changes
      for(int i = 0; i < dim; i++) {
        if(Math.abs(se.getMin(i) - mbr.getMin(i)) > Float.MIN_NORMAL) {
          changed = true;
          break;
        }
        if(Math.abs(se.getMax(i) - mbr.getMax(i)) > Float.MIN_NORMAL) {
          changed = true;
          break;
        }
      }
    }
    else { // No preexisting MBR.
      changed = true;
    }
    if(changed) {
      se.setMBR(mbr);
    }
    return changed;
  }

  /**
   * Adjusts the parameters of the entry representing this node. Only applicable
   * if one object was inserted or the size of an existing node increased.
   *
   * @param entry the entry representing this node
   * @param responsibleMBR the MBR of the object or node which is responsible
   *        for the call of the method
   * @return true when the entry has changed
   */
  public boolean adjustEntryIncremental(E entry, SpatialComparable responsibleMBR) {
    return ((SpatialDirectoryEntry) entry).extendMBR(responsibleMBR);
  }

  /**
   * Tests this node (for debugging purposes).
   */
  @SuppressWarnings("unchecked")
  public final void integrityCheck(AbstractRStarTree<N, E, ?> tree) {
    // leaf node
    if(isLeaf()) {
      for(int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);
        if(i < getNumEntries() && e == null) {
          throw new RuntimeException("i < numEntries && entry == null");
        }
        if(i >= getNumEntries() && e != null) {
          throw new RuntimeException("i >= numEntries && entry != null");
        }
      }
    }

    // dir node
    else {
      N tmp = tree.getNode(getEntry(0));
      boolean childIsLeaf = tmp.isLeaf();

      for(int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);

        if(i < getNumEntries() && e == null) {
          throw new RuntimeException("i < numEntries && entry == null");
        }

        if(i >= getNumEntries() && e != null) {
          throw new RuntimeException("i >= numEntries && entry != null");
        }

        if(e != null) {
          N node = tree.getNode(e);

          if(childIsLeaf && !node.isLeaf()) {
            for(int k = 0; k < getNumEntries(); k++) {
              tree.getNode(getEntry(k));
            }

            throw new RuntimeException("Wrong Child in " + this + " at " + i);
          }

          if(!childIsLeaf && node.isLeaf()) {
            throw new RuntimeException("Wrong Child: child id no leaf, but node is leaf!");
          }

          node.integrityCheckParameters((N) this, i);
          node.integrityCheck(tree);
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
   * @param parent the parent holding the entry representing this node
   * @param index the index of the entry in the parents child array
   */
  protected void integrityCheckParameters(N parent, int index) {
    // test if mbr is correctly set
    E entry = parent.getEntry(index);
    HyperBoundingBox mbr = computeMBR();

    if(/* entry.getMBR() == null && */mbr == null) {
      return;
    }
    if(!SpatialUtil.equals(entry, mbr)) {
      String soll = mbr.toString();
      String ist = new HyperBoundingBox(entry).toString();
      throw new RuntimeException("Wrong MBR in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }

  /**
   * Calls the super method and writes the id of this node, the numEntries and
   * the entries array to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    // TODO: do we need to write/read the capacity?
    out.writeInt(entries.length);
    for(Entry entry : entries) {
      if(entry == null) {
        break;
      }
      entry.writeExternal(out);
    }
  }

  /**
   * Reads the id of this node, the numEntries and the entries array from the
   * specified stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);

    // TODO: do we need to write/read the capacity?
    final int capacity = in.readInt();
    if(isLeaf()) {
      entries = (E[]) new SpatialPointLeafEntry[capacity];
      for(int i = 0; i < numEntries; i++) {
        SpatialPointLeafEntry s = new SpatialPointLeafEntry();
        s.readExternal(in);
        entries[i] = (E) s;
      }
    }
    else {
      entries = (E[]) new SpatialDirectoryEntry[capacity];
      for(int i = 0; i < numEntries; i++) {
        SpatialDirectoryEntry s = new SpatialDirectoryEntry();
        s.readExternal(in);
        entries[i] = (E) s;
      }
    }
  }
}