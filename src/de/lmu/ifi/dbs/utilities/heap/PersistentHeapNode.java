package de.lmu.ifi.dbs.utilities.heap;

import java.io.DataOutputStream;
import java.io.DataInputStream;

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

  /**
   * Writes this persistent heap node to the specified output stream
   * @param ds the output stream to write this node to
   */
  void write(DataOutputStream ds);

  /**
   * Reads the specified input stream and initializes persistent heap node
   * @param ds the input stream to read
   */
  void read(DataInputStream ds);
}
