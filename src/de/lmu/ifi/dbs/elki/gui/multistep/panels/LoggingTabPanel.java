package de.lmu.ifi.dbs.elki.gui.multistep.panels;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Panel to handle logging
 * 
 * @author Erich Schubert
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
    StringParameter debugParam = new StringParameter(OptionID.DEBUG);
    debugParam.setOptional(true);
    Flag verboseFlag = new Flag(OptionID.VERBOSE_FLAG);
    // Verbose mode is a lot simpler
    if (config.grab(verboseFlag) && verboseFlag.isTrue()) {
      LoggingConfiguration.setVerbose(true);
    }
    if (config.grab(debugParam)) {
      try {
        LoggingUtil.parseDebugParameter(debugParam);
      } catch (WrongParameterValueException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
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
