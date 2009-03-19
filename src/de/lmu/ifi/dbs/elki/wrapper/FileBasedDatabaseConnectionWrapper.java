package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;

import java.util.List;

/**
 * FileBasedDatabaseConnectionWrapper is an abstract super class for all wrapper
 * classes running algorithms in a kdd task using a file based database connection.
 *
 * @author Elke Achtert
 */
public abstract class FileBasedDatabaseConnectionWrapper<O extends DatabaseObject> extends KDDTaskWrapper<O> {
    /**
     * Parameter that specifies the name of the input file to be parsed.
     * <p>Key: {@code -dbc.in} </p>
     */
    private final FileParameter INPUT_PARAM = new FileParameter(
        FileBasedDatabaseConnection.INPUT_ID,
        FileParameter.FileType.INPUT_FILE);

    /**
     * Adds parameter
     * {@link #INPUT_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public FileBasedDatabaseConnectionWrapper() {
        super();
        addOption(INPUT_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();
        // input
        OptionUtil.addParameter(parameters, INPUT_PARAM, INPUT_PARAM.getValue().getPath());
        return parameters;
    }
}


