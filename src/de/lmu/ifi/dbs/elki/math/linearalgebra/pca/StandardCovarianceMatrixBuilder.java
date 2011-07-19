package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Class for building a "traditional" covariance matrix.
 * Reasonable default choice for a {@link CovarianceMatrixBuilder}
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses CovarianceMatrix
 *
 * @param <V> Vector class to use.
 */
public class StandardCovarianceMatrixBuilder<V extends NumberVector<? extends V, ?>> extends AbstractCovarianceMatrixBuilder<V> {
  /**
   * Compute Covariance Matrix for a complete database
   * 
   * @param database the database used
   * @return Covariance Matrix
   */
  @Override
  public Matrix processDatabase(Relation<? extends V> database) {
    return CovarianceMatrix.make(database).destroyToNaiveMatrix();
  }

  /**
   * Compute Covariance Matrix for a collection of database IDs
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return Covariance Matrix
   */
  @Override
  public Matrix processIds(DBIDs ids, Relation<? extends V> database) {
    return CovarianceMatrix.make(database, ids).destroyToNaiveMatrix();
  }
}