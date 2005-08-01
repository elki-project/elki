package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * Specifies the requirements for any algorithm that is to be executable by the main class.
 * 
 * Note that any implementation is supposed to provide a constructor without parameters.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Algorithm extends Parameterizable
{
    /**
     * Runs the algorithm.
     * 
     *
     */
    void run();
    
    /**
     * Returns the result of the algorithm.
     * 
     * 
     * @return the result of the algorithm
     */
    Result getResult();
    
    /**
     * Returns a description of the algorithm.
     * 
     * 
     * @return a description of the algorithm
     */
    Description getDescription();
}
