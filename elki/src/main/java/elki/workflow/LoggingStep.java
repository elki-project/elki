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
package elki.workflow;

import elki.application.AbstractApplication;
import elki.logging.Logging.Level;
import elki.logging.LoggingConfiguration;
import elki.utilities.optionhandling.AbstractParameterizer;
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
    AbstractApplication.Parameterizer.applyLoggingLevels(levels);
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
    protected java.util.logging.Level verbose = Level.WARNING;

    /**
     * Enable logging levels manually
     */
    protected String[][] levels = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      verbose = AbstractApplication.Parameterizer.parseVerbose(config);
      levels = AbstractApplication.Parameterizer.parseDebugParameter(config);
    }

    @Override
    protected LoggingStep makeInstance() {
      return new LoggingStep(verbose, levels);
    }
  }
}
