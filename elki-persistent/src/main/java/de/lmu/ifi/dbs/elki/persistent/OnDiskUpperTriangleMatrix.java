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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Class representing an upper triangle matrix backed by an on-disk array of
 * O((n+1)*n/2) size
 * 
 * @composed - - - OnDiskArray
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class OnDiskUpperTriangleMatrix implements AutoCloseable {
  /**
   * Serial number, also used for generating a magic
   */
  private static final long serialVersionUID = -4489942156357634702L;

  /**
   * Size of this class' header
   */
  private static final int TRIANGLE_HEADER_SIZE = 4;

  /**
   * Size of the matrix
   */
  private int matrixsize;

  /**
   * Data storage
   */
  private OnDiskArray array;

  /**
   * Constructor to access an existing array.
   * 
   * @param filename File name
   * @param magicseed Magic number
   * @param extraheadersize Size of extra header data
   * @param recordsize Record size
   * @param writable flag to open writable
   * @throws IOException on IO errors
   */
  public OnDiskUpperTriangleMatrix(File filename, int magicseed, int extraheadersize, int recordsize, boolean writable) throws IOException {
    array = new OnDiskArray(filename, OnDiskArray.mixMagic((int) serialVersionUID, magicseed), extraheadersize + TRIANGLE_HEADER_SIZE, recordsize, writable);
    ByteBuffer header = array.getExtraHeader();
    this.matrixsize = header.getInt();
    if(arraysize(matrixsize) != array.getNumRecords()) {
      throw new IOException("Matrix file size doesn't match specified dimensions: " + matrixsize + "->" + arraysize(matrixsize) + " vs. " + array.getNumRecords());
    }
  }

  /**
   * Constructor to access a new array.
   * 
   * @param filename File name
   * @param magicseed Magic number
   * @param extraheadersize Size of extra header data
   * @param recordsize Record size
   * @param matrixsize Size of matrix to store
   * @throws IOException on IO errors
   */
  public OnDiskUpperTriangleMatrix(File filename, int magicseed, int extraheadersize, int recordsize, int matrixsize) throws IOException {
    if(matrixsize >= 0xFFFF) {
      throw new RuntimeException("Matrix size is too big and will overflow the integer datatype.");
    }
    this.matrixsize = matrixsize;
    array = new OnDiskArray(filename, OnDiskArray.mixMagic((int) serialVersionUID, magicseed), extraheadersize + TRIANGLE_HEADER_SIZE, recordsize, arraysize(matrixsize));
    ByteBuffer header = array.getExtraHeader();
    header.putInt(this.matrixsize);
  }

  /**
   * Resize the matrix to cover newsize x newsize.
   * 
   * @param newsize New matrix size.
   * @throws IOException on IO errors
   */
  public synchronized void resizeMatrix(int newsize) throws IOException {
    if(newsize >= 0xFFFF) {
      throw new RuntimeException("Matrix size is too big and will overflow the integer datatype.");
    }
    if(!array.isWritable()) {
      throw new IOException("Can't resize a read-only array.");
    }
    array.resizeFile(arraysize(newsize));
    this.matrixsize = newsize;
    ByteBuffer header = array.getExtraHeader();
    header.putInt(this.matrixsize);
  }

  /**
   * Compute the size of the needed backing array from the matrix dimensions.
   * 
   * @param matrixsize size of the matrix
   * @return size of the array
   */
  private static int arraysize(int matrixsize) {
    return (matrixsize * (matrixsize + 1)) >> 1;
  }

  /**
   * Compute the offset within the file.
   * 
   * @param x First coordinate
   * @param y Second coordinate
   * @return Linear offset
   */
  private int computeOffset(int x, int y) {
    if(y > x) {
      return computeOffset(y, x);
    }
    return ((x * (x + 1)) >> 1) + y;
  }

  /**
   * Get a record buffer
   * 
   * @param x First coordinate
   * @param y Second coordinate
   * @return Byte buffer for the record
   * @throws IOException on IO errors
   */
  public synchronized ByteBuffer getRecordBuffer(int x, int y) throws IOException {
    if(x >= matrixsize || y >= matrixsize) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return array.getRecordBuffer(computeOffset(x, y));
  }
  
  /**
   * Close the matrix file.
   * 
   * @throws IOException on IO errors
   */
  public synchronized void close() throws IOException {
    array.close();
  }

  /**
   * Query the size of the matrix.
   * 
   * @return size of the matrix
   */
  public int getMatrixSize() {
    return matrixsize;
  }
}
