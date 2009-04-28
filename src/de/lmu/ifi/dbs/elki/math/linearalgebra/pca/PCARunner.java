package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Class to run PCA on given data.
 * 
 * The various methods will start PCA at different places (e.g. with database IDs,
 * database query results, a precomputed covariance matrix or eigenvalue decomposition).
 * 
 * The runner can be parametrized by setting a covariance matrix builder (e.g. to
 * a weighted covariance matrix builder) 
 * 
 * @author Erich Schubert
 *
 * @param <V>
 */
public class PCARunner<V extends RealVector<V, ?>> extends AbstractParameterizable {
  /**
   * OptionID for {@link #COVARIANCE_PARAM}
   */
  public static final OptionID PCA_COVARIANCE_MATRIX = OptionID.getOrCreateOptionID("pca.covariance",
      "Class used to compute the covariance matrix.");

  /**
   * Parameter to specify the class to compute the covariance matrix. must be a
   * subclass of {@link CovarianceMatrixBuilder}.
   * <p>
   * Default value: {@link CovarianceMatrixBuilder}
   * </p>
   * <p>
   * Key: {@code -pca.covariance}
   * </p>
   */
  private ClassParameter<CovarianceMatrixBuilder<V>> COVARIANCE_PARAM = 
    new ClassParameter<CovarianceMatrixBuilder<V>>(PCA_COVARIANCE_MATRIX, 
        CovarianceMatrixBuilder.class, StandardCovarianceMatrixBuilder.class.getName());

  /**
   * The covariance computation class.
   */
  protected CovarianceMatrixBuilder<V> covarianceMatrixBuilder;

  /**
   * Constructor for the covariance runner.
   */

  public PCARunner() {
    super();
    addOption(COVARIANCE_PARAM);
  }

  /**
   * Parameter handling.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // small value
    covarianceMatrixBuilder = COVARIANCE_PARAM.instantiateClass();
    remainingParameters = covarianceMatrixBuilder.setParameters(remainingParameters);
    addParameterizable(covarianceMatrixBuilder);

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Run PCA on the complete database
   * 
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processDatabase(Database<V> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processDatabase(database));
  }

  /**
   * Run PCA on a collection of database IDs
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processIds(Collection<Integer> ids, Database<V> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processIds(ids, database));
  }

  /**
   * Run PCA on a QueryResult Collection
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processQueryResult(Collection<DistanceResultPair<DoubleDistance>> results, Database<V> database) {
    return processCovarMatrix(covarianceMatrixBuilder.processQueryResults(results, database));
  }

  /**
   * Process an existing covariance Matrix
   * 
   * @param covarMatrix the matrix used for performing pca
   * @return PCA result
   */
  public PCAResult processCovarMatrix(Matrix covarMatrix) {
    // TODO: add support for a different implementation to do EVD?
    EigenvalueDecomposition evd = covarMatrix.eig();
    return processEVD(evd);
  }

  /**
   * Process an existing eigenvalue decomposition
   * 
   * @param evd eigenvalue decomposition to use
   * @return PCA result
   */
  public PCAResult processEVD(EigenvalueDecomposition evd) {
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    return new PCAResult(eigenPairs);
  }

  /**
   * Get covariance matrix builder
   * 
   * @return covariance matrix builder in use
   */
  public CovarianceMatrixBuilder<V> getCovarianceMatrixBuilder() {
    return covarianceMatrixBuilder;
  }

  /**
   * Set covariance matrix builder.
   * 
   * @param covarianceBuilder New covariance matrix builder.
   */
  public void setCovarianceMatrixBuilder(CovarianceMatrixBuilder<V> covarianceBuilder) {
    this.covarianceMatrixBuilder = covarianceBuilder;
  }
}
