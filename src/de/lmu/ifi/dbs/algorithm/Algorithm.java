package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * <p>Specifies the requirements for any algorithm that is to be executable by the
 * main class.</p>
 * <p/>
 * <p>Any implementation needs not to take care of input nor
 * output, parsing and so on. Those tasks are performed by the framework. An
 * algorithm simply needs to ask for parameters that are algorithm specific.
 * </p>
 * <p/>
 * <p><b>Note:</b> Any implementation is supposed to provide a constructor
 * without parameters (default constructor).</p>
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @see AbstractAlgorithm
 */
public interface Algorithm<O extends DatabaseObject> extends Parameterizable {
    /**
     * Runs the algorithm.
     *
     * @param database the database to run the algorithm on
     * @throws IllegalStateException if the algorithm has not been initialized
     *                               properly (e.g. the setParameters(String[]) method has been failed
     *                               to be called).
     */
    void run(Database<O> database) throws IllegalStateException;

    /**
     * Returns the result of the algorithm.
     *
     * @return the result of the algorithm
     */
    Result<O> getResult();

    /**
     * Returns a description of the algorithm.
     *
     * @return a description of the algorithm
     */
    Description getDescription();

    /**
     * Sets whether verbose messages should be printed while executing the
     * algorithm.
     *
     * @param verbose the flag to allow verbose messages while performing the algorithm
     */
    public void setVerbose(boolean verbose);

    /**
     * Sets whether whether the time should be assessed while executing the
     * algorithm.
     *
     * @param time the flag to request output of performance time
     */
    public void setTime(boolean time);
}
