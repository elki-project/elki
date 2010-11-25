package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
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
 * @see AbstractLocalPCAPreprocessor
 * @author Elke Achtert
 */
@Title("Range Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on epsilon range queries.")
public class RangeQueryBasedLocalPCAPreprocessor extends AbstractLocalPCAPreprocessor implements Parameterizable {
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
    config = config.descend(this);
    EPSILON_PARAM = new DistanceParameter<DoubleDistance>(EPSILON_ID, pcaDistanceFunction != null ? pcaDistanceFunction.getDistanceFactory() : null);
    if(config.grab(EPSILON_PARAM)) {
      epsilon = EPSILON_PARAM.getValue();
    }
  }

  @Override
  public <V extends NumberVector<?, ?>> Instance<V> instantiate(Database<V> database) {
    return new Instance<V>(database, pcaDistanceFunction, pca, epsilon);
  }

  /**
   * The actual preprocessor instance.
   * 
   * @param <V> the type of NumberVector handled by this Preprocessor
   * @author Erich Schubert
   * 
   * Note: final, since overriding the constructor will likely fail!
   */
  public static final class Instance<V extends NumberVector<?, ?>> extends AbstractLocalPCAPreprocessor.Instance<V> {
    /**
     * Epsilon
     */
    final private DoubleDistance epsilon;

    /**
     * Our range query.
     */
    final private RangeQuery<V, DoubleDistance> rangeQuery;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param pcaDistanceFunction distance for determining the neighborhood
     * @param pca PCA runner
     * @param epsilon Epsilon value
     */
    public Instance(Database<V> database, DistanceFunction<? super V, DoubleDistance> pcaDistanceFunction, PCAFilteredRunner<? super V, DoubleDistance> pca, DoubleDistance epsilon) {
      super(database);
      this.epsilon = epsilon;
      this.rangeQuery = database.getRangeQuery(pcaDistanceFunction);
      preprocess(database, pca);
    }

    @Override
    protected List<DistanceResultPair<DoubleDistance>> objectsForPCA(DBID id) {
      return rangeQuery.getRangeForDBID(id, epsilon);
    }
  }
}