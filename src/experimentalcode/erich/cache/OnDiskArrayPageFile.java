package experimentalcode.erich.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.Cache;
import de.lmu.ifi.dbs.elki.persistent.Page;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageHeader;

/**
 * A OnDiskArrayPageFile stores objects persistently that implement the
 * <code>Page</code> interface. For convenience each page is represented by a
 * single file. All pages are stored in a specified directory.
 * 
 * @author Elke Achtert
 * @param <P> Page type
 */
public class OnDiskArrayPageFile<P extends Page<P>> extends PageFile<P> {
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
  private final OnDiskArray file;

  /**
   * The header of this page file.
   */
  protected final PageHeader header;

  /**
   * Creates a new OnDiskArrayPageFile from an existing file.
   * 
   * @param header the header of this file
   * @param fileName the name of the file
   * @param cacheSize the size of the cache in Byte
   * @param cache the class of the cache to be used
   */
  public OnDiskArrayPageFile(PageHeader header, int cacheSize, Cache<P> cache, String fileName) {
    super();

    try {
      // init the file
      File f = new File(fileName);

      // create from existing file
      if(f.exists()) {
        Logging.getLogger(this.getClass()).verbose("Create from existing file.");
        this.file = new OnDiskArray(f, 0, header.size(), pageSize, true);

        // init the header
        this.header = header;
        header.readHeader(file.readExtraHeader());

        // init the cache
        initCache(header.getPageSize(), cacheSize, cache);

        // reading empty nodes in Stack
        for(int i = 0; i < file.getNumRecords(); i++) {
          byte[] buffer = file.readRecord(i);

          ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
          ObjectInputStream ois = new ObjectInputStream(bais);
          int type = ois.readInt();
          if(type == EMPTY_PAGE) {
            emptyPages.push(i);
          }
          else if(type == FILLED_PAGE) {
            nextPageID = i + 1;
          }
          else {
            throw new IllegalArgumentException("Unknown type: " + type);
          }
          i++;
        }
      }
      // create new file
      else {
        Logging.getLogger(this.getClass()).verbose("Create a new file.");

        // init the file
        this.file = new OnDiskArray(f, 0, header.size(), pageSize, 0);

        // writing header
        this.header = header;
        this.file.writeExtraHeader(header.asByteArray());

        // init the cache
        initCache(header.getPageSize(), cacheSize, cache);
      }
    }
    catch(IOException e) {
      throw new RuntimeException("IOException occurred.", e);
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
      if(page == null) {
        readAccess++;
        page = byteArrayToPage(this.file.readRecord(pageID));
        if(page != null) {
          // noinspection unchecked
          page.setFile(this);
          cache.put(page);
        }
      }
      return page;

    }
    catch(IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException("IOException occurred during reading of page " + pageID, e);
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
      file.writeRecord(pageID, array);
    }
    catch(IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * This method is called by the cache if the <code>page</code> is not longer
   * stored in the cache and has to be written to disk.
   * 
   * @param page the page which has to be written to disk
   */
  public void objectRemoved(P page) {
    if(page.isDirty()) {
      try {
        page.setDirty(false);
        writeAccess++;
        byte[] array = pageToByteArray(page);
        file.writeRecord(page.getID(), array);
      }
      catch(IOException e) {
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
    catch(IOException e) {
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
      file.resizeFile(0);
    }
    catch(IOException e) {
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
      if(type == EMPTY_PAGE) {
        return null;
      }
      else if(type == FILLED_PAGE) {
        return (P) ois.readObject();
      }
      else {
        throw new IllegalArgumentException("Unknown type: " + type);
      }
    }
    catch(IOException e) {
      // TODO exception handling
      e.printStackTrace();
      return null;
    }
    catch(ClassNotFoundException e) {
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
      if(page == null) {
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
        if(array.length > this.pageSize) {
          throw new IllegalArgumentException("Size of page " + page + " is greater than specified" + " pagesize: " + array.length + " > " + pageSize);
        }
        else if(array.length == this.pageSize) {
          return array;
        }

        else {
          byte[] result = new byte[pageSize];
          System.arraycopy(array, 0, result, 0, array.length);
          return result;
        }
      }
    }
    catch(IOException e) {
      throw new RuntimeException("IOException occurred! ", e);
    }
  }
}
