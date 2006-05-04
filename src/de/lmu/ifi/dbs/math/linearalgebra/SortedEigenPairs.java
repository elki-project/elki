package de.lmu.ifi.dbs.math.linearalgebra;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Helper class which encapsulates an array of eigenpairs (i.e. an array
 * of eigenvectors and their corresponding eigenvalues).
 * This class is used to sort eigenvectors (and -values).
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SortedEigenPairs {
  /**
   * The array of eigenpairs.
   */
  private EigenPair[] eigenPairs;

  /**
   * Creates a new EigenPairs object from the specified eigenvalue
   * decomposition. The eigenvectors are sorted according to the specified
   * order.
   *
   * @param evd       the underlying eigenvalue decomposition
   * @param ascending a boolean that indicates ascending order
   */
  public SortedEigenPairs(EigenvalueDecomposition evd, final boolean ascending) {
    double[] eigenvalues = evd.getD().getDiagonal();
    Matrix eigenvectors = evd.getV();

    this.eigenPairs = new EigenPair[eigenvalues.length];
    for (int i = 0; i < eigenvalues.length; i++) {
      double e = eigenvalues[i];
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
   * Returns a string representation of this EigenPair.
   *
   * @return a string representation of this EigenPair
   */
  public String toString() {
    return Arrays.asList(eigenPairs).toString();
  }
}
