package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;

/**
 * Provides the local neighborhood to be considered in the PCA as the neighbors
 * within an epsilon range query of an object.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.uses RangeQuery
 *
 * @param <NV> Vector type
 */
// TODO: loosen DoubleDistance restriction.
@Title("Range Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on epsilon range queries.")
public class RangeQueryFilteredPCAIndex<NV extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex<NV> {
  /**
   * Logger.
   */
  private static final Logging logger = Logging.getLogger(RangeQueryFilteredPCAIndex.class);

  /**
   * The kNN query instance we use
   */
  final private RangeQuery<NV, DoubleDistance> rangeQuery;

  /**
   * Query epsilon
   */
  final private DoubleDistance epsilon;

  /**
   * Constructor.
   * 
   * @param database Database to use
   * @param pca PCA Runner to use
   * @param rangeQuery Range Query to use
   * @param epsilon Query range
   */
  public RangeQueryFilteredPCAIndex(Database<NV> database, PCAFilteredRunner<? super NV, DoubleDistance> pca, RangeQuery<NV, DoubleDistance> rangeQuery, DoubleDistance epsilon) {
    super(database, pca);
    this.rangeQuery = rangeQuery;
    this.epsilon = epsilon;
  }

  @Override
  protected List<DistanceResultPair<DoubleDistance>> objectsForPCA(DBID id) {
    return rangeQuery.getRangeForDBID(id, epsilon);
  }

  @Override
  public String getLongName() {
    return "kNN-based local filtered PCA";
  }

  @Override
  public String getShortName() {
    return "kNNFilteredPCA";
  }

  @Override
  public Logging getLogger() {
    return logger;
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses RangeQueryFilteredPCAIndex oneway - - «create»
   */
  public static class Factory<V extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex.Factory<V, RangeQueryFilteredPCAIndex<V>> {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered in the PCA, must be suitable to the distance function specified.
     * 
     * Key: {@code -localpca.epsilon}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("localpca.epsilon", "The maximum radius of the neighborhood to be considered in the PCA.");

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
    public Factory(Parameterization config) {
      super(config);
      config = config.descend(this);

      DistanceParameter<DoubleDistance> EPSILON_PARAM = new DistanceParameter<DoubleDistance>(EPSILON_ID, pcaDistanceFunction != null ? pcaDistanceFunction.getDistanceFactory() : null);
      if(config.grab(EPSILON_PARAM)) {
        epsilon = EPSILON_PARAM.getValue();
      }
    }

    @Override
    public RangeQueryFilteredPCAIndex<V> instantiate(Database<V> database) {
      // TODO: set bulk flag, once the parent class supports bulk.
      RangeQuery<V, DoubleDistance> rangequery = database.getRangeQuery(pcaDistanceFunction);
      return new RangeQueryFilteredPCAIndex<V>(database, pca, rangequery, epsilon);
    }
  }
}