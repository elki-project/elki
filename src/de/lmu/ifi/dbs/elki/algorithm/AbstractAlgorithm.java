package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * <p>
 * This class serves also as a model of implementing an algorithm within this
 * framework. Any Algorithm that makes use of these flags may extend this class.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * @apiviz.excludeSubtypes
 * 
 * @param <O> the type of objects handled by this Algorithm
 * @param <R> Algorithm result type
 */
public abstract class AbstractAlgorithm<O, R extends Result> implements Algorithm<R> {
  /**
   * Constructor.
   */
  protected AbstractAlgorithm() {
    super();
  }

  @Override
  public abstract R run(Database data) throws IllegalStateException;

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  @Override
  public abstract TypeInformation getInputTypeRestriction();

  /**
   * Get a data query.
   * 
   * @param database Database to process
   * @return Data query
   */
  protected final Relation<O> getRelation(Database database) {
    return database.getRelation(getInputTypeRestriction());
  }

  /**
   * Get the (STATIC) logger for this class.
   * 
   * @return the static logger
   */
  abstract protected Logging getLogger();

  /**
   * Make a default distance function configuration option
   * 
   * @param <F> Distance function type
   * @param defaultDistanceFunction Default value
   * @param restriction Restriction class
   * @return Parameter object
   */
  public static <F extends DistanceFunction<?, ?>> ObjectParameter<F> makeParameterDistanceFunction(Class<?> defaultDistanceFunction, Class<?> restriction) {
    return new ObjectParameter<F>(AbstractDistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, restriction, defaultDistanceFunction);
  }
}