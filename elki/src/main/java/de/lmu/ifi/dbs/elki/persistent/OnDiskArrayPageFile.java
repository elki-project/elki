/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.persistent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferInputStream;

/**
 * A OnDiskArrayPageFile stores objects persistently that implement the
 * <code>Page</code> interface. For convenience each page is represented by a
 * single file. All pages are stored in a specified directory.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @composed - - - OnDiskArray
 * @composed - - - PageHeader
 * 
 * @param <P> Page type
 */
public class OnDiskArrayPageFile<P extends Page> extends AbstractStoringPageFile<P> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(OnDiskArrayPageFile.class);

  /**
   * Indicates an empty page.
   */
  private static final int EMPTY_PAGE = 0;

  /**
   * Indicates a filled page.
   */
  private static final int FILLED_PAGE = 1;

  /**
   * The file name to use
   */
  private File filename;

  /**
   * The file storing the pages.
   */
  private OnDiskArray file;

  /**
   * The header of this page file.
   */
  protected PageHeader header;

  /**
   * Whether or not the file originally existed
   */
  private final boolean existed;

  /**
   * Creates a new OnDiskArrayPageFile from an existing file.
   * 
   * @param pageSize page size
   * @param fileName the name of the file
   */
  public OnDiskArrayPageFile(int pageSize, String fileName) {
    super(pageSize);

    // init the file
    this.filename = new File(fileName);

    // create from existing file
    existed = this.filename.exists();
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
      countRead();
      return byteBufferToPage(this.file.getRecordBuffer(pageID));
    } catch (IOException e) {
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
      countWrite();
      byte[] array = pageToByteArray(null);
      file.getRecordBuffer(pageID).put(array);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write page to disk.
   * 
   * @param pageID page id
   * @param page the page which has to be written to disk
   */
  @Override
  public void writePage(int pageID, P page) {
    if (page.isDirty()) {
      try {
        countWrite();
        byte[] array = pageToByteArray(page);
        file.getRecordBuffer(pageID).put(array);
        page.setDirty(false);
      } catch (IOException e) {
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Clears this PageFile.
   */
  @Override
  public void clear() {
    try {
      file.resizeFile(0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reconstruct a serialized object from the specified byte array.
   * 
   * @param buffer the buffer from which the object should be reconstructed
   * @return a serialized object from the specified byte array
   */
  @SuppressWarnings("unchecked")
  private P byteBufferToPage(ByteBuffer buffer) {
    try (InputStream bais = new ByteBufferInputStream(buffer);
        ObjectInputStream ois = new ObjectInputStream(bais)) {
      int type = ois.readInt();
      if (type == EMPTY_PAGE) {
        return null;
      } else if (type == FILLED_PAGE) {
        return (P) ois.readObject();
      } else {
        throw new IllegalArgumentException("Unknown type: " + type);
      }
    } catch (IOException|ClassNotFoundException e) {
      LoggingUtil.exception(e);
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
      } else {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeInt(FILLED_PAGE);
        oos.writeObject(page);
        oos.close();
        baos.close();
        byte[] array = baos.toByteArray();
        if (array.length > this.pageSize) {
          throw new IllegalArgumentException("Size of page " + page + " is greater than specified" + " pagesize: " + array.length + " > " + pageSize);
        } else if (array.length == this.pageSize) {
          return array;
        }

        else {
          byte[] result = new byte[pageSize];
          System.arraycopy(array, 0, result, 0, array.length);
          return result;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("IOException occurred! ", e);
    }
  }

  @Override
  public boolean initialize(PageHeader header) {
    this.header = header;
    try {
      if (existed) {
        LoggingUtil.logExpensive(Level.INFO, "Create from existing file.");
        this.file = new OnDiskArray(filename, 0, header.size(), pageSize, true);

        // init the header
        {
          ByteBuffer buffer = file.getExtraHeader();
          byte[] bytes = new byte[buffer.remaining()];
          buffer.get(bytes);
          header.readHeader(bytes);
        }

        // reading empty nodes in Stack
        for (int i = 0; i < file.getNumRecords(); i++) {
          ByteBuffer buffer = file.getRecordBuffer(i);

          int type = buffer.getInt();
          if (type == EMPTY_PAGE) {
            emptyPages.push(i);
          } else if (type == FILLED_PAGE) {
            nextPageID = i + 1;
          } else {
            throw new IllegalArgumentException("Unknown type: " + type);
          }
          i++;
        }
        return true;
      }
      // create new file
      else {
        LoggingUtil.logExpensive(Level.INFO, "Create a new file.");

        // init the file
        this.file = new OnDiskArray(filename, 0, header.size(), pageSize, 0);

        // write the header
        ByteBuffer buffer = file.getExtraHeader();
        buffer.put(header.asByteArray());
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException("IOException occurred.", e);
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
