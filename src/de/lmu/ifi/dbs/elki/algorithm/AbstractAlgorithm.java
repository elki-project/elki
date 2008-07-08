package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.IndexDatabase;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * <p>AbstractAlgorithm sets the values for flags verbose and time.</p>
 * <p/>
 * <p>This class serves also as a model of implementing an algorithm within this framework.
 * Any Algorithm that makes use of these flags may extend this class. Beware to make
 * correct use of parameter settings via optionHandler as commented with
 * constructor and methods.</p>
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public abstract class AbstractAlgorithm<O extends DatabaseObject> extends AbstractParameterizable implements Algorithm<O> {

    /**
     * Flag to allow verbose messages while performing the algorithm.
     * <p>Key: {@code -verbose} </p>
     */
    private final Flag VERBOSE_FLAG = new Flag(OptionID.ALGORITHM_VERBOSE);

    /**
     * Flag to request output of performance time.
     * <p>Key: {@code -time} </p>
     */
    private final Flag TIME_FLAG = new Flag(OptionID.ALGORITHM_TIME);

    /**
     * Holds the value of AbstractAlgorithm#VERBOSE_FLAG.
     */
    private boolean verbose;

    /**
     * Holds the value of AbstractAlgorithm#TIME_FLAG.
     */
    private boolean time;

    /**
     * Adds the flags #VERBOSE_FLAG and #TIME_FLAG to the option handler. Any extending
     * class should call this constructor, then add further parameters.
     * Subclasses can add further parameters using
     * {@link #addOption(de.lmu.ifi.dbs.elki.utilities.optionhandling.Option)}
     */
    protected AbstractAlgorithm() {
        super();

        this.addOption(VERBOSE_FLAG);
        this.addOption(TIME_FLAG);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#description()
     */
    @Override
    public String description() {
        return description("", false);
    }

    /**
     * Grabs the options from the option handler. Any extending class should
     * call this method first and return the returned array without further
     * changes, but after setting further required parameters. An example for
     * overwritting this method taking advantage from the previously (in
     * superclasses) defined options would be:
     * <p/>
     * <pre>
     * {
     *   String[] remainingParameters = super.setParameters(args);
     *   // set parameters for your class
     *   // for example like this:
     *   if(isSet(MY_PARAM_VALUE_PARAM))
     *   {
     *      myParamValue = getParameterValue(MY_PARAM_VALUE_PARAM);
     *   }
     *   .
     *   .
     *   .
     *   return remainingParameters;
     *   // or in case of attributes requesting parameters themselves
     *   // return parameterizableAttribbute.setParameters(remainingParameters);
     * }
     * </pre>
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = optionHandler.grabOptions(args);
        verbose = isSet(VERBOSE_FLAG);
        time = isSet(TIME_FLAG);
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Returns whether the performance time of the algorithm should be assessed.
     *
     * @return true, if output of performance time is requested, false otherwise
     */
    public boolean isTime() {
        return time;
    }

    /**
     * Returns whether verbose messages should be printed while executing the
     * algorithm.
     *
     * @return true, if verbose messages should be printed while executing the
     *         algorithm, false otherwise
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#setVerbose(boolean)
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        VERBOSE_FLAG.setValue(verbose);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#setTime(boolean)
     */
    public void setTime(boolean time) {
        this.time = time;
        TIME_FLAG.setValue(time);
    }

    /**
     * Calls the runInTime()-method of extending classes. Measures and prints
     * the runtime and, in case of an index based database,
     * the I/O costs of this method.
     *
     * @see Algorithm#run(de.lmu.ifi.dbs.elki.database.Database)
     */
    public final void run(Database<O> database) throws IllegalStateException {
        long start = System.currentTimeMillis();
        runInTime(database);
        long end = System.currentTimeMillis();
        if (isTime()) {
            long elapsedTime = end - start;
            verbose(this.getClass().getName() + " runtime  : " + elapsedTime + " milliseconds.");

        }
        if (database instanceof IndexDatabase && isVerbose()) {
            IndexDatabase<?> db = (IndexDatabase<?>) database;
            StringBuffer msg = new StringBuffer();
            msg.append(getClass().getName()).append(" physical read access : ").append(db.getPhysicalReadAccess()).append("\n");
            msg.append(getClass().getName()).append(" physical write access : ").append(db.getPhysicalWriteReadAccess()).append("\n");
            msg.append(getClass().getName()).append(" logical page access : ").append(db.getLogicalPageAccess()).append("\n");
            verbose(msg.toString());
        }
    }

    /**
     * The run method encapsulated in measure of runtime. An extending class
     * needs not to take care of runtime itself.
     *
     * @param database the database to run the algorithm on
     * @throws IllegalStateException if the algorithm has not been initialized
     *                               properly (e.g. the setParameters(String[]) method has been failed
     *                               to be called).
     */
    protected abstract void runInTime(Database<O> database) throws IllegalStateException;

}
