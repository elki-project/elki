package de.lmu.ifi.dbs.elki.index.tree;

import de.lmu.ifi.dbs.elki.persistent.AbstractPage;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Abstract superclass for nodes in an tree based index structure.
 * 
 * @author Elke Achtert
 * @param <N> the type of Node used in the index
 * @param <E> the type of Entry used in the index
 */
public abstract class AbstractNode<N extends AbstractNode<N, E>, E extends Entry> extends AbstractPage<N> implements Node<N, E> {
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
   * @param file the file storing the index
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public AbstractNode(PageFile<N> file, int capacity, boolean isLeaf) {
    super(file);
    this.numEntries = 0;
    this.entries = ClassGenericsUtil.newArrayOfNull(capacity, Entry.class);
    this.isLeaf = isLeaf;
  }

  public final Enumeration<TreeIndexPath<E>> children(final TreeIndexPath<E> parentPath) {
    return new Enumeration<TreeIndexPath<E>>() {
      int count = 0;

      public boolean hasMoreElements() {
        return count < numEntries;
      }

      public TreeIndexPath<E> nextElement() {
        synchronized(AbstractNode.this) {
          if(count < numEntries) {
            return parentPath.pathByAddingChild(new TreeIndexPathComponent<E>(entries[count], count++));
          }
        }
        throw new NoSuchElementException();
      }
    };
  }

  public final int getNumEntries() {
    return numEntries;
  }

  public final boolean isLeaf() {
    return isLeaf;
  }

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
    out.writeInt(numEntries);
    out.writeObject(entries);
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
    numEntries = in.readInt();
    entries = (E[]) in.readObject();
  }

  /**
   * Returns <code>true</code> if <code>this == o</code> has the value
   * <code>true</code> or o is not null and o is of the same class as this
   * instance and <code>super.equals(o)</code> returns <code>true</code> and
   * both nodes are of the same type (leaf node or directory node) and have
   * contain the same entries, <code>false</code> otherwise.
   * 
   * @see de.lmu.ifi.dbs.elki.persistent.AbstractPage#equals(Object)
   */
  @SuppressWarnings("unchecked")
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

    final N that = (N) o;

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
      return "LeafNode " + getID();
    }
    else {
      return "DirNode " + getID();
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
  protected final void deleteAllEntries() {
    this.entries = ClassGenericsUtil.newArrayOfNull(entries.length, Entry.class);
    this.numEntries = 0;
  }

  /**
   * Increases the length of the entries array to entries.length + 1.
   */
  public final void increaseEntries() {
    E[] tmp = entries;
    entries = ClassGenericsUtil.newArrayOfNull(tmp.length + 1, Entry.class);
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
    for(E entry : entries) {
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

}