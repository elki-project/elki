package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract superclass for preprocessors performing for each object of a certain
 * database a filtered PCA based on the local neighborhood of the object. The
 * result of the PCA is assigned to the database with association id
 * {@link AssociationID#LOCAL_PCA}. Additionally, a copy of the PCAs similarity
 * matrix is assigned with association id
 * {@link AssociationID#LOCALLY_WEIGHTED_MATRIX}.
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by this Preprocessor
 */
@Title("Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database.")
public abstract class LocalPCAPreprocessor<V extends NumberVector<V, ?>> extends AbstractLoggable implements Preprocessor<V> {
  /**
   * The default distance function for the PCA.
   */
  public static final Class<?> DEFAULT_PCA_DISTANCE_FUNCTION = EuclideanDistanceFunction.class;

  /**
   * OptionID for {@link #PCA_DISTANCE_PARAM}
   */
  public static final OptionID PCA_DISTANCE_ID = OptionID.getOrCreateOptionID("localpca.distancefunction", "The distance function used to select objects for running PCA.");

  /**
   * Parameter to specify the distance function used for running PCA.
   * 
   * Key: {@code -localpca.distancefunction}
   */
  protected final ObjectParameter<DistanceFunction<V, DoubleDistance>> PCA_DISTANCE_PARAM = new ObjectParameter<DistanceFunction<V, DoubleDistance>>(PCA_DISTANCE_ID, DistanceFunction.class, DEFAULT_PCA_DISTANCE_FUNCTION);

  /**
   * Holds the instance of the distance function specified by
   * {@link #PCA_DISTANCE_PARAM}.
   */
  protected DistanceFunction<V, DoubleDistance> pcaDistanceFunction;

  /**
   * PCA utility object.
   */
  private PCAFilteredRunner<V, DoubleDistance> pca;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public LocalPCAPreprocessor(Parameterization config) {
    super();

    // parameter pca distance function
    if(config.grab(PCA_DISTANCE_PARAM)) {
      pcaDistanceFunction = PCA_DISTANCE_PARAM.instantiateClass(config);
    }

    pca = new PCAFilteredRunner<V, DoubleDistance>(config);
  }

  /**
   * Performs for each object of the specified database a filtered PCA based on
   * the local neighborhood of the object. The result of the PCA is assigned to
   * the database with association id {@link AssociationID#LOCAL_PCA}.
   * Additionally, a copy of the PCAs similarity matrix is assigned with
   * association id {@link AssociationID#LOCALLY_WEIGHTED_MATRIX}.
   * 
   * @param database the database for which the preprocessing is performed
   * @param verbose flag to allow verbose messages while performing the
   *        algorithm
   * @param time flag to request output of performance time
   */
  public void run(Database<V> database, boolean verbose, boolean time) {
    if(database == null || database.size() <= 0) {
      throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
    }

    long start = System.currentTimeMillis();
    FiniteProgress progress = new FiniteProgress("Performing local PCA", database.size());
    if(logger.isVerbose()) {
      logger.verbose("\nPreprocessing...");
    }

    int processed = 1;
    for(Iterator<Integer> it = database.iterator(); it.hasNext();) {
      Integer id = it.next();
      List<DistanceResultPair<DoubleDistance>> objects = objectsForPCA(id, database, verbose, false);

      PCAFilteredResult pcares = pca.processQueryResult(objects, database);

      database.associate(AssociationID.LOCAL_PCA, id, pcares);
      database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pcares.similarityMatrix());
      progress.setProcessed(processed++);

      if(logger.isVerbose()) {
        logger.progress(progress);
      }
    }

    long end = System.currentTimeMillis();
    if(time) {
      long elapsedTime = end - start;
      logger.verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }

  /**
   * Returns the objects to be considered within the PCA for the specified query
   * object.
   * 
   * @param id the id of the query object for which a PCA should be performed
   * @param database the database holding the objects
   * @param verbose flag to allow verbose messages while performing the
   *        algorithm
   * @param time flag to request output of performance time
   * @return the list of the objects (i.e. the ids and the distances to the
   *         query object) to be considered within the PCA
   */
  protected abstract List<DistanceResultPair<DoubleDistance>> objectsForPCA(Integer id, Database<V> database, boolean verbose, boolean time);
}