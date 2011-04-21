package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * <p>
 * Specifies the requirements for any algorithm that is to be executable by the
 * main class.
 * </p>
 * <p/>
 * <p>
 * Any implementation needs not to take care of input nor output, parsing and so
 * on. Those tasks are performed by the framework. An algorithm simply needs to
 * ask for parameters that are algorithm specific.
 * </p>
 * <p/>
 * <p>
 * <b>Note:</b> Any implementation is supposed to provide a constructor without
 * parameters (default constructor).
 * </p>
 * 
 * @author Arthur Zimek
 */
public interface Algorithm extends Parameterizable {
  /**
   * Runs the algorithm.
   * 
   * @param database the database to run the algorithm on
   * @return the Result computed by this algorithm
   * @throws IllegalStateException if the algorithm has not been initialized
   *         properly (e.g. the setParameters(String[]) method has been failed
   *         to be called).
   */
  Result run(Database database) throws IllegalStateException;

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  // TODO: this is only appropriate for single-input algorithms!
  public TypeInformation[] getInputTypeRestriction();
}