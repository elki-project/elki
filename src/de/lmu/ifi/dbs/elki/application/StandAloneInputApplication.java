package de.lmu.ifi.dbs.elki.application;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.io.File;

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
  protected StandAloneInputApplication() {
    super();
    INPUT_PARAM.setShortDescription(getInputDescription());
    addOption(INPUT_PARAM);
  }

  /**
   * Calls the super method and sets additionally the value of the parameter
   * {@link #INPUT_PARAM}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // input
    input = INPUT_PARAM.getValue();
    return remainingParameters;
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
