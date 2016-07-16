package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Result class for a filtered PCA. This differs from regular PCA by having the
 * Eigenvalues and Eigenvectors separated into "strong" and "weak" Eigenvectors,
 * and thus a dimension. Usually this will be interpreted as having a "data"
 * subspace and an "error" subspace.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @apiviz.landmark
 */
public class PCAFilteredResult extends PCAResult {
  /**
   * The strong eigenvalues.
   */
  private double[] strongEigenvalues;

  /**
   * The strong eigenvectors to their corresponding filtered eigenvalues.
   */
  private Matrix strongEigenvectors;

  /**
   * The weak eigenvalues.
   */
  private double[] weakEigenvalues;

  /**
   * The weak eigenvectors to their corresponding filtered eigenvalues.
   */
  private Matrix weakEigenvectors;

  /**
   * The amount of Variance explained by strong Eigenvalues
   */
  private double explainedVariance;

  /**
   * The selection matrix of the weak eigenvectors.
   */
  private Matrix e_hat;

  /**
   * The selection matrix of the strong eigenvectors.
   */
  private Matrix e_czech;

  /**
   * The similarity matrix.
   */
  private Matrix m_hat;

  /**
   * The dissimilarity matrix.
   */
  private Matrix m_czech;

  /**
   * The diagonal matrix of adapted strong eigenvalues: eigenvectors * e_czech.
   */
  private Matrix adapatedStrongEigenvectors = null;

  /**
   * Construct a result object for the filtered PCA result.
   * 
   * @param eigenPairs All EigenPairs
   * @param numstrong Number of strong eigenvalues
   * @param big large value in selection matrix
   * @param small small value in selection matrix
   */

  public PCAFilteredResult(SortedEigenPairs eigenPairs, int numstrong, double big, double small) {
    super(eigenPairs);

    int dim = eigenPairs.getEigenPair(0).getEigenvector().length;

    double sumStrongEigenvalues = 0;
    double sumWeakEigenvalues = 0;
    {// strong eigenpairs
      strongEigenvalues = new double[numstrong];
      strongEigenvectors = new Matrix(dim, numstrong);
      for (int i = 0; i < numstrong; i++) {
        EigenPair eigenPair = eigenPairs.getEigenPair(i);
        strongEigenvalues[i] = eigenPair.getEigenvalue();
        strongEigenvectors.setCol(i, eigenPair.getEigenvector());
        sumStrongEigenvalues += strongEigenvalues[i];
      }
    }

    {// weak eigenpairs
      weakEigenvalues = new double[dim - numstrong];
      weakEigenvectors = new Matrix(dim, dim - numstrong);
      for (int i = numstrong, j = 0; i < dim; i++, j++) {
        EigenPair eigenPair = eigenPairs.getEigenPair(i);
        weakEigenvalues[j] = eigenPair.getEigenvalue();
        weakEigenvectors.setCol(j, eigenPair.getEigenvector());
        sumWeakEigenvalues += weakEigenvalues[j];
      }
    }
    explainedVariance = sumStrongEigenvalues / (sumStrongEigenvalues + sumWeakEigenvalues);
    int localdim = strongEigenvalues.length;

    // selection Matrix for weak and strong EVs
    e_hat = new Matrix(dim, dim);
    e_czech = new Matrix(dim, dim);
    for(int d = 0; d < dim; d++) {
      if(d < localdim) {
        e_czech.set(d, d, big);
        e_hat.set(d, d, small);
      }
      else {
        e_czech.set(d, d, small);
        e_hat.set(d, d, big);
      }
    }

    Matrix V = getEigenvectors();
    m_hat = V.times(e_hat).timesTranspose(V);
    m_czech = V.times(e_czech).timesTranspose(V);
  }

  /**
   * Returns the matrix of strong eigenvectors after passing the eigen pair
   * filter.
   * 
   * @return the matrix of eigenvectors
   */
  public final Matrix getStrongEigenvectors() {
    return strongEigenvectors;
  }

  /**
   * Returns the strong eigenvalues of the object after passing the eigen pair
   * filter.
   * 
   * @return the eigenvalues
   */
  public final double[] getStrongEigenvalues() {
    return strongEigenvalues;
  }

  /**
   * Returns the matrix of weak eigenvectors after passing the eigen pair
   * filter.
   * 
   * @return the matrix of eigenvectors
   */
  public final Matrix getWeakEigenvectors() {
    return weakEigenvectors;
  }

  /**
   * Returns the weak eigenvalues of the object after passing the eigen pair
   * filter.
   * 
   * @return the eigenvalues
   */
  public final double[] getWeakEigenvalues() {
    return weakEigenvalues;
  }

  /**
   * Get correlation (subspace) dimensionality
   * 
   * @return length of strong eigenvalues
   */
  public final int getCorrelationDimension() {
    return strongEigenvalues.length;
  }

  /**
   * Returns explained variance
   * 
   * @return the variance explained by the strong Eigenvectors
   */
  public double getExplainedVariance() {
    return explainedVariance;
  }

  /**
   * Returns the selection matrix of the weak eigenvectors (E_hat) of the object
   * to which this PCA belongs to.
   * 
   * @return the selection matrix of the weak eigenvectors E_hat
   */
  public Matrix selectionMatrixOfWeakEigenvectors() {
    return e_hat;
  }

  /**
   * Returns the selection matrix of the strong eigenvectors (E_czech) of this
   * LocalPCA.
   * 
   * @return the selection matrix of the weak eigenvectors E_czech
   */
  public Matrix selectionMatrixOfStrongEigenvectors() {
    return e_czech;
  }

  /**
   * Returns the similarity matrix (M_hat) of this LocalPCA.
   * 
   * @return the similarity matrix M_hat
   */
  public Matrix similarityMatrix() {
    return m_hat;
  }

  /**
   * Returns the dissimilarity matrix (M_czech) of this LocalPCA.
   * 
   * @return the dissimilarity matrix M_hat
   */
  public Matrix dissimilarityMatrix() {
    return m_czech;
  }

  /**
   * Returns the adapted strong eigenvectors.
   * 
   * @return the adapted strong eigenvectors
   */
  public Matrix adapatedStrongEigenvectors() {
    if(adapatedStrongEigenvectors == null) {
      final Matrix ev = getEigenvectors();
      adapatedStrongEigenvectors = ev.times(e_czech).times(Matrix.identity(ev.getRowDimensionality(), strongEigenvalues.length));
    }
    return adapatedStrongEigenvectors;
  }
}