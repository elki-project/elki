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
package de.lmu.ifi.dbs.elki.workflow;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.Logging.Level;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Pseudo-step to configure logging / verbose mode.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - LoggingConfiguration
 */
public class LoggingStep implements WorkflowStep {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(LoggingStep.class);

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param levels Level settings array
   */
  public LoggingStep(int verbose, String[][] levels) {
    super();
    if (verbose <= 0) {
      LoggingConfiguration.setVerbose(Level.WARNING);
    } else if (verbose == 1) {
      LoggingConfiguration.setVerbose(Level.VERBOSE);
    } else {
      // Extra verbosity - do not call with "false" to not undo!
      LoggingConfiguration.setVerbose(Level.VERYVERBOSE);
    }
    if (levels != null) {
      for (String[] pair : levels) {
        try {
          if (pair.length == 1) {
            // Try to parse as level:
            try {
              java.util.logging.Level level = Level.parse(pair[0]);
              LoggingConfiguration.setDefaultLevel(level);
            } catch (IllegalArgumentException e) {
              LoggingConfiguration.setLevelFor(pair[0], Level.FINEST.getName());
            }
          } else if (pair.length == 2) {
            LoggingConfiguration.setLevelFor(pair[0], pair[1]);
          } else {
            throw new AbortException("Invalid logging settings");
          }
        } catch (IllegalArgumentException e) {
          LOG.warning("Invalid logging statement for package " + pair[0] + ": " + e.getMessage());
        }
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Verbose mode.
     */
    protected int verbose = 0;

    /**
     * Enable logging levels manually
     */
    protected String[][] levels = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final Flag verboseF = new Flag(AbstractApplication.Parameterizer.VERBOSE_ID);
      if (config.grab(verboseF) && verboseF.isTrue()) {
        verbose++;
        final Flag verbose2F = new Flag(AbstractApplication.Parameterizer.VERBOSE_ID);
        if (config.grab(verbose2F) && verbose2F.isTrue()) {
          verbose++;
        }
      }
      final StringParameter debugP = new StringParameter(AbstractApplication.Parameterizer.DEBUG_ID) //
          .setOptional(true);
      if (config.grab(debugP)) {
        String[] opts = debugP.getValue().split(",");
        levels = new String[opts.length][];
        int i = 0;
        for (String opt : opts) {
          String[] chunks = opt.split("=");
          if (chunks.length != 1 && chunks.length != 2) {
            config.reportError(new WrongParameterValueException(debugP, debugP.getValue(), "Invalid debug option."));
            break;
          }
          levels[i] = chunks;
          i++;
        }
      }
    }

    @Override
    protected LoggingStep makeInstance() {
      return new LoggingStep(verbose, levels);
    }
  }
}
