package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * Result class for Principal Component Analysis with some convenience methods
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs
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