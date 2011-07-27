package de.lmu.ifi.dbs.elki.workflow;

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