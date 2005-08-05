package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.QueryResult;

import java.util.List;

/**
 * A CorrelationPCA is a principal component analysis that belongs to an object stored in a database.
 * The CorrelationPCA determines the local principal components of the object and
 * encapsulates the correlation dimension, the eigenvectors and the
 * adapted eigenvalues of the object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface CorrelationPCA extends PCA {

  /**
   * Performs a PCA for the object with the specified ids stored in the given database.
   *
   * @param objects   the list of the objects for which a pca should be performed
   * @param database  the database containing the objects
   * @param alpha     the threshold for strong eigenvectors: the strong eigenvectors
   *                  explain portion of at least alpha of the total variance
   */
  void run(List<QueryResult> objects, Database database, double alpha);

  /**
   * Returns the correlation dimension (i.e. the number of strong eigenvectors)
   * of the object to which this PCA belongs to.
   *
   * @return the correlation dimension
   */
  int getCorrelationDimension();

  /**
   * Returns a copy of the selection matrix of the weak eigenvectors (E_hat) of the object
   * to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_hat
   */
  Matrix getSelectionMatrixOfWeakEigenvectors();

  /**
   * Returns a copy of the selection matrix of the strong eigenvectors (E_czech) of the object
   * to which this PCA belongs to.
   *
   * @return the selection matrix of the weak eigenvectors E_czech
   */
  Matrix getSelectionMatrixOfStrongEigenvectors();

  /**
   * Returns a copy of the strong eigenvectors of the object to which this PCA belongs to.
   *
   * @return the matrix of eigenvectors
   */
  Matrix strongEigenVectors();

}
