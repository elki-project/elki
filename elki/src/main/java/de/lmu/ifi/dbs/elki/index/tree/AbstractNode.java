package de.lmu.ifi.dbs.elki.index.tree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.persistent.AbstractExternalizablePage;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract superclass for nodes in an tree based index structure.
 * 
 * @author Elke Achtert
 * @param <E> the type of Entry used in the index
 */
public abstract class AbstractNode<E extends Entry> extends AbstractExternalizablePage implements Node<E> {
  /**
   * The number of entries in this node.
   */
  protected int numEntries;

  /**
   * The entries (children) of this node.
   */
  protected E[] entries;

  /**
   * Indicates whether this node is a leaf node.
   */
  protected boolean isLeaf;

  /**
   * Empty constructor for Externalizable interface.
   */
  public AbstractNode() {
    super();
  }

  /**
   * Creates a new Node with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   * @param eclass Entry class, to initialize array storage
   */
  public AbstractNode(int capacity, boolean isLeaf, Class<? super E> eclass) {
    super();
    this.numEntries = 0;
    Class<E> cls = ClassGenericsUtil.uglyCastIntoSubclass(eclass);
    this.entries = ClassGenericsUtil.newArrayOfNull(capacity, cls);
    this.isLeaf = isLeaf;
  }

  @Override
  public final Enumeration<IndexTreePath<E>> children(final IndexTreePath<E> parentPath) {
    return new Enumeration<IndexTreePath<E>>() {
      int count = 0;

      @Override
      public boolean hasMoreElements() {
        return count < numEntries;
      }

      @Override
      public IndexTreePath<E> nextElement() {
        synchronized(AbstractNode.this) {
          if(count < numEntries) {
            return new IndexTreePath<>(parentPath, entries[count], count++);
          }
        }
        throw new NoSuchElementException();
      }
    };
  }

  @Override
  public final int getNumEntries() {
    return numEntries;
  }

  @Override
  public final boolean isLeaf() {
    return isLeaf;
  }

  @Override
  public final E getEntry(int index) {
    return entries[index];
  }

  /**
   * Calls the super method and writes the id of this node, the numEntries and
   * the entries array to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeBoolean(isLeaf);
    out.writeInt(numEntries);
    // Entries will be written in subclasses
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
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    isLeaf = in.readBoolean();
    numEntries = in.readInt();
    // Entries will be read in subclasses
  }

  /**
   * Returns <code>true</code> if <code>this == o</code> has the value
   * <code>true</code> or o is not null and o is of the same class as this
   * instance and <code>super.equals(o)</code> returns <code>true</code> and
   * both nodes are of the same type (leaf node or directory node) and have
   * contain the same entries, <code>false</code> otherwise.
   * 
   * @see de.lmu.ifi.dbs.elki.persistent.AbstractExternalizablePage#equals(Object)
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    if(!super.equals(o)) {
      return false;
    }

    final AbstractNode<?> that = (AbstractNode<?>) o;

    return isLeaf == that.isLeaf && numEntries == that.numEntries && Arrays.equals(entries, that.entries);
  }

  /**
   * Returns a string representation of this node.
   * 
   * @return the type of this node (LeafNode or DirNode) followed by its id
   */
  @Override
  public final String toString() {
    if(isLeaf) {
      return "LeafNode " + getPageID();
    }
    else {
      return "DirNode " + getPageID();
    }
  }

  /**
   * Adds a new leaf entry to this node's children and returns the index of the
   * entry in this node's children array. An UnsupportedOperationException will
   * be thrown if the entry is not a leaf entry or this node is not a leaf node.
   * 
   * @param entry the leaf entry to be added
   * @return the index of the entry in this node's children array
   * @throws UnsupportedOperationException if entry is not a leaf entry or this
   *         node is not a leaf node
   */
  @Override
  public final int addLeafEntry(E entry) {
    // entry is not a leaf entry
    if(!entry.isLeafEntry()) {
      throw new UnsupportedOperationException("Entry is not a leaf entry!");
    }
    // this is a not a leaf node
    if(!isLeaf()) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    // leaf node
    return addEntry(entry);
  }

  /**
   * Adds a new directory entry to this node's children and returns the index of
   * the entry in this node's children array. An UnsupportedOperationException
   * will be thrown if the entry is not a directory entry or this node is not a
   * directory node.
   * 
   * @param entry the directory entry to be added
   * @return the index of the entry in this node's children array
   * @throws UnsupportedOperationException if entry is not a directory entry or
   *         this node is not a directory node
   */
  @Override
  public final int addDirectoryEntry(E entry) {
    // entry is not a directory entry
    if(entry.isLeafEntry()) {
      throw new UnsupportedOperationException("Entry is not a directory entry!");
    }
    // this is a not a directory node
    if(isLeaf()) {
      throw new UnsupportedOperationException("Node is not a directory node!");
    }

    return addEntry(entry);
  }

