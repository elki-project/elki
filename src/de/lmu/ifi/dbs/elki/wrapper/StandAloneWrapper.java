package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import experimentalcode.erich.GoldenGeneratorXML;

import java.io.File;

/**
 * StandAloneWrapper sets additionally to the flags set by AbstractWrapper
 * the output parameter out. <p/>
 * Any Wrapper class that makes use of these flags may extend this class. Beware to
 * make correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public abstract class StandAloneWrapper extends AbstractWrapper {

    /**
     * OptionID for {@link #OUTPUT_PARAM}
     */
    public static final OptionID OUTPUT_ID = OptionID.getOrCreateOptionID(
        "wrapper.out",
        ""
    );

    /**
     * Parameter that specifies the name of the output file.
     * <p>Key: {@code -wrapper.out} </p>
     */
    private final FileParameter OUTPUT_PARAM =
        new FileParameter(OUTPUT_ID, FileParameter.FileType.OUTPUT_FILE);

    /**
     * Holds the value of {@link #OUTPUT_PARAM}.
     */
    private File output;

    /**
     * Adds parameter
     * {@link #OUTPUT_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    protected StandAloneWrapper() {
        super();
        OUTPUT_PARAM.setShortDescription(getOutputDescription());
        addOption(OUTPUT_PARAM);
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameter
     * {@link #OUTPUT_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // output
        output = getParameterValue(OUTPUT_PARAM);

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
     * Returns the description for the output parameter.
     *
     * @return the description for the output parameter
     */
    public abstract String getOutputDescription();
    
}
