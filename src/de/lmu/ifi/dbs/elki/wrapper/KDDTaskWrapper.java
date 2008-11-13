package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;

import java.io.File;
import java.util.List;
import java.util.Arrays;

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
    private final FileParameter OUTPUT_PARAM = new FileParameter(
        OptionID.OUTPUT,
        FileParameter.FileType.OUTPUT_FILE, true);

    /**
     * The output file.
     */
    private File output;

    /**
     * The result of the kdd task.
     */
    private Result result;

    /**
     * The value of the time flag.
     */
    private boolean time;

    /**
     * Adds parameter
     * {@link #OUTPUT_PARAM} and flag {@link #TIME_FLAG}
     * to the option handler additionally to parameters of super class.
     */
    protected KDDTaskWrapper() {
        super();

        // output
        addOption(OUTPUT_PARAM);

        // time
        addOption(TIME_FLAG);
    }

    public final void run() throws UnableToComplyException {
        try {
            List<String> parameters = getKDDTaskParameters();
            debugFiner("got KDD Task parametes");
            KDDTask<O> task = new KDDTask<O>();
            debugFiner("KDD task has been instanstiated");
            String[] remainingParameters = task.setParameters(parameters.toArray(new String[parameters.size()]));
            if (remainingParameters.length != 0) {
                task.warning(task.usage("Unnecessary parameters specified: "+
                    Arrays.asList(remainingParameters) + "\n\nUSAGE:\n"));
                return;
            }
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
    public final Result getResult() {
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

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // output
        if (OUTPUT_PARAM.isSet()) {
            output = OUTPUT_PARAM.getValue();
        }

        // time
        time = optionHandler.isSet(TIME_FLAG);

        return remainingParameters;
    }

    /**
     * Returns the parameters that are necessary to run the kdd task correctly.
     *
     * @return the array containing the parameter setting that is necessary to
     *         run the kdd task correctly
     */
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
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
