package de.lmu.ifi.dbs.elki.application;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * AbstractApplication sets the values for flags verbose and help.
 * <p/>
 * Any Wrapper class that makes use of these flags may extend this class. Beware
 * to make correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 * 
 * @author Elke Achtert
 */

public abstract class AbstractApplication extends AbstractParameterizable {
  /**
   * Flag to obtain help-message.
   * <p>
   * Key: {@code -h}
   * </p>
   */
  private final Flag HELP_FLAG = new Flag(OptionID.HELP);

  /**
   * Flag to obtain help-message.
   * <p>
   * Key: {@code -help}
   * </p>
   */
  private final Flag HELP_LONG_FLAG = new Flag(OptionID.HELP_LONG);

  /**
   * Flag to allow verbose messages while running the application.
   * <p>
   * Key: {@code -verbose}
   * </p>
   */
  private final Flag VERBOSE_FLAG = new Flag(OptionID.ALGORITHM_VERBOSE);

  /**
   * Value of verbose flag.
   */
  private boolean verbose;

  /**
   * The remaining parameters after the option handler grabbed the options for
   * this application.
   */
  private List<String> remainingParameters;

  /**
   * Adds the flags {@link #VERBOSE_FLAG} and {@link #HELP_FLAG} to the option
   * handler. Any extending class should call this constructor, then add further
   * parameters.
   */
  protected AbstractApplication() {
    // verbose
    addOption(VERBOSE_FLAG);

    // help
    addOption(HELP_FLAG);
    addOption(HELP_LONG_FLAG);
  }

  /**
   * Calls
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(String[])
   * AbstractParameterizable#setParameters(args)} and sets additionally the
   * values of the flags {@link #VERBOSE_FLAG} and {@link #HELP_FLAG}.
   * 
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    if(args.length == 0) {
      throw new AbortException("No options specified. Try flag -h to gain more information.");
    }

    String[] remainingParameters = super.setParameters(args);
    this.remainingParameters = new ArrayList<String>(remainingParameters.length);
    for(String s : remainingParameters) {
      this.remainingParameters.add(s);
    }

    // verbose
    verbose = VERBOSE_FLAG.isSet();
    if(verbose) {
      LoggingConfiguration.setVerbose(true);
    }

    rememberParametersExcept(args, new String[0]);
    return new String[0];
  }

  /**
   * Returns whether verbose messages should be printed while executing the
   * application.
   * 
   * @return whether verbose messages should be printed while executing the
   *         application
   */
  public final boolean isVerbose() {
    return verbose;
  }

  /**
   * Returns a copy of the remaining parameters after the option handler grabbed
   * the options for this application.
   * 
   * @return the remaining parameters
   */
  public final List<String> getRemainingParameters() {
    if(remainingParameters == null) {
      return new ArrayList<String>();
    }

    List<String> result = new ArrayList<String>(remainingParameters.size());
    result.addAll(remainingParameters);
    return result;
  }
  
  /**
   * Returns a usage message with the specified message as leading line, and
   * information as provided by optionHandler. If an algorithm is specified, the
   * description of the algorithm is returned.
   * 
   * @return a usage message with the specified message as leading line, and
   *         information as provided by optionHandler
   */
  public String usage() {
    StringBuffer usage = new StringBuffer();
    // Collect options
    List<Pair<Parameterizable, Option<?>>> options = new ArrayList<Pair<Parameterizable, Option<?>>>();
    collectOptions(options);
    OptionUtil.formatForConsole(usage, 77, "   ", options);
    return usage.toString();
  }  

  /**
   * Generic command line invocation.
   * 
   * Refactored to have a central place for outermost exception handling.
   * 
   * @param args the arguments to run this application
   */
  public void runCLIApplication(String[] args) {
    LoggingConfiguration.assertConfigured();
    try {
      this.setParameters(args);
      // help
      if(HELP_FLAG.isSet()) {
        throw new AbortException("Usage:\n\n"+usage());
      }
      run();
    }
    catch(AbortException e) {
      LoggingConfiguration.setVerbose(true);
      logger.verbose(e.getMessage());
    }
    catch(ParameterException e) {
      if(HELP_FLAG.isSet()) {
        LoggingConfiguration.setVerbose(true);
        logger.verbose("Usage:\n\n"+usage());
      }
      logger.warning(e.toString());
    }
    catch(Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      logger.exception(e.toString(), cause);
    }
  }
  
  /**
   * Runs the application.
   * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException if an error occurs during running the application
   */
  public abstract void run() throws UnableToComplyException;
}
