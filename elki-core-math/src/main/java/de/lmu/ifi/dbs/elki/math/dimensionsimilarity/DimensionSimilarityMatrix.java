package de.lmu.ifi.dbs.elki.math.dimensionsimilarity;

import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

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

/**
 * Class representing a similarity matrix between dimensions.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @apiviz.uses PrimsMinimumSpanningTree
 */
public abstract class DimensionSimilarityMatrix {
  /**
   * Adapter class for running Prim's minimum spanning tree algorithm.
   */
  public static final PrimAdapter PRIM_ADAPTER = new PrimAdapter();

  /**
   * Flat, symmetric storage. We use a lower triangle matrix.
   * 
   * Basic memory layout (X = undef, S = symmetric)
   * 
   * <pre>
   *  X  S  S  S  S  S
   *  0  X  S  S  S  S
   *  1  2  X  S  S  S
   *  3  4  5  X  S  S
   *  6  7  8  9  X  S
   * 10 11 12 13 14  X
   * </pre>
   * 
   * 
   */
  private final double[] sim;

  /**
   * Constructor.
   * 
   * @param dims Number of dimensions to allocate.
   */
  protected DimensionSimilarityMatrix(int dims) {
    super();
    this.sim = new double[index(0, dims)];
  }

  /**
   * Number of dimensions.
   * 
   * @return Size of dimensions array.
   */
  public abstract int size();

  /**
   * Get the dimension at position idx.
   * 
   * @param idx Position
   * @return Dimension
   */
  public abstract int dim(int idx);

  /**
   * Set the value of the given matrix position.
   * 
   * Note that {@code x == y} is invalid!
   * 
   * @param x X index coordinate
   * @param y Y index coordinate
   * @param val Value
   */
  public void set(int x, int y, double val) {
    sim[index(x, y)] = val;
  }

  /**
   * Get the value of the given matrix position.
   * 
   * Note that {@code x == y} is invalid!
   * 
   * @param x X index coordinate
   * @param y Y index coordinate
   * @return Value
   */
  public double get(int x, int y) {
    return sim[index(x, y)];
  }

  /**
   * Indexing function for triangular matrix.
   * 
   * @param x X coordinate
   * @param y Y coordinate
   * @return Array index
   */
  private int index(int x, int y) {
    assert (x != y);
    if (x > y) {
      return index(y, x);
    }
    return ((y * (y - 1)) >> 1) + x;
  }

  /**
   * Transform linear triangle matrix into a full matrix.
   * 
   * @return New matrix
   */
  public Matrix copyToFullMatrix() {
    final int dim = size();
    Matrix m = new Matrix(dim, dim);
    double[][] ref = m.getArrayRef();
    int i = 0;
    for (int y = 1; y < dim; y++) {
      for (int x = 0; x < y; x++) {
        ref[x][y] = sim[i];
        ref[y][x] = sim[i];
        ++i;
      }
    }
    return m;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    final int d = size();
    for (int x = 1; x < d; x++) {
      for (int y = 0; y < x; y++) {
        if (y > 0) {
          buf.append(' ');
        }
        buf.append(get(x, y));
      }
      buf.append('\n');
    }
    return buf.toString();
  }

  /**
   * Complete matrix of pairwise dimension similarities.
   * 
   * @author Erich Schubert
   */
  public static class FullDimensionSimilarityMatrix extends DimensionSimilarityMatrix {
    /**
     * Number of dimensions.
     */
    final int dims;

    /**
     * Constructor.
     * 
     * @param dims Number of dimensions
     */
    public FullDimensionSimilarityMatrix(int dims) {
      super(dims);
      this.dims = dims;
    }

    @Override
    public int size() {
      return dims;
    }

    @Override
    public int dim(int idx) {
      return idx;
    }
  }

  /**
   * Partial matrix of pairwise dimension similarities.
   * 
   * @author Erich Schubert
   */
  public static class PartialDimensionSimilarityMatrix extends DimensionSimilarityMatrix {
    /**
     * Enumeration of dimensions to use (so we could use a subset only!)
     */
    final int[] dims;

    /**
     * Constructor.
     * 
     * @param dims Array of dimensions to process.
     */
    public PartialDimensionSimilarityMatrix(int[] dims) {
      super(dims.length);
      this.dims = dims;
    }

    @Override
    public int size() {
      return dims.length;
    }

    @Override
    public int dim(int idx) {
      return dims[idx];
    }
  }

  /**
   * Adapter class for running prim's algorithm.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class PrimAdapter implements PrimsMinimumSpanningTree.Adapter<DimensionSimilarityMatrix> {
    /**
     * Constructor. Use static instance!
     */
    protected PrimAdapter() {
      super();
    }

    @Override
    public double distance(DimensionSimilarityMatrix data, int i, int j) {
      return -Math.abs(data.get(i, j));
    }

    @Override
    public int size(DimensionSimilarityMatrix data) {
      return data.size();
    }

  }

  /**
   * Make a full dimension similarity matrix.
   * 
   * @param dims Number of dimensions.
   * @return Matrix
   */
  public static DimensionSimilarityMatrix make(int dims) {
    return new FullDimensionSimilarityMatrix(dims);
  }

  /**
   * Make a partial dimension similarity matrix.
   * 
   * @param dims Array of relevant dimensions
   * @return Matrix
   */
  public static DimensionSimilarityMatrix make(int[] dims) {
    return new PartialDimensionSimilarityMatrix(dims);
  }
}
