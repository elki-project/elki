package de.lmu.ifi.dbs.elki.workflow;

import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
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
 * @apiviz.uses de.lmu.ifi.dbs.elki.logging.LoggingConfiguration
 */
public class LoggingStep implements WorkflowStep {
  /**
   * Logger
   */
  private final static Logging logger = Logging.getLogger(LoggingStep.class);

  public LoggingStep(boolean verbose, String[][] levels) {
    super();
    LoggingConfiguration.setVerbose(verbose);
    if(levels != null) {
      for(String[] pair : levels) {
        try {
          if(pair.length == 1) {
            LoggingConfiguration.setLevelFor(pair[0], Level.FINEST.getName());
          }
          else if(pair.length == 2) {
            LoggingConfiguration.setLevelFor(pair[0], pair[1]);
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
      final Flag verboseF = new Flag(OptionID.ALGORITHM_VERBOSE);
      if(config.grab(verboseF)) {
        verbose = verboseF.getValue();
      }
      final StringParameter debugP = new StringParameter(OptionID.DEBUG, true);
      if(config.grab(debugP)) {
        String[] opts = debugP.getValue().split(",");
        for(String opt : opts) {
          String[] chunks = opt.split("=");
          if(chunks.length != 1 && chunks.length != 2) {
            config.reportError(new WrongParameterValueException(debugP, debugP.getValue(), "Invalid debug option."));
            break;
          }
        }
      }
    }

    @Override
    protected LoggingStep makeInstance() {
      return new LoggingStep(verbose, levels);
    }
  }
}