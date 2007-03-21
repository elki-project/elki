package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.ICAResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.varianceanalysis.ica.FastICA;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ICA<V extends RealVector<V,?>> extends AbstractAlgorithm<V> {
  /**
   * The result.
   */
  private ICAResult<V> result;

  /**
   * The independent component analysis.
   */
  private FastICA<V> ica;

  /**
   * todo
   */
  public ICA() {
    super();
    this.debug = true;
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  protected void runInTime(Database<V> database) throws IllegalStateException {
    ica.run(database, isVerbose());
    result = new ICAResult<V>(database, ica);
    if (debug) {
      debugFine(result.toString());
    }
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<V> getResult() {
    return result;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   *         todo
   */
  public Description getDescription() {
    return new Description("todo", "todo", "todo", "todo");
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // ica
    ica = new FastICA<V>();
    remainingParameters = ica.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    return remainingParameters;
  }
}
