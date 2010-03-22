package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * Result class for Principal Component Analysis with some convenience methods
 * 
 * @author erich
 */
public class PCAResult {
  /**
   * The eigenpairs in decreasing order.
   */
  private SortedEigenPairs eigenPairs;
  
  /**
   * The eigenvalues in decreasing order.
   */
  private double[] eigenvalues;

  /**
   * The eigenvectors in decreasing order to their corresponding eigenvalues.
   */
  private Matrix eigenvectors;

  /**
   * Build a PCA result object.
   * 
   * @param eigenvalues Eigenvalues
   * @param eigenvectors Eigenvector matrix
   * @param eigenPairs Eigenpairs
   */

  public PCAResult(double[] eigenvalues, Matrix eigenvectors, SortedEigenPairs eigenPairs) {
    super();
    this.eigenPairs = eigenPairs;
    this.eigenvalues = eigenvalues;
    this.eigenvectors = eigenvectors;
  }

  /**
   * Build a PCA result from an existing set of EigenPairs.
   * 
   * @param eigenPairs existing eigenpairs
   */
  public PCAResult(SortedEigenPairs eigenPairs) {
    super();
    // TODO: we might want to postpone the instantiation of eigenvalue and eigenvectors.
    this.eigenPairs = eigenPairs;
    this.eigenvalues = eigenPairs.eigenValues();
    this.eigenvectors = eigenPairs.eigenVectors();
  }

  /**
   * Returns a copy of the matrix of eigenvectors of the object to which this
   * PCA belongs to.
   * 
   * @return the matrix of eigenvectors
   */
  public final Matrix getEigenvectors() {
    return eigenvectors.copy();
  }

  /**
   * Returns a copy of the eigenvalues of the object to which this PCA belongs
   * to in decreasing order.
   * 
   * @return the eigenvalues
   */
  public final double[] getEigenvalues() {
    return Util.copy(eigenvalues);
  }

  /**
   * Returns a copy of the eigenpairs of the object to which this PCA belongs to
   * in decreasing order.
   * 
   * @return the eigenpairs
   */
  public final SortedEigenPairs getEigenPairs() {
    return eigenPairs.copy();
  }

  /**
   * Returns the number of eigenvectors stored
   * @return length
   */
  public final int length() {
    return eigenPairs.size();
  }
}