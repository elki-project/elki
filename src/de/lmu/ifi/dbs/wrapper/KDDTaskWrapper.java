package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.logging.Logger;

/**
 * KDDTaskWrapper is an abstract super class for all wrapper classes running
 * algorithms in a kdd task.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class KDDTaskWrapper extends AbstractWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The result of the kdd task.
   */
  private Result result;

  /**
   * The name of the output file.
   */
  private String output;

  /**
   * Time flag;
   */
  private boolean time;

  /**
   * Sets additionally to the parameters set by the super class the
   * time flag and the parameter out in the parameter map. Any extending
   * class should call this constructor, then add further parameters. Any
   * non-abstract extending class should finally initialize optionHandler like
   * this:
   * <p/>
   * <pre>
   *  {
   *      parameterToDescription.put(YOUR_PARAMETER_NAME+OptionHandler.EXPECTS_VALUE,YOUR_PARAMETER_DESCRIPTION);
   *      ...
   *      optionHandler = new OptionHandler(parameterToDescription,yourClass.class.getName());
   *  }
   * </pre>
   */
  protected KDDTaskWrapper() {
    parameterToDescription.put(KDDTask.OUTPUT_P + OptionHandler.EXPECTS_VALUE, KDDTask.OUTPUT_D);
    parameterToDescription.put(AbstractAlgorithm.TIME_F, AbstractAlgorithm.TIME_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see Wrapper#run()
   */
  public final void run() throws UnableToComplyException {
    try {
      List<String> parameters = getKDDTaskParameters();
      KDDTask task = new KDDTask();
      task.setParameters(parameters.toArray(new String[parameters.size()]));
      result = task.run();
    }
    catch (ParameterException e) {
      e.printStackTrace();
      throw new UnableToComplyException(e);
    }
  }

  /**
   * Returns the result of the kdd task.
   *
   * @return the result of the kdd task
   */
  public final Result getResult() {
    return result;
  }

  /**
   * Returns the name of the output file.
   *
   * @return the name of the output file
   */
  public final String getOutput() {
    return output;
  }

  /**
   * Returns the value of the time flag.
   *
   * @return the value of the time flag.
   */
  public final boolean isTime() {
    return time;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // output
    output = optionHandler.getOptionValue(KDDTask.OUTPUT_P);
    // time
    time = optionHandler.isSet(AbstractAlgorithm.TIME_F);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(KDDTask.OUTPUT_P, output);
    mySettings.addSetting(AbstractAlgorithm.TIME_F, Boolean.toString(time));
    return settings;
  }

  /**
   * Returns the parameters that are necessary to run the kdd task correctly.
   *
   * @return the array containing the parametr setting that is necessary to
   *         run the kdd task correctly
   */
  public List<String> getKDDTaskParameters() {
    List<String> result = getRemainingParameters();

    // verbose
    if (isVerbose()) {
      result.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }
    // time
    if (isTime()) {
      result.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }
    // output
    result.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    result.add(getOutput());

    return result;
  }
}
