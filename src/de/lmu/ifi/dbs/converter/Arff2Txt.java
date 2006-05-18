package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.wrapper.StandAloneWrapper;
import de.lmu.ifi.dbs.wrapper.StandAloneInputWrapper;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts an arff file into a whitespace seperated txt file.
 */
public class Arff2Txt extends StandAloneInputWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());


  static {
    INPUT_D = "<filename>the arff-file to convert";
    OUTPUT_D = "<filename>the txt-file to write the converted arff-file in";
  }

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    Arff2Txt wrapper = new Arff2Txt();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (UnableToComplyException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.logger.info(e.getMessage());
    }
    catch (Exception e) {
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Runs the wrapper with the specified arguments.
   *
   * @param args parameter list
   */
  public void run(String[] args) throws UnableToComplyException, ParameterException, AbortException {
    super.run(args);
    try {
      File inputFile = new File(getInput());
      File outputFile = new File(getOutput());

      if (outputFile.exists()) {
        outputFile.delete();
        if (isVerbose()) {
          System.out.println("The file " + outputFile + " exists and was replaced.");
        }
      }

      outputFile.createNewFile();
      PrintStream outStream = new PrintStream(new FileOutputStream(outputFile));

      String line;
      boolean headerDone = false;
      BufferedReader br = new BufferedReader(new FileReader(inputFile));
      while (!((line = br.readLine()) == null)) {
        if (line.startsWith("%")) {
          outStream.println(AbstractParser.COMMENT + " " + line);
          continue;
        }

        int indexOfAt = line.indexOf(64);
        if (! headerDone && indexOfAt != -1) {
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
    }
    catch (IOException e) {
      UnableToComplyException ue = new UnableToComplyException("I/O Exception occured. " + e.getMessage());
      ue.fillInStackTrace();
      throw ue;
    }
  }
}
	
	