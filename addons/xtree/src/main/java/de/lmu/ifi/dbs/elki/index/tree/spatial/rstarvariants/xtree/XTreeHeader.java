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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree;

import java.io.IOException;
import java.io.RandomAccessFile;

import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;

/**
 * Encapsulates the header information of an XTree index structure. This
 * information is needed for persistent storage.
 * 
 * @author Marisa Thoma
 * @since 0.7.5
 */
public class XTreeHeader extends TreeIndexHeader {
  /**
   * The size of this header in bytes, which is 32 Bytes. We have the integers
   * {@link #min_fanout} and {@link #dimensionality} (each 4 bytes), the floats
   * {@link #max_overlap} (each 4 bytes) and the
   * 8 bytes each for the longs {@link #num_elements} and
   * {@link #supernode_offset}.
   */
  private static int SIZE = 32;

  /**
   * Minimum size to be allowed for page sizes after a split in case of a
   * minimum overlap split.
   */
  private int min_fanout;

  /** Maximally allowed overlap. */
  private float max_overlap = (float) .2;

  /** Number of elements stored in this tree. */
  private long num_elements;

  /** Dimensionality of the objects in this header's tree. */
  private int dimensionality;

  /**
   * Number of bytes to be skipped for reading the first supernode on disc. This
   * number is only to be used in the beginning, when first loading the XTree.
   * Later on, the supernodes may be located elsewhere (or disappear).
   */
  private long supernode_offset = -1;

  public XTreeHeader() {
    super();
  }

  public XTreeHeader(int pageSize, int dirCapacity, int leafCapacity, int dirMinimum, int leafMinimum, int min_fanout, long num_elements, int dimensionality, float max_overlap) {
    super(pageSize, dirCapacity, leafCapacity, dirMinimum, leafMinimum);
    this.min_fanout = min_fanout;
    this.num_elements = num_elements;
    this.dimensionality = dimensionality;
    this.max_overlap = max_overlap;
  }

  /**
   * Initializes this header from the specified file. Reads the integer values
   * <code>version</code>, <code>pageSize</code>, {@link #dirCapacity},
   * {@link #leafCapacity}, {@link #dirMinimum}, and {@link #leafMinimum}, as
   * well as the minimum fanout {@link #min_fanout}, the tree's dimension
   * {@link #dimensionality}, the maximum overlap
   * {@link #max_overlap} and the supernode offset {@link #supernode_offset}
   * from the file.
   */
  @Override
  public void readHeader(RandomAccessFile file) throws IOException {
    super.readHeader(file);
    this.min_fanout = file.readInt();
    this.num_elements = file.readLong();
    this.dimensionality = file.readInt();
    this.max_overlap = file.readFloat();
    this.supernode_offset = file.readLong();
  }

  /**
   * Writes this header to the specified file. Writes to file the integer values
   * <code>version</code>, <code>pageSize</code>, {@link #dirCapacity},
   * {@link #leafCapacity}, {@link #dirMinimum}, {@link #leafMinimum},
   * {@link #min_fanout}, {@link #dimensionality}, the <code>float</code>
   * {@link #max_overlap} and the
   * <code>long</code> {@link #supernode_offset}.
   */
  @Override
  public void writeHeader(RandomAccessFile file) throws IOException {
    super.writeHeader(file);
    file.writeInt(min_fanout);
    file.writeLong(num_elements);
    file.writeInt(dimensionality);
    file.writeFloat(max_overlap);
    file.writeLong(supernode_offset);
  }

  /**
   * @return the minimum size to be allowed for page sizes after a split in case
   *         of a minimum overlap split.
   */
  public int getMin_fanout() {
    return min_fanout;
  }

  /**
   * @return the fraction of pages to be re-inserted before splitting
   */
  public float getMaxOverlap() {
    return max_overlap;
  }

  /**
   * @return the bytes offset for the first supernode entry on disc
   */
  public long getSupernode_offset() {
    return supernode_offset;
  }

  /**
   * Assigns this header a new supernode offset.
   * 
   * @param supernode_offset
   */
  public void setSupernode_offset(long supernode_offset) {
    this.supernode_offset = supernode_offset;
  }

  /**
   * Returns {@link TreeIndexHeader#size()} plus the value of {@link #SIZE}).
   */
  @Override
  public int size() {
    return super.size() + SIZE;
  }

  /**
   * @param dimensionality the dimensionality of the objects to be stored in
   *        this header's tree
   */
  public void setDimensionality(int dimensionality) {
    this.dimensionality = dimensionality;
  }

  /**
   * @return Dimensionality of the objects in this header's tree
   */
  public int getDimensionality() {
    return dimensionality;
  }

  /**
   * @param num_elements The number of elements stored in the tree
   */
  public void setNumberOfElements(long num_elements) {
    this.num_elements = num_elements;
  }

  /**
   * @return The number of elements stored in the tree
   */
  public long getNumberOfElements() {
    return num_elements;
  }
}