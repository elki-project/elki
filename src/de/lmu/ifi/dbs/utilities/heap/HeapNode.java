package de.lmu.ifi.dbs.utilities.heap;

/**
 * Defines the requirements for an object that can be used as a node in a Heap.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface HeapNode extends Comparable<HeapNode> {


  /**
   * Sets the index of this node in the heap.
   * @param index the ondex to be set
   */
  void setIndex(int index);

  /**
   * Returns the index of this node in the heap.
   * @return the index of this node in the heap
   */
  int getIndex();

}
