package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

import java.util.List;

/**
 * Specifies the requirements for any algorithm that is to be executable by the main class.
 * <p/>
 * Any implementation needs not to take care of input nor output, parsing and so on.
 * Those tasks are performed by the framework. An algorithm simply needs to ask for
 * parameters that are algorithm specific.
 * <p/>
 * Note that any implementation is supposed to provide a constructor without parameters.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Algorithm<T extends MetricalObject> extends Parameterizable {
  /**
   * Runs the algorithm.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly
   *                               (e.g. the setParameters(String[]) method has been failed to be called).
   */
  void run(Database<T> database) throws IllegalStateException;

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  Result<T> getResult();

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  Description getDescription();

  /**
   * Returns the setting of the attributes of the algorithm.
   *
   * @return the setting of the attributes of the algorithm
   */
  public List<AttributeSettings> getAttributeSettings();
}
