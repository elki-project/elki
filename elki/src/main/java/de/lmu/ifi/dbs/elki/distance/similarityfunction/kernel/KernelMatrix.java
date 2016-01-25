package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Kernel matrix representation.
 * 
 * @author Simon Paradies
 * @since 0.2
 * 
 * @apiviz.uses PrimitiveSimilarityFunction
 */
public class KernelMatrix {
  /**
   * The kernel matrix
   */
  Matrix kernel;

  /**
   * Static mapping from DBIDs to indexes.
   */
  DBIDMap idmap;

  /**
   * Map a DBID to its offset
   * 
   * TODO: move to shared code.
   * 
   * @author Erich Schubert
   * @apiviz.exclude
   */
  private static interface DBIDMap {
    /**
     * Get the offset of the DBID in the range.
     * 
     * @param id ID
     * @return Offset
     */
    int getOffset(DBIDRef id);

    /**
     * Get an array iterator, for scanning.
     * 
     * @return Array iterator
     */
    DBIDArrayIter iter();
  }

  /**
   * Map a DBID to an integer offset, DBIDRange version.
   * 
   * @author Erich Schubert
   * @apiviz.exclude
   */
  private static class RangeMap implements DBIDMap {
    DBIDRange range;

    public RangeMap(DBIDRange range) {
      super();
      this.range = range;
    }

    @Override
    public int getOffset(DBIDRef id) {
      return range.getOffset(id);
    }

    @Override
    public DBIDArrayIter iter() {
      return range.iter();
    }
  }

  /**
   * Map a DBID to an integer offset, Version to support arbitrary DBIDs.
   * 
   * @author Erich Schubert
   * @apiviz.exclude
   */
  private static class SortedArrayMap implements DBIDMap {
    ArrayModifiableDBIDs ids;

    public SortedArrayMap(DBIDs ids) {
      super();
      this.ids = DBIDUtil.newArray(ids);
      this.ids.sort();
    }

    @Override
    public int getOffset(DBIDRef id) {
      return ids.binarySearch(id);
    }

    @Override
    public DBIDArrayIter iter() {
      return ids.iter();
    }
  }

  /**
   * Provides a new kernel matrix.
   * 
   * @param kernelFunction the kernel function used to compute the kernel matrix
   * @param relation the database that holds the objects
   * @param ids the IDs of those objects for which the kernel matrix is computed
   */
  public <O> KernelMatrix(PrimitiveSimilarityFunction<? super O> kernelFunction, final Relation<? extends O> relation, final DBIDs ids) {
    this.kernel = new Matrix(ids.size(), ids.size());
    if(ids instanceof DBIDRange) {
      this.idmap = new RangeMap((DBIDRange) ids);
    }
    else {
      this.idmap = new SortedArrayMap(ids);
    }

    DBIDArrayIter i1 = this.idmap.iter(), i2 = this.idmap.iter();
    for(i1.seek(0); i1.valid(); i1.advance()) {
      O o1 = relation.get(i1);
      for(i2.seek(i1.getOffset()); i2.valid(); i2.advance()) {
        double value = kernelFunction.similarity(o1, relation.get(i2));
        kernel.set(i1.getOffset(), i2.getOffset(), value);
        kernel.set(i2.getOffset(), i1.getOffset(), value);
      }
    }
  }

  /**
   * Provides a new kernel matrix.
   * 
   * @param kernelFunction the kernel function used to compute the kernel matrix
   * @param relation the database that holds the objects
   * @param ids the IDs of those objects for which the kernel matrix is computed
   */
  public <O> KernelMatrix(SimilarityQuery<? super O> kernelFunction, final Relation<? extends O> relation, final DBIDs ids) {
    LoggingUtil.logExpensive(Level.FINER, "Computing kernel matrix");
    kernel = new Matrix(ids.size(), ids.size());
    if(ids instanceof DBIDRange) {
      this.idmap = new RangeMap((DBIDRange) ids);
    }
    else {
      this.idmap = new SortedArrayMap(ids);
    }
    DBIDArrayIter i1 = idmap.iter(), i2 = idmap.iter();
    for(i1.seek(0); i1.valid(); i1.advance()) {
      O o1 = relation.get(i1);
      for(i2.seek(i1.getOffset()); i2.valid(); i2.advance()) {
        double value = kernelFunction.similarity(o1, i2);
        kernel.set(i1.getOffset(), i2.getOffset(), value);
        kernel.set(i2.getOffset(), i1.getOffset(), value);
      }
    }
  }

