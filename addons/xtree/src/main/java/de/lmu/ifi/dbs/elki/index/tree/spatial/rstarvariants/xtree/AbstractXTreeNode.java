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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.index.tree.Entry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * XTree node extension for testing on supernodes
 * 
 * @author Marisa Thoma
 * @since 0.7.5
 * 
 * @param <N> Type of this node (for extendability)
 */
public abstract class AbstractXTreeNode<N extends AbstractXTreeNode<N>> extends AbstractRStarTreeNode<N, SpatialEntry> {
  /**
   * Is this node a supernode?
   */
  private boolean supernode = false;

  /**
   * Utility field for maintaining the loading of supernodes. Initialized by
   * {@link #readExternal(ObjectInput)} if the node is a supernode. Must then be
   * filled by {@link #readSuperNode(ObjectInput, AbstractXTree)}.
   */
  private int capacity_to_be_filled = 0;

  /**
   * @return <code>true</code> if this node is a supernode.
   */
  public boolean isSuperNode() {
    return supernode;
  }

  /**
   * Empty constructor for Externalizable interface.
   */
  public AbstractXTreeNode() {
    // empty constructor
    super();
  }

  /**
   * Creates a new XTreeNode with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public AbstractXTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf);
  }

  /**
   * Grows the supernode by duplicating its capacity.
   * 
   * @return the new page capacity of this node
   */
  public int growSuperNode() {
    if(getNumEntries() < getCapacity()) {
      throw new IllegalStateException("This node is not yet overflowing (only " + getNumEntries() + " of " + getCapacity() + " entries)");
    }
    Entry[] old_nodes = super.entries.clone();
    assert old_nodes[old_nodes.length - 1] != null;
    super.entries = (Entry[]) java.util.Arrays.copyOfRange(old_nodes, 0, getCapacity() * 2 - 1, entries.getClass());
    assert super.entries.length == old_nodes.length * 2 - 1;
    return getCapacity();
  }

  /**
   * Convert this node into a supernode. This node must be inserted into the
   * supernode map of its parenting XTree(tree.put(getID(), this);), and its
   * file entry must be updated (file.writePage(this);). Supernodes are kept in
   * main memory and are only sketched on disc (basically, the capacity is
   * given). This is handled in the methods {@link #writeExternal(ObjectOutput)}
   * and {@link #readExternal(ObjectInput)} .
   */
  public void makeSuperNode() {
    if(isSuperNode()) {
      throw new IllegalStateException("This node already is a supernode");
    }
    supernode = true;
    growSuperNode();
  }

  /**
   * Halves the size of this supernode. If the new capacity equals
   * <code>dirCapacity</code>, the node is converted back into a normal
   * (non-super) node. If this happens, the node must be removed from the
   * supernode map of the parenting XTree and updated in the index file.
   * 
   * @param dirCapacity The regular directory capacity
   */
  public int shrinkSuperNode(int dirCapacity) {
    if(!isSuperNode()) {
      throw new IllegalStateException("Cannot shrink a non-super node");
    }
    int newCapacity = getCapacity() / 2 + 1;
    if(numEntries >= newCapacity) {
      throw new IllegalStateException("This node is not yet underflowing and cannot be shrunken yet.");
    }
    Entry[] new_entries = java.util.Arrays.copyOfRange(super.entries, 0, newCapacity);
    assert new_entries[newCapacity - 1] == null;
    super.entries = new_entries;
    if(dirCapacity == getCapacity()) {
      supernode = false; // this node is no more a supernode
    }
    return getCapacity();
  }

