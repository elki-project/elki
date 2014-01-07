package de.lmu.ifi.dbs.elki.application;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.Logging.Level;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
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
 * 
 * @apiviz.uses LoggingConfiguration oneway
 * @apiviz.excludeSubtypes
 */
public abstract class AbstractApplication implements Parameterizable {
  /**
   * We need a static logger in this class, for code used in "main" methods.
   */
  private static final Logging LOG = Logging.getLogger(AbstractApplication.class);

  /**
   * The newline string according to system.
   */
  private static final String NEWLINE = System.getProperty("line.separator");

  /**
   * Information for citation and version.
   */
  public static final String INFORMATION = "ELKI Version 0.6.0 (2014, January)" + NEWLINE + NEWLINE //
      + "published in:" + NEWLINE //
      + "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:" + NEWLINE //
      + "Interactive Data Mining with 3D-Parallel-Coordinate-Trees." + NEWLINE //
      + "In Proceedings of the ACM International Conference on " + NEWLINE //
      + "Management of Data (SIGMOD), New York City, NY, 2013." + NEWLINE;

  /**
   * Constructor.
   */
  public AbstractApplication() {
    super();
  }

  /**
   * Generic command line invocation.
   * 
   * Refactored to have a central place for outermost exception handling.
   * 
   * @param cls Application class to run.
   * @param args the arguments to run this application with
   */
  public static void runCLIApplication(Class<?> cls, String[] args) {
    final Flag helpF = new Flag(Parameterizer.HELP_ID);
    final Flag helpLongF = new Flag(Parameterizer.HELP_LONG_ID);
    final ClassParameter<Object> descriptionP = new ClassParameter<>(Parameterizer.DESCRIPTION_ID, Object.class, true);
    final StringParameter debugP = new StringParameter(Parameterizer.DEBUG_ID);
    final Flag verboseF = new Flag(Parameterizer.VERBOSE_ID);
    debugP.setOptional(true);

    SerializedParameterization params = new SerializedParameterization(args);
    try {
      params.grab(helpF);
      params.grab(helpLongF);
      params.grab(descriptionP);
      params.grab(debugP);
      if(descriptionP.isDefined()) {
        params.clearErrors();
        printDescription(descriptionP.getValue());
        return;
      }
      // Fail silently on errors.
      if(params.getErrors().size() > 0) {
        params.logAndClearReportedErrors();
        return;
      }
      if(debugP.isDefined()) {
        LoggingUtil.parseDebugParameter(debugP);
      }
    }
    catch(Exception e) {
      printErrorMessage(e);
      return;
    }
    try {
      TrackParameters config = new TrackParameters(params);
      if(config.grab(verboseF) && verboseF.isTrue()) {
        // Extra verbosity by repeating the flag:
        final Flag verbose2F = new Flag(Parameterizer.VERBOSE_ID);
        if(config.grab(verbose2F) && verbose2F.isTrue()) {
          LoggingConfiguration.setVerbose(Level.VERYVERBOSE);
        }
        else {
          LoggingConfiguration.setVerbose(Level.VERBOSE);
        }
      }
      AbstractApplication task = ClassGenericsUtil.tryInstantiate(AbstractApplication.class, cls, config);

      if((helpF.isDefined() && helpF.getValue()) || (helpLongF.isDefined() && helpLongF.getValue())) {
        LoggingConfiguration.setVerbose(Level.VERBOSE);
        LOG.verbose(usage(config.getAllParameters()));
      }
      else {
        params.logUnusedParameters();
        if(params.getErrors().size() > 0) {
          LoggingConfiguration.setVerbose(Level.VERBOSE);
          LOG.verbose("The following configuration errors prevented execution:\n");
          for(ParameterException e : params.getErrors()) {
            LOG.verbose(e.getMessage());
          }
          LOG.verbose("\n");
          LOG.verbose("Stopping execution because of configuration errors.");
          System.exit(1);
        }
        else {
          task.run();
        }
      }
    }
    catch(Exception e) {
      printErrorMessage(e);
    }
  }

