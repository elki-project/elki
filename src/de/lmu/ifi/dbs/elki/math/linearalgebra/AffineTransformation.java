package de.lmu.ifi.dbs.elki.math.linearalgebra;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * Affine transformations implemented using homogeneous coordinates.
 * 
 * The use of homogeneous coordinates allows the combination of multiple affine
 * transformations (rotations, translations, scaling) into a single matrix
 * operation (of dimensionality dim+1), and also the construction of an inverse
 * transformation.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Matrix
 * @apiviz.uses Matrix
 * @apiviz.uses Vector
 */
public class AffineTransformation {
  /**
   * the dimensionality of the transformation
   */
  private int dim;

  /**
   * The transformation matrix of dim+1 x dim+1 for homogeneous coordinates
   */
  private Matrix trans;

  /**
   * the inverse transformation
   */
  private Matrix inv = null;

  /**
   * Constructor for an identity transformation.
   * 
   * @param dim dimensionality
   */
  public AffineTransformation(int dim) {
    super();
    this.dim = dim;
    this.trans = Matrix.unitMatrix(dim + 1);
  }

  /**
   * Trivial constructor with all fields, mostly for cloning
   * 
   * @param dim dimensionality
   * @param trans transformation matrix
   * @param inv inverse matrix
   */
  public AffineTransformation(int dim, Matrix trans, Matrix inv) {
    super();
    this.dim = dim;
    this.trans = trans;
    this.inv = inv;
  }

  /**
   * Generate a transformation that reorders axes in the given way.
   * 
   * The list of axes to be used should not contain duplicates, or the resulting
   * matrix will not be invertible. It does not have to be complete however, in
   * particular an empty list will result in the identity transform: unmentioned
   * axes will be appended in their original order.
   * 
   * @param dim Dimensionality of vector space (resulting Matrix will be dim+1 x
   *        dim+1)
   * @param axes (Partial) list of axes
   * @return new transformation to do the requested reordering
   */
  public static AffineTransformation reorderAxesTransformation(int dim, int[] axes) {
    Matrix m = Matrix.zeroMatrix(dim + 1);
    // insert ones appropriately:
    for(int i = 0; i < axes.length; i++) {
      assert (0 < axes[i] && axes[i] <= dim);
      m.set(i, axes[i] - 1, 1.0);
    }
    int useddim = 1;
    for(int i = axes.length; i < dim + 1; i++) {
      // find next "unused" dimension.
      {
        boolean search = true;
        while(search) {
          search = false;
          for(int a : axes) {
            if(a == useddim) {
              search = true;
              useddim++;
              break;
            }
          }
        }
      }
      m.set(i, useddim - 1, 1.0);
      useddim++;
    }
    assert (useddim - 2 == dim);
    return new AffineTransformation(dim, m, null);
  }

  /**
   * Return a clone of the affine transformation
   * 
   * @return cloned affine transformation
   */
  @Override
  public AffineTransformation clone() {
    // Note that we're NOT using copied matrices here, since this class
    // supposedly never modifies it's matrixes but replaces them with new
    // ones. Thus it is safe to re-use it for a cloned copy.
    return new AffineTransformation(this.dim, this.trans, this.inv);
  }

  /**
   * Query dimensionality of the transformation.
   * 
   * @return dimensionality
   */
  public int getDimensionality() {
    return dim;
  }

  /**
   * Add a translation operation to the matrix
   * 
   * @param v translation vector
   */
  public void addTranslation(Vector v) {
    assert (v.getDimensionality() == dim);

    // reset inverse transformation - needs recomputation.
    inv = null;

    Matrix homTrans = Matrix.unitMatrix(dim + 1);
    for(int i = 0; i < dim; i++) {
      homTrans.set(i, dim, v.get(i));
    }
    trans = homTrans.times(trans);
  }

  /**
   * Add a matrix operation to the matrix.
   * 
   * Be careful to use only invertible matrices if you want an invertible affine
   * transformation.
   * 
   * @param m matrix (should be invertible)
   */
  public void addMatrix(Matrix m) {
    assert (m.getRowDimensionality() == dim);
    assert (m.getColumnDimensionality() == dim);

    // reset inverse transformation - needs recomputation.
    inv = null;

    // extend the matrix with an extra row and column
    double[][] ht = new double[dim + 1][dim + 1];
    for(int i = 0; i < dim; i++) {
      for(int j = 0; j < dim; j++) {
        ht[i][j] = m.get(i, j);
      }
    }
    // the other cells default to identity matrix
    ht[dim][dim] = 1.0;
    // Multiply from left.
    trans = new Matrix(ht).times(trans);
  }

  /**
   * Convenience function to apply a rotation in 2 dimensions.
   * 
   * @param axis1 first dimension
   * @param axis2 second dimension
   * @param angle rotation angle in radians.
   */
  public void addRotation(int axis1, int axis2, double angle) {
    // TODO: throw an exception instead of using assert
    assert (axis1 >= 0);
    assert (axis1 < dim);
    assert (axis1 >= 0);
    assert (axis2 < dim);
    assert (axis1 != axis2);

    // reset inverse transformation - needs recomputation.
    inv = null;

    double[][] ht = new double[dim + 1][dim + 1];
    // identity matrix
    for(int i = 0; i < dim + 1; i++) {
      ht[i][i] = 1.0;
    }
    // insert rotation values
    ht[axis1][axis1] = +Math.cos(angle);
    ht[axis1][axis2] = -Math.sin(angle);
    ht[axis2][axis1] = +Math.sin(angle);
    ht[axis2][axis2] = +Math.cos(angle);
    // Multiply from left
    trans = new Matrix(ht).times(trans);
  }

