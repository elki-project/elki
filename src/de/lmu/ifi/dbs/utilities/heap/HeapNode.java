package de.lmu.ifi.dbs.utilities.heap;

import java.io.Serializable;

/**
 * Defines the requirements for an object that can be used as a node in a Heap.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface HeapNode<K extends Comparable<K>, V>
extends Comparable<HeapNode<K,V>>, Serializable {

  /**
   * Returns the key of this HeapNode.
   * @return the key of this HeapNode
   */
  K getKey();

  /**
   * Returns the value of this HeapNode.
   * @return the value of this HeapNode
   */
  V getValue();
}
