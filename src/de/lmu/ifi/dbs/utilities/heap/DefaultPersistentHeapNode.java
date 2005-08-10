package de.lmu.ifi.dbs.utilities.heap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A default implementation of an object that can be stored in a persistent heap.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultPersistentHeapNode implements PersistentHeapNode {
  /**
   * Logger object for logging messages.
   */
  private static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  private static Level level = Level.OFF;

  /**
   * The unique id of the underlying object of this heap node.
   */
  private int id;

  /**
   * The index of this heap node in the heap.
   */
  private int index;

  /**
   * The key of this heap node.
   */
  private PersistentKey key;

  /**
   * The index of the deap holding this node in the persistent heap.
   */
  private int persistentHeapIndex;

  private String persistentKeyClassName;

  /**
   * Empty constructor, to construct a heap node from a file.
   */
  public DefaultPersistentHeapNode() {
    initLogger();
  }

  /**
   * Creates a new DefaultPersistentHeapNode with the specified parameters.
   *
   * @param id  the unique id of the underlying object of this heap node
   * @param key the key of this heap node
   */
  public DefaultPersistentHeapNode(final int id, final PersistentKey key) {
    initLogger();
    this.id = id;
    this.key = key;
    this.index = -1;
    this.persistentHeapIndex = -1;
    this.persistentKeyClassName = key.getClass().getName();
  }

  /**
   * Sets the index of this node in the heap.
   *
   * @param index the ondex to be set
   */
  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * Returns the index of this node in the heap.
   *
   * @return the index of this node in the heap
   */
  public int getIndex() {
    return index;
  }

  /**
   * Compares this HeapNode with the specified HeapNode.
   *
   * @param heapNode HeapNode to be compared
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(HeapNode heapNode) {
    DefaultPersistentHeapNode other = (DefaultPersistentHeapNode) heapNode;
    int comp = this.key.compareTo(other.key);
    if (comp != 0) return comp;

    if (this.id < other.id) return -1;
    if (this.id > other.id) return -1;
    return 0;
  }

  /**
   * Returns the size of this node in Bytes if it is written to disk.
   *
   * @return the size of this node in Bytes
   */
  public double size() {
    // id, index, persistentHeapIndex, persistentKeyClassName, key
    return 12 + 4 + persistentKeyClassName.length() * 2 + key.size();
  }

  /**
   * Returns the index of the deap holding this node in the persistent heap.
   *
   * @return the index of the deap holding this node in the persistent heap
   */
  public int getPersistentHeapIndex() {
    return persistentHeapIndex;
  }

  /**
   * Sets the index of the deap holding this node in the persistent heap.
   *
   * @param index the index to be set
   */
  public void setPersistentHeapIndex(int index) {
    this.persistentHeapIndex = index;
  }

  /**
   * Writes this persistent heap node to the specified output stream
   *
   * @param ds the output stream to write this node to
   */
  public void write(DataOutputStream ds) {
    try {
      ds.writeInt(id);
      ds.writeInt(index);
      ds.writeInt(persistentHeapIndex);
      ds.writeInt(persistentKeyClassName.length());
      ds.writeChars(persistentKeyClassName);

      StringBuffer msg = new StringBuffer();
      msg.append("\n-- id ").append(id);
      msg.append("\n   index ").append(index);
      msg.append("\n   persistentHeapIndex ").append(persistentHeapIndex);
      msg.append("\n   persistentKeyClassName.length ").append(persistentKeyClassName.length());
      msg.append("\n   persistentKeyClassName ").append(persistentKeyClassName);
      logger.info(msg.toString());

      key.write(ds);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads the specified input stream and initializes persistent heap node
   *
   * @param ds the input stream to read
   */
  public void read(DataInputStream ds) {
    try {
      id = ds.readInt();
      index = ds.readInt();
      persistentHeapIndex = ds.readInt();

      int length = ds.readInt();
      char[] keyClassName = new char[length];
      for (int i = 0; i < length; i++) {
        keyClassName[i] = ds.readChar();
      }
      persistentKeyClassName = new String(keyClassName);
      key = (PersistentKey) Class.forName(persistentKeyClassName).newInstance();
      key.read(ds);

      StringBuffer msg = new StringBuffer();
      msg.append("\n-- id ").append(id);
      msg.append("\n   index ").append(index);
      msg.append("\n   persistentHeapIndex ").append(persistentHeapIndex);
      msg.append("\n   persistentKeyClassName.length ").append(persistentKeyClassName.length());
      msg.append("\n   persistentKeyClassName ").append(persistentKeyClassName);
      logger.info(msg.toString());

    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    catch (InstantiationException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return Integer.toString(id);
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
  }
}
