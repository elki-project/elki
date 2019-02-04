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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeDiagonalTimes;

import java.util.Arrays;

/**
 * Result class for a filtered PCA. This differs from regular PCA by having the
 * Eigenvalues and Eigenvectors separated into "strong" and "weak" Eigenvectors,
 * and thus a dimension. Usually this will be interpreted as having a "data"
 * subspace and an "error" subspace.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @opt nodefillcolor LemonChiffon
 */
public class PCAFilteredResult extends PCAResult {
  /**
   * The strong eigenvalues.
   */
  private double[] strongEigenvalues;

  /**
   * The strong eigenvectors to their corresponding filtered eigenvalues.
   */
  private double[][] strongEigenvectors;

  /**
   * The weak eigenvalues.
   */
  private double[] weakEigenvalues;

  /**
   * The weak eigenvectors to their corresponding filtered eigenvalues.
   */
  private double[][] weakEigenvectors;

  /**
   * The amount of Variance explained by strong Eigenvalues
   */
  private double explainedVariance;

  /**
   * The selection matrix of the weak eigenvectors.
   */
  // private double[][] e_hat;

  /**
   * The selection matrix of the strong eigenvectors.
   */
  // private double[][] e_czech;

  /**
   * The similarity matrix.
   */
  private double[][] m_hat;

  /**
   * The dissimilarity matrix.
   */
  private double[][] m_czech;

  /**
   * Construct a result object for the filtered PCA result.
   * 
   * @param eigenPairs All EigenPairs
   * @param numstrong Number of strong eigenvalues
   * @param big large value in selection matrix
   * @param small small value in selection matrix
   */
  public PCAFilteredResult(EigenPair[] eigenPairs, int numstrong, double big, double small) {
    super(eigenPairs);

    int dim = eigenPairs[0].getEigenvector().length;

    double sumStrongEigenvalues = 0;
    double sumWeakEigenvalues = 0;
    {// strong eigenpairs
      strongEigenvalues = new double[numstrong];
      strongEigenvectors = new double[numstrong][];
      for(int i = 0; i < numstrong; i++) {
        EigenPair eigenPair = eigenPairs[i];
        strongEigenvectors[i] = eigenPair.getEigenvector();
        strongEigenvalues[i] = eigenPair.getEigenvalue();
        sumStrongEigenvalues += strongEigenvalues[i];
      }
    }

    {// weak eigenpairs
      weakEigenvalues = new double[dim - numstrong];
      weakEigenvectors = new double[dim - numstrong][];
      for(int i = numstrong, j = 0; i < dim; i++, j++) {
        EigenPair eigenPair = eigenPairs[i];
        weakEigenvectors[j] = eigenPair.getEigenvector();
        weakEigenvalues[j] = eigenPair.getEigenvalue();
        sumWeakEigenvalues += weakEigenvalues[j];
      }
    }
    explainedVariance = sumStrongEigenvalues / (sumStrongEigenvalues + sumWeakEigenvalues);
    int localdim = strongEigenvalues.length;

    // Diagonal selections for weak and strong EVs
    double[] e_hat_d = new double[dim];
    Arrays.fill(e_hat_d, 0, localdim, small);
    Arrays.fill(e_hat_d, localdim, dim, big);
    double[] e_czech_d = new double[dim];
    Arrays.fill(e_czech_d, 0, localdim, big);
    Arrays.fill(e_czech_d, localdim, dim, small);

    double[][] Vt = getEigenvectors(); // = transposed matrix!
    m_hat = transposeDiagonalTimes(Vt, e_hat_d, Vt);
    m_czech = transposeDiagonalTimes(Vt, e_czech_d, Vt);
  }

  /**
   * Returns the matrix of strong eigenvectors after passing the eigen pair
   * filter.
   * 
   * @return the matrix of eigenvectors
   */
  public final double[][] getStrongEigenvectors() {
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
  public final double[][] getWeakEigenvectors() {
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
   * Returns the similarity matrix (M_hat) of this LocalPCA.
   * 
   * @return the similarity matrix M_hat
   */
  public double[][] similarityMatrix() {
    return m_hat;
  }

  /**
   * Returns the dissimilarity matrix (M_czech) of this LocalPCA.
   * 
   * @return the dissimilarity matrix M_hat
   */
  public double[][] dissimilarityMatrix() {
    return m_czech;
  }
}