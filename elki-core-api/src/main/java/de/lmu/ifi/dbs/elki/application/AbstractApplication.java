/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.application;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.Logging.Level;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * AbstractApplication sets the values for flags verbose and help.
 * <p>
 * Any Wrapper class that makes use of these flags may extend this class. Beware
 * to make correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.2
 *
 * @navassoc - - - LoggingConfiguration
 */
public abstract class AbstractApplication {
  /**
   * We need a static logger in this class, for code used in "main" methods.
   */
  private static final Logging LOG = Logging.getLogger(AbstractApplication.class);

  /**
   * The newline string according to system.
   */
  private static final String NEWLINE = System.getProperty("line.separator");

  /**
   * Version information.
   */
  public static final String VERSION;

  /**
   * Get the version number from the properties.
   */
  static {
    String version = "DEVELOPMENT";
    try {
      Properties prop = new Properties();
      prop.load(AbstractApplication.class.getClassLoader().getResourceAsStream("META-INF/elki.properties"));
      version = prop.getProperty("elki.version");
    }
    catch(Exception e) {
    }
    VERSION = version;
  }

  /**
   * Version number of the reference below.
   */
  private static final String REFERENCE_VERSION = "0.7.5";

  /**
   * Information for citation and version.
   */
  @Reference(authors = "Erich Schubert and Arthur Zimek", //
      title = "ELKI: A large open-source library for data analysis - ELKI Release 0.7.5 \"Heidelberg\"", //
      booktitle = "CoRR", //
      url = "https://arxiv.org/abs/1902.03616", //
      bibkey = "DBLP:journals/corr/abs-1902-03616")
  public static final String REFERENCE = "ELKI Release 0.7.5 (2019, February) published in:" + NEWLINE + NEWLINE //
      + "Erich Schubert and Arthur Zimek:" + NEWLINE //
      + "ELKI: A large open-source library for data analysis - ELKI Release 0.7.5 \"Heidelberg\"." + NEWLINE //
      + "CoRR arXiv:1902.03616" + NEWLINE;

  /**
   * Constructor.
   */
  public AbstractApplication() {
    super();
  }

