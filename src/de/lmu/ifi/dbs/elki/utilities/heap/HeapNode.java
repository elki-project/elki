package de.lmu.ifi.dbs.elki.utilities.heap;

import de.lmu.ifi.dbs.elki.utilities.Identifiable;

import java.io.Serializable;

/**
 * Defines the requirements for an object that can be used as a node in a Heap.
 * 
 * @param <K> Key type
 * @param <V> Value type
 * @author Elke Achtert
 */
public interface HeapNode<K extends Comparable<K>, V extends Identifiable> extends Comparable<HeapNode<K, V>>, Serializable {

  /**
   * Returns the key of this HeapNode.
   * 
   * @return the key of this HeapNode
   */
  K getKey();

  /**
   * Sets the key of this HeapNode.
   * 
   * @param key the key to be set
   */
  void setKey(K key);

  /**
   * Returns the value of this HeapNode.
   * 
   * @return the value of this HeapNode
   */
  V getValue();

  /**
   * Sets the value of this HeapNode.
   * 
   * @param value the value to be set
   */
  void setValue(V value);
}