  /**
   * Deletes the entry at the specified index and shifts all entries after the
   * index to left.
   * 
   * @param index the index at which the entry is to be deleted
   * @return true id deletion was successful
   */
  public boolean deleteEntry(int index) {
    System.arraycopy(entries, index + 1, entries, index, numEntries - index - 1);
    entries[--numEntries] = null;
    return true;
  }

  /**
   * Deletes all entries in this node.
   */
  public final void deleteAllEntries() {
    if(numEntries > 0) {
      Arrays.fill(entries, null);
      this.numEntries = 0;
    }
  }

  /**
   * Returns the capacity of this node (i.e. the length of the entries arrays).
   * 
   * @return the capacity of this node
   */
  public final int getCapacity() {
    return entries.length;
  }

  /**
   * Returns a list of the entries.
   * 
   * @return a list of the entries
   * 
   * @deprecated Using this method means an extra copy - usually at the cost of
   *             performance.
   */
  @Deprecated
  public final List<E> getEntries() {
    List<E> result = new ArrayList<>(numEntries);
    for(E entry : entries) {
      if(entry != null) {
        result.add(entry);
      }
    }
    return result;
  }

  /**
   * Adds the specified entry to the entries array and increases the numEntries
   * counter.
   * 
   * @param entry the entry to be added
   * @return the current number of entries
   */
  private int addEntry(E entry) {
    entries[numEntries++] = entry;
    return numEntries - 1;
  }

  /**
   * Remove entries according to the given mask.
   * 
   * @param mask Mask to remove
   */
  public void removeMask(long[] mask) {
    int dest = BitsUtil.nextSetBit(mask, 0);
    if(dest < 0) {
      return;
    }
    int src = BitsUtil.nextSetBit(mask, dest);
    while(src < numEntries) {
      if(!BitsUtil.get(mask, src)) {
        entries[dest] = entries[src];
        dest++;
      }
      src++;
    }
    int rm = src - dest;
    while(dest < numEntries) {
      entries[dest] = null;
      dest++;
    }
    numEntries -= rm;
  }

  /**
   * Redistribute entries according to the given sorting.
   * 
   * @param newNode Node to split to
   * @param sorting Sorting to use
   * @param splitPoint Split point
   */
  public final void splitTo(AbstractNode<E> newNode, List<E> sorting, int splitPoint) {
    assert (isLeaf() == newNode.isLeaf());
    deleteAllEntries();
    StringBuilder msg = LoggingConfiguration.DEBUG ? new StringBuilder("\n") : null;

    for(int i = 0; i < splitPoint; i++) {
      addEntry(sorting.get(i));
      if(msg != null) {
        msg.append("n_").append(getPageID()).append(" ");
        msg.append(sorting.get(i)).append("\n");
      }
    }

    for(int i = splitPoint; i < sorting.size(); i++) {
      newNode.addEntry(sorting.get(i));
      if(msg != null) {
        msg.append("n_").append(newNode.getPageID()).append(" ");
        msg.append(sorting.get(i)).append("\n");
      }
    }
    if(msg != null) {
      Logging.getLogger(this.getClass().getName()).fine(msg.toString());
    }
  }

  /**
   * Splits the entries of this node into a new node using the given assignments
   * 
   * @param newNode Node to split to
   * @param assignmentsToFirst the assignment to this node
   * @param assignmentsToSecond the assignment to the new node
   */
  public final void splitTo(AbstractNode<E> newNode, List<E> assignmentsToFirst, List<E> assignmentsToSecond) {
    assert (isLeaf() == newNode.isLeaf());
    deleteAllEntries();
    StringBuilder msg = LoggingConfiguration.DEBUG ? new StringBuilder() : null;

    // assignments to this node
    for(E entry : assignmentsToFirst) {
      if(msg != null) {
        msg.append("n_").append(getPageID()).append(" ").append(entry).append("\n");
      }
      addEntry(entry);
    }

    // assignments to the new node
    for(E entry : assignmentsToSecond) {
      if(msg != null) {
        msg.append("n_").append(newNode.getPageID()).append(" ").append(entry).append("\n");
      }
      newNode.addEntry(entry);
    }
    if(msg != null) {
      Logging.getLogger(this.getClass()).fine(msg.toString());
    }
  }

  /**
   * Splits the entries of this node into a new node using the given assignments
   * 
   * @param newNode Node to split to
   * @param assignment Assignment mask
   */
  public final void splitByMask(AbstractNode<E> newNode, long[] assignment) {
    assert (isLeaf() == newNode.isLeaf());
    int dest = BitsUtil.nextSetBit(assignment, 0);
    if(dest < 0) {
      throw new AbortException("No bits set in splitting mask.");
    }
    // FIXME: use faster iteration/testing
    int pos = dest;
    while(pos < numEntries) {
      if(BitsUtil.get(assignment, pos)) {
        // Move to new node
        newNode.addEntry(entries[pos]);
      }
      else {
        // Move to new position
        entries[dest] = entries[pos];
        dest++;
      }
      pos++;
    }
    final int rm = numEntries - dest;
    while(dest < numEntries) {
      entries[dest] = null;
      dest++;
    }
    numEntries -= rm;
  }
}