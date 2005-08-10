package de.lmu.ifi.dbs.utilities.heap;

/**
 * Defines the requirements for an object that can be used as a node in a persistent Heap.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface PersistentHeapNode extends HeapNode {
  /**
   * Returns the size of this node in Bytes if it is written to disk.
   *
   * @return the size of this node in Bytes
   */
  double size();

  /**
   * Returns the index of the deap holding this node in the persistent heap.
   * @return the index of the deap holding this node in the persistent heap
   */
  int getPersistentHeapIndex();

  /**
   * Sets the index of the deap holding this node in the persistent heap.
   * @param index the index to be set
   */
  void setPersistentHeapIndex(int index);
}
