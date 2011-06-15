package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for a local PCA based index.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.has PCAFilteredRunner
 * 
 * @param <NV> Vector type
 */
// TODO: loosen DoubleDistance restriction.
@Title("Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database.")
public abstract class AbstractFilteredPCAIndex<NV extends NumberVector<? extends NV, ?>> extends AbstractPreprocessorIndex<NV, PCAFilteredResult> implements FilteredLocalPCAIndex<NV> {
  /**
   * PCA utility object.
   */
  final protected PCAFilteredRunner<NV> pca;

  /**
   * Constructor.
   * 
   * @param relation Relation to use
   * @param pca PCA runner to use
   */
  public AbstractFilteredPCAIndex(Relation<NV> relation, PCAFilteredRunner<NV> pca) {
    super(relation);
    this.pca = pca;
  }

  /**
   * Preprocessing step.
   */
  protected void preprocess() {
    if(relation == null || relation.size() <= 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }

    // Note: this is required for ERiC to work properly, otherwise the data is
    // recomputed for the partitions!
    if(storage != null) {
      return;
    }

    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, PCAFilteredResult.class);

    long start = System.currentTimeMillis();
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Performing local PCA", relation.size(), getLogger()) : null;

    // TODO: use a bulk operation?
    for(DBID id : relation.iterDBIDs()) {
      List<DistanceResultPair<DoubleDistance>> objects = objectsForPCA(id);

      PCAFilteredResult pcares = pca.processQueryResult(objects, relation);

      storage.put(id, pcares);

      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }
    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }

    long end = System.currentTimeMillis();
    if(getLogger().isVerbose()) {
      long elapsedTime = end - start;
      getLogger().verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }

  @Override
  public PCAFilteredResult getLocalProjection(DBID objid) {
    if(storage == null) {
      preprocess();
    }
    return storage.get(objid);
  }

  /**
   * Returns the objects to be considered within the PCA for the specified query
   * object.
   * 
   * @param id the id of the query object for which a PCA should be performed
   * @return the list of the objects (i.e. the ids and the distances to the
   *         query object) to be considered within the PCA
   */
  protected abstract List<DistanceResultPair<DoubleDistance>> objectsForPCA(DBID id);

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AbstractFilteredPCAIndex oneway - - «create»
   */
  public static abstract class Factory<NV extends NumberVector<NV, ?>, I extends AbstractFilteredPCAIndex<NV>> implements FilteredLocalPCAIndex.Factory<NV, I>, Parameterizable {
    /**
     * Parameter to specify the distance function used for running PCA.
     * 
     * Key: {@code -localpca.distancefunction}
     */
    public static final OptionID PCA_DISTANCE_ID = OptionID.getOrCreateOptionID("localpca.distancefunction", "The distance function used to select objects for running PCA.");

    /**
     * Holds the instance of the distance function specified by
     * {@link #PCA_DISTANCE_ID}.
     */
    protected DistanceFunction<NV, DoubleDistance> pcaDistanceFunction;

    /**
     * PCA utility object.
     */
    protected PCAFilteredRunner<NV> pca;

    /**
     * Constructor.
     * 
     * @param pcaDistanceFunction distance Function
     * @param pca PCA runner
     */
    public Factory(DistanceFunction<NV, DoubleDistance> pcaDistanceFunction, PCAFilteredRunner<NV> pca) {
      super();
      this.pcaDistanceFunction = pcaDistanceFunction;
      this.pca = pca;
    }

    @Override
    public abstract I instantiate(Relation<NV> relation);

    @Override
    public TypeInformation getInputTypeRestriction() {
      return pcaDistanceFunction.getInputTypeRestriction();
    }
    
    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static abstract class Parameterizer<NV extends NumberVector<NV, ?>, I extends AbstractFilteredPCAIndex<NV>> extends AbstractParameterizer {
      /**
       * Holds the instance of the distance function specified by
       * {@link #PCA_DISTANCE_ID}.
       */
      protected DistanceFunction<NV, DoubleDistance> pcaDistanceFunction;

      /**
       * PCA utility object.
       */
      protected PCAFilteredRunner<NV> pca;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final ObjectParameter<DistanceFunction<NV, DoubleDistance>> pcaDistanceFunctionP = new ObjectParameter<DistanceFunction<NV, DoubleDistance>>(PCA_DISTANCE_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

        if(config.grab(pcaDistanceFunctionP)) {
          pcaDistanceFunction = pcaDistanceFunctionP.instantiateClass(config);
        }

        Class<PCAFilteredRunner<NV>> cls = ClassGenericsUtil.uglyCastIntoSubclass(PCAFilteredRunner.class);
        pca = config.tryInstantiate(cls);
      }
    }
  }
}