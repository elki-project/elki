package de.lmu.ifi.dbs.elki.math.linearalgebra;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Helper class which encapsulates an array of eigenpairs (i.e. an array
 * of eigenvectors and their corresponding eigenvalues).
 * This class is used to sort eigenvectors (and -values).
 *
 * @author Elke Achtert 
 */
public class SortedEigenPairs {
  /**
   * The array of eigenpairs.
   */
  private EigenPair[] eigenPairs;

  /**
   * Creates a new empty SortedEigenPairs object.
   * Can only be called from the copy() method.
   */
  private SortedEigenPairs() {    
  }

  /**
   * Creates a new SortedEigenPairs object from the specified eigenvalue
   * decomposition. The eigenvectors are sorted according to the specified
   * order.
   *
   * @param evd       the underlying eigenvalue decomposition
   * @param ascending a boolean that indicates ascending order
   */
  public SortedEigenPairs(EigenvalueDecomposition evd, final boolean ascending) {
    double[] eigenvalues = evd.getRealEigenvalues();
    Matrix eigenvectors = evd.getV();

    this.eigenPairs = new EigenPair[eigenvalues.length];
    for (int i = 0; i < eigenvalues.length; i++) {
      double e = java.lang.Math.abs(eigenvalues[i]);
      Matrix v = eigenvectors.getColumn(i);
      eigenPairs[i] = new EigenPair(v, e);
    }

    Comparator<EigenPair> comp = new Comparator<EigenPair>() {
      public int compare(EigenPair o1, EigenPair o2) {
        int comp = o1.compareTo(o2);
        if (!ascending)
          comp = -1 * comp;
        return comp;
      }
    };

    Arrays.sort(eigenPairs, comp);
  }

  /**
   * Creates a new SortedEigenPairs object from the specified list.
   * The eigenvectors are sorted in descending order.
   *
   * @param eigenPairs the eigenpairs to be sorted
   */
  public SortedEigenPairs(List<EigenPair> eigenPairs) {
    Comparator<EigenPair> comp = new Comparator<EigenPair>() {
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
    for (int i = 0; i < eigenPairs.length; i++) {
      EigenPair eigenPair = eigenPairs[i];
      eigenValues[i] = eigenPair.getEigenvalue();
    }
    return eigenValues;
  }

  /**
   * Returns the sorted eigenvectors.
   *
   * @return the sorted eigenvectors
   */
  public Matrix eigenVectors() {
    Matrix eigenVectors = new Matrix(eigenPairs.length, eigenPairs.length);
    for (int i = 0; i < eigenPairs.length; i++) {
      EigenPair eigenPair = eigenPairs[i];
      eigenVectors.setColumn(i, eigenPair.getEigenvector());
    }
    return eigenVectors;
  }

  /**
   * Returns the first <code>n</code> sorted eigenvectors as a matrix.
   *
   * @param n the number of eigenvectors (columns) to be returned
   * @return the first <code>n</code> sorted eigenvectors
   */
  public Matrix eigenVectors(int n) {
    Matrix eigenVectors = new Matrix(eigenPairs.length, n);
    for (int i = 0; i < n; i++) {
      EigenPair eigenPair = eigenPairs[i];
      eigenVectors.setColumn(i, eigenPair.getEigenvector());
    }
    return eigenVectors;
  }

  /**
   * Returns the last <code>n</code> sorted eigenvectors as a matrix.
   *
   * @param n the number of eigenvectors (columns) to be returned
   * @return the last <code>n</code> sorted eigenvectors
   */
  public Matrix reverseEigenVectors(int n) {
    Matrix eigenVectors = new Matrix(eigenPairs.length, n);
    for (int i = 0; i < n; i++) {
      EigenPair eigenPair = eigenPairs[eigenPairs.length-1-i];
      eigenVectors.setColumn(i, eigenPair.getEigenvector());
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
  public String toString() {
    StringBuffer result = new StringBuffer();
    for(EigenPair eigenPair: eigenPairs) {
      result.append("\n").append(eigenPair);
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
