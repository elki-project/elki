package de.lmu.ifi.dbs.elki.workflow;

import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
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
 * @apiviz.uses de.lmu.ifi.dbs.elki.logging.LoggingConfiguration
 */
public class LoggingStep implements WorkflowStep {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public LoggingStep(Parameterization config) {
    config = config.descend(this);
    // Verbose flag.
    final Flag VERBOSE_FLAG = new Flag(OptionID.ALGORITHM_VERBOSE);
    if(config.grab(VERBOSE_FLAG)) {
      LoggingConfiguration.setVerbose(VERBOSE_FLAG.getValue());
    }
    final StringParameter DEBUG_PARAM = new StringParameter(OptionID.DEBUG, true);
    if(config.grab(DEBUG_PARAM)) {
      String[] opts = DEBUG_PARAM.getValue().split(",");
      for(String opt : opts) {
        try {
          String[] chunks = opt.split("=");
          if(chunks.length == 1) {
            LoggingConfiguration.setLevelFor(chunks[0], Level.FINEST.getName());
          }
          else if(chunks.length == 2) {
            LoggingConfiguration.setLevelFor(chunks[0], chunks[1]);
          }
          else {
            throw new IllegalArgumentException("More than one '=' in debug parameter.");
          }
        }
        catch(IllegalArgumentException e) {
          config.reportError(new WrongParameterValueException(DEBUG_PARAM, DEBUG_PARAM.getValue(), "Could not process value.", e));
        }
      }
    }
  }
}