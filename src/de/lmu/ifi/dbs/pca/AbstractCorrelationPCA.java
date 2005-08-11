package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.database.AbstractDatabase;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AbstractCorrelationPCA provides some methods valid for any extending class.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractCorrelationPCA implements CorrelationPCA {
  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level loggerLevel = Level.OFF;

  /**
   * The correlation dimension (i.e. the number of strong eigenvectors)
   * of the object to which this PCA belongs to.
   */
  private int correlationDimension = 0;

  /**
   * The eigenvalues in decreasing order.
   */
  private double[] eigenvalues;

  /**
   * The eigenvectors in decreasing order to their corresponding eigenvalues.
   */
  private Matrix eigenvectors;

  /**
   * The strong eigenvectors.
   */
  private Matrix strongEigenvectors;

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

  public AbstractCorrelationPCA() {
    initLogger();
  }

  /**
   * Performs a PCA for the object with the specified ids stored in the given database.
   *
   * @param ids       the ids of the objects for which the PCA should be performed
   * @param database the database containing the objects
   * @param alpha    the threshold for strong eigenvectors: the strong eigenvectors
   */
  public void run(List<Integer> ids, Database database, double alpha) {
    // logging
    StringBuffer msg = new StringBuffer();
    FeatureVector o = (FeatureVector) database.get(ids.get(0));
    String label = (String) database.getAssociation(AbstractDatabase.ASSOCIATION_ID_LABEL,
                                                    ids.get(0));
    msg.append("object ");
    msg.append(o);
    msg.append(" ");
    msg.append(label);

    // eigenvalues and -vectors
    EigenvalueDecomposition evd = eigenValueDecomposition(database, ids);

    // eigenvalues (and eigenvectors) in ascending order
    eigenvalues = evd.getD().getDiagonal();
    eigenvectors = evd.getV();
    sortEigenvectors();
    computeSelectionMatrices(alpha);

    msg.append("\n  E = ");
    msg.append(Util.format(eigenvalues));

    msg.append("\n  V = ");
    msg.append(eigenvectors);

    msg.append("\n  E_hat = ");
    msg.append(e_hat);

    msg.append("\n  E_czech = ");
    msg.append(e_czech);

    msg.append("\n  corrDim = ");
    msg.append(correlationDimension);

    logger.info(msg.toString() + "\n");

  }

  /**
   * Returns the correlation dimension (i.e. the number of strong eigenvectors)
   * of the object to which this PCA belongs to.
   *
   * @return the correlation dimension
   */
  public int getCorrelationDimension() {
    return correlationDimension;
  }

  /**
   * Returns a copy of the matrix of eigenvectors
   * of the object to which this PCA belongs to.
   *
   * @return the matrix of eigenvectors
   */
  public Matrix getEigenvectors() {
    return eigenvectors.copy();
  }

  /**
   * Returns a copy of the eigenvalues of the object to which this PCA belongs to
   * in decreasing order.
   *
   * @return the eigenvalues
   */
  public double[] getEigenvalues() {
    return Util.copy(eigenvalues);
  }

  /**
   * Returns a copy of the selection matrix of the weak eigenvectors (E_hat) of the object
   * to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_hat
   */
  public Matrix getSelectionMatrixOfWeakEigenvectors() {
    return e_hat.copy();
  }

  /**
   * Returns a copy of the selection matrix of the strong eigenvectors (E_czech) of the object
   * to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_czech
   */
  public Matrix getSelectionMatrixOfStrongEigenvectors() {
    return e_czech.copy();
  }

  /**
   * Returns a copy of the similarity matrix (M_hat) of the object
   * to which this PCA belongs to.
   *
   * @return the similarity matrix M_hat
   */
  public Matrix getSimilarityMatrix() {
    return m_hat.copy();
  }

  /**
   * Returns a copy of the dissimilarity matrix (M_czech) of the object
   * to which this PCA belongs to.
   *
   * @return the dissimilarity matrix M_hat
   */
  public Matrix getDissimilarityMatrix() {
    return m_czech.copy();
  }

  /**
   * Returns a copy of the strong eigenvectors of the object to which this PCA belongs to.
   *
   * @return the matrix of eigenvectors
   */
  public Matrix strongEigenVectors() {
    return strongEigenvectors.copy();
  }

  /**
   * Performs the actual eigenvalue decomposition on the specified object ids
   * stored in the given database.
   *
   * @param database the database holding the objects
   * @param ids  the list of the object ids for which the eigenvalue decomposition
   *                 should be performed
   * @return the actual eigenvalue decomposition on the specified object ids
   *         stored in the given database
   */
  protected abstract EigenvalueDecomposition eigenValueDecomposition(Database database,
                                                                     List<Integer> ids);

  /**
   * Sorts the eigenvalues and eigenvectors in decreasing order
   * to the eigenvalues.
   */
  private void sortEigenvectors() {
    EigenPair[] result = new EigenPair[eigenvalues.length];
    for (int i = 0; i < eigenvalues.length; i++) {
      double e = eigenvalues[i];
      Matrix v = eigenvectors.getColumn(i);
      result[i] = new EigenPair(v, e);
    }
    // sortEigenvectors in decreasing order
    Arrays.sort(result);

    // set the eigenvalue and -vectors
    for (int i = 0; i < result.length; i++) {
      EigenPair eigenPair = result[i];
      eigenvalues[i] = eigenPair.eigenvalue;
      eigenvectors.setColumn(i, eigenPair.eigenvector);
    }
  }

  /**
   * Computes the selection matrices of the weak and strong eigenvectors,
   * the similarity matrix and the correlation dimension.
   *
   * @param alpha the threshold for strong eigenvectors
   */
  private void computeSelectionMatrices(double alpha) {
    StringBuffer msg = new StringBuffer();

    msg.append("alpha = ");
    msg.append(alpha);

    e_hat = new Matrix(eigenvalues.length, eigenvalues.length);
    e_czech = new Matrix(eigenvalues.length, eigenvalues.length);

    double totalSum = 0;
    for (double eigenvalue : eigenvalues) {
      totalSum += eigenvalue;
    }
    msg.append("\n totalSum = ");
    msg.append(totalSum);

    double currSum = 0;
    boolean found = false;
    for (int i = 0; i < eigenvalues.length; i++) {
      currSum += eigenvalues[i];
      // weak EV -> set EW to 1
      if (currSum / totalSum >= alpha) {
        if (! found) {
          found = true;
          correlationDimension++;
          e_czech.set(i, i, 1);
        }
        else {
          e_hat.set(i, i, 1);
        }
      }
      else {
        correlationDimension++;
        e_czech.set(i, i, 1);
      }

//      if (currSum / totalSum >= alpha) {
//        if (! found) {
//          e_czech.set(i, i, 1);
//          correlationDimension++;
//          found = true;
//        }
//        else {
//          e_hat.set(i, i, 1);
//        }
//      }
//      else {
//        e_hat.set(i, i, 1);
//      }
    }
    strongEigenvectors = eigenvectors.times(e_czech);

    m_hat = eigenvectors.times(e_hat).times(eigenvectors.transpose());
    m_czech = eigenvectors.times(e_czech).times(eigenvectors.transpose());

    logger.info(msg.toString());
  }


  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(AbstractCorrelationPCA.class.toString());
    logger.setLevel(loggerLevel);
  }

  /**
   * Helper class which encapsulates a eigenvector and its corresponding eigenvalue. This class is used to
   * sortEigenvectors the eigenvectors (and -values) in descending order
   */
  private class EigenPair implements Comparable<EigenPair> {
    /**
     * The eigenvector as a matrix.
     */
    Matrix eigenvector;

    /**
     * The corresponding eigenvalue.
     */
    double eigenvalue;

    /**
     * Creates a new EigenPair object.
     *
     * @param eigenvector the eigenvector as a matrix
     * @param eigenvalue  the corresponding eigenvalue
     */
    private EigenPair(Matrix eigenvector, double eigenvalue) {
      this.eigenvalue = eigenvalue;
      this.eigenvector = eigenvector;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object's eigenvalue
     * is greater than, equal to, or less than the specified object's eigenvalue.
     *
     * @param o the Eigenvector to be compared.
     * @return a negative integer, zero, or a positive integer as this object's eigenvalue
     *         is greater than, equal to, or less than the specified object's eigenvalue.
     */
    public int compareTo(EigenPair o) {
      // sortEigenvectors in descending order!
      if (this.eigenvalue > o.eigenvalue) return -1;
      if (this.eigenvalue < o.eigenvalue) return +1;
      return 0;
    }

    /**
     * Returns a string representation of this Eigenvector.
     *
     * @return a string representation of this Eigenvector
     */
    public String toString() {
      return "(ew = " + Util.format(eigenvalue) + ", ev = [" + Util.format(eigenvector.getColumnPackedCopy()) + "])";
    }
  }
}
