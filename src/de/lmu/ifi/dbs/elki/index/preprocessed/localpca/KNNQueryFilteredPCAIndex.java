package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides the local neighborhood to be considered in the PCA as the k nearest
 * neighbors of an object.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.uses KNNQuery
 *
 * @param <NV> Vector type
 */
// TODO: loosen DoubleDistance restriction.
@Title("Knn Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on k nearest neighbor queries.")
public class KNNQueryFilteredPCAIndex<NV extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex<NV> {
  /**
   * Logger.
   */
  private static final Logging logger = Logging.getLogger(KNNQueryFilteredPCAIndex.class);

  /**
   * The kNN query instance we use
   */
  final private KNNQuery<NV, DoubleDistance> knnQuery;

  /**
   * Query k
   */
  final private int k;

  /**
   * Constructor.
   * 
   * @param database Database to use
   * @param pca PCA Runner to use
   * @param knnQuery KNN Query to use
   * @param k k value
   */
  public KNNQueryFilteredPCAIndex(Database<NV> database, PCAFilteredRunner<? super NV, DoubleDistance> pca, KNNQuery<NV, DoubleDistance> knnQuery, int k) {
    super(database, pca);
    this.knnQuery = knnQuery;
    this.k = k;
  }

  @Override
  protected List<DistanceResultPair<DoubleDistance>> objectsForPCA(DBID id) {
    return knnQuery.getKNNForDBID(id, k);
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
   * @apiviz.uses KNNQueryFilteredPCAIndex oneway - - «create»
   */
  public static class Factory<V extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex.Factory<V, KNNQueryFilteredPCAIndex<V>> {
    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("localpca.k", "The number of nearest neighbors considered in the PCA. " + "If this parameter is not set, k ist set to three " + "times of the dimensionality of the database objects.");

    /**
     * Optional parameter to specify the number of nearest neighbors considered
     * in the PCA, must be an integer greater than 0. If this parameter is not
     * set, k is set to three times of the dimensionality of the database
     * objects.
     * <p>
     * Key: {@code -localpca.k}
     * </p>
     * <p>
     * Default value: three times of the dimensionality of the database objects
     * </p>
     */
    private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0), true);

    /**
     * Holds the value of {@link #K_PARAM}.
     */
    private Integer k = null;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super(config);
      config = config.descend(this);

      if(config.grab(K_PARAM)) {
        k = K_PARAM.getValue();
      }
    }

    @Override
    public KNNQueryFilteredPCAIndex<V> instantiate(Database<V> database) {
      // TODO: set bulk flag, once the parent class supports bulk.
      KNNQuery<V, DoubleDistance> knnquery = database.getKNNQuery(pcaDistanceFunction, k);
      return new KNNQueryFilteredPCAIndex<V>(database, pca, knnquery, k);
    }
  }
}