package de.lmu.ifi.dbs.utilities.heap;

import java.io.DataOutputStream;
import java.io.DataInputStream;

/**
 * Defines the requirements of an object that can be used as a key in a persistent heap node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface PersistentKey extends Comparable<PersistentKey> {
  /**
   * Returns the size of this key in Bytes.
   * @return the size of this key in Bytes
   */
  int size();

  /**
   * Writes this key to the specified output stream
   * @param ds the output stream to write this key to
   */
  void write(DataOutputStream ds);

  /**
   * Reads the specified input stream and initializes this key
   * @param ds the input stream to read
   */
  void read(DataInputStream ds);
}
