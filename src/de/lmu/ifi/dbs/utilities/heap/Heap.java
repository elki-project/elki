package de.lmu.ifi.dbs.utilities.heap;

/**
 * Encapsulates methods common to all heaps.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Heap {

  /**
   * Adds a node to this heap.
   *
   * @param node the node to be added
   */
  void addNode(final HeapNode node);

  /**
   * Retrieves and removes the minimum node of this heap.
   * If the heap is empty, null will be returned.
   *
   * @return the minimum node of this heap, null in case of emptyness
   */
  HeapNode getMinNode();

  /**
   * Indicates wether this heap ist empty.
   *
   * @return true if this heap is empty, false otherwise
   */
  boolean isEmpty();

  /**
   * Clears this heap.
   */
  void clear();
}