  /**
   * Returns a usage message, explaining all known options
   * 
   * @param options Options to show in usage.
   * @return a usage message explaining all known options
   */
  public static String usage(Collection<Pair<Object, Parameter<?>>> options) {
    StringBuilder usage = new StringBuilder();
    usage.append(INFORMATION);

    // Collect options
    usage.append(NEWLINE).append("Parameters:").append(NEWLINE);
    OptionUtil.formatForConsole(usage, FormatUtil.getConsoleWidth(), "   ", options);

    // FIXME: re-add global constraints!
    return usage.toString();
  }

  /**
   * Print an error message for the given error.
   * 
   * @param e Error Exception.
   */
  protected static void printErrorMessage(Exception e) {
    if(e instanceof AbortException) {
      // ensure we actually show the message:
      LoggingConfiguration.setVerbose(Level.VERBOSE);
      LOG.verbose(e.getMessage());
    }
    else if(e instanceof UnspecifiedParameterException) {
      LOG.error(e.getMessage());
    }
    else if(e instanceof ParameterException) {
      LOG.error(e.getMessage());
    }
    else {
      LOG.exception(e);
    }
  }

  /**
   * Print the description for the given parameter
   */
  private static void printDescription(Class<?> descriptionClass) {
    if(descriptionClass != null) {
      LoggingConfiguration.setVerbose(Level.VERBOSE);
      LOG.verbose(OptionUtil.describeParameterizable(new StringBuilder(), descriptionClass, FormatUtil.getConsoleWidth(), "    ").toString());
    }
  }

  /**
   * Runs the application.
   * 
   * @throws UnableToComplyException if an error occurs during running the
   *         application
   */
  public abstract void run() throws UnableToComplyException;

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter that specifies the name of the output file.
     * <p>
     * Key: {@code -app.out}
     * </p>
     */
    public static final OptionID OUTPUT_ID = new OptionID("app.out", "");

    /**
     * Parameter that specifies the name of the input file.
     * <p>
     * Key: {@code -app.in}
     * </p>
     */
    public static final OptionID INPUT_ID = new OptionID("app.in", "");

    /**
     * Flag to obtain help-message.
     * <p>
     * Key: {@code -h}
     * </p>
     */
    public static final OptionID HELP_ID = new OptionID("h", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

    /**
     * Flag to obtain help-message.
     * <p>
     * Key: {@code -help}
     * </p>
     */
    public static final OptionID HELP_LONG_ID = new OptionID("help", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

    /**
     * Optional Parameter to specify a class to obtain a description for.
     * <p>
     * Key: {@code -description}
     * </p>
     */
    public static final OptionID DESCRIPTION_ID = new OptionID("description", "Class to obtain a description of. " + "Causes immediate stop of the program.");

    /**
     * Optional Parameter to specify a class to enable debugging for.
     * <p>
     * Key: {@code -enableDebug}
     * </p>
     */
    public static final OptionID DEBUG_ID = new OptionID("enableDebug", "Parameter to enable debugging for particular packages.");

    /**
     * Flag to allow verbose messages while running the application.
     * <p>
     * Key: {@code -verbose}
     * </p>
     */
    public static final OptionID VERBOSE_ID = new OptionID("verbose", "Enable verbose messages.");

    /**
     * Get the output file parameter.
     * 
     * @param config Options
     * @return Output file
     */
    protected File getParameterOutputFile(Parameterization config) {
      return getParameterOutputFile(config, "Output filename.");
    }

    /**
     * Get the output file parameter.
     * 
     * @param config Options
     * @param description Short description
     * @return Output file
     */
    protected File getParameterOutputFile(Parameterization config, String description) {
      final FileParameter outputP = new FileParameter(OUTPUT_ID, FileParameter.FileType.OUTPUT_FILE);
      outputP.setShortDescription(description);
      if(config.grab(outputP)) {
        return outputP.getValue();
      }
      return null;
    }

    /**
     * Get the input file parameter.
     * 
     * @param config Options
     * @return Input file
     */
    protected File getParameterInputFile(Parameterization config) {
      return getParameterInputFile(config, "Input filename.");
    }

    /**
     * Get the input file parameter
     * 
     * @param config Options
     * @param description Description
     * @return Input file
     */
    protected File getParameterInputFile(Parameterization config, String description) {
      final FileParameter inputP = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
      inputP.setShortDescription(description);
      if(config.grab(inputP)) {
        return inputP.getValue();
      }
      return null;
    }

    @Override
    protected abstract AbstractApplication makeInstance();
  }
}
