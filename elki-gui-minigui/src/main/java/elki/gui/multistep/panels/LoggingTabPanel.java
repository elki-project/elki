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
package elki.gui.multistep.panels;

import elki.application.AbstractApplication;
import elki.logging.Logging.Level;
import elki.logging.LoggingConfiguration;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Panel to handle logging
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class LoggingTabPanel extends ParameterTabPanel {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public LoggingTabPanel() {
    super();
  }

  @Override
  protected synchronized void configureStep(Parameterization config) {
    StringParameter debugParam = new StringParameter(AbstractApplication.Parameterizer.DEBUG_ID) //
        .setOptional(true);
    Flag verboseFlag = new Flag(AbstractApplication.Parameterizer.VERBOSE_ID);
    // Verbose mode is a lot simpler
    if (config.grab(verboseFlag) && verboseFlag.isTrue()) {
      LoggingConfiguration.setVerbose(Level.VERBOSE);
    }
    // FIXME: add second level of verbosity!
    if (config.grab(debugParam)) {
      try {
        AbstractApplication.Parameterizer.parseDebugParameter(debugParam);
      } catch (WrongParameterValueException e) {
       elki.logging.LoggingUtil.exception(e);
      }
    }
  }

  @Override
  protected void executeStep() {
    // Pass - we don't need to do anything
  }

  @Override
  protected Status getStatus() {
    // We're always complete, too!
    return Status.STATUS_COMPLETE;
  }
}
