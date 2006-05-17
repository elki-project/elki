package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.LOFResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.logging.Logger;

/**
 * Online algorithm to efficiently update density-based local
 * outlier factors in a database after insertion or deletion of new objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class OnlineLOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance> {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = LOF.MINPTS_P;

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = LOF.MINPTS_D;

  /**
   * Minimum points.
   */
  private int minpts;

  /**
   * Provides the result of the algorithm.
   */
  private LOFResult<O> result;

  /**
   * Provides a new online algorithm to efficiently update density-based local outlier factors in a
   * database after insertion of new objects.
   */
  public OnlineLOF() {
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<O> getResult() {
    return result;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("OnlineLOF", "Online Local Outlier Factor",
                           "Algorithm to efficiently update density-based local outlier factors in a database " +
                           "after insertion or deletion of new objects. ",
                           "unpublished.");
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // minpts
    String minptsString = optionHandler.getOptionValue(MINPTS_P);
    try {
      minpts = Integer.parseInt(minptsString);
      if (minpts <= 0) {
        throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D, e);
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

}
