package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.wrapper.AbstractWrapper;

import java.io.*;

/**
 * Converts a txt file to an arff file. All attributes that can be parsed as doubles
 * will be declared as numeric attributes, the others will be declared as string attributes.
 */
public class Txt2Arff extends AbstractWrapper {

  public static void main(String[] args) {
    Txt2Arff wrapper = new Txt2Arff();
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
      File inputFile = new File(input);
      File outputFile = new File(output);

      if (outputFile.exists()) {
        outputFile.delete();
        if (verbose) {
          System.out.println("The file " + output + " exists and was be replaced.");
        }
      }

      outputFile.createNewFile();
      PrintStream outStream = new PrintStream(new FileOutputStream(outputFile));

      outStream.println("@relation " + inputFile.getName());
      outStream.println();

      String line;
      boolean headerDone = false;
      BufferedReader br = new BufferedReader(new FileReader(inputFile));
      while ((line = br.readLine()) != null) {
        if (line.startsWith(AbstractParser.COMMENT)) {
          continue;
        }

        String[] attributes = AbstractParser.WHITESPACE_PATTERN.split(line);

        if (! headerDone) {
          for (int i = 0; i < attributes.length; i++) {
            String attribute = attributes[i];
            try {
              Double.parseDouble(attribute);
              outStream.println("@attribute d" + i + " numeric");
            }
            catch (NumberFormatException e) {
              outStream.println("@attribute d " + i + " string");
            }
          }
          outStream.println();
          outStream.println("@data");
          headerDone = true;
        }

        for (int i = 0; i < attributes.length; i++) {
          if (i == 0) outStream.print(attributes[i]);
          if (i > 0) outStream.print(", " + attributes[i]);
        }
        outStream.println();
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

