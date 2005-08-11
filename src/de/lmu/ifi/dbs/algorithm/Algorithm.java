package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * Specifies the requirements for any algorithm that is to be executable by the main class.
 * 
 * Any implementation needs not to take care of input nor output, parsing and so on.
 * Those tasks are performed by the framework. An algorithm simply needs to ask for
 * parameters that are algorithm specific.
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
     * @param database the database to run the algorithm on
     * @throws IllegalStateException if the algorithm has not been initialized properly
     * (e.g. the setParameters(String[]) method has been failed to be called).
     */
    void run(Database database) throws IllegalStateException;
    
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
