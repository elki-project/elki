/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.persistent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import elki.utilities.io.ByteArrayUtil;

/**
 * On Disc Array storage for records of a given size.
 * <p>
 * This can be used to implement various fixed size record-based data
 * structures. The file format is designed to have a fixed-size header followed
 * by the actual data.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @composed - - - RandomAccessFile
 */
// TODO: ensure file doesn't become to big - check for overflows in recordsize *
// numrecs + headersize
public class OnDiskArray implements AutoCloseable {
  /**
   * Serial version.
   * <p>
   * NOTE: Change this version whenever the file structure is changed in an
   * incompatible way: This will modify the file magic, and thus prevent
   * applications from reading incompatible files.
   */
  private static final long serialVersionUID = 7586497243452875056L;

  /**
   * Magic number used to identify files.
   */
  protected int magic;

  /**
   * Size of the header in the file. Note that the internal header is four
   * integers already.
   */
  private int headersize;

  /**
   * Size of the records in the file.
   */
  private int recordsize;

  /**
   * Number of records in the file.
   */
  private int numrecs;

  /**
   * File name.
   */
  private Path filename;

  /**
   * Random Access File object.
   */
  private final FileChannel file;

  /**
   * Lock for the file that will be kept while writing.
   */
  private FileLock lock = null;

  /**
   * Writable or read-only object.
   */
  private boolean writable;

  /**
   * The memory mapped buffer.
   */
  private MappedByteBuffer map;

  /**
   * Size of the classes header size.
   */
  private static final int INTERNAL_HEADER_SIZE = 4 * ByteArrayUtil.SIZE_INT;

  /**
   * Position of file size (in records).
   */
  private static final int HEADER_POS_SIZE = 3 * ByteArrayUtil.SIZE_INT;

