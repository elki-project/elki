/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import elki.logging.Logging;
import elki.logging.LoggingConfiguration;
import elki.persistent.AbstractExternalizablePage;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.exceptions.AbortException;

/**
 * Abstract superclass for nodes in an tree based index structure.
 *
 * @author Elke Achtert
 *
 * @since 0.1
 *
 * @param <E> the type of Entry used in the index
 */
public abstract class AbstractNode<E> extends AbstractExternalizablePage implements Node<E> {
  /**
   * The number of entries in this node.
   */
  protected int numEntries;

  /**
   * The entries (children) of this node.
   */
  protected Object[] entries;

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
   */
  public AbstractNode(int capacity, boolean isLeaf) {
    super();
    this.numEntries = 0;
    this.entries = new Object[capacity];
    this.isLeaf = isLeaf;
  }

  @Override
  public final Iterator<IndexTreePath<E>> children(final IndexTreePath<E> parentPath) {
    return new Iterator<IndexTreePath<E>>() {
      int count = 0;

      @Override
      public boolean hasNext() {
        return count < numEntries;
      }

      @Override
      public IndexTreePath<E> next() {
        synchronized(AbstractNode.this) {
          if(count < numEntries) {
            return new IndexTreePath<>(parentPath, getEntry(count), count++);
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

  @SuppressWarnings("unchecked")
  @Override
  public final E getEntry(int index) {
    return (E) entries[index];
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
   * Returns a string representation of this node.
   *
   * @return the type of this node (LeafNode or DirNode) followed by its id
   */
  @Override
  public String toString() {
    return (isLeaf ? "LeafNode " : "DirNode ") + getPageID();
  }

  @Override
  public final int addEntry(E entry) {
    if((entry instanceof LeafEntry) != isLeaf()) {
      throw new IllegalStateException("Inserting " + (entry instanceof LeafEntry ? "leaf" : "directory") //
          + " entry into a " + (isLeaf() ? "leaf" : "directory") + "node.");
    }
    entries[numEntries] = entry;
    return numEntries++;
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
        entries[dest++] = entries[src];
      }
      src++;
    }
    int rm = src - dest;
    while(dest < numEntries) {
      entries[dest++] = null;
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
    StringBuilder msg = LoggingConfiguration.DEBUG ? new StringBuilder(1000) : null;

    for(int i = 0; i < splitPoint; i++) {
      addEntry(sorting.get(i));
      if(msg != null) {
        msg.append("n_").append(getPageID()).append(' ').append(sorting.get(i)).append('\n');
      }
    }

    for(int i = splitPoint; i < sorting.size(); i++) {
      newNode.addEntry(sorting.get(i));
      if(msg != null) {
        msg.append("n_").append(newNode.getPageID()).append(' ').append(sorting.get(i)).append('\n');
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
    StringBuilder msg = LoggingConfiguration.DEBUG ? new StringBuilder(1000) : null;

    // assignments to this node
    for(E entry : assignmentsToFirst) {
      if(msg != null) {
        msg.append("n_").append(getPageID()).append(' ').append(entry).append('\n');
      }
      addEntry(entry);
    }

    // assignments to the new node
    for(E entry : assignmentsToSecond) {
      if(msg != null) {
        msg.append("n_").append(newNode.getPageID()).append(' ').append(entry).append('\n');
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
    for(int pos = dest; pos < numEntries; pos++) {
      if(BitsUtil.get(assignment, pos)) {
        // Move to new node
        newNode.addEntry(getEntry(pos));
      }
      else {
        // Move to new position
        entries[dest++] = entries[pos];
      }
    }
    final int rm = numEntries - dest;
    while(dest < numEntries) {
      entries[dest++] = null;
    }
    numEntries -= rm;
  }
}
