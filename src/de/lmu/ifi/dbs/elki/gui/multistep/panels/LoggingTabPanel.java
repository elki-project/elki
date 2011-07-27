package de.lmu.ifi.dbs.elki.gui.multistep.panels;

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
    StringParameter debugParam = new StringParameter(OptionID.DEBUG, true);
    Flag verboseFlag = new Flag(OptionID.VERBOSE_FLAG);
    // Verbose mode is a lot simpler
    if (config.grab(verboseFlag) && verboseFlag.getValue()) {
      LoggingConfiguration.setVerbose(true);
    }
    if (config.grab(debugParam)) {
      try {
        LoggingUtil.parseDebugParameter(debugParam);
      }
      catch(WrongParameterValueException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
      }
    }
  }
  
  @Override
  protected void executeStep() {
    // Pass - we don't need to do anything
  }

  @Override
  protected String getStatus() {
    // We're always complete, too!
    return STATUS_COMPLETE;
  }
}