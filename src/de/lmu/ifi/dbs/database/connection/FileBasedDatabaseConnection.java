package de.lmu.ifi.dbs.database.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Provides a file based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FileBasedDatabaseConnection<O extends DatabaseObject> extends InputStreamDatabaseConnection<O> {

	/**
	 * Label for parameter input.
	 */
	public final static String INPUT_P = "in";

	/**
	 * Description for parameter input.
	 */
	public final static String INPUT_D = "input file to be parsed.";

	/**
	 * Provides a file based database connection based on the parser to be set.
	 */
	public FileBasedDatabaseConnection() {
		super();
		optionHandler.put(INPUT_P, new FileParameter(INPUT_P, INPUT_D, FileParameter.FILE_IN));
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingOptions = super.setParameters(args);

		File file = (File) optionHandler.getOptionValue(INPUT_P);
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new WrongParameterValueException(INPUT_P, file.getPath(), INPUT_D, e);
		}
		setParameters(args, remainingOptions);
		return remainingOptions;
	}
}