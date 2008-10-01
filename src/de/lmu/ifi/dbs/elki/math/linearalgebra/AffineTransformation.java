package de.lmu.ifi.dbs.elki.math.linearalgebra;

/**
 * Affine transformations implemented using homogeneous coordinates.
 * 
 * The use of homogeneous coordinates allows the combination of
 * multiple affine transformations (rotations, translations, scaling)
 * into a single matrix operation (of dimensionality dim+1), and also
 * the construction of an inverse transformation.
 * 
 * {@see <a href="http://en.wikipedia.org/wiki/Homogeneous_coordinates">Wikipedia on Homogeneous Coordinates</a>}
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 */
public class AffineTransformation {
  /**
   * the dimensionality of the transformation
   */
  private int dim;
  /**
   * The transformation matrix of dim+1 x dim+1
   * for homogeneous coordinates
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
    this.trans = Matrix.unitMatrix(dim+1);
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
   * Return a clone of the affine transformation
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
   * @return dimensionality
   */
  public int getDimensionality() {
    return dim;
  }

  /**
   * Add a translation operation to the matrix
   * @param v translation vector
   */
  public void addTranslation(Vector v) {
    assert(v.getRowDimensionality() == dim);

    // reset inverse transformation - needs recomputation.
    inv = null;
    double[][] ht = new double[dim+1][dim+1];
    for (int i=0; i < dim+1; i++)
      ht[i][i] = 1.0;
    for (int i=0; i < dim; i++)
      ht[i][dim] = v.get(i);
    Matrix homTrans = new Matrix(ht);
    trans = homTrans.times(trans);
  }

  /**
   * Add a matrix operation to the matrix.
   * Be careful to use only invertible matrices if you want an invertible
   * affine transformation.
   * 
   * @param m matrix (should be invertible)
   */
  public void addMatrix(Matrix m) {
    assert(m.getRowDimensionality() == dim);
    assert(m.getColumnDimensionality() == dim);

    // reset inverse transformation - needs recomputation.
    inv = null;
    // extend the matrix with an extra row and column
    double[][] ht = new double[dim+1][dim+1];
    for (int i=0; i < dim; i++)
      for (int j=0; j < dim; j++)
        ht[i][j] = m.get(i,j);
    // the other cells default to identity matrix
    ht[dim][dim] = 1.0;
    Matrix homTrans = new Matrix(ht);
    trans = homTrans.times(trans);
  }

  /**
   * Convenience function to apply a rotation in 2 dimensions.
   * @param axis1 first dimension
   * @param axis2 second dimension
   * @param angle rotation angle in radians.
   */
  public void addRotation(int axis1, int axis2, double angle) {
    // TODO: throw an exception instead of using assert
    assert(axis1 >= 0); assert(axis1 < dim);
    assert(axis1 >= 0); assert(axis2 < dim);
    assert(axis1 != axis2);

    // reset inverse transformation - needs recomputation.
    inv = null;
    double[][] ht = new double[dim+1][dim+1];
    // identity matrix
    for (int i=0; i < dim+1; i++)
      ht[i][i] = 1.0;
    // insert rotation values
    ht[axis1][axis1] = + Math.cos(angle);
    ht[axis1][axis2] = - Math.sin(angle);
    ht[axis2][axis1] = + Math.sin(angle);
    ht[axis2][axis2] = + Math.cos(angle);
    // apply
    Matrix homTrans = new Matrix(ht);
    trans = homTrans.times(trans);    
  }

  /**
   * Get a copy of the transformation matrix
   * @return copy of the transformation matrix
   */
  public Matrix getTransformation() {
    return trans.copy();
  }
  
  /**
   * Get a copy of the inverse matrix
   * @return a copy of the inverse transformation matrix
   */
  public Matrix getInverse() {
    if (inv == null) updateInverse();
    return inv.copy();
  }
  
  /**
   * Compute the inverse transformation matrix
   */
  private void updateInverse() {
    inv = trans.inverse();
  }

  /**
   * Transform a vector into homogeneous coordinates.
   * @param v initial vector
   * @return vector of dim+1, with new column having the value 1.0
   */
  public Vector homogeneVector(Vector v) {
    assert(v.getRowDimensionality() == dim);
    double[] dv = new double[dim+1];
    for (int i=0; i<dim; i++)
      dv[i] = v.get(i);
    dv[dim] = 1.0;
    return new Vector(dv);
  }

  /**
   * Project an homogeneous vector back into the original space.
   * @param v homogeneous vector of dim+1
   * @return vector of dimension dim
   */
  public Vector unhomogeneVector(Vector v) {
    assert(v.getRowDimensionality() == dim+1);
    double[] dv = new double[dim];
    double scale = v.get(dim);
    assert(Math.abs(scale) > 0.0);
    for (int i=0; i<dim; i++)
      dv[i] = v.get(i) / scale;
    return new Vector(dv);
  }
  
  /**
   * Project an homogeneous vector back into the original space.
   * @param v Matrix of 1 x dim+1 containing the homogeneous vector
   * @return vector of dimension dim
   */
  public Vector unhomogeneVector(Matrix v) {
    assert(v.getRowDimensionality() == dim+1);
    assert(v.getColumnDimensionality() == 1);
    double[] dv = new double[dim];
    double scale = v.get(dim,0);
    assert(Math.abs(scale) > 0.0);
    for (int i=0; i<dim; i++)
      dv[i] = v.get(i,0) / scale;
    return new Vector(dv);
  }
  
  /**
   * Apply the transformation onto a vector
   * @param v vector of dimensionality dim
   * @return transformed vector of dimensionality dim
   */
  public Vector apply(Vector v) {
    return unhomogeneVector(trans.times(homogeneVector(v)));
  }

  /**
   * Apply the inverse transformation onto a vector
   * @param v vector of dimensionality dim
   * @return transformed vector of dimensionality dim
   */
  public Vector applyInverse(Vector v) {
    if (inv == null) updateInverse();
    return unhomogeneVector(inv.times(homogeneVector(v)));
  }
}
