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
package elki.index.tree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import elki.persistent.DefaultPageHeader;
import elki.utilities.datastructures.arraylike.IntegerArray;

/**
 * Encapsulates the header information of a tree-like index structure. This
 * information is needed for persistent storage.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class TreeIndexHeader extends DefaultPageHeader {
  /**
   * The size of this header in Bytes, which is 20 Bytes ( 4 Bytes for
   * {@link #dirCapacity}, {@link #leafCapacity}, {@link #dirMinimum},
   * {@link #leafMinimum}, {@link #emptyPagesSize}), and {@link #largestPageID}.
   */
  private static int SIZE = 6 * 4;

  /**
   * The capacity of a directory node (= 1 + maximum number of entries in a
   * directory node).
   */
  int dirCapacity;

  /**
   * The capacity of a leaf node (= 1 + maximum number of entries in a leaf
   * node).
   */
  int leafCapacity;

  /**
   * The minimum number of entries in a directory node.
   */
  int dirMinimum;

  /**
   * The minimum number of entries in a leaf node.
   */
  int leafMinimum;

  /**
   * The number of bytes additionally needed for the listing of empty pages of
   * the headed page file.
   */
  private int emptyPagesSize = 0;

  /**
   * The largest ID used so far
   */
  private int largestPageID = 0;

  /**
   * Empty constructor for serialization.
   */
  public TreeIndexHeader() {
    super();
  }

  /**
   * Creates a new header with the specified parameters.
   * 
   * @param pageSize the size of a page in bytes
   * @param dirCapacity the maximum number of entries in a directory node
   * @param leafCapacity the maximum number of entries in a leaf node
   * @param dirMinimum the minimum number of entries in a directory node
   * @param leafMinimum the minimum number of entries in a leaf node
   */
  public TreeIndexHeader(int pageSize, int dirCapacity, int leafCapacity, int dirMinimum, int leafMinimum) {
    super(pageSize);
    this.dirCapacity = dirCapacity;
    this.leafCapacity = leafCapacity;
    this.dirMinimum = dirMinimum;
    this.leafMinimum = leafMinimum;
  }

  /**
   * Initializes this header from the specified file. Calls
   * {@link elki.persistent.DefaultPageHeader#readHeader(FileChannel)
   * DefaultPageHeader#readHeader(file)} and reads the integer values of
   * {@link #dirCapacity}, {@link #leafCapacity}, {@link #dirMinimum},
   * {@link #leafMinimum} and {@link #emptyPagesSize} from the file.
   */
  @Override
  public void readHeader(ByteBuffer buffer) {
    super.readHeader(buffer);
    this.dirCapacity = buffer.getInt();
    this.leafCapacity = buffer.getInt();
    this.dirMinimum = buffer.getInt();
    this.leafMinimum = buffer.getInt();
    this.emptyPagesSize = buffer.getInt();
    this.largestPageID = buffer.getInt();
  }

  /**
   * Writes this header to the specified file. Writes the integer values of
   * {@link #dirCapacity}, {@link #leafCapacity}, {@link #dirMinimum},
   * {@link #leafMinimum} and {@link #emptyPagesSize} to the file.
   */
  @Override
  public void writeHeader(ByteBuffer buffer) {
    super.writeHeader(buffer);
    buffer.putInt(this.dirCapacity) //
        .putInt(this.leafCapacity) //
        .putInt(this.dirMinimum) //
        .putInt(this.leafMinimum) //
        .putInt(this.emptyPagesSize) //
        .putInt(this.largestPageID) //
        .flip();
  }

  /**
   * Returns the capacity of a directory node (= 1 + maximum number of entries
   * in a directory node).
   * 
   * @return the capacity of a directory node (= 1 + maximum number of entries
   *         in a directory node)
   */
  public int getDirCapacity() {
    return dirCapacity;
  }

  /**
   * Returns the capacity of a leaf node (= 1 + maximum number of entries in a
   * leaf node).
   * 
   * @return the capacity of a leaf node (= 1 + maximum number of entries in a
   *         leaf node)
   */
  public int getLeafCapacity() {
    return leafCapacity;
  }

  /**
   * Returns the minimum number of entries in a directory node.
   * 
   * @return the minimum number of entries in a directory node
   */
  public int getDirMinimum() {
    return dirMinimum;
  }

  /**
   * Returns the minimum number of entries in a leaf node.
   * 
   * @return the minimum number of entries in a leaf node
   */
  public int getLeafMinimum() {
    return leafMinimum;
  }

  /** @return the number of bytes needed for the listing of empty pages */
  public int getEmptyPagesSize() {
    return emptyPagesSize;
  }

  /**
   * Set the size required by the listing of empty pages.
   * 
   * @param emptyPagesSize the number of bytes needed for this listing of empty
   *        pages
   */
  public void setEmptyPagesSize(int emptyPagesSize) {
    this.emptyPagesSize = emptyPagesSize;
  }

  public int getLargestPageID() {
    return largestPageID;
  }

  public void setLargestPageID(int largestPageID) {
    this.largestPageID = largestPageID;
  }

  /**
   * Returns {@link elki.persistent.DefaultPageHeader#size()}
   * plus the value of {@link #SIZE}). Note, this is only the base size and
   * probably <em>not</em> the overall size of this header, as there may be
   * empty pages to be maintained.
   */
  @Override
  public int size() {
    return super.size() + SIZE;
  }

  /**
   * Write the indices of empty pages the the end of <code>file</code>. Calling
   * this method should be followed by a {@link #writeHeader(FileChannel)}.
   * 
   * @param emptyPages the stack of empty page ids which remain to be filled
   * @param file File to work with
   * @throws IOException thrown on IO errors
   */
  public void writeEmptyPages(IntegerArray emptyPages, FileChannel file) throws IOException {
    if(emptyPages.isEmpty()) {
      this.emptyPagesSize = 0;
      return; // nothing to write
    }
    emptyPages.sort();
    this.emptyPagesSize = emptyPages.size * Integer.BYTES;
    ByteBuffer buf = ByteBuffer.allocateDirect(this.emptyPagesSize);
    buf.asIntBuffer().put(emptyPages.data, 0, emptyPages.size);
    file.write(buf, file.size());
  }

  /**
   * Read the empty pages from the end of <code>file</code>.
   * 
   * @param file File to work with
   * @return a stack of empty pages in <code>file</code>
   * @throws IOException thrown on IO errors
   * @throws ClassNotFoundException if the stack of empty pages could not be
   *         correctly read from file
   */
  public IntegerArray readEmptyPages(FileChannel file) throws IOException, ClassNotFoundException {
    IntegerArray emptyPages = new IntegerArray();
    if(emptyPagesSize > 0) {
      int n = emptyPagesSize / Integer.BYTES;
      if(n > emptyPages.data.length) {
        emptyPages.data = new int[n];
      }
      ByteBuffer buf = ByteBuffer.allocateDirect(emptyPagesSize);
      file.read(buf, file.size() - emptyPagesSize);
      buf.asIntBuffer().get(emptyPages.data, 0, n);
    }
    return emptyPages;
  }

}
