package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalDistanceFunctionPatternConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Computes the HiCO correlation dimension of objects of a certain database. The
 * PCA is based on epsilon range queries.
 * 
 * @author Elke Achtert
 * @param <V> Vector type
 */
public class RangeQueryBasedHiCOPreprocessor<V extends NumberVector<V,?>> extends HiCOPreprocessor<V> implements Parameterizable {
  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("preprocessor.epsilon", "An epsilon value suitable to the specified distance function.");

  /**
   * Parameter to specify the epsilon used in the preprocessor
   * 
   * Key: {@code -preprocessor.epsilon}
   */
  protected final StringParameter EPSILON_PARAM = new StringParameter(EPSILON_ID);

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database based on a range query.
   */
  public RangeQueryBasedHiCOPreprocessor(Parameterization config) {
    super(config);
    if (config.grab(this, EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }

    GlobalParameterConstraint gpc = new GlobalDistanceFunctionPatternConstraint<DistanceFunction<V, DoubleDistance>>(EPSILON_PARAM, PCA_DISTANCE_PARAM);
    config.checkConstraint(gpc);
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
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   * 
   * @return String a description of the class and the required parameters
   */
  @Override
  public String shortDescription() {
    StringBuffer description = new StringBuffer();
    description.append(RangeQueryBasedHiCOPreprocessor.class.getName());
    description.append(" computes the correlation dimension of objects of a certain database.\n");
    description.append("The PCA is based on epsilon range queries.\n");
    return description.toString();
  }
}
