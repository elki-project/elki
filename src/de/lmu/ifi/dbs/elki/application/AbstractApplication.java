package de.lmu.ifi.dbs.elki.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * AbstractApplication sets the values for flags verbose and help.
 * <p/>
 * Any Wrapper class that makes use of these flags may extend this class. Beware
 * to make correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 */

public abstract class AbstractApplication extends AbstractParameterizable {
  /**
   * The newline string according to system.
   */
  private static final String NEWLINE = System.getProperty("line.separator");

  /**
   * Information for citation and version.
   */
  public static final String INFORMATION = "ELKI Version 0.2 (2009, July)" + NEWLINE + NEWLINE + "published in:" + NEWLINE + "Elke Achtert, Thomas Bernecker, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:\n" + "ELKI in Time: ELKI 0.2 for the Performance Evaluation of Distance Measures for Time Series.\n" + "In Proc. 11th International Symposium on Spatial and Temporal Databases (SSTD 2009), Aalborg, Denmark, 2009." + NEWLINE;

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
   * Optional Parameter to specify a class to obtain a description for, must
   * extend {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * .
   * <p>
   * Key: {@code -description}
   * </p>
   */
  private final ClassParameter<Parameterizable> DESCRIPTION_PARAM = new ClassParameter<Parameterizable>(OptionID.DESCRIPTION, Parameterizable.class, true);

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

    // description parameter
    addOption(DESCRIPTION_PARAM);
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    if(args.size() == 0) {
      throw new AbortException("No options specified. Try flag -h to gain more information.");
    }

    this.remainingParameters = super.setParameters(args);

    // verbose
    verbose = VERBOSE_FLAG.isSet();
    if(verbose) {
      LoggingConfiguration.setVerbose(true);
    }

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
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
   * Returns a usage message, explaining all known options
   * 
   * @return a usage message explaining all known options
   */
  public String usage() {
    StringBuffer usage = new StringBuffer();
    usage.append(INFORMATION);

    // Collect options
    List<Pair<Parameterizable, Option<?>>> options = new ArrayList<Pair<Parameterizable, Option<?>>>();
    collectOptions(options);
    usage.append(NEWLINE).append("Parameters:").append(NEWLINE);
    OptionUtil.formatForConsole(usage, 77, "   ", options);

    // TODO: cleanup:
    List<GlobalParameterConstraint> globalParameterConstraints = optionHandler.getGlobalParameterConstraints();
    if(!globalParameterConstraints.isEmpty()) {
      usage.append(NEWLINE).append("Global parameter constraints:");
      for(GlobalParameterConstraint gpc : globalParameterConstraints) {
        usage.append(NEWLINE).append(" - ");
        usage.append(gpc.getDescription());
      }
    }

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
    boolean stop = false;
    
    List<String> argslist = Arrays.asList(args);

    Exception error = null;
    try {
      List<String> remainingParameters = this.setParameters(argslist);
      if(remainingParameters.size() > 0) {
        logger.warning("Unused parameters specified: " + remainingParameters + "\n");
      }
    }
    catch(Exception t) {
      // we do error handling below.
      stop = true;
      error = t;
    }

    // When help was requested, print the usage statement.
    if(HELP_FLAG.isSet() || HELP_LONG_FLAG.isSet()) {
      stop = true;
      printHelp();
    }

    // When a description is requested, give the requested description next.
    if(DESCRIPTION_PARAM.isSet()) {
      stop = true;
      printDescription();
    }

    // If we didn't have to stop yet, run the algorithm.
    if(!stop && error == null) {
      try {
        // Try to run the actual algorithms.
        run();
      }
      catch(Exception t) {
        stop = true;
        error = t;
      }
    }

    if(error != null) {
      printErrorMessage(error);
    }
  }

  /**
   * Print an error message for the given error.
   * 
   * @param e Error Exception.
   */
  private void printErrorMessage(Exception e) {
    if(e instanceof AbortException) {
      // ensure we actually show the message:
      LoggingConfiguration.setVerbose(true);
      logger.verbose(e.getMessage());
    }
    else if(e instanceof UnspecifiedParameterException) {
      logger.error(e.getMessage());
    }
    else if(e instanceof ParameterException) {
      logger.error(e.getMessage());
    }
    else {
      LoggingUtil.exception(e.getMessage(), e);
    }
  }

  /**
   * Print the help message.
   */
  private void printHelp() {
    LoggingConfiguration.setVerbose(true);
    logger.verbose(usage());
  }

  /**
   * Print the description for the given parameter
   */
  private void printDescription() {
    String descriptionClass;
    try {
      descriptionClass = DESCRIPTION_PARAM.getValue();
    }
    catch(UnusedParameterException e) {
      return;
    }
    try {
      Parameterizable p = ClassGenericsUtil.instantiate(Parameterizable.class, descriptionClass);
      LoggingConfiguration.setVerbose(true);
      logger.verbose(OptionUtil.describeParameterizable(new StringBuffer(), p, 77, "   ").toString());
    }
    catch(UnableToComplyException e) {
      LoggingUtil.exception(e.getMessage(), e);
    }
  }

  /**
   * Runs the application.
   * 
   * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException if an error
   *         occurs during running the application
   */
  public abstract void run() throws UnableToComplyException;
}
