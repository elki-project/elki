package de.lmu.ifi.dbs.index.metrical.mtree.mknn;

import de.lmu.ifi.dbs.index.metrical.mtree.MTreeHeader;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Encapsulates the header information of a MDkNN-Tree. This information is needed for
 * persistent storage.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkNNTreeHeader extends MTreeHeader {
  /**
   * The size of this header.
   */
  private static int SIZE = 4;

  /**
   * The parameter k.
   */
  int k;

  /**
   * Empty constructor for serialization.
   */
  public MkNNTreeHeader() {
    super();
  }

  /**
   * Creates a nerw header with the specified parameters.
   *
   * @param pageSize     the size of a page in bytes
   * @param dirCapacity  the capacity of a directory node
   * @param leafCapacity the capacity of a leaf node
   * @param k            the parameter k
   */
  public MkNNTreeHeader(int pageSize, int dirCapacity, int leafCapacity, int k) {
    super(pageSize, dirCapacity, leafCapacity);
    this.k = k;
  }

  /**
   * Initializes this header from the specified file,
   *
   * @param file the file to which this header belongs
   * @throws java.io.IOException
   */
  public void readHeader(RandomAccessFile file) throws IOException {
    super.readHeader(file);
    this.k = file.readInt();
  }

  /**
   * Writes this header to the specified file,
   *
   * @param file the file to which this header belongs
   * @throws java.io.IOException
   */
  public void writeHeader(RandomAccessFile file) throws IOException {
    super.writeHeader(file);
    file.writeInt(this.k);
  }

  /**
   * Returns the parameter k.
   *
   * @return the parameter k
   */
  public int getK() {
    return k;
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
