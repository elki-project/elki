package de.lmu.ifi.dbs.utilities.heap;

/**
 * A default implementation of a heap node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public final class DefaultHeapNode implements HeapNode {
  /**
   * The underlying object of this heap node.
   */
  private final Comparable object;

  /**
   * The index of this heap node in the heap.
   */
  private int index;

  /**
   * The key of this heap node.
   */
  Comparable key;

  /**
   * Creates a new heap node with the specified parameters.
   *
   * @param object the underlying object of this heap node
   * @param key    the key of this heap node
   */
  public DefaultHeapNode(final Comparable object, final Comparable key) {
    this.object = object;
    this.key = key;
    this.index = -1;
  }

  /**
   * Compares this HeapNode with the specified HeapNode.
   *
   * @param heapNode HeapNode to be compared
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(HeapNode heapNode) {
    DefaultHeapNode other = (DefaultHeapNode) heapNode;
    int comp = this.key.compareTo(other.key);
    if (comp != 0) return comp;

    return this.object.compareTo(other.object);
  }

  /**
   * Sets the index of this node in the heap.
   *
   * @param index the ondex to be set
   */
  public void setIndex(final int index) {
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
   * Returns the underlying object of this heap node.
   *
   * @return the underlying object of this heap node
   */
  public Object getObject() {
    return object;
  }

  /**
   * Returns the key of this heap node.
   *
   * @return the key of this heap node
   */
  public Comparable getKey() {
    return key;
  }

  /**
   * Returns a string representation of this heap node.
   *
   * @return a string representation of this heap node
   */
  public String toString() {
    return object.toString();
  }
}