  /**
   * Makes a new kernel matrix from matrix (with data copying).
   * 
   * @param matrix a matrix
   */
  public KernelMatrix(final Matrix matrix) {
    kernel = matrix.copy();
  }

  /**
   * Returns the kernel distance between the two specified objects.
   * 
   * @param o1 first ObjectID
   * @param o2 second ObjectID
   * @return the distance between the two objects
   */
  public double getDistance(final DBIDRef o1, final DBIDRef o2) {
    return Math.sqrt(getSquaredDistance(o1, o2));
  }

  /**
   * Get the kernel matrix.
   * 
   * @return kernel
   */
  public Matrix getKernel() {
    return kernel;
  }

  /**
   * Returns the squared kernel distance between the two specified objects.
   * 
   * @param id1 first ObjectID
   * @param id2 second ObjectID
   * @return the distance between the two objects
   */
  public double getSquaredDistance(final DBIDRef id1, final DBIDRef id2) {
    final int o1 = idmap.getOffset(id1), o2 = idmap.getOffset(id2);
    return kernel.get(o1, o1) + kernel.get(o2, o2) - 2 * kernel.get(o1, o2);
  }

  /**
   * Returns the ith kernel matrix column for all objects in ids
   * 
   * @param i1 the column which should be returned
   * @param ids the objects
   * @return the ith kernel matrix column for all objects in ids
   */
  @Deprecated
  public Matrix getSubColumn(final DBIDRef i1, final DBIDs ids) {
    final int[] ID = new int[1];
    ID[0] = idmap.getOffset(i1);
    final int[] IDs = new int[ids.size()];
    int i = 0;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance(), i++) {
      IDs[i] = idmap.getOffset(it);
    }
    return kernel.getMatrix(IDs, ID);
  }

  /**
   * Returns a sub kernel matrix for all objects in ids
   * 
   * @param ids the objects
   * @return a sub kernel matrix for all objects in ids.
   */
  public Matrix getSubMatrix(DBIDs ids) {
    final int[] IDs = new int[ids.size()];
    int i = 0;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance(), i++) {
      IDs[i] = idmap.getOffset(it);
    }
    return kernel.getMatrix(IDs, IDs);
  }

  /**
   * Centers the matrix in feature space according to Smola et Schoelkopf,
   * Learning with Kernels p. 431 Alters the input matrix. If you still need the
   * original matrix, use
   * <code>centeredMatrix = centerKernelMatrix(uncenteredMatrix.copy()) {</code>
   * 
   * @param matrix the matrix to be centered
   * @return centered matrix (for convenience)
   */
  public static Matrix centerMatrix(final Matrix matrix) {
    // FIXME: implement more efficiently. Maybe in matrix class itself?
    final Matrix normalizingMatrix = new Matrix(matrix.getRowDimensionality(), matrix.getColumnDimensionality(), 1.0 / matrix.getColumnDimensionality());
    return matrix.minusEquals(normalizingMatrix.times(matrix)).minusEquals(matrix.times(normalizingMatrix)).plusEquals(normalizingMatrix.times(matrix).times(normalizingMatrix));
  }

  @Override
  public String toString() {
    return super.toString();
  }

  /**
   * Centers the Kernel Matrix in Feature Space according to Smola et.
   * Schoelkopf, Learning with Kernels p. 431 Alters the input matrix. If you
   * still need the original matrix, use
   * <code>centeredMatrix = centerKernelMatrix(uncenteredMatrix.copy()) {</code>
   * 
   * @param kernelMatrix the kernel matrix to be centered
   * @return centered kernelMatrix (for convenience)
   */
  public static Matrix centerKernelMatrix(final KernelMatrix kernelMatrix) {
    return centerMatrix(kernelMatrix.getKernel());
  }

  /**
   * Get the kernel similarity for the given objects.
   * 
   * @param id1 First object
   * @param id2 Second object
   * @return Similarity.
   */
  public double getSimilarity(DBIDRef id1, DBIDRef id2) {
    return kernel.get(idmap.getOffset(id1), idmap.getOffset(id2));
  }
}
