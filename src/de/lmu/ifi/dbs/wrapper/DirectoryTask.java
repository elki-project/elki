package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.io.File;
import java.util.List;

/**
 * Wrapper to run another wrapper for all files in the directory given as input.
 *
 * @author Arthur Zimek
 * todo parameter
 */
public class DirectoryTask extends StandAloneInputWrapper {

  /**
   * Label for parameter wrapper.
   */
  public static final String WRAPPER_P = "wrapper";

  /**
   * Description for parameter wrapper.
   */
  public static final String WRAPPER_D = "wrapper to run over all files in a specified directory " +
                                         Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Wrapper.class) +
                                         ".";

  /**
   * Wrapper to run over all files.
   */
  private Wrapper wrapper;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    DirectoryTask wrapper = new DirectoryTask();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.verbose(e.getMessage());
    }
    catch (Exception e) {
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  public DirectoryTask() {
    optionHandler.put(new ClassParameter<Wrapper>(WRAPPER_P, WRAPPER_D, Wrapper.class));
  }

  /**
   * Runs the wrapper.
   */
  public void run() throws UnableToComplyException {
    File inputDir = getInput();
    if (!inputDir.isDirectory()) {
      throw new IllegalArgumentException(getInput() + " is not a directory");
    }
    File[] inputFiles = inputDir.listFiles();
    for (File inputFile : inputFiles) {
      try {
        List<String> wrapperParameters = getRemainingParameters();
        wrapperParameters.add(OptionHandler.OPTION_PREFIX + INPUT_P);
        wrapperParameters.add(inputFile.getAbsolutePath());
        wrapperParameters.add(OptionHandler.OPTION_PREFIX + OUTPUT_P);
        wrapperParameters.add(getOutput() + File.separator + inputFile.getName());
        wrapper.setParameters(wrapperParameters.toArray(new String[wrapperParameters.size()]));
        wrapper.run();
      }
      catch (ParameterException e) {
        throw new UnableToComplyException(e.getMessage(), e);
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // wrapper
    try {
      wrapper = Util.instantiate(Wrapper.class, (String) optionHandler.getOptionValue(WRAPPER_P));
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(WRAPPER_P, (String) optionHandler.getOptionValue(WRAPPER_P), WRAPPER_D);
    }

    return remainingParameters;
  }
}
