package de.lmu.ifi.dbs.elki.wrapper;

import java.io.File;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Wrapper to run another wrapper for all files in the directory given as input.
 *
 * @author Arthur Zimek
 */
public class DirectoryTask extends StandAloneInputWrapper {

    /**
     * OptionID for {@link #WRAPPER_PARAM}
     */
    public static final OptionID WRAPPER_ID = OptionID.getOrCreateOptionID(
        "directorytask.wrapper",
        "Wrapper to run over all files in the specified directory.."
    );

    /**
     * Parameter to specify the wrapper to run over all files in the specified directory,
     * must extend {@link Wrapper}.
     * <p>Key: {@code -directorytask.wrapper} </p>
     */
    protected final ClassParameter<Wrapper> WRAPPER_PARAM =
        new ClassParameter<Wrapper>(WRAPPER_ID, Wrapper.class);

    /**
     * Holds the instance of the wrapper specified by {@link #WRAPPER_PARAM}.
     */
    private Wrapper wrapper;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        new DirectoryTask().runCLIWrapper(args);
    }

    /**
     * Adds parameter
     * {@link #WRAPPER_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public DirectoryTask() {
        super();
        addOption(WRAPPER_PARAM);
    }

    /**
     * Runs the wrapper.
     */
    public void run() throws UnableToComplyException {
        File inputDir = getInput();
        if (!inputDir.isDirectory()) {
            throw new IllegalArgumentException(getInput() + " is not a directory");
        }
        File[] inputFiles = inputDir.listFiles();
        for (File inputFile : inputFiles) {
            try {
                List<String> wrapperParameters = getRemainingParameters();
                // input
                OptionUtil.addParameter(wrapperParameters, INPUT_ID, inputFile.getAbsolutePath());
                // output
                OptionUtil.addParameter(wrapperParameters, OUTPUT_ID, getOutput() + File.separator + inputFile.getName());

                wrapper.setParameters(wrapperParameters.toArray(new String[wrapperParameters.size()]));
                addParameterizable(wrapper);
                wrapper.run();
            }
            catch (ParameterException e) {
                throw new UnableToComplyException(e.getMessage(), e);
            }
        }
    }

    /**
     * Calls the super method
     * and instantiates {@link #wrapper} according to the value of parameter
     * {@link #WRAPPER_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // wrapper
        wrapper = WRAPPER_PARAM.instantiateClass();

        return remainingParameters;
    }


    /**
     * Returns the description for the input parameter.
     *
     * @return the description for the input parameter
     */
    @Override
    public String getInputDescription() {
        return "The name of the directory to run the wrapper on.";
    }

    /**
     * Returns the description for the output parameter.
     *
     * @return the description for the output parameter
     */
    @Override
    public String getOutputDescription() {
        return "The name of the output file.";
    }
}
