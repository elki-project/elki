package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * TODO comment
 * AbstractWrapper sets the values for flags verbose, time, in and out. <p/> Any
 * Wrapper class that makes use of these flags may extend this class. Beware to
 * make correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class StandAloneWrapper implements Wrapper {
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
   * Flag to allow verbose messages.
   */
  public static final String VERBOSE_F = "verbose";

  /**
   * Description for verbose flag.
   */
  public static final String VERBOSE_D = "flag to allow verbose messages while performing the wrapper";

  /**
   * Label for parameter input.
   */
  public final static String INPUT_P = "in";

  /**
   * Description for parameter input.
   */
  public static String INPUT_D = "<filename>input file";

  /**
   * Parameter output.
   */
  public static final String OUTPUT_P = "out";

  /**
   * Description for parameter output.
   */
  public static String OUTPUT_D = "<filename>output file";

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler to handler options. optionHandler should be initialized
   * using parameterToDescription in any non-abstract class extending this
   * class.
   */
  protected OptionHandler optionHandler;

  /**
   * Sets the flags for verbose in the parameter map. Any extending
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
  protected StandAloneWrapper() {
    parameterToDescription.put(StandAloneWrapper.VERBOSE_F, StandAloneWrapper.VERBOSE_D);
    parameterToDescription.put(StandAloneWrapper.INPUT_P + OptionHandler.EXPECTS_VALUE, StandAloneWrapper.INPUT_D);
    parameterToDescription.put(StandAloneWrapper.OUTPUT_P + OptionHandler.EXPECTS_VALUE, StandAloneWrapper.OUTPUT_D);

    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Returns whether verbose messages should be printed while executing the
   * algorithm.
   *
   * @return whether verbose messages should be printed while executing the
   *         algorithm
   */
  public boolean isVerbose() {
    return optionHandler.isSet(VERBOSE_F);
  }

  /**
   * Returns the input string.
   *
   * @return the input string
   */
  public String getInput() throws ParameterException {
    return optionHandler.getOptionValue(INPUT_P);
  }

  /**
   * Returns the output string.
   *
   * @return the output string
   */
  public String getOutput() throws ParameterException {
    return optionHandler.getOptionValue(OUTPUT_P);
  }
}
