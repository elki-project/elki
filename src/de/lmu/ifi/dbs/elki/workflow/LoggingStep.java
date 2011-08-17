package de.lmu.ifi.dbs.elki.workflow;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Pseudo-step to configure logging / verbose mode.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses LoggingConfiguration
 */
public class LoggingStep implements WorkflowStep {
  /**
   * Logger
   */
  private final static Logging logger = Logging.getLogger(LoggingStep.class);

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param levels Level settings array
   */
  public LoggingStep(boolean verbose, String[][] levels) {
    super();
    LoggingConfiguration.setVerbose(verbose);
    if(levels != null) {
      for(String[] pair : levels) {
        try {
          if(pair.length == 1) {
            // Try to parse as level:
            try {
              Level level = Level.parse(pair[0]);
              LoggingConfiguration.setDefaultLevel(level);
            }
            catch(IllegalArgumentException e) {
              LoggingConfiguration.setLevelFor(pair[0], Level.FINEST.getName());
            }
          }
          else if(pair.length == 2) {
            LoggingConfiguration.setLevelFor(pair[0], pair[1]);
          }
          else {
            throw new AbortException("Invalid logging settings");
          }
        }
        catch(IllegalArgumentException e) {
          logger.warning("Invalid logging statement for package " + pair[0] + ": " + e.getMessage());
        }
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected boolean verbose = false;

    protected String[][] levels = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final Flag verboseF = new Flag(OptionID.VERBOSE_FLAG);
      if(config.grab(verboseF)) {
        verbose = verboseF.getValue();
      }
      final StringParameter debugP = new StringParameter(OptionID.DEBUG, true);
      if(config.grab(debugP)) {
        String[] opts = debugP.getValue().split(",");
        levels = new String[opts.length][];
        int i = 0;
        for(String opt : opts) {
          String[] chunks = opt.split("=");
          if(chunks.length != 1 && chunks.length != 2) {
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