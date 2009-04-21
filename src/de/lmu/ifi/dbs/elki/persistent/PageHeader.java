package de.lmu.ifi.dbs.elki.persistent;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Defines the requirements for a header of a persistent page file. A header
 * must at least store the size of a page in Bytes.
 *
 * @author Elke Achtert
 */
public interface PageHeader {
    /**
     * Returns the size of this header in Bytes.
     *
     * @return the size of this header in Bytes
     */
    int size();

    /**
     * Initializes this header from the specified file.
     *
     * @param file the file to which this header belongs
     * @throws IOException if an I/O-error occurs during reading
     */
    void readHeader(RandomAccessFile file) throws IOException;

    /**
     * Initializes this header from the specified file.
     *
     * @param data byte array with the page data.
     */
    void readHeader(byte[] data);

    /**
     * Writes this header to the specified file.
     *
     * @param file the file to which this header belongs
     * @throws IOException IOException if an I/O-error occurs during writing
     */
    void writeHeader(RandomAccessFile file) throws IOException;

    /**
     * Return the header as byte array
     * @return header as byte array
     */
    byte[] asByteArray();

    /**
     * Returns the size of a page in Bytes.
     *
     * @return the size of a page in Bytes
     */
    int getPageSize();
}
