package de.lmu.ifi.dbs.persistent;

import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * Defines the requirements for a header of a persistent page file. A header must at least
 * store the size of a page in Bytes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface PageHeader {
  /**
   * Returns the size of this header in Bytes.
   * @return the size of this header in Bytes
   */
  int size();

  /**
   * Initializes this header from the specified file,
   * @param file the file to which this header belongs
   * @throws IOException
   */
  void readHeader(RandomAccessFile file) throws IOException;

  /**
   * Writes this header to the specified file,
   * @param file the file to which this header belongs
   * @throws IOException
   */
  void writeHeader(RandomAccessFile file) throws IOException;

  /**
   * Returns the size of a page in Bytes.
   * @return the size of a page in Bytes
   */
  int getPageSize();
}