  /**
   * Constructor to write a new file.
   * 
   * @param filename File name to be opened.
   * @param magicseed Magic number to derive real magic from.
   * @param extraheadersize header size NOT including the internal header
   * @param recordsize Record size
   * @param initialsize Initial file size (in records)
   * @throws IOException on IO errors
   */
  public OnDiskArray(Path filename, int magicseed, int extraheadersize, int recordsize, int initialsize) throws IOException {
    this.magic = mixMagic((int) serialVersionUID, magicseed);
    this.headersize = extraheadersize + INTERNAL_HEADER_SIZE;
    this.recordsize = recordsize;
    this.filename = filename;
    this.writable = true;

    // do not allow overwriting, unless empty (for pre-created temp files)
    if(Files.exists(filename) && Files.size(filename) != 0) {
      throw new IOException("File already exists: " + filename);
    }

    // open file.
    file = FileChannel.open(filename, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    // and acquire a file write lock
    lock = file.lock();

    ByteBuffer bbuf = ByteBuffer.allocateDirect(INTERNAL_HEADER_SIZE);
    bbuf.putInt(this.magic) // write magic header
        .putInt(this.headersize) // write header size
        .putInt(this.recordsize) // write size of a single record
        .putInt(initialsize) // write number of records
        .flip();
    file.write(bbuf, 0);
    // resize file
    resizeFile(initialsize);
    // map array
    mapArray();
  }

  /**
   * Constructor to open an existing file. The provided record size must match
   * the record size stored within the files header. If you don't know this size
   * yet and/or need to access the extra header you should use the other
   * constructor below
   * 
   * @param filename File name to be opened.
   * @param magicseed Magic number to derive real magic from.
   * @param extraheadersize header size NOT including the internal header
   * @param recordsize Record size
   * @param writable flag to open the file writable
   * @throws IOException on IO errors
   */
  public OnDiskArray(Path filename, int magicseed, int extraheadersize, int recordsize, boolean writable) throws IOException {
    this.magic = mixMagic((int) serialVersionUID, magicseed);
    this.headersize = extraheadersize + INTERNAL_HEADER_SIZE;
    this.recordsize = recordsize;
    this.filename = filename;
    this.writable = writable;

    file = FileChannel.open(filename, writable //
        ? new OpenOption[] { StandardOpenOption.READ, StandardOpenOption.WRITE } //
        : new OpenOption[] { StandardOpenOption.READ });
    lock = writable ? file.lock() : null;

    validateHeader(true);
    mapArray();
  }

  /**
   * Constructor to open an existing file. The record size is read from the
   * file's header and can be obtained by <code>getRecordsize()</code>
   * 
   * @param filename File name to be opened.
   * @param magicseed Magic number to derive real magic from.
   * @param extraheadersize header size NOT including the internal header
   * @param writable flag to open the file writable
   * @throws IOException on IO errors
   */
  public OnDiskArray(Path filename, int magicseed, int extraheadersize, boolean writable) throws IOException {
    this.magic = mixMagic((int) serialVersionUID, magicseed);
    this.headersize = extraheadersize + INTERNAL_HEADER_SIZE;
    this.filename = filename;
    this.writable = writable;

    file = FileChannel.open(filename, writable //
        ? new OpenOption[] { StandardOpenOption.READ, StandardOpenOption.WRITE } //
        : new OpenOption[] { StandardOpenOption.READ });
    lock = writable ? file.lock() : null;

    validateHeader(false);
    mapArray();
  }

  /**
   * (Re-) map the data array.
   * 
   * @throws IOException on mapping error.
   */
  private synchronized void mapArray() throws IOException {
    if(map != null) {
      map.force();
      map = null;
    }
    MapMode mode = writable ? MapMode.READ_WRITE : MapMode.READ_ONLY;
    long size = recordsize * (long) numrecs;
    if(size > Integer.MAX_VALUE) {
      throw new ArrayIndexOutOfBoundsException("OnDiskArray currently has a maximum size of: " + Integer.MAX_VALUE + " (see Java FileChannel#map).");
    }
    map = file.map(mode, headersize, size);
  }

  /**
   * Validates the header and throws an IOException if the header is invalid. If
   * validateRecordSize is set to true the record size must match exactly the
   * stored record size within the files header, else the record size is read
   * from the header and used.
   * 
   * @param validateRecordSize
   * @throws IOException
   */
  private void validateHeader(boolean validateRecordSize) throws IOException {
    ByteBuffer bbuf = ByteBuffer.allocateDirect(INTERNAL_HEADER_SIZE);
    if(file.read(bbuf, 0) != INTERNAL_HEADER_SIZE) {
      throw new IOException("Incomplete read validating the header");
    }
    bbuf.flip();
    int readmagic = bbuf.getInt();
    // Validate magic number
    if(readmagic != this.magic) {
      file.close();
      throw new IOException("Magic in LinearDiskCache does not match: " + readmagic + " instead of " + this.magic);
    }
    // Validate header size
    if(bbuf.getInt() != this.headersize) {
      file.close();
      throw new IOException("Header size in LinearDiskCache does not match.");
    }

    if(validateRecordSize) {
      // Validate record size
      if(bbuf.getInt() != this.recordsize) {
        file.close();
        throw new IOException("Recordsize in LinearDiskCache does not match.");
      }
    }
    else {
      // or just read it from file
      this.recordsize = bbuf.getInt();
    }

    // read the number of records and validate with file size.
    this.numrecs = bbuf.getInt();
    if(numrecs < 0 || file.size() != indexToFileposition(numrecs)) {
      throw new IOException("File size and number of records do not agree.");
    }
  }

  /**
   * Mix two magic numbers into one, to obtain a combined magic. Note:
   * mixMagic(a,b) != mixMagic(b,a) usually.
   * 
   * @param magic1 Magic number to mix.
   * @param magic2 Magic number to mix.
   * @return Mixed magic number.
   */
  public static final int mixMagic(int magic1, int magic2) {
    final long prime = 2654435761L;
    return (int) (prime * (prime + magic1) + magic2);
  }

  /**
   * Compute file position from index number
   * 
   * @param index Index offset
   * @return file position
   */
  private long indexToFileposition(long index) {
    return headersize + index * recordsize;
  }

  /**
   * Resize file to the intended size
   * 
   * @param newsize New file size.
   * @throws IOException on IO errors
   */
  public synchronized void resizeFile(int newsize) throws IOException {
    if(!writable) {
      throw new IOException("File is not writeable!");
    }
    // update the number of records
    this.numrecs = newsize;
    ByteBuffer bbuf = ByteBuffer.allocateDirect(4);
    bbuf.putInt(numrecs).flip();
    file.write(bbuf, HEADER_POS_SIZE);

    // resize file
    file.truncate(indexToFileposition(numrecs));
    mapArray();
  }

  /**
   * Get a record buffer
   * 
   * @param index Record index
   * @return Byte buffer for the record
   * @throws IOException on IO errors
   */
  public synchronized ByteBuffer getRecordBuffer(int index) throws IOException {
    if(index < 0 || index >= numrecs) {
      throw new IOException("Access beyond end of file.");
    }
    // Adjust buffer view
    synchronized(map) {
      map.limit(recordsize * (index + 1));
      map.position(recordsize * index);
      return map.slice();
    }
  }

  /**
   * Return the size of the extra header. Accessor.
   * 
   * @return Extra header size
   */
  protected int getExtraHeaderSize() {
    return headersize - INTERNAL_HEADER_SIZE;
  }

  /**
   * Read the extra header data.
   * 
   * @return additional header data
   * @throws IOException on IO errors
   */
  public synchronized ByteBuffer getExtraHeader() throws IOException {
    final int size = headersize - INTERNAL_HEADER_SIZE;
    final MapMode mode = writable ? MapMode.READ_WRITE : MapMode.READ_ONLY;
    return file.map(mode, INTERNAL_HEADER_SIZE, size);
  }

  /**
   * Get the size of a single record.
   * 
   * @return Record size.
   */
  protected int getRecordsize() {
    return recordsize;
  }

  /**
   * Get the file name.
   * 
   * @return File name
   */
  public Path getFilename() {
    return filename;
  }

  /**
   * Check if the file is writable.
   * 
   * @return true if the file is writable.
   */
  public boolean isWritable() {
    return writable;
  }

  /**
   * Explicitly close the file. Note: following operations will likely cause
   * IOExceptions.
   * 
   * @throws IOException on IO errors
   */
  @Override
  public synchronized void close() throws IOException {
    writable = false;
    if(map != null) {
      map.force();
      map = null;
    }
    if(lock != null) {
      lock.release();
      lock = null;
    }
    file.close();
  }

  /**
   * Get number of records in file.
   * 
   * @return Number of records in the file.
   */
  public int getNumRecords() {
    return numrecs;
  }

  /**
   * Ensure that the file can fit the given number of records.
   * 
   * @param size Size
   * @throws IOException
   */
  public void ensureSize(int size) throws IOException {
    if(size > getNumRecords()) {
      resizeFile(size);
    }
  }
}
