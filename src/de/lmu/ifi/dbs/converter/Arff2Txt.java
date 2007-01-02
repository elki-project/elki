package de.lmu.ifi.dbs.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.wrapper.StandAloneInputWrapper;

/**
 * Converts an arff file into a whitespace seperated txt file.
 */
public class Arff2Txt extends StandAloneInputWrapper {

	static {
		INPUT_D = "<filename>the arff-file to convert";
		OUTPUT_D = "<filename>the txt-file to write the converted arff-file in";
	}

	/**
	 * Main method to run this wrapper.
	 * 
	 * @param args
	 *            the arguments to run this wrapper
	 */
	public static void main(String[] args) {
		Arff2Txt wrapper = new Arff2Txt();
		try {
			wrapper.setParameters(args);
			wrapper.run();
		} catch (ParameterException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			wrapper.exception(wrapper.optionHandler.usage(e
					.getMessage()), cause);
		} catch (UnableToComplyException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			wrapper.exception(wrapper.optionHandler.usage(e
					.getMessage()), cause);
		} catch (AbortException e) {
			wrapper.verbose(e.getMessage());
		} catch (Exception e) {
			wrapper.exception(wrapper.optionHandler.usage(e
					.getMessage()), e);
		}
	}

	/**
	 * Runs the wrapper with the specified arguments.
	 */
	public void run() throws UnableToComplyException {
		try {
			File inputFile = getInput();
			File outputFile = getOutput();

			if (outputFile.exists()) {
				outputFile.delete();
				if (isVerbose()) {
					verbose("The file " + outputFile+ " exists and was replaced.");
				}
			}

			outputFile.createNewFile();
			PrintStream outStream = new PrintStream(new FileOutputStream(
					outputFile));

			String line;
			boolean headerDone = false;
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			while (!((line = br.readLine()) == null)) {
				if (line.startsWith("%")) {
					outStream.println(AbstractParser.COMMENT + " " + line);
					continue;
				}

				int indexOfAt = line.indexOf(64);
				if (!headerDone && indexOfAt != -1) {
					if (line.substring(indexOfAt, 5).equals("@data")) {
						headerDone = true;
						continue;
					}
				}

				if (headerDone) {
					line = line.replace(',', ' ');
					outStream.println(line);
				}

			}

			outStream.close();
			br.close();
		} catch (IOException e) {
			UnableToComplyException ue = new UnableToComplyException(
					"I/O Exception occured. " + e.getMessage());
			ue.fillInStackTrace();
			throw ue;
		}
	}
}
