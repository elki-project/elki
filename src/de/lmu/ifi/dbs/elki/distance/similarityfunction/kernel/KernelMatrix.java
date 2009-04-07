package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

/**
 * Provides a class for storing the kernel matrix and several extraction methods for convenience.
 *
 * @author Simon Paradies
 */
public class KernelMatrix<O extends RealVector<O,? >> extends AbstractParameterizable {

  /**
   * The kernel matrix
   */
  Matrix kernel;

  /**
   * Wraps the matrixArray in a KernelMatrix
   *
   * @param matrixArray two dimensional double array
   */
  public KernelMatrix(final double[][] matrixArray) {
    kernel = new Matrix(matrixArray);
  }

  /**
   * Provides a new kernel matrix.
   *
   * @param kernelFunction the kernel function used to compute the kernel matrix
   * @param database       the database for which the kernel matrix is computed
   */
  public KernelMatrix(final KernelFunction<O, DoubleDistance> kernelFunction,
                      final Database<O> database) {
    this(kernelFunction, database, database.getIDs());
  }

  /**
   * Provides a new kernel matrix.
   *
   * @param kernelFunction the kernel function used to compute the kernel matrix
   * @param database       the database that holds the objects
   * @param ids            the IDs of those objects for which the kernel matrix is computed
   */
  public KernelMatrix(final KernelFunction<O, DoubleDistance> kernelFunction,
                      final Database<O> database, final List<Integer> ids) {
    logger.debugFiner("Computing kernel matrix");
    kernel = new Matrix(ids.size(), ids.size());
    double value;
    Collections.sort(ids);
    for (int idx = 0; idx < ids.size(); idx++) {
      for (int idy = idx; idy < ids.size(); idy++) {
        value = kernelFunction.similarity(database.get(ids.get(idx)), database.get(ids.get(idy))).getValue();
        kernel.set(idx, idy, value);
        kernel.set(idy, idx, value);
      }
    }
  }

  /**
   * Makes a new kernel matrix from matrix.
   *
   * @param matrix a matrix
   */
  public KernelMatrix(final Matrix matrix) {
    kernel = new Matrix(matrix.getArrayCopy());
  }

  /**
   * Returns the kernel distance between the two specified objects.
   *
   * @param o1 first ObjectID
   * @param o2 second ObjectID
   * @return the distance between the two objects
   */
  public double getDistance(final int o1, final int o2) {
    return Math.sqrt(getSquaredDistance(o1, o2));
  }

  public Matrix getKernel() {
    return kernel;
  }

  /**
   * Returns the kernel value of object o1 and object o2
   *
   * @param o1 ID of first object
   * @param o2 ID of second object
   * @return the kernel value of object o1 and object o2
   */
  public double getSimilarity(final int o1, final int o2) {
    return kernel.get(o1 - 1, o2 - 1); //correct index shifts
  }

  /**
   * Returns the squared kernel distance between the two specified objects.
   *
   * @param o1 first ObjectID
   * @param o2 second ObjectID
   * @return the distance between the two objects
   */
  public double getSquaredDistance(final int o1, final int o2) {
    return getSimilarity(o1, o1) + getSimilarity(o2, o2) - 2 * getSimilarity(o1, o2);
  }

  /**
   * Returns the ith kernel matrix column for all objects in ids
   *
   * @param i   the column which should be returned
   * @param ids the objects
   * @return the ith kernel matrix column for all objects in ids
   */
  public Matrix getSubColumn(final int i, final List<Integer> ids) {
    final int[] ID = new int[1];
    ID[0] = i - 1; //correct index shift
    final int[] IDs = new int[ids.size()];
    for (int x = 0; x < IDs.length; x++) {
      IDs[x] = ids.get(x) - 1; //correct index shift
    }
    return kernel.getMatrix(IDs, ID);
  }

  /**
   * Returns a sub kernel matrix for all objects in ids
   *
   * @param ids the objects
   * @return a sub kernel matrix for all objects in ids.
   */
  public Matrix getSubMatrix(final Collection<Integer> ids) {
    final int[] IDs = new int[ids.size()];
    int i = 0;
    for (Iterator<Integer> it = ids.iterator(); it.hasNext(); i++) {
      IDs[i] = it.next() - 1; //correct index shift
    }
    return kernel.getMatrix(IDs, IDs);
  }

  /**
   * Centers the matrix in feature space
   * according to Smola et. Schoelkopf, Learning with Kernels p. 431
   * Alters the input matrix. If you still need the original matrix, use
   * <code>centeredMatrix = centerKernelMatrix(uncenteredMatrix.copy()) {</code>
   *
   * @param matrix the matrix to be centered
   * @return centered matrix (for convenience)
   */
  public static Matrix centerMatrix(final Matrix matrix) {
    final Matrix normalizingMatrix = new Matrix(matrix.getRowDimensionality(),
                                                matrix.getColumnDimensionality(),
                                                1.0 / matrix.getColumnDimensionality());
    return matrix.minusEquals(
        normalizingMatrix.times(matrix)).minusEquals(
        matrix.times(normalizingMatrix)).plusEquals(
        normalizingMatrix.times(matrix).times(normalizingMatrix));
  }

  /**
   * @see Matrix#toString()
   */
  @Override
  public String toString() {
    return super.toString();
  }

  /**
   * Centers the Kernel Matrix in Feature Space
   * according to Smola et. Schoelkopf, Learning with Kernels p. 431
   * Alters the input matrix. If you still need the original matrix, use
   * <code>centeredMatrix = centerKernelMatrix(uncenteredMatrix.copy()) {</code>
   *
   * @param kernelMatrix the kernel matrix to be centered
   * @return centered kernelMatrix (for convenience)
   */
  public static Matrix centerKernelMatrix(final KernelMatrix<? extends RealVector<?, ? extends Number>> kernelMatrix) {
    return centerMatrix(kernelMatrix.getKernel());
  }
}
