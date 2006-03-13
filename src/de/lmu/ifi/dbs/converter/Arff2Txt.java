package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.wrapper.AbstractWrapper;

import java.io.*;

/**
 * Converts an arff file into a whitespace seperated txt file.
 */
public class Arff2Txt extends AbstractWrapper {

  public static void main(String[] args) {
    Arff2Txt wrapper = new Arff2Txt();
    try {
      wrapper.run(args);
    }
    catch (WrongParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (UnableToComplyException e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Runs the wrapper with the specified arguments.
   *
   * @param args parameter list
   */
  public void run(String[] args) throws UnableToComplyException {
    this.setParameters(args);

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
        if (! headerDone &&  indexOfAt != -1) {
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
	
	