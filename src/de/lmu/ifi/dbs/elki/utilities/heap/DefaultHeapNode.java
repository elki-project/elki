package de.lmu.ifi.dbs.elki.utilities.heap;

import de.lmu.ifi.dbs.elki.utilities.Identifiable;

/**
 * A default implementation of an object that can be stored in a heap.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Elke Achtert 
 */
@SuppressWarnings("serial")
public class DefaultHeapNode<K extends Comparable<K>, V extends Identifiable> implements HeapNode<K, V> {

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
	  // empty constructor
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
   * @return a negative integer, zero, or a positive integer as this object is
   *         less than, equal to, or greater than the specified object.
   */
  public int compareTo(HeapNode<K, V> heapNode) {
    int comp = this.key.compareTo(heapNode.getKey());
    if (comp != 0)
      return comp;

    return this.value.getID().compareTo(heapNode.getValue().getID());
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
  @Override
  public String toString() {
    return key + ":" + value;
  }
}
