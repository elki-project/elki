package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractWrapper sets the values for flags verbose and help. <p/> Any Wrapper
 * class that makes use of these flags may extend this class. Beware to make
 * correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public abstract class AbstractWrapper extends AbstractParameterizable implements Wrapper {

  /**
   * Label for help flag.
   */
  public static final String HELP_F = "help";

  /**
   * Description for help flag.
   */
  public static final String HELP_D = "flag to obtain help-message. Causes immediate stop of the program.";

  /**
   * Label for verbose flag.
   */
  public static final String VERBOSE_F = "verbose";

  /**
   * Description for verbose flag.
   */
  public static final String VERBOSE_D = "flag to allow verbose messages while performing the wrapper";

  /**
   * Verbose flag.
   */
  private Flag verboseFlag;

  /**
   * Value of verbose flag.
   */
  private boolean verbose;

  /**
   * The remaining parameters after the option handler grabbed the options for
   * this wrapper.
   */
  private List<String> remainingParameters;

  /**
   * Sets the parameters for the verbose and help flags in the parameter map.
   * Any extending class should call this constructor, then add further
   * parameters. Any non-abstract extending class should finally initialize
   * optionHandler like this: <p/>
   * <p/>
   * <pre>
   *   {
   *       parameterToDescription.put(YOUR_PARAMETER_NAME+OptionHandler.EXPECTS_VALUE,YOUR_PARAMETER_DESCRIPTION);
   *       ...
   *       optionHandler = new OptionHandler(parameterToDescription,yourClass.class.getName());
   *   }
   * </pre>
   */
  protected AbstractWrapper() {
    // verbose
    verboseFlag = new Flag(VERBOSE_F, VERBOSE_D);
    optionHandler.put(verboseFlag);

    // help
    optionHandler.put(new Flag(HELP_F, HELP_D));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    this.remainingParameters = new ArrayList<String>(remainingParameters.length);
    for (String s : remainingParameters) {
      this.remainingParameters.add(s);
    }

    // help
    if (optionHandler.isSet(HELP_F)) {
      throw new AbortException(optionHandler.usage(""));
    }

    // verbose
    verbose = optionHandler.isSet(verboseFlag);

    setParameters(args, new String[0]);
    return new String[0];
  }

  /**
   * Returns whether verbose messages should be printed while executing the
   * wrapper.
   *
   * @return whether verbose messages should be printed while executing the
   *         wrapper
   */
  public final boolean isVerbose() {
    return verbose;
  }

  /**
   * Returns a copy of the remaining parameters after the option handler
   * grabbed the options for this wrapper.
   *
   * @return the remaining parameters
   */
  public final List<String> getRemainingParameters() {
    List<String> result = new ArrayList<String>(remainingParameters.size());
    result.addAll(remainingParameters);
    return result;
  }

}
