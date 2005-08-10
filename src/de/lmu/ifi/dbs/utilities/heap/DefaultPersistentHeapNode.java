package de.lmu.ifi.dbs.utilities.heap;

/**
 * A default implementation of an object that can be stored in a persistent heap.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultPersistentHeapNode implements PersistentHeapNode {
  /**
   * The unique id of the underlying object of this heap node.
   */
  private final int id;

  /**
   * The index of this heap node in the heap.
   */
  private int index;

  /**
   * The key of this heap node.
   */
  private PersistentKey key;

  /**
   * The index of the deap holding this node in the persistent heap.
   */
  private int persistentHeapIndex;

  public DefaultPersistentHeapNode(final int id, final PersistentKey key) {
    this.id = id;
    this.key = key;
    this.index = -1;
    this.persistentHeapIndex = -1;
  }

  /**
   * Sets the index of this node in the heap.
   *
   * @param index the ondex to be set
   */
  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * Returns the index of this node in the heap.
   *
   * @return the index of this node in the heap
   */
  public int getIndex() {
    return index;
  }

   /**
   * Compares this HeapNode with the specified HeapNode.
   *
   * @param heapNode HeapNode to be compared
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(HeapNode heapNode) {
    DefaultPersistentHeapNode other = (DefaultPersistentHeapNode) heapNode;
    int comp = this.key.compareTo(other.key);
    if (comp != 0) return comp;

    if (this.id < other.id) return -1;
    if (this.id > other.id) return -1;
    return 0;
  }

  /**
   * Returns the size of this node in Bytes if it is written to disk.
   *
   * @return the size of this node in Bytes
   */
  public double size() {
    // id, index, persistentHeapIndex, key
    return 12 + key.size();
  }

  /**
   * Returns the index of the deap holding this node in the persistent heap.
   *
   * @return the index of the deap holding this node in the persistent heap
   */
  public int getPersistentHeapIndex() {
    return persistentHeapIndex;
  }

  /**
   * Sets the index of the deap holding this node in the persistent heap.
   *
   * @param index the index to be set
   */
  public void setPersistentHeapIndex(int index) {
    this.persistentHeapIndex = index;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return Integer.toString(id);
  }
}
