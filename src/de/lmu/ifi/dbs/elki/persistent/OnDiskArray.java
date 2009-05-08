package de.lmu.ifi.dbs.elki.persistent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.channels.FileLock;

import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;

/**
 * On Disc Array storage for records of a given size.
 * 
 * This can be used to implement various fixed size record-based data structures.
 * The file format is designed to have a fixed-size header followed by the actual data.
 * 
 * @author Erich Schubert
 *
 */
// TODO: make serializable - by just keeping the required information, restoring the other.
// TODO: ensure file doesn't become to big - check for overflows in recordsize * numrecs + headersize
public class OnDiskArray implements Serializable {
  /**
   * Serial version.
   * 
   * NOTE: Change this version whenever the file structure is changed in an incompatible way:
   * This will modify the file magic, and thus prevent applications from reading incompatible files.
   */
  private static final long serialVersionUID = 7586497243452875056L;
  /**
   * Magic number used to identify files
   */
  protected int magic;
  /**
   * Size of the header in the file.
   * Note that the internal header is four integers already.
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
  private File filename;
  /**
   * Random Access File object.
   */
  private RandomAccessFile file;
  /**
   * Lock for the file that will be kept while writing.
   */
  private FileLock lock = null;
  /**
   * Writable or read-only object
   */
  private boolean writable;
  
  /**
   * Size of the classes header size.
   */
  private final static int INTERNAL_HEADER_SIZE = 4 * 4;
  
  /**
   * Position of file size (in records)
   */
  private final static int HEADER_POS_SIZE = 3 * 4;
  
  /**
   * Constructor to write a new file.
   * 
   * @param filename File name to be opened.
   * @param magicseed Magic number to derive real magic from.
   * @param extraheadersize header size NOT including the internal header
   * @param recordsize Record size
   * @param initialsize Initial file size (in records)
   * @throws IOException
   */
  public OnDiskArray(File filename, int magicseed, int extraheadersize, int recordsize, int initialsize) throws IOException {
    this.magic = mixMagic((int) serialVersionUID, magicseed);
    this.headersize = extraheadersize + INTERNAL_HEADER_SIZE;
    this.recordsize = recordsize;
    this.filename = filename;
    this.writable = true;

    // do not allow overwriting.
    if (filename.exists()) {
      throw new IOException(ExceptionMessages.FILE_EXISTS);
    }
    
    // open file.
    file = new RandomAccessFile(filename, "rw");
    // and acquire a file write lock
    lock = file.getChannel().lock();
    
    // write magic header
    file.writeInt(this.magic);
    
    // write header size
    file.writeInt(this.headersize);
    
    // write size of a single record
    file.writeInt(this.recordsize);
    
    // write number of records
    // verify position.
    if (file.getFilePointer() != HEADER_POS_SIZE) {
      // TODO: more appropriate exception class?
      throw new IOException("File position doesn't match when writing file size.");
    }
    file.writeInt(initialsize);

    // we should have written the complete internal header now.
    if (file.getFilePointer() != INTERNAL_HEADER_SIZE) {
      // TODO: more appropriate exception class?
      throw new IOException("File position doesn't match header size after writing header.");      
    }
    // resize file
    resizeFile(initialsize);
  }

  /**
   * Constructor to open an existing file.
   * 
   * @param filename File name to be opened.
   * @param magicseed Magic number to derive real magic from.
   * @param extraheadersize header size NOT including the internal header
   * @param recordsize Record size
   * @param writable flag to open the file writable
   * @throws IOException
   */
  public OnDiskArray(File filename, int magicseed, int extraheadersize, int recordsize, boolean writable) throws IOException {
    this.magic = mixMagic((int) serialVersionUID, magicseed);
    this.headersize = extraheadersize + INTERNAL_HEADER_SIZE;
    this.recordsize = recordsize;
    this.filename = filename;
    this.writable = writable;
    
    String mode = writable ? "rw" : "r";
    
    file = new RandomAccessFile(filename, mode);
    if (writable) {
      // acquire a file write lock
      lock = file.getChannel().lock();      
    }
    
    int readmagic = file.readInt();
    // Validate magic number
    if (readmagic != this.magic) {
      file.close();
      throw new IOException("Magic in LinearDiskCache does not match: "+readmagic+" instead of "+this.magic);
    }
    // Validate header size
    if (file.readInt() != this.headersize) {
      file.close();
      throw new IOException("Header size in LinearDiskCache does not match.");
    }
    // Validate record size
    if (file.readInt() != this.recordsize) {
      file.close();
      throw new IOException("Recordsize in LinearDiskCache does not match.");
    }
    // read the number of records and validate with file size.
    if (file.getFilePointer() != HEADER_POS_SIZE) {
      throw new IOException("Incorrect file position when reading header.");
    }
    this.numrecs = file.readInt();
    if (numrecs < 0 || file.length() != indexToFileposition(numrecs)) {
      throw new IOException("File size and number of records do not agree.");
    }
    // yet another sanity check. We should have read all of our internal header now.
    if (file.getFilePointer() != INTERNAL_HEADER_SIZE) {
      throw new IOException("Incorrect file position after reading header.");
    }
  }
  
