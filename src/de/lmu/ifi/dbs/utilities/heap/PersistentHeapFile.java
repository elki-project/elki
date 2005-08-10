package de.lmu.ifi.dbs.utilities.heap;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A PersistentHeapFile object stores the pages of a persistent heap.
 * In this sense a page is a deap in a persistent heap.
 * <p/>
 * Structure of the File
 * <p/>
 * -- Header (8 Byte) --
 * int fileVersion </br>
 * int length of a deap </br>
 * <p/>
 * -- Body --
 * a sequence of deaps one after another with:
 * int index - index of the deap in the persistent heap </br>
 * int cacheIndex - index of the deap in the persistent heap's cache </br>
 * int lastHeap - the last entry in the deap </br>
 * - for(i = 0; i < length; i++) </br>
 * - persistent heap node </br>
 * <p/>
 * deapSize = 12 + length * (size of persistent heap node);
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class PersistentHeapFile {

  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level level = Level.OFF;

  /**
   * magic number
   */
  private static final int FILE_VERSION = 060676002;

  /**
   * The file storing the nodes.
   */
  private final RandomAccessFile file;

  /**
   * The I/O-Access of this file.
   */
  private int ioAccess;

  /**
   * The size of a deap in byte.
   */
  private int deapSize;

  /**
   * The capacity of a deap.
   */
  private int deapCapacity;

  /**
   * The size in bytes of the header of this file.
   */
  private final int headerSize;

  /**
   * The buffer for reading the bytes of the underlying file.
   */
  private final byte[] buffer;

  /**
   * The class of the persistent heap nodes stored in this file.
   */
  private Class persistenHeapNode;

  /**
   * Creates a new PersistentHeapFile with the specified parameters.
   *
   * @param fileName          the name of this file
   * @param deapCapacity      the capacity of one deap (i.e. how many PersistentHeapNodes
   *                          fit in one deap)
   * @param nodeSize          the size of one PersistentHeapNode
   * @param persistenHeapNode the class name of the PersistentHeapNodes stored in the heap
   */
  public PersistentHeapFile(String fileName, int deapCapacity, int nodeSize, Class persistenHeapNode) {
    initLogger();
    try {
      this.headerSize = 8;

      File fileTest = new File(fileName);
      this.file = new RandomAccessFile(fileTest, "rw");
      file.setLength(0);

      this.deapCapacity = deapCapacity;
      this.deapSize = deapCapacity * nodeSize + 8;
      this.persistenHeapNode = persistenHeapNode;
      this.buffer = new byte[this.deapSize];
      this.ioAccess = 0;

      // writing header
      file.seek(0);
      file.writeInt(FILE_VERSION);
      file.writeInt(this.deapCapacity);
    }
    catch (FileNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
    catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Writes the specified deap to disk.
   *
   * @param deap the deap to be written
   */
  public void write(Deap deap) {
    StringBuffer msg = new StringBuffer();
    msg.append("\n*************************  WRITE ");
    msg.append(deap.getIndex());
    msg.append(" ");
    msg.append(deap);

    ioAccess++;

    try {
      ByteArrayOutputStream bs = new ByteArrayOutputStream();
      DataOutputStream ds = new DataOutputStream(bs);

      ds.writeInt(deap.getIndex());
      ds.writeInt(deap.lastHeap);
      msg.append("\nindex ");
      msg.append(deap.getIndex());
      msg.append("\nlastHeap ");
      msg.append(deap.lastHeap);

      // write entries
      for (int i = 0; i <= deap.lastHeap; i++) {
        PersistentHeapNode heapNode = (PersistentHeapNode) deap.array[i];
        heapNode.write(ds);
      }

      ds.flush();
      bs.flush();

      msg.append("\n file length ");
      msg.append(file.length());

      msg.append("\n seek ");
      msg.append(headerSize + (deapSize * deap.getIndex()));

      file.seek(headerSize + (deapSize * deap.getIndex()));
      file.write(bs.toByteArray());

      msg.append("\n file length ");
      msg.append(file.length());
      ds.close();

      logger.info(msg.toString());
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException("IOException occured ! \n " + e.getMessage());
    }
  }

  /**
   * Reads the deap with the specified index from disk.
   *
   * @param index the index of the deap to be read
   * @return the deap with the specified index from disk
   */
  public Deap read(int index) {
    ioAccess++;
    StringBuffer msg = new StringBuffer();
    msg.append("****************** READ ");
    msg.append(index);

    try {
      int fileIndex = headerSize + index * deapSize;
      msg.append("\n seek ");
      msg.append(fileIndex);
      file.seek(fileIndex);

      int read = file.read(buffer);
      if (deapSize == read) {
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(buffer));

        int deapIndex = ds.readInt();
        Deap deap = new Deap(deapCapacity, deapIndex, -1);
        deap.lastHeap = ds.readInt();

        for (int i = 0; i <= deap.lastHeap; i++) {
          PersistentHeapNode heapNode = (PersistentHeapNode) persistenHeapNode.newInstance();
          heapNode.read(ds);
          deap.array[i] = heapNode;
        }
        ds.close();

        logger.info(msg.toString());
        return deap;
      }
      else {
        throw new RuntimeException("deapSize != read: " + deapSize + " != " + read);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      logger.warning(msg.toString());
      e.fillInStackTrace();
      throw new RuntimeException("Exception during read operation of " + index +
                                 "\n" + e.getMessage());
    }
    catch (IllegalAccessException e) {
      logger.warning(msg.toString());
      e.fillInStackTrace();
      throw new RuntimeException("Exception during read operation of " + index +
                                 "\n" + e.getMessage());
    }
    catch (InstantiationException e) {
      logger.warning(msg.toString());
      e.fillInStackTrace();
      throw new RuntimeException("Exception during read operation of " + index +
                                 "\n" + e.getMessage());
    }
  }

  /**
   * Deletes the last deap in this file. The index is needed to decide weather the deap
   * to be deleted was already written to disk or not.
   *
   * @param index the index of the last deap
   */
  public void deleteLast(int index) {
    try {
      int newLength = index * deapSize + headerSize;
      // last deap was already written
      if (newLength < file.length()) {
        if (newLength == file.length() - deapSize)
          file.setLength(newLength);
        else throw new IllegalArgumentException("newLength != file.length() - deapSize: " +
                                                newLength + " != " + (file.length() - deapSize));
      }
      // last deap was not written
      else if (newLength != file.length())
      throw new IllegalArgumentException("newLength != file.length(): " +
                                                newLength + " != " + file.length());
    }
    catch (IOException e) {
      throw new RuntimeException("Exception during delete operation of last deap" +
                                 "\n" + e.getMessage());
    }
  }

  /**
   * Clears all entries in this file, only the header remains.
   */
  public void clear() {
    try {
      file.setLength(headerSize);
    }
    catch (IOException e) {
      throw new RuntimeException("Exception during clear operation" +
                                 "\n" + e.getMessage());
    }
  }

  /**
   * Returns the I/O-Access of this file.
   * @return the I/O-Access of this file
   */
  public int getIOAccess() {
    return ioAccess;
  }

  /**
   * Resets the I/O-Access of this file
   */
  public void resetIOAccess() {
    this.ioAccess = 0;
  }

  /**
   * Closes this file.
   */
  protected void close() {
    try {
      file.close();
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException("IOException during close()");
    }
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(PersistentHeapFile.class.toString());
    logger.setLevel(level);
  }
}
