package de.lmu.ifi.dbs.elki.preprocessing;

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
 * Provides the local neighborhood to be considered in the PCA as the neighbors
 * within an epsilon range query of an object.
 * 
 * @see LocalPCAPreprocessor
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by this Preprocessor
 */
@Title("Range Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on epsilon range queries.")
public class RangeQueryBasedLocalPCAPreprocessor<V extends NumberVector<V, ?>> extends LocalPCAPreprocessor<V> implements Parameterizable {
  /**
   * OptionID for {@link #EPSILON_PARAM}
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("localpca.epsilon", "The maximum radius of the neighborhood to be considered in the PCA.");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered in the PCA, must be suitable to the distance function specified.
   * 
   * Key: {@code -localpca.epsilon}
   */
  protected final DistanceParameter<DoubleDistance> EPSILON_PARAM;

  /**
   * Holds the value of {@link #EPSILON_PARAM}.
   */
  protected DoubleDistance epsilon;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public RangeQueryBasedLocalPCAPreprocessor(Parameterization config) {
    super(config);
    EPSILON_PARAM = new DistanceParameter<DoubleDistance>(EPSILON_ID, pcaDistanceFunction);
    if(config.grab(EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }
  }

  @Override
  protected List<DistanceResultPair<DoubleDistance>> objectsForPCA(Integer id, Database<V> database) {
    pcaDistanceFunction.setDatabase(database);
    return database.rangeQuery(id, epsilon, pcaDistanceFunction);
  }
}
