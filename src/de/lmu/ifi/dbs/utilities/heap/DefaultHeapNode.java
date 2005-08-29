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
   * Empty constructor for serialization purposes.
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
   * Returns the value of this heap node.
   *
   * @return the value of this heap node
   */
  public V getValue() {
    return value;
  }

  /**
   * Sets the value of this HeapNode.
   *
   * @param value the value to be set
   */
  public void setValue(V value) {
    this.value = value;
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
   * Sets the key of this HeapNode.
   *
   * @param key the key to be set
   */
  public void setKey(K key) {
    this.key = key;
  }

  /**
   * Returns a string representation of this heap node.
   *
   * @return a string representation of this heap node
   */
  public String toString() {
    return key + ":" + value;
  }
}

