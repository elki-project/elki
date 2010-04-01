package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Materialize kNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors of objects of a database.")
public class MaterializeKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractLoggable implements Preprocessor<O, List<DistanceResultPair<D>>>, Parameterizable {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * materialized. must be an integer greater than 1.
   * <p>
   * Key: {@code -materialize.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  protected int k;

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

  /**
   * Parameter to indicate the distance function to be used to ascertain the
   * nearest neighbors.
   * <p/>
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code materialize.distance}
   * </p>
   */
  public final ObjectParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * Hold the distance function to be used.
   */
  protected DistanceFunction<O, D> distanceFunction;

  /**
   * Materialized neighborhood
   */
  protected HashMap<Integer, List<DistanceResultPair<D>>> materialized;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MaterializeKNNPreprocessor(Parameterization config) {
    super();
    // number of neighbors
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }

    // distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  /**
   * Annotates the nearest neighbors based on the values of {@link #k} and
   * {@link #distanceFunction} to each database object.
   */
  public void run(Database<O> database) {
    distanceFunction.setDatabase(database);
    materialized = new HashMap<Integer, List<DistanceResultPair<D>>>(database.size());
    if(logger.isVerbose()) {
      logger.verbose("Assigning nearest neighbor lists to database objects");
    }
    FiniteProgress preprocessing = new FiniteProgress("Materializing k nearest neighbors (k=" + k + ")", database.size());
    int count = 0;
    for(Integer id : database) {
      List<DistanceResultPair<D>> kNN = database.kNNQueryForID(id, k, distanceFunction);
      materialized.put(id, kNN);
      if(logger.isVerbose()) {
        count++;
        preprocessing.setProcessed(count);
        logger.progress(preprocessing);
      }
    }
    if(logger.isVerbose()) {
      logger.verbose("kNN materialization completed.");
    }
  }

  /**
   * Materialize a neighborhood.
   * 
   * @return the materialized neighborhoods
   */
  public HashMap<Integer, List<DistanceResultPair<D>>> getMaterialized() {
    return materialized;
  }

  /** {@inheritDoc} */
  @Override
  public List<DistanceResultPair<D>> get(Integer id) {
    return materialized.get(id);
  }
}