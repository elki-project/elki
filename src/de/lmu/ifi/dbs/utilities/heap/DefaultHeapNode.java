package de.lmu.ifi.dbs.utilities.heap;

/**
 * A default implementation of an object that can be stored in a heap.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultHeapNode<K extends Comparable<K>, V extends Comparable<V>>
implements HeapNode<K, V> {

  /**
   * The key of this heap node.
   */
  private K key;

  /**
   * The value of this heap node.
   */
  private V value;

  /**
   * The index of this heap node in the heap.
   */
  private int index;

  /**
   * Empty constructor.
   */
  public DefaultHeapNode() {
  }


  /**
   * Creates a new heap node with the specified parameters.
   *
   * @param key   the key of this heap node
   * @param value the value of this heap node
   */
  public DefaultHeapNode(final K key, final V value) {
    this.key = key;
    this.value = value;
    this.index = -1;
  }

  /**
   * Compares this HeapNode with the specified HeapNode.
   *
   * @param heapNode HeapNode to be compared
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(HeapNode<K, V> heapNode) {
    int comp = this.key.compareTo(heapNode.getKey());
    if (comp != 0) return comp;

    return (this.value.compareTo(heapNode.getValue()));
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
   * Returns the value of this heap node.
   *
   * @return the value of this heap node
   */
  public V getValue() {
    return value;
  }

  /**
   * Returns the key of this heap node.
   *
   * @return the key of this heap node
   */
  public K getKey() {
    return key;
  }

  /**
   * Returns a string representation of this heap node.
   *
   * @return a string representation of this heap node
   */
  public String toString() {
    return ""+value;
  }
}

