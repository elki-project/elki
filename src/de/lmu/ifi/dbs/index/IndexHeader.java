package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.persistent.DefaultPageHeader;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Encapsulates the header information of a tree like index structure.
 * This information is needed for persistent storage.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class IndexHeader extends DefaultPageHeader {
  /**
   * The size of this header.
   */
  private static int SIZE = 16;

  /**
   * The capacity of a directory node (= 1 + maximum number of entries in a
   * directory node).
   */
  int dirCapacity;

  /**
   * The capacity of a leaf node (= 1 + maximum number of entries in a leaf
   * node).
   */
  int leafCapacity;

  /**
   * The minimum number of entries in a directory node.
   */
  int dirMinimum;

  /**
   * The minimum number of entries in a leaf node.
   */
  int leafMinimum;

  /**
   * Empty constructor for serialization.
   */
  public IndexHeader() {
    super();
  }

  /**
   * Creates a nerw header with the specified parameters.
   *
   * @param pageSize     the size of a page in bytes
   * @param dirCapacity  the maximum number of entries in a directory node
   * @param leafCapacity the maximum number of entries in a leaf node
   * @param dirMinimum   the minmum number of entries in a directory node
   * @param leafMinimum  the minmum number of entries in a leaf node
   */
  public IndexHeader(int pageSize, int dirCapacity, int leafCapacity,
                     int dirMinimum, int leafMinimum) {
    super(pageSize);
    this.dirCapacity = dirCapacity;
    this.leafCapacity = leafCapacity;
    this.dirMinimum = dirMinimum;
    this.leafMinimum = leafMinimum;
  }

  /**
   * Initializes this header from the specified file,
   *
   * @param file the file to which this header belongs
   * @throws java.io.IOException
   */
  public void readHeader(RandomAccessFile file) throws IOException {
    super.readHeader(file);
    this.dirCapacity = file.readInt();
    this.leafCapacity = file.readInt();
    this.dirMinimum = file.readInt();
    this.leafMinimum = file.readInt();
  }

  /**
   * Writes this header to the specified file,
   *
   * @param file the file to which this header belongs
   * @throws java.io.IOException
   */
  public void writeHeader(RandomAccessFile file) throws IOException {
    super.writeHeader(file);
    file.writeInt(this.dirCapacity);
    file.writeInt(this.leafCapacity);
    file.writeInt(this.dirMinimum);
    file.writeInt(this.leafMinimum);
  }

  /**
   * Returns the capacity of a directory node (= 1 + maximum number of entries
   * in a directory node).
   *
   * @return the capacity of a directory node (= 1 + maximum number of entries
   *         in a directory node)
   */
  public int getDirCapacity() {
    return dirCapacity;
  }

  /**
   * Returns the capacity of a leaf node (= 1 + maximum number of entries in a
   * leaf node).
   *
   * @return the capacity of a leaf node (= 1 + maximum number of entries in a
   *         leaf node)
   */
  public int getLeafCapacity() {
    return leafCapacity;
  }

  /**
   * Returns the minmum number of entries in a directory node.
   *
   * @return the minmum number of entries in a directory node
   */
  public int getDirMinimum() {
    return dirMinimum;
  }

  /**
   * Returns the minmum number of entries in a leaf node.
   *
   * @return the minmum number of entries in a leaf node
   */
  public int getLeafMinimum() {
    return leafMinimum;
  }

  /**
   * Returns the size of this header in Bytes.
   *
   * @return the size of this header in Bytes
   */
  public int size() {
    return super.size() + SIZE;
  }
}
