package de.lmu.ifi.dbs.utilities.heap;

import de.lmu.ifi.dbs.utilities.Util;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A default implementation of a PersistentKey: the key is a double value.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultPersistentKey implements PersistentKey {
  /**
   * Logger object for logging messages.
   */
  private static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  private static Level level = Level.OFF;

  /**
   * The key.
   */
  private double key;

  /**
   * Empty constructor, is needed for initialization from file.
   */
  public DefaultPersistentKey() {
    initLogger();
  }

  /**
   * Creates a new DefaultPersistentKey object with the specified key value.
   * @param key the value of this key
   */
  public DefaultPersistentKey(double key) {
    initLogger();
    this.key = key;
  }

  /**
   * Returns the size of this key in Bytes.
   * @return the size of this key in Bytes
   */
  public int size() {
    return 8;
  }

  /**
   * Compares this PersistentKey with the specified PersistentKey.
   *
   * @param key PersistentKey to be compared
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(PersistentKey key) {
    DefaultPersistentKey other = (DefaultPersistentKey) key;
    if (this.key < other.key) return -1;
    if (this.key > other.key) return +1;
    return 0;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return Util.format(key);
  }

  /**
   * Writes this key to the specified output stream
   *
   * @param ds the output stream to write this key to
   */
  public void write(DataOutputStream ds) {
    try {
      ds.writeDouble(key);
      logger.info("\n   key \" + key");
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads the specified input stream and initializes this key
   *
   * @param ds the input stream to read
   */
  public void read(DataInputStream ds) {
    try {
      this.key = ds.readDouble();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
  }


}
