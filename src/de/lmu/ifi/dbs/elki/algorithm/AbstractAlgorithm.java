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
 * @param <R> the type of result to retrieve from this Algorithm
 */
public abstract class AbstractAlgorithm<O, R extends Result> implements Algorithm<R> {
  /**
   * Constructor.
   */
  protected AbstractAlgorithm() {
    super();
  }

  /**
   * Calls the runInTime()-method of extending classes. Measures and prints the
   * runtime and, in case of an index based database, the I/O costs of this
   * method.
   * 
   * @param database the database to run the algorithm on
   * @return the Result computed by this algorithm
   */
  @Override
  public final R run(Database database) throws IllegalStateException {
    long start = System.currentTimeMillis();
    R res = runInTime(database);
    long end = System.currentTimeMillis();
    if(getLogger().isVerbose()) {
      long elapsedTime = end - start;
      getLogger().verbose(this.getClass().getName() + " runtime  : " + elapsedTime + " milliseconds.");

    }
    return res;
  }

  /**
   * Get a data query.
   * 
   * @param database Database to process
   * @return Data query
   */
  protected Relation<O> getRelation(Database database) {
    TypeInformation restriction = getInputTypeRestriction();
    return database.getRelation(restriction);
  }

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  public abstract TypeInformation getInputTypeRestriction();

  /**
   * The run method encapsulated in measure of runtime. An extending class needs
   * not to take care of runtime itself.
   * 
   * @param data the data to run the query on
   * @return the Result computed by this algorithm
   * @throws IllegalStateException if the algorithm has not been initialized
   *         properly (e.g. the setParameters(String[]) method has been failed
   *         to be called).
   */
  protected abstract R runInTime(Database data) throws IllegalStateException;

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