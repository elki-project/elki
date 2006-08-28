package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.persistent.PageFile;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

/**
 * Abstract superclass for nodes in an index structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractNode<N extends AbstractNode<N, E>, E extends Entry> extends AbstractLoggable implements Node<E> {

  /**
   * The file storing the Index Structure.
   */
  private PageFile<N> file;

  /**
   * The unique id if this node.
   */
  private Integer id;

  /**
   * The number of entries in this node.
   */
  private int numEntries;

  /**
   * The entries (children) of this node.
   */
  private E[] entries;

  /**
   * Indicates wether this node is a leaf node.
   */
  private boolean isLeaf;

  /**
   * The dirty flag of this node.
   */
  private boolean dirty;

  /**
   * Empty constructor for Externalizable interface.
   */
  public AbstractNode() {
    super(LoggingConfiguration.DEBUG);
  }

  /**
   * Creates a new Node with the specified parameters.
   *
   * @param file     the file storing the index
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public AbstractNode(PageFile<N> file, int capacity, boolean isLeaf) {
    this();
    this.file = file;
    this.id = null;
    this.numEntries = 0;
    //noinspection unchecked
    this.entries = (E[]) new Entry[capacity];
    this.isLeaf = isLeaf;
  }

  /**
   * @see Node#children(IndexPath)
   */
  public final Enumeration<IndexPath<E>> children(final IndexPath<E> parentPath) {
    return new Enumeration<IndexPath<E>>() {
      int count = 0;

      public boolean hasMoreElements() {
        return count < numEntries;
      }

      public IndexPath<E> nextElement() {
        synchronized (AbstractNode.this) {
          if (count < numEntries) {
            return parentPath.pathByAddingChild(
                new IndexPathComponent<E>(entries[count], count++));
          }
        }
        throw new NoSuchElementException();
      }
    };
  }

  /**
   * @see Node#getNumEntries()
   */
  public final int getNumEntries() {
    return numEntries;
  }

  /**
   * @see Node#isLeaf()
   */
  public final boolean isLeaf() {
    return isLeaf;
  }

  /**
   * @see Node#getEntry(int)
   */
  public final E getEntry(int index) {
    return entries[index];
  }

  /**
   * @see de.lmu.ifi.dbs.persistent.Page#getID()
   */
  public final Integer getID() {
    return id;
  }

  /**
   * @see de.lmu.ifi.dbs.persistent.Page#setID(int)
   */
  public final void setID(int id) {
    this.id = id;
  }

  /**
   * Returns the file storing the Index Structure.
   *
   * @return the file storing the Index Structure
   */
  public PageFile<N> getFile() {
    return file;
  }

  /**
   * @see de.lmu.ifi.dbs.persistent.Page#setFile(de.lmu.ifi.dbs.persistent.PageFile)
   */
  public final void setFile(PageFile file) {
    //noinspection unchecked
    this.file = file;
  }

  /**
   * @see de.lmu.ifi.dbs.persistent.Page#isDirty()
   */
  public final boolean isDirty() {
    return dirty;
  }

  /**
   * @see de.lmu.ifi.dbs.persistent.Page#setDirty(boolean)
   */
  public final void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  /**
   * Writes the id of this node, the numEntries and the entries array to the
   * specified stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(id);
    out.writeInt(numEntries);
    out.writeObject(entries);
  }

  /**
   * Reads the id of this node, the numEntries and the entries array from the
   * specified stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    id = in.readInt();
    numEntries = in.readInt();
    entries = (E[]) in.readObject();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the object to be tested
   * @return true, if o is an AbstractNode and has the same
   *         id and the same entries as this node.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final AbstractNode that = (AbstractNode) o;

    if (isLeaf != that.isLeaf) return false;
    if (numEntries != that.numEntries) return false;
    if (!Arrays.equals(entries, that.entries)) return false;
    return !(id != null ? !id.equals(that.id) : that.id != null);
  }

  /**
   * Returns as hash code value for this node the id of this node.
   *
   * @return the id of this node
   */
  public final int hashCode() {
    return (id != null ? id.hashCode() : 0);
  }

  /**
   * Returns a string representation of this node.
   *
   * @return a string representation of this node
   */
  public final String toString() {
    if (isLeaf)
      return "LeafNode " + id;
    else
      return "DirNode " + id;
  }

  /**
   * Adds a new leaf entry to this node's children
   * and returns the index of the entry in this node's children array.
   * An UnsupportedOperationException will be thrown if the entry is not a leaf entry or
   * this node is not a leaf node.
   *
   * @param entry the leaf entry to be added
   * @return the index of the entry in this node's children array
   * @throws UnsupportedOperationException if entry is not a leaf entry or
   *                                       this node is not a leaf node
   */
  public final int addLeafEntry(E entry) {
    // entry is not a leaf entry
    if (! entry.isLeafEntry()) {
      throw new UnsupportedOperationException("Entry is not a leaf entry!");
    }
    // this is a not a leaf node
    if (!isLeaf()) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    // leaf node
    return addEntry(entry);
  }

  /**
   * Adds a new directory entry to this node's children
   * and returns the index of the entry in this node's children array.
   * An UnsupportedOperationException will be thrown if the entry is not a directory entry or
   * this node is not a directory node.
   *
   * @param entry the directory entry to be added
   * @return the index of the entry in this node's children array
   * @throws UnsupportedOperationException if entry is not a directory entry or
   *                                       this node is not a directory node
   */
  public final int addDirectoryEntry(E entry) {
    // entry is not a directory entry
    if (entry.isLeafEntry()) {
      throw new UnsupportedOperationException("Entry is not a directory entry!");
    }
    // this is a not a directory node
    if (isLeaf()) {
      throw new UnsupportedOperationException("Node is not a directory node!");
    }

    return addEntry(entry);
  }

  /**
   * Deletes the entry at the specified index and shifts all
   * entries after the index to left.
   *
   * @param index the index at which the entry is to be deketed
   */
  public boolean deleteEntry(int index) {
    System.arraycopy(entries, index + 1, entries, index, numEntries - index - 1);
    entries[--numEntries] = null;
    return true;
  }

  /**
   * Deletes all  entries in this node.
   */
  protected final void deleteAllEntries() {
    //noinspection unchecked
    this.entries = (E[]) new Entry[entries.length];
    this.numEntries = 0;
  }

  /**
   * Increases the length of the entries array to entries.length + 1.
   */
  public final void increaseEntries() {
    E[] tmp = entries;
    //noinspection unchecked
    entries = (E[]) new Entry[tmp.length + 1];
    System.arraycopy(tmp, 0, entries, 0, tmp.length);
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
   */
  public final List<E> getEntries() {
    List<E> result = new ArrayList<E>();
    for (E entry : entries) {
      result.add(entry);
    }
    return result;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected abstract N createNewLeafNode(int capacity);

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected abstract N createNewDirectoryNode(int capacity);

  /**
   * Adds the specified entry to the entries array and increases the
   * numEntries counter.
   *
   * @param entry the entry to be added
   */
  private int addEntry(E entry) {
    entries[numEntries++] = entry;
    return numEntries - 1;
  }

}