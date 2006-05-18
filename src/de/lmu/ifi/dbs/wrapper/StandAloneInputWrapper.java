package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.logging.Logger;

/**
 * StandAloneInputWrapper extends StandAloneWrapper and
 * sets additionally the value for the flag in. <p/> Any
 * Wrapper class that makes use of these flags may extend this class. Beware to
 * make correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class StandAloneInputWrapper extends StandAloneWrapper {
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
   * Label for parameter input.
   */
  public final static String INPUT_P = "in";

  /**
   * Description for parameter input.
   */
  public static String INPUT_D = "<filename>input file";

  /**
   * Sets the parameter in in the parameter map. Any extending
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
  protected StandAloneInputWrapper() {
    parameterToDescription.put(INPUT_P + OptionHandler.EXPECTS_VALUE, INPUT_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Returns the input string.
   *
   * @return the input string
   */
  public String getInput() throws ParameterException {
    return optionHandler.getOptionValue(INPUT_P);
  }
}
