package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * @author Arthur Zimek
 */
public interface Wrapper extends Parameterizable {
  /**
   * Runs the wrapper.
   */
  public void run() throws UnableToComplyException;
}
