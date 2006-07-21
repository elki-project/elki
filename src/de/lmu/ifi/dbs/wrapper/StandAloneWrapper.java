package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * StandAloneWrapper sets additionally to the flags set by AbstractWrapper
 * the output parameter out. <p/>
 * Any Wrapper class that makes use of these flags may extend this class. Beware to
 * make correct use of parameter settings via optionHandler as commented with
 * constructor and methods.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class StandAloneWrapper extends AbstractWrapper {

  /**
   * Parameter output.
   */
  public static final String OUTPUT_P = "out";

  /**
   * Description for parameter output.
   */
  public static String OUTPUT_D = "<filename>output file";

  /**
   * The name of the output file.
   */
  private String output;

  /**
   * Sets additionally to the parameters set by the super class the
   * parameter for out in the parameter map. Any extending
   * class should call this constructor, then add further parameters. Any
   * non-abstract extending class should finally initialize optionHandler like
   * this:
   * <p/>
   * <pre>
   *  {
   *      parameterToDescription.put(YOUR_PARAMETER_NAME+OptionHandler.EXPECTS_VALUE,YOUR_PARAMETER_DESCRIPTION);
   *      ...
   *      optionHandler = new OptionHandler(parameterToDescription,yourClass.class.getName());
   *  }
   * </pre>
   */
  protected StandAloneWrapper() {
    super();
    optionHandler.put(OUTPUT_P, new Parameter(OUTPUT_P,OUTPUT_D));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // output
    output = optionHandler.getOptionValue(OUTPUT_P);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(OUTPUT_P, output);
    return settings;
  }

  /**
   * Returns the output string.
   *
   * @return the output string
   */
  public final String getOutput() {
    return output;
  }
}
