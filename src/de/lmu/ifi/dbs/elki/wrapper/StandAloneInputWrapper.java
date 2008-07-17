package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.io.File;

/**
 * StandAloneInputWrapper extends StandAloneWrapper and
 * sets additionally the parameter in. Any
 * Wrapper class that makes use of these flags may extend this class.
 *
 * @author Elke Achtert
 */
public abstract class StandAloneInputWrapper extends StandAloneWrapper {

    /**
     * OptionID for {@link #INPUT_PARAM}
     */
    public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID(
        "wrapper.in",
        ""
    );

    /**
     * Parameter that specifies the name of the input file.
     * <p>Key: {@code -wrapper.in} </p>
     */
    private final FileParameter INPUT_PARAM =
        new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);

    /**
     * Holds the value of {@link #INPUT_PARAM}.
     */
    private File input;

    /**
     * Adds parameter
     * {@link #INPUT_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    protected StandAloneInputWrapper() {
        super();
        INPUT_PARAM.setShortDescription(getInputDescription());
        addOption(INPUT_PARAM);
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.wrapper.StandAloneWrapper#setParameters(String[]) StandAloneWrapper#setParameters(args)}
     * and sets additionally the value of the parameter
     * {@link #INPUT_PARAM}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // input
        input = getParameterValue(INPUT_PARAM);
        return remainingParameters;
    }

    /**
     * Returns the input file.
     *
     * @return the input file
     */
    public final File getInput() {
        return input;
    }

    /**
     * Returns the description for the input parameter.
     *
     * @return the description for the input parameter
     */
    public abstract String getInputDescription();
}
