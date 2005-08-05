package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.linearalgebra.Matrix;

/**
 * A PCA is a principal component analysis that belongs to an object stored in a
 * database. The PCA determines the principal components of the object and
 * holds the eigenvectors and eigenvalues of the object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface PCA {
  /**
   * Returns a copy of the matrix of eigenvectors
   * of the object to which this PCA belongs to.
   *
   * @return the matrix of eigenvectors
   */
  public Matrix getEigenvectors();

  /**
   * Returns a copy of the eigenvalues of the object to which this PCA belongs to
   * in decreasing order.
   *
   * @return the eigenvalues
   */
  public double[] getEigenvalues();
}
