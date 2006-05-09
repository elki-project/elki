package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wrapper to run another wrapper for all files in the directory given as input.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DirectoryTask extends StandAloneWrapper {
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

  /**
   * Label for parameter wrapper.
   */
  public static final String WRAPPER_P = "wrapper";

  /**
   * description for parameter wrapper.
   */
  public static final String WRAPPER_D = "<class>wrapper to run over all files in a specified directory";

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    DirectoryTask wrapper = new DirectoryTask();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
  }

  public DirectoryTask() {
    parameterToDescription.put(WRAPPER_P + OptionHandler.EXPECTS_VALUE,
                               WRAPPER_D);
    optionHandler = new OptionHandler(parameterToDescription, this
    .getClass().getName());
  }

  /**
   * Runs the specified wrapper with given arguiments for all files in
   * directory given as input.
   *
   * @see Wrapper#run(String[])
   */
  public void run(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    Wrapper wrapper;
    try {
      wrapper = Util.instantiate(Wrapper.class, optionHandler
      .getOptionValue(WRAPPER_P));
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(WRAPPER_P, optionHandler
      .getOptionValue(WRAPPER_P), WRAPPER_D);
    }

    int inputIndex = -1;
    int outputIndex = -1;
    for (int i = 0; i < remainingParameters.length; i++) {
      if (remainingParameters[i].equals(OptionHandler.OPTION_PREFIX
                                        + INPUT_P)) {
        inputIndex = i + 1;
      }
      if (remainingParameters[i].equals(OptionHandler.OPTION_PREFIX
                                        + OUTPUT_P)) {
        outputIndex = i + 1;
      }
    }
    if (inputIndex < 0 || inputIndex >= remainingParameters.length) {
      throw new IllegalArgumentException(
      "Invalid parameter array: value of " + INPUT_P
      + " out of range.");
    }
    if (outputIndex < 0 || outputIndex >= remainingParameters.length) {
      throw new IllegalArgumentException(
      "Invalid parameter array: value of " + OUTPUT_P
      + " out of range.");
    }

    File inputDir = new File(remainingParameters[inputIndex]);
    if (!inputDir.isDirectory()) {
      throw new IllegalArgumentException(remainingParameters[inputIndex]
                                         + " is not a directory");
    }
    File[] inputFiles = inputDir.listFiles();
    for (File inputFile : inputFiles) {
      try {
        String[] parameterCopy = Util.copy(remainingParameters);
        parameterCopy[inputIndex] = remainingParameters[inputIndex]
                                    + File.separator + inputFile.getName();
        parameterCopy[outputIndex] = remainingParameters[outputIndex]
                                     + File.separator + inputFile.getName();

        Wrapper newWrapper = Util.instantiate(Wrapper.class, wrapper
        .getClass().getName());
        newWrapper.run(parameterCopy);
      }
      catch (UnableToComplyException e) {
        // tested before
        throw new RuntimeException("This should never happen!", e);
      }
    }
  }

}
