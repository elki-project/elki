package de.lmu.ifi.dbs.pca;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.util.List;

/**
 * A LocalPCA is a principal component analysis that belongs to an object stored in a database.
 * The LocalPCA determines the local principal components according to a local neighborhood
 * of the object and encapsulates the correlation dimension (i.e. the number of weak eigenvectors),
 * the eigenvectors and the adapted eigenvalues of the object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface LocalPCA extends PCA {

  /**
   * Performs a PCA for the object with the specified ids stored in the given database.
   *
   * @param ids      the ids of the objects for which the PCA should be performed
   * @param database the database containing the objects
   * @param alpha    the threshold for strong eigenvectors: the strong eigenvectors
   */
  void run(List<Integer> ids, Database<RealVector> database, double alpha);

  /**
   * Performs a PCA for the object with the specified ids stored in the given database.
   *
   * @param ids       the ids of the objects for which the PCA should be performed
   * @param database  the database containing the objects
   * @param strongEVs the number of strong eigenvectors
   */
  public void run(List<Integer> ids, Database<RealVector> database, int strongEVs);

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

  /**
   * Returns a copy of the similarity matrix (M_hat) of the object
   * to which this PCA belongs to.
   *
   * @return the similarity matrix M_hat
   */
  public Matrix getSimilarityMatrix();

  /**
   * Returns a copy of the dissimilarity matrix (M_czech) of the object
   * to which this PCA belongs to.
   *
   * @return the dissimilarity matrix M_hat
   */
  public Matrix getDissimilarityMatrix();

}
