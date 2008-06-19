package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.io.File;
import java.util.List;

/**
 * KDDTaskWrapper is an abstract super class for all wrapper classes running
 * algorithms in a kdd task.
 *
 * @author Elke Achtert
 */
public abstract class KDDTaskWrapper<O extends DatabaseObject> extends AbstractWrapper {

    /**
     * Flag to request output of performance time.
     */
    private final Flag TIME_FLAG = new Flag(OptionID.ALGORITHM_TIME);

    /**
     * Optional Parameter to specify the file to write the obtained results in.
     * If this parameter is omitted, per default the output will sequentially be given to STDOUT.
     * <p>Key: {@code -out} </p>
     */
    private final FileParameter OUTPUT_PARAM = new FileParameter(OptionID.OUTPUT,
        FileParameter.FileType.OUTPUT_FILE, true);

    /**
     * The output file.
     */
    private File output;

    /**
     * The result of the kdd task.
     */
    private Result<O> result;

    /**
     * The value of the time flag.
     */
    private boolean time;

    /**
     * Sets additionally to the parameters set by the super class the time flag
     * and the parameter out in the parameter map. Any extending class should
     * call this constructor, then add further parameters.
     */
    protected KDDTaskWrapper() {
        super();

        // outpout
        optionHandler.put(OUTPUT_PARAM);

        // time
        optionHandler.put(TIME_FLAG);
    }

    /**
     * @see Wrapper#run()
     */
    public final void run() throws UnableToComplyException {
        try {
            List<String> parameters = getKDDTaskParameters();
            debugFiner("got KDD Task parametes");
            KDDTask<O> task = new KDDTask<O>();
            debugFiner("KDD task has been instanstiated");
            task.setParameters(parameters.toArray(new String[parameters.size()]));
            debugFiner("set KDD Task parameters, will run kdd Task");
            result = task.run();
        }
        catch (ParameterException e) {
            throw new UnableToComplyException(e);
        }
    }

    /**
     * Returns the result of the kdd task.
     *
     * @return the result of the kdd task
     */
    public final Result<O> getResult() {
        return result;
    }

    /**
     * Returns the output file.
     *
     * @return the output file
     */
    public final File getOutput() {
        return output;
    }

    /**
     * Returns the value of the time flag.
     *
     * @return the value of the time flag.
     */
    public final boolean isTime() {
        return time;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // output
        if (optionHandler.isSet(OUTPUT_PARAM)) {
            output = getParameterValue(OUTPUT_PARAM);
        }

        // time
        time = optionHandler.isSet(TIME_FLAG);

        return remainingParameters;
    }

    /**
     * Returns the parameters that are necessary to run the kdd task correctly.
     *
     * @return the array containing the parametr setting that is necessary to
     *         run the kdd task correctly
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = getRemainingParameters();

        // verbose
        if (isVerbose()) {
            Util.addFlag(parameters, OptionID.ALGORITHM_VERBOSE);
        }

        // time
        if (isTime()) {
            Util.addFlag(parameters, OptionID.ALGORITHM_TIME);
        }

        // output
        if (getOutput() != null) {
            Util.addParameter(parameters, OUTPUT_PARAM, getOutput().getPath());
        }

        return parameters;
    }
}
