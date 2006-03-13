package de.lmu.ifi.dbs.wrapper;


import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper to run another wrapper for all files in the directory given as input.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DirectoryTask extends AbstractWrapper {
  public static final String WRAPPER_P = "wrapper";

  public static final String WRAPPER_D = "<class> wrapper to run over all files in a specified directory";

  private Wrapper wrapper;

  public DirectoryTask() {
    parameterToDescription.put(WRAPPER_P + OptionHandler.EXPECTS_VALUE, WRAPPER_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) {
    String[] remainingParameters = super.setParameters(args);

    try {
      wrapper = Util.instantiate(Wrapper.class, optionHandler.getOptionValue(WRAPPER_P));
    }
    catch (IllegalArgumentException e) {
      WrongParameterValueException pe = new WrongParameterValueException(WRAPPER_P, optionHandler.getOptionValue(WRAPPER_P), WRAPPER_D);
      pe.fillInStackTrace();
      throw pe;
    }

    return remainingParameters;
  }

  /**
   * @see AbstractAlgorithmWrapper#addParameters(java.util.List<String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    return new ArrayList<String>();
  }

  /**
   * Runs the specified wrapper with given arguiments for all files in directory given as input.
   *
   * @see Wrapper#run(String[])
   */
  public void run(String[] args) {
    String[] remainingParameters = setParameters(args);
    int inputIndex = -1;
    int outputIndex = -1;
    for (int i = 0; i < remainingParameters.length; i++) {
      if (remainingParameters[i].equals(OptionHandler.OPTION_PREFIX + INPUT_P)) {
        inputIndex = i + 1;
      }
      if (remainingParameters[i].equals(OptionHandler.OPTION_PREFIX + OUTPUT_P)) {
        outputIndex = i + 1;
      }
    }
    if (inputIndex < 0 || inputIndex >= remainingParameters.length) {
      throw new IllegalArgumentException("Invalid parameter array: value of " + INPUT_P + " out of range.");
    }
    if (outputIndex < 0 || outputIndex >= remainingParameters.length) {
      throw new IllegalArgumentException("Invalid parameter array: value of " + OUTPUT_P + " out of range.");
    }

    File inputDir = new File(remainingParameters[inputIndex]);
    if (!inputDir.isDirectory()) {
      throw new IllegalArgumentException(remainingParameters[inputIndex] + " is not a directory");
    }
    File[] inputFiles = inputDir.listFiles();
    for (File inputFile : inputFiles) {
      try {
        String[] parameterCopy = Util.copy(remainingParameters);
        parameterCopy[inputIndex] = remainingParameters[inputIndex] + File.separator + inputFile.getName();
        parameterCopy[outputIndex] = remainingParameters[outputIndex] + File.separator + inputFile.getName();

        Wrapper newWrapper = wrapper.getClass().newInstance();
        newWrapper.run(parameterCopy);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    DirectoryTask wrapper = new DirectoryTask();
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
  }

}
