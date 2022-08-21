/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.workflow;

import elki.application.AbstractApplication;
import elki.logging.Logging.Level;
import elki.logging.LoggingConfiguration;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;

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
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param levels Level settings array
   */
  public LoggingStep(java.util.logging.Level verbose, String[][] levels) {
    super();
    LoggingConfiguration.setVerbose(verbose);
    AbstractApplication.Par.applyLoggingLevels(levels);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Verbose mode.
     */
    protected java.util.logging.Level verbose = Level.WARNING;

    /**
     * Enable logging levels manually
     */
    protected String[][] levels = null;

    @Override
    public void configure(Parameterization config) {
      verbose = AbstractApplication.Par.parseVerbose(config);
      levels = AbstractApplication.Par.parseDebugParameter(config);
    }

    @Override
    public LoggingStep make() {
      return new LoggingStep(verbose, levels);
    }
  }
}
