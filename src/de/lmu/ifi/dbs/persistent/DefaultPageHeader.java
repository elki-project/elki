package de.lmu.ifi.dbs.persistent;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Default implementation of a page header.
 *
 * @author Elke Achtert 
 */
public class DefaultPageHeader implements PageHeader {

  /**
   * Magic number.
   */
  private static final int FILE_VERSION = 06210571002;

  /**
   * The size of this header.
   */
  private static int SIZE = 8;

  /**
   * The size of a page in bytes.
   */
  private int pageSize = -1;

  /**
   * Empty constructor for serialization.
   */
  public DefaultPageHeader() {
	  // empty constructor
  }


  /**
   * Creates a nerw header with the specified parameters.
   * @param pageSize the size of a page in bytes
   */
  public DefaultPageHeader(int pageSize) {
    this.pageSize = pageSize;
  }


  /**
   * Returns the size of this header in Bytes.
   *
   * @return the size of this header in Bytes
   */
  public int size() {
    return SIZE;
  }

  /**
   * Initializes this header from the specified file,
   *
   * @param file the file to which this header belongs
   * @throws java.io.IOException
   */
  public void readHeader(RandomAccessFile file) throws IOException {
    file.seek(0);
    if (file.readInt() != FILE_VERSION)
      throw new RuntimeException("File " + file + " is not a PersistentPageFile or wrong version!");

    this.pageSize = file.readInt();
  }

  /**
   * Writes this header to the specified file,
   *
   * @param file the file to which this header belongs
   * @throws java.io.IOException
   */
  public void writeHeader(RandomAccessFile file) throws IOException {
    file.seek(0);
    file.writeInt(FILE_VERSION);
    file.writeInt(this.pageSize);
  }

  /**
   * Returns the size of a page in Bytes.
   *
   * @return the size of a page in Bytes
   */
  public int getPageSize() {
    return pageSize;
  }
}
