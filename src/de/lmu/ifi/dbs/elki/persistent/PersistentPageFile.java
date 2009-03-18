package de.lmu.ifi.dbs.elki.persistent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;

/**
 * A PersistentPageFile stores objects persistently that implement the
 * <code>Page</code> interface. For convenience each page is represented by a
 * single file. All pages are stored in a specified directory.
 *
 * @author Elke Achtert 
 */
public class PersistentPageFile<P extends Page<P>> extends PageFile<P> {

  /**
   * Indicates an empty page.
   */
  private static final int EMPTY_PAGE = 0;

  /**
   * Indicates a filled page.
   */
  private static final int FILLED_PAGE = 1;

  /**
   * The file storing the pages.
   */
  private final RandomAccessFile file;

  /**
   * The header of this page file.
   */
  protected final PageHeader header;

  /**
   * Creates a new PersistentPageFile from an existing file.
   *
   * @param header    the header of this file
   * @param fileName  the name of the file
   * @param cacheSize the size of the cache in Byte
   * @param cache     the class of the cache to be used
   */
  public PersistentPageFile(PageHeader header, int cacheSize, Cache<P> cache,
                            String fileName) {
    super();

    try {
      // init the file
      File f = new File(fileName);

      // create from existing file
      if (f.exists()) {
    	  verbose("Create from existing file.");
        file = new RandomAccessFile(f, "rw");

        // init the header
        this.header = header;
        header.readHeader(file);

        // init the cache
        initCache(header.getPageSize(), cacheSize, cache);

        // reading empty nodes in Stack
        int i = 0;
        while (file.getFilePointer() + pageSize <= file.length()) {
          int offset = header.size() + pageSize * i;
          byte[] buffer = new byte[pageSize];
          file.seek(offset);
          file.read(buffer);

          ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
          ObjectInputStream ois = new ObjectInputStream(bais);
          int type = ois.readInt();
          if (type == EMPTY_PAGE) {
            emptyPages.push(i);
          }
          else if (type == FILLED_PAGE) {
            nextPageID = i + 1;
          }
          else
            throw new IllegalArgumentException("Unknown type: "
                                               + type);
          i++;
        }
      }
      // create new file
      else {
    	  verbose("Create a new file.");

        // init the file
        this.file = new RandomAccessFile(f, "rw");
        this.file.setLength(0);

        // writing header
        this.header = header;
        header.writeHeader(file);

        // init the cache
        initCache(header.getPageSize(), cacheSize, cache);
      }
    }
    catch (IOException e) {
      throw new RuntimeException("IOException occurred: \n " + e);
    }
  }

  /**
   * Reads the page with the given id from this file.
   *
   * @param pageID the id of the page to be returned
   * @return the page with the given pageId
   */
  @Override
  public P readPage(int pageID) {
    try {
      // try to get from cache
      P page = super.readPage(pageID);

      // get from file and put to cache
      if (page == null) {
        readAccess++;
        int offset = header.size() + pageSize * pageID;
        byte[] buffer = new byte[pageSize];
        file.seek(offset);
        file.read(buffer);
        page = byteArrayToPage(buffer);
        if (page != null) {
          //noinspection unchecked
          page.setFile(this);
          cache.put(page);
        }
      }
      return page;

    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException(
      "IOException occurred during reading of page " + pageID
      + "\n" + e);
    }
  }

  /**
   * Deletes the node with the specified id from this file.
   *
   * @param pageID the id of the node to be deleted
   */
  @Override
  public void deletePage(int pageID) {
    try {
      // / put id to empty nodes and
      // delete from cache
      super.deletePage(pageID);

      // delete from file
      writeAccess++;
      byte[] array = pageToByteArray(null);
      int offset = header.size() + pageSize * pageID;
      file.seek(offset);
      file.write(array);
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * This method is called by the cache if the <code>page</code> is not
   * longer stored in the cache and has to be written to disk.
   *
   * @param page the page which has to be written to disk
   */
  public void objectRemoved(P page) {
    if (page.isDirty()) {
      try {
        page.setDirty(false);
        writeAccess++;
        byte[] array = pageToByteArray(page);
        int offset = header.size() + pageSize * page.getID();
        file.seek(offset);
        file.write(array);
      }
      catch (IOException e) {
        e.fillInStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Closes this file.
   */
  @Override
  public void close() {
    try {
      super.close();
      file.close();
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * Clears this PageFile.
   */
  @Override
  public void clear() {
    try {
      super.clear();
      file.setLength(header.size());
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * Reconstruct a serialized object from the specified byte array.
   *
   * @param array the byte array from which the object should be reconstructed
   * @return a serialized object from the specified byte array
   */
  @SuppressWarnings("unchecked")
  private P byteArrayToPage(byte[] array) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(array);
      ObjectInputStream ois = new ObjectInputStream(bais);
      int type = ois.readInt();
      if (type == EMPTY_PAGE)
        return null;
      else if (type == FILLED_PAGE) {
        return (P) ois.readObject();
      }
      else
        throw new IllegalArgumentException("Unknown type: " + type);
    }
    catch (IOException e) {
        // TODO exception handling
      e.printStackTrace();
      return null;
    }
    catch (ClassNotFoundException e) {
        // TODO exception handling
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Serializes an object into a byte array.
   *
   * @param page the object to be serialized
   * @return the byte array
   */
  private byte[] pageToByteArray(P page) {
    try {
      if (page == null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeInt(EMPTY_PAGE);
        oos.close();
        baos.close();
        byte[] array = baos.toByteArray();
        byte[] result = new byte[pageSize];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
      }
      else {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeInt(FILLED_PAGE);
        oos.writeObject(page);
        oos.close();
        baos.close();
        byte[] array = baos.toByteArray();
        if (array.length > this.pageSize) {
          throw new IllegalArgumentException("Size of page " + page
                                             + " is greater than specified" + " pagesize: "
                                             + array.length + " > " + pageSize);
        }
        else if (array.length == this.pageSize)
          return array;

        else {
          byte[] result = new byte[pageSize];
          System.arraycopy(array, 0, result, 0, array.length);
          return result;
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException("IOException occurred! ", e);
    }
  }
}
