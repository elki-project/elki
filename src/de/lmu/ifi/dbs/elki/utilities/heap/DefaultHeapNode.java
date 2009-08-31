package de.lmu.ifi.dbs.elki.utilities.heap;

import de.lmu.ifi.dbs.elki.utilities.Identifiable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * A default implementation of an object that can be stored in a heap.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Elke Achtert 
 */
@SuppressWarnings("serial")
public class DefaultHeapNode<K extends Comparable<K>, V extends Identifiable> extends Pair<K,V> implements HeapNode<K, V> {
  /**
   * Empty constructor for serialization purposes.
   */
  public DefaultHeapNode() {
	  this(null,null);
  }

  /**
   * Creates a new heap node with the specified parameters.
   *
   * @param key   the key of this heap node
   * @param value the value of this heap node
   */
  public DefaultHeapNode(final K key, final V value) {
    super(key,value);
  }

  /**
   * Compares this HeapNode with the specified HeapNode.
   *
   * @param heapNode HeapNode to be compared
   * @return a negative integer, zero, or a positive integer as this object is
   *         less than, equal to, or greater than the specified object.
   */
  public int compareTo(HeapNode<K, V> heapNode) {
    int comp = this.getKey().compareTo(heapNode.getKey());
    if (comp != 0) {
      return comp;
    }

    return this.getValue().getID().compareTo(heapNode.getValue().getID());
  }

  /**
   * Returns the value of this heap node.
   *
   * @return the value of this heap node
   */
  public V getValue() {
    return getSecond();
  }

  /**
   * Sets the value of this HeapNode.
   *
   * @param value the value to be set
   */
  public void setValue(V value) {
    this.setSecond(value);
  }

  /**
   * Returns the key of this heap node.
   *
   * @return the key of this heap node
   */
  public K getKey() {
    return getFirst();
  }

  /**
   * Sets the key of this HeapNode.
   *
   * @param key the key to be set
   */
  public void setKey(K key) {
    this.setFirst(key);
  }

  /**
   * Returns a string representation of this heap node.
   *
   * @return a string representation of this heap node
   */
  @Override
  public String toString() {
    return getKey() + ":" + getValue();
  }
}
