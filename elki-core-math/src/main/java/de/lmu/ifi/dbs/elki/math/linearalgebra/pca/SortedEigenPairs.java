/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * Helper class which encapsulates an array of eigenpairs (i.e. an array of
 * eigenvectors and their corresponding eigenvalues). This class is used to sort
 * eigenvectors (and -values).
 * 
 * @author Elke Achtert
 * @since 0.2
 * 
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair
 */
public class SortedEigenPairs {
  /**
   * The array of eigenpairs.
   */
  private EigenPair[] eigenPairs;

  /**
   * Creates a new empty SortedEigenPairs object. Can only be called from the
   * copy() method.
   */
  private SortedEigenPairs() {
    // nothing to do here.
  }

  /**
   * Creates a new SortedEigenPairs object from the specified eigenvalue
   * decomposition. The eigenvectors are sorted according to the specified
   * order.
   * 
   * @param evd the underlying eigenvalue decomposition
   * @param ascending a boolean that indicates ascending order
   */
  public SortedEigenPairs(EigenvalueDecomposition evd, final boolean ascending) {
    double[] eigenvalues = evd.getRealEigenvalues();
    double[][] eigenvectors = evd.getV();

    this.eigenPairs = new EigenPair[eigenvalues.length];
    for(int i = 0; i < eigenvalues.length; i++) {
      double e = Math.abs(eigenvalues[i]);
      double[] v = VMath.getCol(eigenvectors, i);
      eigenPairs[i] = new EigenPair(v, e);
    }

    Comparator<EigenPair> comp = new Comparator<EigenPair>() {
      @Override
      public int compare(EigenPair o1, EigenPair o2) {
        int comp = o1.compareTo(o2);
        if(!ascending) {
          comp = -1 * comp;
        }
        return comp;
      }
    };

    Arrays.sort(eigenPairs, comp);
  }

  /**
   * Creates a new SortedEigenPairs object from the specified list. The
   * eigenvectors are sorted in descending order.
   * 
   * @param eigenPairs the eigenpairs to be sorted
   */
  public SortedEigenPairs(List<EigenPair> eigenPairs) {
    Comparator<EigenPair> comp = new Comparator<EigenPair>() {
      @Override
      public int compare(EigenPair o1, EigenPair o2) {
        return -1 * o1.compareTo(o2);
      }
    };

    this.eigenPairs = eigenPairs.toArray(new EigenPair[eigenPairs.size()]);
    Arrays.sort(this.eigenPairs, comp);
  }

  /**
   * Returns the sorted eigenvalues.
   * 
   * @return the sorted eigenvalues
   */
  public double[] eigenValues() {
    double[] eigenValues = new double[eigenPairs.length];
    for(int i = 0; i < eigenPairs.length; i++) {
      eigenValues[i] = eigenPairs[i].getEigenvalue();
    }
    return eigenValues;
  }

  /**
   * Get the ith eigenvalue.
   * 
   * @param i Index
   * @return Eigenvalue
   */
  public double eigenValue(int i) {
    return eigenPairs[i].getEigenvalue();
  }

  /**
   * Returns the sorted eigenvectors.
   * 
   * @return the sorted eigenvectors
   */
  public double[][] eigenVectors() {
    double[][] eigenVectors = new double[eigenPairs.length][eigenPairs.length];
    for(int i = 0; i < eigenPairs.length; i++) {
      EigenPair eigenPair = eigenPairs[i];
      VMath.setCol(eigenVectors, i, eigenPair.getEigenvector());
    }
    return eigenVectors;
  }

  /**
   * Returns the first <code>n</code> sorted eigenvectors as a matrix.
   * 
   * @param n the number of eigenvectors (columns) to be returned
   * @return the first <code>n</code> sorted eigenvectors
   */
  public double[][] eigenVectors(int n) {
    double[][] eigenVectors = new double[eigenPairs.length][n];
    for(int i = 0; i < n; i++) {
      EigenPair eigenPair = eigenPairs[i];
      VMath.setCol(eigenVectors, i, eigenPair.getEigenvector());
    }
    return eigenVectors;
  }

  /**
   * Returns the last <code>n</code> sorted eigenvectors as a matrix.
   * 
   * @param n the number of eigenvectors (columns) to be returned
   * @return the last <code>n</code> sorted eigenvectors
   */
  public double[][] reverseEigenVectors(int n) {
    double[][] eigenVectors = new double[eigenPairs.length][n];
    for(int i = 0; i < n; i++) {
      EigenPair eigenPair = eigenPairs[eigenPairs.length - 1 - i];
      VMath.setCol(eigenVectors, i, eigenPair.getEigenvector());
    }
    return eigenVectors;
  }

  /**
   * Returns the eigenpair at the specified index.
   * 
   * @param index the index of the eigenpair to be returned
   * @return the eigenpair at the specified index
   */
  public EigenPair getEigenPair(int index) {
    return eigenPairs[index];
  }

  /**
   * Returns the number of the eigenpairs.
   * 
   * @return the number of the eigenpairs
   */
  public int size() {
    return eigenPairs.length;
  }

  /**
   * Returns a string representation of this EigenPair.
   * 
   * @return a string representation of this EigenPair
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for(EigenPair eigenPair : eigenPairs) {
      result.append('\n').append(eigenPair);
    }
    return result.toString();
  }

  /**
   * Returns a deep copy of this object
   * 
   * @return new copy
   */
  public SortedEigenPairs copy() {
    SortedEigenPairs cp = new SortedEigenPairs();
    cp.eigenPairs = this.eigenPairs.clone();
    return cp;
  }
}