  /**
   * Calls the super method and writes the id of this node, the numEntries and
   * the entries array to the specified stream. If this node is a supernode, it
   * cannot fit into <code>out</code> and thus, only the header is written. The
   * remaining space is left empty, since supernodes are to be written to the
   * end of the file via {@link #writeSuperNode(ObjectOutput)}.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(getPageID());
    out.writeBoolean(isLeaf());
    out.writeBoolean(supernode);
    out.writeInt(numEntries);
    out.writeInt(entries.length);
    if(isSuperNode())
      return; // cannot fit this into out
    for(Entry entry : entries) {
      if(entry == null) {
        break;
      }
      entry.writeExternal(out);
    }
  }

  /**
   * Reads the id of this node, the numEntries and the entries array from the
   * specified stream. If the {@link #supernode} field is set, <code>this</code>
   * cannot be contained in <code>in</code>. Such a node has to be manually
   * filled using {@link #readSuperNode(ObjectInput, AbstractXTree)}.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    setPageID(in.readInt());
    isLeaf = in.readBoolean();
    supernode = in.readBoolean();
    numEntries = in.readInt();

    final int capacity = in.readInt();
    if(supernode) {
      // this node is a supernode and is yet to be filled
      capacity_to_be_filled = capacity;
      return;
    }
    // the following causes a null pointer -- something is obviously missing
    // entries = (E[]) java.lang.reflect.Array.newInstance(eclass, capacity);
    if(isLeaf()) {
      entries = (Entry[]) new SpatialPointLeafEntry[capacity];
    }
    else {
      entries = (Entry[]) new XTreeDirectoryEntry[capacity];
    }
    for(int i = 0; i < numEntries; i++) {
      SpatialEntry s = isLeaf() ? new SpatialPointLeafEntry() : new XTreeDirectoryEntry();
      s.readExternal(in);
      entries[i] = s;
    }
  }

  /**
   * This node is a supernode and we only write it to file in order to be able
   * to re-load it in another session. It can be loaded by
   * {@link #readSuperNode}.
   * 
   * @param out object output
   */
  public void writeSuperNode(ObjectOutput out) throws IOException {
    if(!isSuperNode()) {
      throw new IllegalStateException("Cannot write as non-super node via writeSuperNode()");
    }
    // write header
    writeExternal(out);
    for(Entry entry : entries) {
      if(entry == null) {
        break;
      }
      entry.writeExternal(out);
    }
  }

  /**
   * Reads the id of this supernode, the numEntries and the entries array from
   * the specified stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @param tree the tree this supernode is to be assigned to
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   * @throws IllegalStateException if the parameters of the file's supernode do
   *         not match this
   */
  public <T extends AbstractXTree<N>> void readSuperNode(ObjectInput in, T tree) throws IOException, ClassNotFoundException {
    readExternal(in);
    if(capacity_to_be_filled <= 0 || !isSuperNode()) {
      throw new IllegalStateException("This node does not appear to be a supernode");
    }
    if(isLeaf) {
      throw new IllegalStateException("A supernode is cannot be a leaf");
    }
    // TODO: verify
    entries = new Entry[capacity_to_be_filled];
    // old way:
    // entries = (E[]) new XDirectoryEntry[capacity_to_be_filled];
    capacity_to_be_filled = 0;
    for(int i = 0; i < numEntries; i++) {
      SpatialEntry s = new SpatialDirectoryEntry();
      s.readExternal(in);
      entries[i] = s;
    }
    N n = tree.getSupernodes().put((long) getPageID(), (N) this);
    if(n != null) {
      Logging.getLogger(this.getClass()).fine("Warning: this supernode should only be read once. Now a node of size " + entries.length + " has replaced a node of size " + n.entries.length + " for id " + getPageID());
    }
  }

  /**
   * @return A list of all contained children in this node. In contrast to
   *         {@link #getEntries()} this listing ONLY contains existing children
   *         and not empty slots for filling up the capacity.
   */
  public List<Entry> getChildren() {
    List<Entry> children = new ArrayList<>(getNumEntries());
    for(int i = 0; i < getNumEntries(); i++) {
      children.add(entries[i]);
    }
    return children;
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly
   * set. Subclasses may need to overwrite this method.
   * 
   * @param parent the parent holding the entry representing this node
   * @param index the index of the entry in the parents child array
   */
  @Override
  protected void integrityCheckParameters(N parent, int index) {
    // test if mbr is correctly set
    SpatialEntry entry = parent.getEntry(index);
    HyperBoundingBox mbr = computeMBR();

    if(/*entry.getMBR() == null && */ mbr == null) {
      return;
    }
    if(!SpatialUtil.equals(entry, mbr)) {
      String soll = mbr.toString();
      String ist = (new HyperBoundingBox(entry)).toString();
      throw new RuntimeException("Wrong MBR in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
    if(isSuperNode() && isLeaf()) {
      throw new RuntimeException("Node " + toString() + " is a supernode and a leaf");
    }
    if(isSuperNode() && !parent.isSuperNode() && parent.getCapacity() >= getCapacity()) {
      throw new RuntimeException("Supernode " + toString() + " has capacity " + getCapacity() + "; its non-super parent node has capacity " + parent.getCapacity());
    }
  }
}