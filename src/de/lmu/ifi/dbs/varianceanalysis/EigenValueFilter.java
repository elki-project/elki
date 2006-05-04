package de.lmu.ifi.dbs.varianceanalysis;

/**
 * The eigenvalue filter is used to exclude some unfit data after
 * it has been analysed using a Variance Analysis Algorithm,
 * e.g. Principal Component Analysis.
 * It is mainly used to exlude data, whose variance is too small.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public interface EigenValueFilter {

  /**
   * This function is called after the data has
   * been analysed using a Variance Analysis Algorithm.
   *
   * @param eigenValues  the computed eigenvalues
   * @param eigenVectors the computed eigenvectors
   */
  public void passEigenValues(double[] eigenValues, double[][] eigenVectors);

  /**
   * Returns the remaining eigenvalues, which
   * were not excluded.
   *
   * @return the remaining eigenvalues
   */
  public double[] getEigenValues();

  /**
   * Returns the remaining eigenvectors, which
   * were not excluded.
   *
   * @return the remaining eigenvectors
   */
  public double[][] getEigenVectors();

}
