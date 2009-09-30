package experimentalcode.marisa.index.xtree;

import java.io.IOException;
import java.io.RandomAccessFile;

import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;

/**
 * Encapsulates the header information of an XTree index structure. This
 * information is needed for persistent storage.
 * 
 * @author Marisa Thoma
 */
public class XTreeHeader extends TreeIndexHeader {

  /**
   * The size of this header in bytes, which is 32 Bytes. We have the integers
   * {@link #min_fanout} and {@link #dimensionality} (each 4 bytes), the floats
   * {@link #reinsert_fraction} and {@link #max_overlap} (each 4 bytes) and the
   * 8 bytes each for the longs {@link #num_elements} and
   * {@link #supernode_offset}.
   */
  private static int SIZE = 32;

  /**
   * Minimum size to be allowed for page sizes after a split in case of a
   * minimum overlap split.
   */
  private int min_fanout;

  /** Fraction of pages to be re-inserted instead of trying a split. */
  private float reinsert_fraction = (float) .3;

  /** Maximally allowed overlap. */
  private float max_overlap = (float) .2;

  /** Number of elements stored in this tree. */
  private long num_elements;

  /** Dimensionality of the objects in this header's tree. */
  private int dimensionality;

  /**
   * Number of bytes to be skipped for reading the first supernode on disc. This
   * number is only to be used in the beginning, when first loading the XTree.
   * Later on, the supernodes may be located elsewhere (or disappear).
   */
  private long supernode_offset = -1;

  public XTreeHeader() {
    super();
  }

  public XTreeHeader(int pageSize, int dirCapacity, int leafCapacity, int dirMinimum, int leafMinimum, int min_fanout, long num_elements, int dimensionality, float reinsert_fraction, float max_overlap) {
    super(pageSize, dirCapacity, leafCapacity, dirMinimum, leafMinimum);
    this.min_fanout = min_fanout;
    this.num_elements = num_elements;
    this.dimensionality = dimensionality;
    this.reinsert_fraction = reinsert_fraction;
    this.max_overlap = max_overlap;
  }

  /**
   * Initializes this header from the specified file. Reads the integer values
   * <code>version</code>, <code>pageSize</code>, {@link #dirCapacity},
   * {@link #leafCapacity}, {@link #dirMinimum}, and {@link #leafMinimum}, as
   * well as the minimum fanout {@link #min_fanout}, the tree's dimension
   * {@link #dimensionality}, the fraction of pages to be re-inserted before
   * splitting {@link #reinsert_fraction}, the maximum overlap
   * {@link #max_overlap} and the supernode offset {@link #supernode_offset}
   * from the file.
   */
  @Override
  public void readHeader(RandomAccessFile file) throws IOException {
    super.readHeader(file);
    this.min_fanout = file.readInt();
    this.num_elements = file.readLong();
    this.dimensionality = file.readInt();
    this.reinsert_fraction = file.readFloat();
    this.max_overlap = file.readFloat();
    this.supernode_offset = file.readLong();
  }

  /**
   * Writes this header to the specified file. Writes to file the integer values
   * <code>version</code>, <code>pageSize</code>, {@link #dirCapacity},
   * {@link #leafCapacity}, {@link #dirMinimum}, {@link #leafMinimum},
   * {@link #min_fanout}, {@link #dimensionality}, the <code>float</code>s
   * {@link #reinsert_fraction} and {@link #max_overlap} and the
   * <code>long</code> {@link #supernode_offset}.
   */
  @Override
  public void writeHeader(RandomAccessFile file) throws IOException {
    super.writeHeader(file);
    file.writeInt(min_fanout);
    file.writeLong(num_elements);
    file.writeInt(dimensionality);
    file.writeFloat(reinsert_fraction);
    file.writeFloat(max_overlap);
    file.writeLong(supernode_offset);
  }

  /**
   * @return the minimum size to be allowed for page sizes after a split in case
   *         of a minimum overlap split.
   */
  public int getMin_fanout() {
    return min_fanout;
  }

  /**
   * @return the fraction of pages to be re-inserted before splitting
   */
  public float getReinsert_fraction() {
    return reinsert_fraction;
  }

  /**
   * @return the fraction of pages to be re-inserted before splitting
   */
  public float getMaxOverlap() {
    return max_overlap;
  }

  /**
   * @return the bytes offset for the first supernode entry on disc
   */
  public long getSupernode_offset() {
    return supernode_offset;
  }

  /**
   * Assigns this header a new supernode offset.
   * 
   * @param supernode_offset
   */
  public void setSupernode_offset(long supernode_offset) {
    this.supernode_offset = supernode_offset;
  }

  /**
   * Returns {@link TreeIndexHeader#size()} plus the value of {@link #SIZE}).
   */
  @Override
  public int size() {
    return super.size() + SIZE;
  }

  /**
   * @param dimensionality the dimensionality of the objects to be stored in
   *        this header's tree
   */
  public void setDimensionality(int dimensionality) {
    this.dimensionality = dimensionality;
  }

  /**
   * @return Dimensionality of the objects in this header's tree
   */
  public int getDimensionality() {
    return dimensionality;
  }

  /**
   * @return The number of elements stored in the tree
   */
  public void setNumberOfElements(long num_elements) {
    this.num_elements = num_elements;
  }

  /**
   * @return The number of elements stored in the tree
   */
  public long getNumberOfElements() {
    return num_elements;
  }

}
