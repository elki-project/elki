package de.lmu.ifi.dbs.elki.varianceanalysis;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

public class PCARunner<V extends RealVector<V, ?>> extends AbstractParameterizable {  
  /**
   * Parameter to specify the class to compute the covariance matrix.
   * must be a subclass of {@link CovarianceMatrixBuilder}.
   * <p>Default value: {@link CovarianceMatrixBuilder}</p>
   * <p>Key: {@code -pca.covariance} </p>
   */
  private ClassParameter<CovarianceMatrixBuilder> COVARIANCE_PARAM =
      new ClassParameter<CovarianceMatrixBuilder>(
          OptionID.PCA_COVARIANCE_MATRIX,
          CovarianceMatrixBuilder.class,
          PlainCovarianceMatrixBuilder.class.getName());

  /**
   * The covariance computation class.
   */
  protected CovarianceMatrixBuilder<V> covarianceBuilder;

  /**
   * Constructor for the covariance runner.
   */

  public PCARunner() {
    super();
    addOption(COVARIANCE_PARAM);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws ParameterException {
      String[] remainingParameters = super.setParameters(args);
      // small value
      covarianceBuilder = COVARIANCE_PARAM.instantiateClass();
      remainingParameters = covarianceBuilder.setParameters(remainingParameters);
      setParameters(args, remainingParameters);

      return remainingParameters;
  }
  
  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
      List<AttributeSettings> attributeSettings = super.getAttributeSettings();
      attributeSettings.addAll(covarianceBuilder.getAttributeSettings());
      return attributeSettings;
  }

  /**
   * Run PCA on the complete database
   * 
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processDatabase(Database<V> database) {
    return processCovarMatrix(covarianceBuilder.processDatabase(database));
  }

  /**
   * Run PCA on a collection of database IDs
   * 
   * @param ids a colleciton of ids
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processIds(Collection<Integer> ids, Database<V> database) {
    return processCovarMatrix(covarianceBuilder.processIds(ids, database));
  }

  /**
   * Run PCA on a QueryResult Collection
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return PCA result
   */
  public PCAResult processQueryResult(Collection<QueryResult<DoubleDistance>> results, Database<V> database) {
    return processCovarMatrix(covarianceBuilder.processQueryResults(results, database));
  }

  /**
   * Process an existing covariance Matrix
   * 
   * @param covarMatrix the matrix used for performing pca
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
   */
  public PCAResult processEVD(EigenvalueDecomposition evd) {
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    return new PCAResult(eigenPairs);
  }
}
