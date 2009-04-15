package experimentalcode.erich.cache;

import java.io.File;
import java.io.IOException;

/**
 * Class representing an upper triangle matrix backed by an on-disk array of O((n+1)*n/2) size
 * 
 * @author Erich Schubert
 */
public class UpperTriangleMatrix {
  /**
   * Serial number, also used for generating a magic 
   */
  private static final long serialVersionUID = -4489942156357634702L;
  
  /**
   * Size of this class' header
   */
  private static final int TRIANGLE_HEADER_SIZE = 4;
  
  /**
   * Size of the matrix
   */
  private int matrixsize;
  
  /**
   * Data storage
   */
  private OnDiskArray array;
  
  /**
   * Constructor to access an existing array.
   * 
   * @param filename File name
   * @param magicseed Magic number
   * @param extraheadersize Size of extra header data
   * @param recordsize Record size
   * @param writable flag to open writable
   * @throws IOException
   */
  public UpperTriangleMatrix(File filename, int magicseed, int extraheadersize, int recordsize, boolean writable) throws IOException {
    array = new OnDiskArray(filename, OnDiskArray.mixMagic((int)serialVersionUID, magicseed), extraheadersize + TRIANGLE_HEADER_SIZE, recordsize, writable);
    byte[] header = array.readExtraHeader();
    this.matrixsize = ByteArrayUtil.readInt(header, 0);
    if (arraysize(matrixsize) != array.getNumRecords()) {
      throw new IOException("Matrix file size doesn't match specified dimensions: "+matrixsize+"->"+arraysize(matrixsize)+" vs. "+array.getNumRecords());
    }
  }

  /**
   * Constructor to access a new array.
   * 
   * @param filename File name
   * @param magicseed Magic number
   * @param extraheadersize Size of extra header data
   * @param recordsize Record size
   * @param matrixsize Size of matrix to store
   * @throws IOException
   */
  public UpperTriangleMatrix(File filename, int magicseed, int extraheadersize, int recordsize, int matrixsize) throws IOException {
    if (matrixsize >= 0xFFFF) {
      throw new RuntimeException("Matrix size is too big and will overflow the integer datatype.");
    }
    this.matrixsize = matrixsize;
    array = new OnDiskArray(filename, OnDiskArray.mixMagic((int)serialVersionUID, magicseed), extraheadersize + TRIANGLE_HEADER_SIZE, recordsize, arraysize(matrixsize));
    byte[] header = new byte[extraheadersize + TRIANGLE_HEADER_SIZE];
    ByteArrayUtil.writeInt(header, 0, this.matrixsize);
    array.writeExtraHeader(header);
  }

  /**
   * Resize the matrix to cover newsize x newsize.
   * @param newsize New matrix size.
   * @throws IOException
   */
  public synchronized void resizeMatrix(int newsize) throws IOException {
    if (newsize >= 0xFFFF) {
      throw new RuntimeException("Matrix size is too big and will overflow the integer datatype.");
    }
    if (! array.isWritable()) {
      throw new IOException("Can't resize a read-only array.");
    }
    array.resizeFile(arraysize(newsize));
    this.matrixsize = newsize;
    byte[] header = array.readExtraHeader();
    ByteArrayUtil.writeInt(header, 0, this.matrixsize);
    array.writeExtraHeader(header);
  }

  /**
   * Compute the size of the needed backing array from the matrix dimensions.
   * 
   * @param matrixsize size of the matrix
   * @return size of the array
   */
  private static int arraysize(int matrixsize) {
    return matrixsize * (matrixsize + 1) / 2;
  }
  
  /**
   * Compute the offset within the file.
   * 
   * @param x First coordinate
   * @param y Second coordinate
   * @return Linear offset
   */
  private int computeOffset(int x, int y) {
    if (y > x) {
      return computeOffset(y, x);
    }
    return (x * (x+1)) / 2 + y;
  }
  
  /**
   * Get data from the matrix
   * 
   * @param x First coordinate
   * @param y Second coordinate
   * @return record raw data
   * @throws IOException 
   */
  public synchronized byte[] readRecord(int x, int y) throws IOException {
    if (x >= matrixsize || y >= matrixsize) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return array.readRecord(computeOffset(x,y));
  }

  /**
   * Put data into the matrix
   * 
   * @param x First coordinate
   * @param y Second coordinate
   * @param data Data
   * @throws IOException 
   */
  public synchronized void writeRecord(int x, int y, byte[] data) throws IOException {
    array.writeRecord(computeOffset(x,y), data);
  }
  
  /**
   * Close the matrix file.
   * 
   * @throws IOException
   */
  public synchronized void close() throws IOException {
    array.close();
  }

  /**
   * Query the size of the matrix.
   * 
   * @return size of the matrix
   */
  public int getMatrixSize() {
    return matrixsize;
  }
}
