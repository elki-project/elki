package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.io.File;

/**
 * StandAloneWrapper sets additionally to the flags set by AbstractWrapper
 * the output parameter out. <p/>
 * Any Wrapper class that makes use of these flags may extend this class. Beware to
 * make correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Elke Achtert
 * todo parameter
 */
public abstract class StandAloneWrapper extends AbstractWrapper {
    /**
     * Label for parameter output.
     */
    public static final String OUTPUT_P = "out";

    /**
     * The parameter output.
     */
    private FileParameter outputParameter;

    /**
     * The output file.
     */
    private File output;

    /**
     * Sets additionally to the parameters set by the super class the
     * parameter for out in the parameter map. Any extending
     * class should call this constructor, then add further parameters.
     */
    protected StandAloneWrapper() {
        super();
        outputParameter = new FileParameter(OUTPUT_P, getOutputDescription(), FileParameter.FILE_OUT);
        optionHandler.put(outputParameter);
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // output
        if (optionHandler.isSet(outputParameter)) {
            output = getParameterValue(outputParameter);
        }

        return remainingParameters;
    }

    /**
     * Returns the output string.
     *
     * @return the output string
     */
    public final File getOutput() {
        return output;
    }

    /**
     * Returns the description for the output parameter. Subclasses may
     * need to overwrite this method.
     *
     * @return the description for the output parameter
     */
    public String getOutputDescription() {
        return "the name of the output file.";
    }
}
