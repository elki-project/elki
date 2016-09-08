package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

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
  private double[][] e_hat;

  /**
   * The selection matrix of the strong eigenvectors.
   */
  private double[][] e_czech;

  /**
   * The similarity matrix.
   */
  private double[][] m_hat;

  /**
   * The dissimilarity matrix.
   */
  private double[][] m_czech;

  /**
   * The diagonal matrix of adapted strong eigenvalues: eigenvectors * e_czech.
   */
  private double[][] adapatedStrongEigenvectors = null;

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
      strongEigenvectors = new double[dim][numstrong];
      for (int i = 0; i < numstrong; i++) {
        EigenPair eigenPair = eigenPairs.getEigenPair(i);
        strongEigenvalues[i] = eigenPair.getEigenvalue();
        setCol(strongEigenvectors, i, eigenPair.getEigenvector());
        sumStrongEigenvalues += strongEigenvalues[i];
      }
    }

    {// weak eigenpairs
      weakEigenvalues = new double[dim - numstrong];
      weakEigenvectors = new double[dim][dim - numstrong];
      for (int i = numstrong, j = 0; i < dim; i++, j++) {
        EigenPair eigenPair = eigenPairs.getEigenPair(i);
        weakEigenvalues[j] = eigenPair.getEigenvalue();
        setCol(weakEigenvectors, j, eigenPair.getEigenvector());
        sumWeakEigenvalues += weakEigenvalues[j];
      }
    }
    explainedVariance = sumStrongEigenvalues / (sumStrongEigenvalues + sumWeakEigenvalues);
    int localdim = strongEigenvalues.length;

    // selection Matrix for weak and strong EVs
    e_hat = new double[dim][dim];
    e_czech = new double[dim][dim];
    for(int d = 0; d < dim; d++) {
      if(d < localdim) {
        e_czech[d][d]=big;
        e_hat[d][d]=small;
      }
      else {
        e_czech[d][d] = small;
        e_hat[d][d] = big;
      }
    }

    double[][] V = getEigenvectors();
    m_hat = timesTranspose(times(V, e_hat), V);
    m_czech = timesTranspose(times(V, e_czech), V);
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
   * Returns the selection matrix of the weak eigenvectors (E_hat) of the object
   * to which this PCA belongs to.
   * 
   * @return the selection matrix of the weak eigenvectors E_hat
   */
  public double[][] selectionMatrixOfWeakEigenvectors() {
    return e_hat;
  }

  /**
   * Returns the selection matrix of the strong eigenvectors (E_czech) of this
   * LocalPCA.
   * 
   * @return the selection matrix of the weak eigenvectors E_czech
   */
  public double[][] selectionMatrixOfStrongEigenvectors() {
    return e_czech;
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

  /**
   * Returns the adapted strong eigenvectors.
   * 
   * @return the adapted strong eigenvectors
   */
  public double[][] adapatedStrongEigenvectors() {
    if(adapatedStrongEigenvectors == null) {
      final double[][] ev = getEigenvectors();
      adapatedStrongEigenvectors = times(times(ev, e_czech), identity(ev.length, strongEigenvalues.length));
    }
    return adapatedStrongEigenvectors;
  }
}