  /**
   * Mix two magic numbers into one, to obtain a combined magic.
   * Note: mixMagic(a,b) != mixMagic(b,a) usually.
   * 
   * @param magic1 Magic number to mix.
   * @param magic2 Magic number to mix.
   * @return Mixed magic number.
   */
  public static final int mixMagic(int magic1, int magic2) {
    final long prime = 2654435761L;
    long result = 1;
    result = prime * result + magic1;
    result = prime * result + magic2;
    return (int) result;
  }
  
  /**
   * Compute file position from index number
   * 
   * @param index Index offset
   * @return file position
   */
  private long indexToFileposition(long index) {
    long pos = headersize + index * recordsize;
    return pos;
  }

  /**
   * Resize file to the intended size
   * 
   * @param newsize New file size.
   * @throws IOException
   */
  public synchronized void resizeFile(int newsize) throws IOException {
    if (!writable) {
      throw new IOException("File is not writeable!");
    }
    // update the number of records
    this.numrecs = newsize;
    file.seek(HEADER_POS_SIZE);
    file.writeInt(numrecs);
    
    // resize file
    file.setLength(indexToFileposition(numrecs));
  }  
  
  /**
   * Read a single record from the file.
   * 
   * @param index Record index
   * @return Byte array with the records data.
   * @throws IOException
   */
  public synchronized byte[] readRecord(int index) throws IOException {
    if (index < 0 || index >= numrecs) {
      throw new IOException("Access beyond end of file.");
    }
    byte[] data = new byte[recordsize];
    file.seek(indexToFileposition(index));
    int read = file.read(data);
    if (read != recordsize) {
      throw new IOException("Read error in LinearDiskCache.");
    }
    return data;
  }

  /**
   * Write a single record.
   * 
   * @param index Record index.
   * @param data Array with record data. MUST have appropriate size.
   * @throws IOException
   */
  public synchronized void writeRecord(int index, byte[] data) throws IOException {
    if (!writable) {
      throw new IOException("File is not writeable!");
    }
    if (index < 0 || index >= numrecs) {
      throw new IOException("Access beyond end of file.");
    }
    if (data.length != recordsize) {
      throw new IOException("Record size does not match.");      
    }
    file.seek(indexToFileposition(index));
    file.write(data);
    return;
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
   * @throws IOException
   */
  public synchronized byte[] readExtraHeader() throws IOException {
    int size = headersize - INTERNAL_HEADER_SIZE;
    file.seek(INTERNAL_HEADER_SIZE);
    byte[] buf = new byte[size];
    file.read(buf);
    return buf;
  }

  /**
   * Write the extra header data.
   *
   * @param buf Header data.
   * @throws IOException
   */
  public synchronized void writeExtraHeader(byte[] buf) throws IOException {
    if (!writable) {
      throw new IOException("File is not writeable!");
    }
    int size = headersize - INTERNAL_HEADER_SIZE;
    if (size != buf.length) {
      throw new IOException("Header size does not match!");
    }
    file.seek(INTERNAL_HEADER_SIZE);
    file.write(buf);
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
  public File getFilename() {
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
   * Explicitly close the file.
   * Note: following operations will likely cause IOExceptions.
   * 
   * @throws IOException
   */
  public synchronized void close() throws IOException {
    writable = false;
    if (lock != null) {
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
}