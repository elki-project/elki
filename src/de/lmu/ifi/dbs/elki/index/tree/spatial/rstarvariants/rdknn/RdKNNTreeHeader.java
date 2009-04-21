package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Encapsulates the header information of a RDkNN-Tree. This information is
 * needed for persistent storage.
 * 
 * @author Elke Achtert
 */
class RdKNNTreeHeader extends TreeIndexHeader {
  /**
   * The size of this header in Bytes, which is 4 Bytes (for {@link #k_max}).
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
   * Creates a new header with the specified parameters.
   * 
   * @param pageSize the size of a page in bytes
   * @param dirCapacity the maximum number of entries in a directory node
   * @param leafCapacity the maximum number of entries in a leaf node
   * @param dirMinimum the minimum number of entries in a directory node
   * @param leafMinimum the minimum number of entries in a leaf node
   * @param k_max the maximum number k of reverse kNN queries to be supported
   */
  public RdKNNTreeHeader(int pageSize, int dirCapacity, int leafCapacity, int dirMinimum, int leafMinimum, int k_max) {
    super(pageSize, dirCapacity, leafCapacity, dirMinimum, leafMinimum);
    this.k_max = k_max;
  }

  /**
   * Initializes this header from the specified file. Calls
   * {@link de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader#readHeader(java.io.RandomAccessFile)
   * TreeIndexHeader#readHeader(file)} and reads additionally the integer value
   * of {@link #k_max} from the file.
   */
  @Override
  public void readHeader(RandomAccessFile file) throws IOException {
    super.readHeader(file);
    this.k_max = file.readInt();
  }

  /**
   * Writes this header to the specified file. Calls
   * {@link de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader#writeHeader(java.io.RandomAccessFile)}
   * and writes additionally the integer value of {@link #k_max} to the file.
   */
  @Override
  public void writeHeader(RandomAccessFile file) throws IOException {
    super.writeHeader(file);
    file.writeInt(this.k_max);
  }

  /**
   * Returns {@link de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader#size()} plus
   * the value of {@link #SIZE}).
   */
  @Override
  public int size() {
    return super.size() + SIZE;
  }
}