  /**
   * Add a reflection along the given axis.
   * 
   * @param axis Axis number to do the reflection at.
   */
  public void addAxisReflection(int axis) {
    assert (0 < axis && axis <= dim);
    // reset inverse transformation - needs recomputation.
    inv = null;

    // Formal:
    // Matrix homTrans = Matrix.unitMatrix(dim + 1);
    // homTrans.set(axis - 1, axis - 1, -1);
    // trans = homTrans.times(trans);
    // Faster:
    for(int i = 0; i <= dim; i++) {
      trans.set(axis - 1, i, -trans.get(axis - 1, i));
    }
  }

  /**
   * Simple linear (symmetric) scaling.
   * 
   * @param scale Scaling factor
   */
  public void addScaling(double scale) {
    // invalidate inverse
    inv = null;
    // Note: last ROW is not included.
    for(int i = 0; i < dim; i++) {
      for(int j = 0; j <= dim; j++) {
        trans.set(i, j, trans.get(i, j) * scale);
      }
    }
    // As long as relative vectors aren't used, this would also work:
    // trans.set(dim, dim, trans.get(dim, dim) / scale);
  }

  /**
   * Get a copy of the transformation matrix
   * 
   * @return copy of the transformation matrix
   */
  public Matrix getTransformation() {
    return trans.copy();
  }

  /**
   * Get a copy of the inverse matrix
   * 
   * @return a copy of the inverse transformation matrix
   */
  public Matrix getInverse() {
    if(inv == null) {
      updateInverse();
    }
    return inv.copy();
  }

  /**
   * Compute the inverse transformation matrix
   */
  private void updateInverse() {
    inv = trans.inverse();
  }

  /**
   * Transform an absolute vector into homogeneous coordinates.
   * 
   * @param v initial vector
   * @return vector of dim+1, with new column having the value 1.0
   */
  public Vector homogeneVector(Vector v) {
    assert (v.getDimensionality() == dim);
    double[] dv = new double[dim + 1];
    for(int i = 0; i < dim; i++) {
      dv[i] = v.get(i);
    }
    dv[dim] = 1.0;
    return new Vector(dv);
  }

  /**
   * Transform a relative vector into homogeneous coordinates.
   * 
   * @param v initial vector
   * @return vector of dim+1, with new column having the value 0.0
   */
  public Vector homogeneRelativeVector(Vector v) {
    assert (v.getDimensionality() == dim);
    // TODO: this only works properly when trans[dim][dim] == 1.0, right?
    double[] dv = new double[dim + 1];
    for(int i = 0; i < dim; i++) {
      dv[i] = v.get(i);
    }
    dv[dim] = 0.0;
    return new Vector(dv);
  }

  /**
   * Project an homogeneous vector back into the original space.
   * 
   * @param v Matrix of 1 x dim+1 containing the homogeneous vector
   * @return vector of dimension dim
   */
  public Vector unhomogeneVector(Vector v) {
    assert (v.getDimensionality() == dim + 1);
    // TODO: this only works properly when trans[dim][dim] == 1.0, right?
    double[] dv = new double[dim];
    double scale = v.get(dim);
    assert (Math.abs(scale) > 0.0);
    for(int i = 0; i < dim; i++) {
      dv[i] = v.get(i) / scale;
    }
    return new Vector(dv);
  }

  /**
   * Project an homogeneous vector back into the original space.
   * 
   * @param v Matrix of 1 x dim+1 containing the homogeneous vector
   * @return vector of dimension dim
   */
  public Vector unhomogeneRelativeVector(Vector v) {
    assert (v.getDimensionality() == dim + 1);
    double[] dv = new double[dim];
    double scale = v.get(dim);
    assert (Math.abs(scale) == 0.0);
    for(int i = 0; i < dim; i++) {
      dv[i] = v.get(i);
    }
    return new Vector(dv);
  }

  /**
   * Apply the transformation onto a vector
   * 
   * @param v vector of dimensionality dim
   * @return transformed vector of dimensionality dim
   */
  public Vector apply(Vector v) {
    return unhomogeneVector(trans.times(homogeneVector(v)));
  }

  /**
   * Apply the inverse transformation onto a vector
   * 
   * @param v vector of dimensionality dim
   * @return transformed vector of dimensionality dim
   */
  public Vector applyInverse(Vector v) {
    if(inv == null) {
      updateInverse();
    }
    return unhomogeneVector(inv.times(homogeneVector(v)));
  }

  /**
   * Apply the transformation onto a vector
   * 
   * @param v vector of dimensionality dim
   * @return transformed vector of dimensionality dim
   */
  public Vector applyRelative(Vector v) {
    return unhomogeneRelativeVector(trans.times(homogeneRelativeVector(v)));
  }

  /**
   * Apply the inverse transformation onto a vector
   * 
   * @param v vector of dimensionality dim
   * @return transformed vector of dimensionality dim
   */
  public Vector applyRelativeInverse(Vector v) {
    if(inv == null) {
      updateInverse();
    }
    return unhomogeneRelativeVector(inv.times(homogeneRelativeVector(v)));
  }
}