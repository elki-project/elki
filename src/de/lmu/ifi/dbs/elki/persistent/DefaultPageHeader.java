package de.lmu.ifi.dbs.elki.persistent;

import java.io.IOException;
import java.io.RandomAccessFile;

import de.lmu.ifi.dbs.elki.utilities.ByteArrayUtil;


/**
 * Default implementation of a page header.
 *
 * @author Elke Achtert
 */
public class DefaultPageHeader implements PageHeader {

    /**
     * The size of this header in Bytes,
     * which is 8 Bytes (
     * 4 Bytes for {@link #FILE_VERSION}
     * and 4 Bytes for {@link #pageSize}).
     */
    private static final int SIZE = 8;

    /**
     * Version number of this header (magic number).
     */
    private static final int FILE_VERSION = 841150978;

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
     * Creates a new header with the specified parameters.
     *
     * @param pageSize the size of a page in bytes
     */
    public DefaultPageHeader(int pageSize) {
        this.pageSize = pageSize;
    }


    /**
     * Returns the value of {@link #SIZE}).
     *
     */
    public int size() {
        return SIZE;
    }

    /**
     * Initializes this header from the specified file.
     * Looks for the right version
     * and reads the integer value of {@link #pageSize} from the file.
     *
     */
    public void readHeader(RandomAccessFile file) throws IOException {
        file.seek(0);
        if (file.readInt() != FILE_VERSION) {
            throw new RuntimeException("File " + file + " is not a PersistentPageFile or wrong version!");
        }

        this.pageSize = file.readInt();
    }

    /**
     * Initializes this header from the given Byte array.
     * Looks for the right version
     * and reads the integer value of {@link #pageSize} from the file.
     *
     */
    public void readHeader(byte[] data) {
        if (ByteArrayUtil.readInt(data, 0) != FILE_VERSION) {
          throw new RuntimeException("PersistentPageFile version does not match!");
        }

        this.pageSize = ByteArrayUtil.readInt(data, 4);
    }

    /**
     * Writes this header to the specified file.
     * Writes the {@link #FILE_VERSION version} of this header and
     * the integer value of {@link #pageSize} to the file.
     *
     */
    public void writeHeader(RandomAccessFile file) throws IOException {
        file.seek(0);
        file.writeInt(FILE_VERSION);
        file.writeInt(this.pageSize);
    }
    
    public byte[] asByteArray() {
      byte[] header = new byte[SIZE];
      ByteArrayUtil.writeInt(header, 0, FILE_VERSION);
      ByteArrayUtil.writeInt(header, 4, this.pageSize);
      return header;
    }


    /**
     * Returns the size of a page in Bytes.
     *
     * @return the size of a page in Bytes
     */
    public int getPageSize() {
        return pageSize;
    }
    
    /**
     * Returns the number of pages necessary for the header
     *
     * @return the number of pages
     */
    public int getReservedPages() {
  	  return size()/getPageSize()+1;
    }
}
