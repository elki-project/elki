package de.lmu.ifi.dbs.elki.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.application.StandAloneInputApplication;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.parser.AbstractParser;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Converts an arff file into a whitespace separated txt file.
 *
 * @author Elke Achtert
 */
public class Arff2Txt extends StandAloneInputApplication {
    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        Arff2Txt wrapper = new Arff2Txt();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
        catch (ParameterException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LoggingUtil.exception(OptionUtil.describeParameterizable(wrapper), cause);
        }
        catch (UnableToComplyException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LoggingUtil.exception(OptionUtil.describeParameterizable(wrapper), cause);
        }
        catch (AbortException e) {
          LoggingUtil.message(e.getMessage());
        }
        catch (Exception e) {
          LoggingUtil.exception(OptionUtil.describeParameterizable(wrapper), e);
        }
    }

    /**
     * Runs the wrapper with the specified arguments.
     */
    @Override
    public void run() throws UnableToComplyException {
        try {
            File inputFile = getInput();
            File outputFile = getOutput();

            if (outputFile.exists()) {
                outputFile.delete();
                if (logger.isVerbose()) {
                  logger.verbose("The file " + outputFile + " existed and has been replaced.");
                }
            }

            outputFile.createNewFile();
            InputStream in = new FileInputStream(inputFile);
            OutputStream out = new FileOutputStream(outputFile);
            translate(in, out);

            in.close();
            out.close();

        }
        catch (IOException e) {
            UnableToComplyException ue = new UnableToComplyException(
                "I/O Exception occurred. " + e.getMessage());
            ue.fillInStackTrace();
            throw ue;
        }
    }

    /**
     * Translates the arff-formatted data in InputStream {@code in}
     * to whitespace separated data into OutputStream {@code out}.
     * <p/>
     * {@code out} is flushed, but neither {@code out} nor {@code in} are closed.
     *
     * @param in  the arff formatted data source
     * @param out the whitespace separated data target
     * @throws IOException if an error occurs during reading the InputStream
     */
    public static void translate(InputStream in, OutputStream out) throws IOException {
        PrintStream outStream = new PrintStream(out);

        String line;
        boolean headerDone = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
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
        outStream.flush();
    }

    /**
     * Returns the description for the output parameter. Subclasses may
     * need to overwrite this method.
     *
     * @return the description for the output parameter
     */
    @Override
    public String getOutputDescription() {
        return "the txt-file to write the converted arff-file in.";
    }


    /**
     * Returns the description for the input parameter.
     *
     * @return the description for the input parameter
     */
    @Override
    public String getInputDescription() {
        return "The name of the arff-file to convert.";
    }
}
