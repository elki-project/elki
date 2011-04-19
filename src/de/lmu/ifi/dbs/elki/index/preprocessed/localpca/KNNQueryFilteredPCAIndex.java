package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
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
 * @apiviz.landmark
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
  public KNNQueryFilteredPCAIndex(Relation<NV> database, PCAFilteredRunner<? super NV, DoubleDistance> pca, KNNQuery<NV, DoubleDistance> knnQuery, int k) {
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
   * @apiviz.landmark
   * @apiviz.uses KNNQueryFilteredPCAIndex oneway - - «create»
   */
  public static class Factory<V extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex.Factory<V, KNNQueryFilteredPCAIndex<V>> {
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
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("localpca.k", "The number of nearest neighbors considered in the PCA. " + "If this parameter is not set, k ist set to three " + "times of the dimensionality of the database objects.");

    /**
     * Holds the value of {@link #K_ID}.
     */
    private Integer k = null;

    /**
     * Constructor.
     * 
     * @param pcaDistanceFunction distance
     * @param pca PCA class
     * @param k k
     */
    public Factory(DistanceFunction<V, DoubleDistance> pcaDistanceFunction, PCAFilteredRunner<V, DoubleDistance> pca, Integer k) {
      super(pcaDistanceFunction, pca);
      this.k = k;
    }

    @Override
    public KNNQueryFilteredPCAIndex<V> instantiate(Relation<V> representation) {
      // TODO: set bulk flag, once the parent class supports bulk.
      KNNQuery<V, DoubleDistance> knnquery = representation.getDatabase().getKNNQuery(representation, pcaDistanceFunction, k);
      return new KNNQueryFilteredPCAIndex<V>(representation, pca, knnquery, k);
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<NV extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex.Factory.Parameterizer<NV, KNNQueryFilteredPCAIndex<NV>> {
      protected int k = 0;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(0), true);
        if(config.grab(kP)) {
          k = kP.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<NV>(pcaDistanceFunction, pca, k);
      }
    }
  }
}