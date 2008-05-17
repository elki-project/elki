package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
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
     * The result of the kdd task.
     */
    private Result<O> result;

    /**
     * The parameter output.
     */
    private FileParameter outputParameter;

    /**
     * The output file.
     */
    private File output;

    /**
     * The time flag.
     */
    private Flag timeFlag;

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

        // file
        outputParameter = new FileParameter(KDDTask.OUTPUT_P, KDDTask.OUTPUT_D, FileParameter.FILE_OUT);
        outputParameter.setOptional(true);
        optionHandler.put(outputParameter);

        // time
        timeFlag = new Flag(AbstractAlgorithm.TIME_FLAG.getName(), AbstractAlgorithm.TIME_FLAG.getDescription());
        optionHandler.put(timeFlag);
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
        if (optionHandler.isSet(outputParameter)) {
            output = (File) optionHandler.getParameterValue(outputParameter);
        }

        // time
        time = optionHandler.isSet(timeFlag);

        return remainingParameters;
    }

    /**
     * Returns the parameters that are necessary to run the kdd task correctly.
     *
     * @return the array containing the parametr setting that is necessary to
     *         run the kdd task correctly
     */
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = getRemainingParameters();

        // verbose
        if (isVerbose()) {
            Util.addFlag(parameters, AbstractAlgorithm.VERBOSE_FLAG);
        }

        // time
        if (isTime()) {
            Util.addFlag(parameters, AbstractAlgorithm.TIME_FLAG);
        }

        // output
        if (getOutput() != null) {
            parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
            parameters.add(getOutput().getPath());
        }

        return parameters;
    }

    /**
     * Puts the value of the specified parameter to the specified parameter list.
     *
     * @param parameters the list of parameters
     * @param parameter  the parameter to be added
     * @throws UnusedParameterException
     */
    public void put(List<String> parameters, Parameter parameter) throws UnusedParameterException {
        parameters.add(OptionHandler.OPTION_PREFIX + parameter.getName());
        parameters.add(parameter.getValue().toString());
    }

    /**
     * Puts the specified flag to the specified parameter list.
     *
     * @param parameters the list of parameters
     * @param flag       the parameter to be added
     * @throws UnusedParameterException
     */
    public void put(List<String> parameters, Flag flag) throws UnusedParameterException {
        parameters.add(OptionHandler.OPTION_PREFIX + flag.getName());
    }
}
