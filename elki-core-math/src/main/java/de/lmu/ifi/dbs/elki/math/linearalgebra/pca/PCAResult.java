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

import java.util.Arrays;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * Result class for Principal Component Analysis with some convenience methods
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @opt nodefillcolor LemonChiffon
 * @has - - - SortedEigenPairs
 */
public class PCAResult {
  /**
   * The eigenpairs in decreasing order.
   */
  private EigenPair[] eigenPairs;

  /**
   * The eigenvalues in decreasing order.
   */
  private double[] eigenvalues;

  /**
   * The eigenvectors in decreasing order to their corresponding eigenvalues.
   */
  private double[][] eigenvectors;

  /**
   * Build a PCA result from an existing set of EigenPairs.
   * 
   * @param eigenPairs existing eigenpairs
   */
  public PCAResult(EigenPair[] eigenPairs) {
    super();
    this.eigenPairs = eigenPairs;
    this.eigenvalues = new double[eigenPairs.length];
    this.eigenvectors = new double[eigenPairs.length][];
    for(int i = 0; i < eigenPairs.length; i++) {
      this.eigenvalues[i] = eigenPairs[i].getEigenvalue();
      this.eigenvectors[i] = eigenPairs[i].getEigenvector();
    }
  }

  /**
   * Constructor from an eigenvalue decomposition.
   *
   * @param evd Eigenvalue decomposition
   */
  public PCAResult(EigenvalueDecomposition evd) {
    this(processDecomposition(evd));
  }

  /**
   * Convert an eigenvalue decomposition into EigenPair objects.
   *
   * @param evd Eigenvalue decomposition
   * @return Eigenpairs
   */
  private static EigenPair[] processDecomposition(EigenvalueDecomposition evd) {
    double[] eigenvalues = evd.getRealEigenvalues();
    double[][] eigenvectors = evd.getV();

    EigenPair[] eigenPairs = new EigenPair[eigenvalues.length];
    for(int i = 0; i < eigenvalues.length; i++) {
      double e = Math.abs(eigenvalues[i]);
      double[] v = VMath.getCol(eigenvectors, i);
      eigenPairs[i] = new EigenPair(v, e);
    }
    Arrays.sort(eigenPairs, Comparator.reverseOrder());
    return eigenPairs;
  }

  /**
   * Returns the local PCA eigenvectors, <b>in rows</b>.
   *
   * @return eigenvectors of the object.
   */
  public final double[][] getEigenvectors() {
    return eigenvectors;
  }

  /**
   * Returns the local PCA eigenvalues in decreasing order.
   *
   * @return the eigenvalues
   */
  public final double[] getEigenvalues() {
    return eigenvalues;
  }

  /**
   * Returns the eigenpairs of the object to which this PCA belongs to
   * in decreasing order.
   * 
   * @return the eigenpairs
   */
  public final EigenPair[] getEigenPairs() {
    return eigenPairs;
  }
}
