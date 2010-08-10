package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract superclass for preprocessors performing for each object of a certain
 * database a filtered PCA based on the local neighborhood of the object.
 * 
 * @author Elke Achtert
 */
@Title("Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database.")
public abstract class AbstractLocalPCAPreprocessor implements LocalProjectionPreprocessor<NumberVector<?, ?>, PCAFilteredResult> {
  /**
   * OptionID for {@link #PCA_DISTANCE_PARAM}
   */
  public static final OptionID PCA_DISTANCE_ID = OptionID.getOrCreateOptionID("localpca.distancefunction", "The distance function used to select objects for running PCA.");

  /**
   * Parameter to specify the distance function used for running PCA.
   * 
   * Key: {@code -localpca.distancefunction}
   */
  protected final ObjectParameter<DistanceFunction<NumberVector<?, ?>, DoubleDistance>> PCA_DISTANCE_PARAM = new ObjectParameter<DistanceFunction<NumberVector<?, ?>, DoubleDistance>>(PCA_DISTANCE_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * Holds the instance of the distance function specified by
   * {@link #PCA_DISTANCE_PARAM}.
   */
  protected DistanceFunction<NumberVector<?, ?>, DoubleDistance> pcaDistanceFunction;

  /**
   * PCA utility object.
   */
  protected PCAFilteredRunner<NumberVector<?, ?>, DoubleDistance> pca;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractLocalPCAPreprocessor(Parameterization config) {
    super();
    config = config.descend(this);

    // parameter pca distance function
    if(config.grab(PCA_DISTANCE_PARAM)) {
      pcaDistanceFunction = PCA_DISTANCE_PARAM.instantiateClass(config);
    }

    pca = new PCAFilteredRunner<NumberVector<?, ?>, DoubleDistance>(config);
  }
  
  @Override
  abstract public <V extends NumberVector<?, ?>> AbstractLocalPCAPreprocessor.Instance<V> instantiate(Database<V> database);

  /**
   * The actual preprocessor instance.
   * 
   * @param <V> the type of NumberVector handled by this Preprocessor
   * @author Erich Schubert
   */
  public abstract static class Instance<V extends NumberVector<?, ?>> implements LocalProjectionPreprocessor.Instance<PCAFilteredResult> {
    /**
     * Logger to use
     */
    private static final Logging logger = Logging.getLogger(AbstractLocalPCAPreprocessor.class);

    /**
     * Storage for the precomputed results.
     */
    private WritableDataStore<PCAFilteredResult> pcaStorage = null;

    /**
     * Constructor.
     * 
     * @param database Database
     */
    public Instance(Database<V> database) {
      super();
    }

    /**
     * Do the actual preprocessing.
     * @param database
     * @param pcaDistanceFunction
     * @param pca
     */
    protected void preprocess(Database<V> database, DistanceFunction<? super V, DoubleDistance> pcaDistanceFunction, PCAFilteredRunner<? super V, DoubleDistance> pca) {
      DistanceQuery<V, DoubleDistance> distQuery = database.getDistanceQuery(pcaDistanceFunction);

      if(database == null || database.size() <= 0) {
        throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
      }

      // Note: this is required for ERiC to work properly, otherwise the data is
      // recomputed for the partitions!
      if(pcaStorage != null) {
        return;
      }

      pcaStorage = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, PCAFilteredResult.class);

      long start = System.currentTimeMillis();
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Performing local PCA", database.size(), logger) : null;

      for(DBID id : database) {
        List<DistanceResultPair<DoubleDistance>> objects = objectsForPCA(id, database, distQuery);

        PCAFilteredResult pcares = pca.processQueryResult(objects, database);

        pcaStorage.put(id, pcares);

        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
      if(progress != null) {
        progress.ensureCompleted(logger);
      }

      long end = System.currentTimeMillis();
      if(logger.isVerbose()) {
        long elapsedTime = end - start;
        logger.verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
      }
    }

    /**
     * Get the precomputed local PCA for a particular object ID.
     * 
     * @param objid Object ID
     * @return Matrix
     */
    @Override
    public PCAFilteredResult get(DBID objid) {
      return pcaStorage.get(objid);
    }

    /**
     * Returns the objects to be considered within the PCA for the specified
     * query object.
     * 
     * @param id the id of the query object for which a PCA should be performed
     * @param database the database holding the objects
     * @param distQuery the distance function
     * @return the list of the objects (i.e. the ids and the distances to the
     *         query object) to be considered within the PCA
     */
    protected abstract List<DistanceResultPair<DoubleDistance>> objectsForPCA(DBID id, Database<V> database, DistanceQuery<V, DoubleDistance> distQuery);
  }
}