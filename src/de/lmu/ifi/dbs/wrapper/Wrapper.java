package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * Interface for all wrapper classes.
 *
 * @author Arthur Zimek
 */
public interface Wrapper extends Parameterizable {
    /**
     * Runs the wrapper.
     * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException if an error occurs during runninmg the wrapper
     */
    public void run() throws UnableToComplyException;
}
