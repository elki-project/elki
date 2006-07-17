package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Wrapper extends Parameterizable {
  /**
   * Runs the wrapper.
   */
  public void run() throws UnableToComplyException;
}
