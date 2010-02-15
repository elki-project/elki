package de.lmu.ifi.dbs.elki.application;

import java.io.File;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * StandAloneInputApplication extends StandAloneApplication and sets
 * additionally the parameter in. Any Application class that makes use of these
 * flags may extend this class.
 * 
 * @author Elke Achtert
 */
public abstract class StandAloneInputApplication extends StandAloneApplication {

  /**
   * OptionID for {@link #INPUT_PARAM}
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("app.in", "");

  /**
   * Parameter that specifies the name of the input file.
   * <p>
   * Key: {@code -app.in}
   * </p>
   */
  private final FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);

  /**
   * Holds the value of {@link #INPUT_PARAM}.
   */
  private File input;

  /**
   * Adds parameter {@link #INPUT_PARAM} to the option handler additionally to
   * parameters of super class.
   */
  protected StandAloneInputApplication(Parameterization config) {
    super(config);
    INPUT_PARAM.setShortDescription(getInputDescription());
    if (config.grab(this, INPUT_PARAM)) {
      input = INPUT_PARAM.getValue();
    }
  }

  /**
   * Returns the input file.
   * 
   * @return the input file
   */
  public final File getInput() {
    return input;
  }

  /**
   * Returns the description for the input parameter.
   * 
   * @return the description for the input parameter
   */
  public abstract String getInputDescription();
}
