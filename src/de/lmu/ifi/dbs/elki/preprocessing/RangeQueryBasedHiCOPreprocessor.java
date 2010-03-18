package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;

/**
 * Computes the HiCO correlation dimension of objects of a certain database. The
 * PCA is based on epsilon range queries.
 * 
 * @author Elke Achtert
 * @param <V> Vector type
 */
@Title("RangeQuery HiCO Preprocessor")
@Description("Computes the correlation dimension of objects of a certain database.\n" + "The PCA is based on epsilon range queries.")
public class RangeQueryBasedHiCOPreprocessor<V extends NumberVector<V, ?>> extends LocalPCAPreprocessor<V> implements Parameterizable {
  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("preprocessor.epsilon", "An epsilon value suitable to the specified distance function.");

  /**
   * Parameter to specify the epsilon used in the preprocessor
   * 
   * Key: {@code -preprocessor.epsilon}
   */
  protected final DistanceParameter<DoubleDistance> EPSILON_PARAM;

  /**
   * Epsilon.
   */
  protected DoubleDistance epsilon;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database based on a range query.
   */
  public RangeQueryBasedHiCOPreprocessor(Parameterization config) {
    super(config);
    EPSILON_PARAM = new DistanceParameter<DoubleDistance>(EPSILON_ID, pcaDistanceFunction);
    if(config.grab(EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }
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
}
