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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessGlobalConstraint;

/**
 * @author erich
 * 
 */
public class PCAFilteredRunner<V extends RealVector<V, ?>> extends PCARunner<V> {
  /**
   * Parameter to specify the filter for determination of the strong and weak
   * eigenvectors, must be a subclass of {@link EigenPairFilter}.
   * <p>
   * Default value: {@link PercentageEigenPairFilter}
   * </p>
   * <p>
   * Key: {@code -pca.filter}
   * </p>
   */
  private ClassParameter<EigenPairFilter> EIGENPAIR_FILTER_PARAM = new ClassParameter<EigenPairFilter>(OptionID.PCA_EIGENPAIR_FILTER, EigenPairFilter.class, PercentageEigenPairFilter.class.getName());

  /**
   * The eigenpair filter to determine the strong and weak eigenvectors.
   */
  private EigenPairFilter eigenPairFilter;

  /**
   * OptionID for {@link #BIG_PARAM}
   */
  public static final OptionID BIG_ID = OptionID.getOrCreateOptionID(
      "localpca.big",
      "A constant big value to reset high eigenvalues."
  );

  /**
   * OptionID for {@link #SMALL_PARAM}
   */
  public static final OptionID SMALL_ID = OptionID.getOrCreateOptionID(
      "localpca.small",
      "A constant small value to reset low eigenvalues."
  );

  /**
   * Parameter to specify a constant big value to reset high eigenvalues,
   * must be a double greater than 0.
   * <p>Default value: {@code 1.0} </p>
   * <p>Key: {@code -localpca.big} </p>
   */
  private final DoubleParameter BIG_PARAM = new DoubleParameter(
      BIG_ID,
      new GreaterConstraint(0),
      1.0);

  /**
   * Parameter to specify a constant small value to reset low eigenvalues,
   * must be a double greater than 0.
   * <p>Default value: {@code 0.0} </p>
   * <p>Key: {@code -localpca.small} </p>
   * 
   * FIXME: why is the default of 0.0 ok when it must be > 0?
   */
  private final DoubleParameter SMALL_PARAM = new DoubleParameter(
      SMALL_ID,
      new GreaterEqualConstraint(0),
      0.0);

  /**
   * Holds the big value.
   */
  private double big;

  /**
   * Holds the small value.
   */
  private double small;

  /**
   * 
   */
  public PCAFilteredRunner() {
    addOption(EIGENPAIR_FILTER_PARAM);
    addOption(BIG_PARAM);
    addOption(SMALL_PARAM);
    
    // global constraint small <--> big
    optionHandler.setGlobalParameterConstraint(new LessGlobalConstraint<Double>(SMALL_PARAM, BIG_PARAM));
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    
    // big and small params
    big = BIG_PARAM.getValue();
    small = SMALL_PARAM.getValue();
    
    // eigenPair filter
    eigenPairFilter = EIGENPAIR_FILTER_PARAM.instantiateClass();
    remainingParameters = eigenPairFilter.setParameters(remainingParameters);
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
      attributeSettings.addAll(eigenPairFilter.getAttributeSettings());
      return attributeSettings;
  }

  /**
   * Run PCA on a collection of database IDs
   * 
   * @param ids a colleciton of ids
   * @param database the database used
   * @return PCA result
   */
  public PCAFilteredResult processIds(Collection<Integer> ids, Database<V> database) {
    return processCovarMatrix(covarianceBuilder.processIds(ids, database));
  }

  /**
   * Run PCA on a QueryResult Collection
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return PCA result
   */
  public PCAFilteredResult processQueryResult(Collection<QueryResult<DoubleDistance>> results, Database<V> database) {
    return processCovarMatrix(covarianceBuilder.processQueryResults(results, database));
  }

  /**
   * Process an existing covariance Matrix
   * 
   * @param covarMatrix the matrix used for performing pca
   */
  public PCAFilteredResult processCovarMatrix(Matrix covarMatrix) {
    // TODO: add support for a different implementation to do EVD?
    EigenvalueDecomposition evd = covarMatrix.eig();
    return processEVD(evd);
  }

  /**
   * Process an existing eigenvalue decomposition
   * 
   * @param evd eigenvalue decomposition to use
   */
  public PCAFilteredResult processEVD(EigenvalueDecomposition evd) {
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
    FilteredEigenPairs filteredEigenPairs = eigenPairFilter.filter(eigenPairs);
    return new PCAFilteredResult(eigenPairs, filteredEigenPairs, big, small);
  }
}