  /**
   * Generic command line invocation.
   * <p>
   * Refactored to have a central place for outermost exception handling.
   *
   * @param cls Application class to run.
   * @param args the arguments to run this application with
   */
  public static void runCLIApplication(Class<?> cls, String[] args) {
    SerializedParameterization params = new SerializedParameterization(args);
    Flag helpF = new Flag(Parameterizer.HELP_ID);
    params.grab(helpF);
    Flag helpLongF = new Flag(Parameterizer.HELP_LONG_ID);
    params.grab(helpLongF);
    try {
      ClassParameter<Object> descriptionP = new ClassParameter<>(Parameterizer.DESCRIPTION_ID, Object.class, true);
      params.grab(descriptionP);
      if(descriptionP.isDefined()) {
        params.clearErrors();
        printDescription(descriptionP.getValue());
        System.exit(1);
      }
      // Parse debug parameter
      StringParameter debugP = new StringParameter(Parameterizer.DEBUG_ID).setOptional(true);
      params.grab(debugP);
      if(debugP.isDefined()) {
        Parameterizer.parseDebugParameter(debugP);
      }
      // Fail silently on errors.
      if(params.getErrors().size() > 0) {
        params.logAndClearReportedErrors();
        System.exit(1);
      }
    }
    catch(Exception e) {
      printErrorMessage(e);
      System.exit(1);
    }
    try {
      TrackParameters config = new TrackParameters(params);
      Flag verboseF = new Flag(Parameterizer.VERBOSE_ID);
      if(config.grab(verboseF) && verboseF.isTrue()) {
        // Extra verbosity by repeating the flag:
        Flag verbose2F = new Flag(Parameterizer.VERBOSE_ID);
        LoggingConfiguration.setVerbose((config.grab(verbose2F) && verbose2F.isTrue()) ? Level.VERYVERBOSE : Level.VERBOSE);
      }
      AbstractApplication task = ClassGenericsUtil.tryInstantiate(AbstractApplication.class, cls, config);

      if((helpF.isDefined() && helpF.getValue()) || (helpLongF.isDefined() && helpLongF.getValue())) {
        LoggingConfiguration.setVerbose(Level.VERBOSE);
        LOG.verbose(usage(config.getAllParameters()));
        System.exit(1);
      }
      if(params.getErrors().size() > 0) {
        LoggingConfiguration.setVerbose(Level.VERBOSE);
        LOG.verbose("ERROR: The following configuration errors prevented execution:");
        for(ParameterException e : params.getErrors()) {
          LOG.verbose(e.getMessage() + "\n");
        }
        params.logUnusedParameters();
        LOG.verbose("Stopping execution because of configuration errors above.");
        System.exit(1);
      }
      params.logUnusedParameters();
      task.run();
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
  public static String usage(Collection<TrackedParameter> options) {
    StringBuilder usage = new StringBuilder(10000);
    if(!REFERENCE_VERSION.equals(VERSION)) {
      usage.append("ELKI build: ").append(VERSION).append(NEWLINE).append(NEWLINE);
    }
    usage.append(REFERENCE);

    // Collect options
    OptionUtil.formatForConsole(usage.append(NEWLINE).append("Parameters:").append(NEWLINE), //
        FormatUtil.getConsoleWidth(), options);
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
    if(descriptionClass == null) {
      return;
    }
    try {
      LoggingConfiguration.setVerbose(Level.VERBOSE);
      LOG.verbose(OptionUtil.describeParameterizable(new StringBuilder(), descriptionClass, FormatUtil.getConsoleWidth(), "").toString());
    }
    catch(Exception e) {
      LOG.exception("Error instantiating class to describe.", e.getCause());
    }
  }

  /**
   * Runs the application.
   */
  public abstract void run();

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter that specifies the name of the output file.
     */
    public static final OptionID OUTPUT_ID = new OptionID("app.out", "");

    /**
     * Parameter that specifies the name of the input file.
     */
    public static final OptionID INPUT_ID = new OptionID("app.in", "");

    /**
     * Option ID to specify the database type
     */
    public static final OptionID DATABASE_ID = new OptionID("db", "Database class.");

    /**
     * Flag to obtain help-message.
     */
    public static final OptionID HELP_ID = new OptionID("h", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

    /**
     * Flag to obtain help-message.
     */
    public static final OptionID HELP_LONG_ID = new OptionID("help", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

    /**
     * Optional Parameter to specify a class to obtain a description for.
     */
    public static final OptionID DESCRIPTION_ID = new OptionID("description", "Class to obtain a description of. " + "Causes immediate stop of the program.");

    /**
     * Optional Parameter to specify a class to enable debugging for.
     */
    public static final OptionID DEBUG_ID = new OptionID("enableDebug", "Parameter to enable debugging for particular packages.");

    /**
     * Flag to allow verbose messages while running the application.
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

    /**
     * Parse the option string to configure logging.
     *
     * @param param Parameter to process.
     * @throws WrongParameterValueException On parsing errors
     */
    public static final void parseDebugParameter(StringParameter param) throws WrongParameterValueException {
      String[] opts = param.getValue().split(",");
      for(String opt : opts) {
        try {
          String[] chunks = opt.split("=");
          if(chunks.length == 1) {
            try {
              java.util.logging.Level level = Level.parse(chunks[0]);
              LoggingConfiguration.setDefaultLevel(level);
            }
            catch(IllegalArgumentException e) {
              LoggingConfiguration.setLevelFor(chunks[0], Level.FINEST.getName());
            }
          }
          else if(chunks.length == 2) {
            LoggingConfiguration.setLevelFor(chunks[0], chunks[1]);
          }
          else {
            throw new WrongParameterValueException(param, param.getValue(), "More than one '=' in debug parameter.");
          }
        }
        catch(IllegalArgumentException e) {
          throw (new WrongParameterValueException(param, param.getValue(), "Could not process value.", e));
        }
      }
    }

    @Override
    protected abstract AbstractApplication makeInstance();
  }
}
