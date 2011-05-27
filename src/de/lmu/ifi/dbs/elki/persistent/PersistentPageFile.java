package de.lmu.ifi.dbs.elki.persistent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;

import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * A PersistentPageFile stores objects persistently that implement the
 * <code>Page</code> interface. For convenience each page is represented by a
 * single file. All pages are stored in a specified directory.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.composedOf PageHeader
 * @apiviz.composedOf RandomAccessFile
 * 
 * @param <P> Page type
 */
public class PersistentPageFile<P extends Page<P>> extends AbstractStoringPageFile<P> {
  /**
   * Our logger
   */
  private static final Logging logger = Logging.getLogger(PersistentPageFile.class);

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
  protected PageHeader header;

  /**
   * The type of pages we use.
   */
  protected final Class<P> pageclass;

  /**
   * Whether we are initializing from an existing file.
   */
  private boolean existed;

  /**
   * Creates a new PersistentPageFile from an existing file.
   * 
   * @param pageSize the page size
   * @param pageclass the class of pages to be used
   */
  public PersistentPageFile(int pageSize, String fileName, Class<P> pageclass) {
    super(pageSize);
    this.pageclass = pageclass;
    // init the file
    File f = new File(fileName);

    // create from existing file
    existed = f.exists();
    try {
      file = new RandomAccessFile(f, "rw");
    }
    catch(IOException e) {
      throw new AbortException("IO error in loading persistent page file.", e);
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
      readAccess++;
      long offset = ((long) (header.getReservedPages() + pageID)) * (long) pageSize;
      byte[] buffer = new byte[pageSize];
      file.seek(offset);
      file.read(buffer);
      P page = byteArrayToPage(buffer);
      if(page != null) {
        page.setFile(this);
      }
      return page;
    }
    catch(IOException e) {
      throw new RuntimeException("IOException occurred during reading of page " + pageID + "\n", e);
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
      // / put id to empty pages list
      super.deletePage(pageID);

      // delete from file
      writeAccess++;
      byte[] array = pageToByteArray(null);
      long offset = ((long) (header.getReservedPages() + pageID)) * (long) pageSize;
      file.seek(offset);
      file.write(array);
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This method is called by the cache if the <code>page</code> is not longer
   * stored in the cache and has to be written to disk.
   * 
   * @param page the page which has to be written to disk
   */
  @Override
  public void writePage(Integer pageID, P page) {
    try {
      writeAccess++;
      byte[] array = pageToByteArray(page);
      long offset = ((long) (header.getReservedPages() + pageID)) * (long) pageSize;
      assert offset >= 0 : header.getReservedPages() + " " + pageID + " " + pageSize + " " + offset;
      file.seek(offset);
      file.write(array);
      page.setDirty(false);
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Closes this file.
   */
  @Override
  public void close() {
    try {
      super.close();
      if(!emptyPages.isEmpty() && header instanceof TreeIndexHeader) {
        // write the list of empty pages to the end of the file
        ((TreeIndexHeader) header).writeEmptyPages(emptyPages, file);
      }
      ((TreeIndexHeader) header).setLargestPageID(nextPageID);
      header.writeHeader(file);
      file.close();
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Clears this PageFile.
   */
  @Override
  public void clear() {
    try {
      file.setLength(header.size());
    }
    catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reconstruct a serialized object from the specified byte array.
   * 
   * @param array the byte array from which the object should be reconstructed
   * @return a serialized object from the specified byte array
   */
  private P byteArrayToPage(byte[] array) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(array);
      ObjectInputStream ois = new ObjectInputStream(bais);
      int type = ois.readInt();
      if(type == EMPTY_PAGE) {
        return null;
      }
      else if(type == FILLED_PAGE) {
        P page;
        try {
          page = pageclass.newInstance();
          page.readExternal(ois);
        }
        catch(InstantiationException e) {
          throw new AbortException("Error instanciating an index page", e);
        }
        catch(IllegalAccessException e) {
          throw new AbortException("Error instanciating an index page", e);
        }
        catch(ClassNotFoundException e) {
          throw new AbortException("Error instanciating an index page", e);
        }
        return page;
      }
      else {
        throw new IllegalArgumentException("Unknown type: " + type);
      }
    }
    catch(IOException e) {
      throw new AbortException("IO Error in page file", e);
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
        page.writeExternal(oos);
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

  /** @return the random access file storing the pages. */
  public RandomAccessFile getFile() {
    return file;
  }

  /**
   * Get the header of this persistent page file.
   * 
   * @return the header used by this page file
   */
  public PageHeader getHeader() {
    return header;
  }

  /**
   * Increases the {@link AbstractStoringPageFile#readAccess readAccess} counter by
   * one.
   */
  public void increaseReadAccess() {
    readAccess++;
  }

  /**
   * Increases the {@link AbstractStoringPageFile#writeAccess writeAccess} counter by
   * one.
   */
  public void increaseWriteAccess() {
    writeAccess++;
  }

  /**
   * Set the next page id to the given value. If this means that any page ids
   * stored in <code>emptyPages</code> are smaller than
   * <code>next_page_id</code>, they are removed from this file's observation
   * stack.
   * 
   * @param next_page_id the id of the next page to be inserted (if there are no
   *        more empty pages to be filled)
   */
  @Override
  public void setNextPageID(int next_page_id) {
    this.nextPageID = next_page_id;
    while(!emptyPages.isEmpty() && emptyPages.peek() >= this.nextPageID) {
      emptyPages.pop();
    }
  }

  @Override
  public boolean initialize(PageHeader header) {
    try {
      if(existed) {
        logger.debug("Initializing from an existing page file.");

        // init the header
        this.header = header;
        header.readHeader(file);

        // reading empty nodes in Stack
        if(header instanceof TreeIndexHeader) {
          TreeIndexHeader tiHeader = (TreeIndexHeader) header;
          nextPageID = tiHeader.getLargestPageID();
          try {
            emptyPages = tiHeader.readEmptyPages(file);
          }
          catch(ClassNotFoundException e) {
            throw new RuntimeException("ClassNotFoundException occurred when reading empty pages.", e);
          }
        }
        else { // must scan complete file
          int i = 0;
          while(file.getFilePointer() + pageSize <= file.length()) {
            long offset = ((long) (header.getReservedPages() + i)) * (long) pageSize;
            byte[] buffer = new byte[pageSize];
            file.seek(offset);
            file.read(buffer);

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
      }
      // create new file
      else {
        logger.debug("Initializing with a new page file.");

        // writing header
        this.header = header;
        header.writeHeader(file);
      }
    }
    catch(IOException e) {
      throw new RuntimeException("IOException occurred.", e);
    }

    // Return "new file" status
    return existed;
  }
}