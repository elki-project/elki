package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.io.File;
import java.util.List;

/**
 * FileBasedDatabaseConnectionWrapper is an abstract super class for all wrapper
 * classes running algorithms in a kdd task using a file based database connection.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public abstract class FileBasedDatabaseConnectionWrapper extends KDDTaskWrapper {

    /**
     * The input file.
     */
    private File input;

    /**
     * Sets the parameter database connection in the parameter map additionally to the
     * parameters provided by super-classes.
     */
    public FileBasedDatabaseConnectionWrapper() {
        super();
        optionHandler.put(new FileParameter(FileBasedDatabaseConnection.INPUT_P,
            FileBasedDatabaseConnection.INPUT_D, FileParameter.FileType.INPUT_FILE));
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> result = super.getKDDTaskParameters();
        // input
        result.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
        result.add(input.getPath());
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // input
        input = (File) optionHandler.getOptionValue(FileBasedDatabaseConnection.INPUT_P);

        return remainingParameters;
    }
}


