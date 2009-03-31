package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;

/**
 * Computes the HiCO correlation dimension of objects of a certain database. The
 * PCA is based on epsilon range queries.
 * 
 * @author Elke Achtert
 */
public class RangeQueryBasedHiCOPreprocessor<V extends RealVector<V, ?>> extends HiCOPreprocessor<V> {
  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("preprocessor.epsilon", "An epsilon value suitable to the specified distance function.");

  /**
   * Parameter to specify the epsilon used in the preprocessor
   * 
   * Key: {@code -preprocessor.epsilon}
   */
  protected final PatternParameter EPSILON_PARAM = new PatternParameter(EPSILON_ID);
  
  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database based on a range query.
   */
  public RangeQueryBasedHiCOPreprocessor() {
    super();
    addOption(EPSILON_PARAM);

    GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint<DistanceFunction<V, DoubleDistance>>(EPSILON_PARAM, PCA_DISTANCE_PARAM);
    optionHandler.setGlobalParameterConstraint(gpc);
  }

  @Override
  protected List<Integer> objectIDsForPCA(Integer id, Database<V> database, boolean verbose, boolean time) {
    pcaDistanceFunction.setDatabase(database, verbose, time);

    List<DistanceResultPair<DoubleDistance>> knns = database.rangeQuery(id, epsilon, pcaDistanceFunction);

    List<Integer> ids = new ArrayList<Integer>(knns.size());
    for(DistanceResultPair<DoubleDistance> knn : knns) {
      ids.add(knn.getID());
    }

    return ids;
  }

  @Override
  protected List<DistanceResultPair<DoubleDistance>> resultsForPCA(Integer id, Database<V> database, boolean verbose, boolean time) {
    pcaDistanceFunction.setDatabase(database, verbose, time);

    return database.rangeQuery(id, epsilon, pcaDistanceFunction);
  }

  /**
   * Sets the value for the required parameter k.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    epsilon = EPSILON_PARAM.getValue();

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns a description of the class and the required parameters. <p/> This
   * description should be suitable for a usage description.
   * 
   * @return String a description of the class and the required parameters
   */
  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(RangeQueryBasedHiCOPreprocessor.class.getName());
    description.append(" computes the correlation dimension of objects of a certain database.\n");
    description.append("The PCA is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}
