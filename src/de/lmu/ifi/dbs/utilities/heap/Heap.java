package de.lmu.ifi.dbs.utilities.heap;

import de.lmu.ifi.dbs.utilities.Identifiable;

import java.io.Serializable;

/**
 * Encapsulates methods common to all heaps.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Heap<K extends Comparable<K>, V extends Identifiable> extends Serializable {

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  void addNode(final HeapNode<K,V> node);

  /**
   * Retrieves and removes the minimum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  HeapNode<K,V> getMinNode();

  /**
   * Indicates wether this heap ist empty.
   *
   * @return true if this heap is empty, false otherwise
   */
  boolean isEmpty();

  /**
   * Returns the current index of the specified value in this heap.
   * @param value the value for which the index should be returned
   * @return the current index of the specified value in this heap
   */
  Integer getIndexOf(V value);

  /**
   * Returns the node at the specified index.
   *
   * @param index the index of the node to be returned
   * @return the node at the specified index
   */
  public HeapNode<K, V> getNodeAt(final int index);

  /**
   * Moves up a node at the specified index until it satisfies the heaporder.
   *
   * @param index the index of the node to be moved up.
   */
  public void flowUp(int index);

  /**
   * Returns the size of this heap.
   * @return the size of this heap
   */
  public int size();
}
