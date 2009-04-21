package de.lmu.ifi.dbs.elki.index.tree;

import de.lmu.ifi.dbs.elki.persistent.DefaultPageHeader;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Encapsulates the header information of a tree like index structure. This
 * information is needed for persistent storage.
 * 
 * @author Elke Achtert
 */
public class TreeIndexHeader extends DefaultPageHeader {
  /**
   * The size of this header in Bytes, which is 16 Bytes ( 4 Bytes for
   * {@link #dirCapacity}, {@link #leafCapacity}, {@link #dirMinimum}, and
   * {@link #leafMinimum}).
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
  public TreeIndexHeader() {
    super();
  }

  /**
   * Creates a new header with the specified parameters.
   * 
   * @param pageSize the size of a page in bytes
   * @param dirCapacity the maximum number of entries in a directory node
   * @param leafCapacity the maximum number of entries in a leaf node
   * @param dirMinimum the minimum number of entries in a directory node
   * @param leafMinimum the minimum number of entries in a leaf node
   */
  public TreeIndexHeader(int pageSize, int dirCapacity, int leafCapacity, int dirMinimum, int leafMinimum) {
    super(pageSize);
    this.dirCapacity = dirCapacity;
    this.leafCapacity = leafCapacity;
    this.dirMinimum = dirMinimum;
    this.leafMinimum = leafMinimum;
  }

  /**
   * Initializes this header from the specified file. Calls
   * {@link de.lmu.ifi.dbs.elki.persistent.DefaultPageHeader#readHeader(java.io.RandomAccessFile)
   * DefaultPageHeader#readHeader(file)} and reads the integer values of
   * {@link #dirCapacity}, {@link #leafCapacity}, {@link #dirMinimum}, and
   * {@link #leafMinimum} from the file.
   */
  @Override
  public void readHeader(RandomAccessFile file) throws IOException {
    super.readHeader(file);
    this.dirCapacity = file.readInt();
    this.leafCapacity = file.readInt();
    this.dirMinimum = file.readInt();
    this.leafMinimum = file.readInt();
  }

  /**
   * Writes this header to the specified file. Writes the integer values of
   * {@link #dirCapacity}, {@link #leafCapacity}, {@link #dirMinimum}, and
   * {@link #leafMinimum} to the file.
   */
  @Override
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
   * Returns the minimum number of entries in a directory node.
   * 
   * @return the minimum number of entries in a directory node
   */
  public int getDirMinimum() {
    return dirMinimum;
  }

  /**
   * Returns the minimum number of entries in a leaf node.
   * 
   * @return the minimum number of entries in a leaf node
   */
  public int getLeafMinimum() {
    return leafMinimum;
  }

  /**
   * Returns {@link de.lmu.ifi.dbs.elki.persistent.DefaultPageHeader#size()}
   * plus the value of {@link #SIZE}).
   */
  @Override
  public int size() {
    return super.size() + SIZE;
  }
}
