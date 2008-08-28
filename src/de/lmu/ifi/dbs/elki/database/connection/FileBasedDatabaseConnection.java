package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Provides a file based database connection based on the parser to be set.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to be provided by the implementing class as element of the supplied database
 */
public class FileBasedDatabaseConnection<O extends DatabaseObject> extends InputStreamDatabaseConnection<O> {
    /**
     * OptionID for {@link #INPUT_PARAM}
     */
    public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID(
        "dbc.in",
        "The name of the input file to be parsed."
    );

    /**
     * Parameter that specifies the name of the input file to be parsed.
     * <p>Key: {@code -dbc.in} </p>
     */
    private final FileParameter INPUT_PARAM =
        new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);

    /**
     * Provides a file based database connection based on the parser to be set,
     * adding parameter
     * {@link #INPUT_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public FileBasedDatabaseConnection() {
        super();
        addOption(INPUT_PARAM);
    }

    /**
     * Calls the super method
     * InputStreamDatabaseConnection#setParameters(args)}
     * and sets additionally the value of the parameter
     * {@link #INPUT_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        try {
            in = new FileInputStream(INPUT_PARAM.getValue());
        }
        catch (FileNotFoundException e) {
            throw new WrongParameterValueException(INPUT_PARAM, INPUT_PARAM.getValue().getPath(), e);
        }

        return remainingParameters;
    }
}