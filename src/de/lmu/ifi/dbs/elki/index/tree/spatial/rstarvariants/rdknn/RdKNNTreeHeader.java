package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Encapsulates the header information of a RDkNN-Tree. This information is needed
 * for persistent storage.
 *
 * @author Elke Achtert 
 */
class RdKNNTreeHeader extends TreeIndexHeader {
  /**
   * The size of this header.
   */
  private static int SIZE = 4;

  /**
   * The maximum number k of reverse kNN queries to be supported.
   */
  int k_max;

  /**
   * Empty constructor for serialization.
   */
  public RdKNNTreeHeader() {
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
   * @param k_max        the maximum number k of reverse kNN queries to be supported
   */
  public RdKNNTreeHeader(int pageSize, int dirCapacity, int leafCapacity,
                         int dirMinimum, int leafMinimum, int k_max) {
    super(pageSize, dirCapacity, leafCapacity, dirMinimum, leafMinimum);
    this.k_max = k_max;
  }

  /**
   * Initializes this header from the specified file,
   *
   * @param file the file to which this header belongs
   * @throws java.io.IOException
   */
  public void readHeader(RandomAccessFile file) throws IOException {
    super.readHeader(file);
    this.k_max = file.readInt();
  }

  /**
   * Writes this header to the specified file,
   *
   * @param file the file to which this header belongs
   * @throws java.io.IOException
   */
  public void writeHeader(RandomAccessFile file) throws IOException {
    super.writeHeader(file);
    file.writeInt(this.k_max);
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
