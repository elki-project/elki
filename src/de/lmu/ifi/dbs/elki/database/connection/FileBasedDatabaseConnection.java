package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Provides a file based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek
 */
public class FileBasedDatabaseConnection<O extends DatabaseObject> extends InputStreamDatabaseConnection<O> {

	/**
	 * Label for parameter input.
	 */
	public final static String INPUT_P = "in";

	/**
	 * Description for parameter input.
	 */
	public final static String INPUT_D = "Input file to be parsed.";

	/**
	 * Provides a file based database connection based on the parser to be set.
	 */
	public FileBasedDatabaseConnection() {
		super();
		optionHandler.put(new FileParameter(INPUT_P, INPUT_D, FileParameter.FileType.INPUT_FILE));
	}

	/**
	 * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingOptions = super.setParameters(args);

		File file = (File) optionHandler.getOptionValue(INPUT_P);
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new WrongParameterValueException(INPUT_P, file.getPath(), INPUT_D, e);
		}

		return remainingOptions;
	}
}