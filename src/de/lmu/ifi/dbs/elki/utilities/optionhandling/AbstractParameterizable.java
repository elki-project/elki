package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Abstract superclass for classes parameterizable. Provides the option handler
 * and the parameter array.
 * 
 * @author Elke Achtert
 */
public abstract class AbstractParameterizable extends AbstractLoggable implements Parameterizable {

  /**
   * OptionHandler for handling options.
   */
  protected OptionHandler optionHandler;

  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];

  /**
   * Creates a new AbstractParameterizable that provides the option handler and
   * the parameter array.
   */
  public AbstractParameterizable() {
    super(LoggingConfiguration.DEBUG);
    optionHandler = new OptionHandler(this.getClass().getName());
  }

  /**
   * Adds the given Option to the set of Options known to this Parameterizable.
   * 
   * @param option the Option to add to the set of known Options of this
   *        Parameterizable
   */
  protected void addOption(Option<?> option) {
    this.optionHandler.put(option);
  }

  /**
   * Deletes the given Option from the set of Options known to this
   * Parameterizable.
   * 
   * @param option the Option to remove from the set of Options known to this
   *        Parameterizable
   * @throws UnusedParameterException if the given Option is unknown
   */
  protected void deleteOption(Option<?> option) throws UnusedParameterException {
    this.optionHandler.remove(option.getName());
  }

  /**
   * Grabs all specified options from the option handler. Any extending class
   * should call this method first and return the returned array without further
   * changes, but after setting further required parameters. An example for
   * overwriting this method taking advantage from the previously (in
   * superclasses) defined options would be:
   * <p/>
   * 
   * <pre>
   * {
   *   String[] remainingParameters = super.setParameters(args);
   *   // set parameters for your class
   *   // for example like this:
   *   if(isSet(MY_PARAM_VALUE_PARAM))
   *   {
   *      myParamValue = getParameterValue(MY_PARAM_VALUE_PARAM);
   *   }
   *   .
   *   .
   *   .
   *   return remainingParameters;
   *   // or in case of attributes requesting parameters themselves
   *   // return parameterizableAttribbute.setParameters(remainingParameters);
   * }
   * </pre>
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Sets the difference of the first array minus the second array as the
   * currently set parameter array.
   * 
   * @param complete the complete array
   * @param part an array that contains only elements of the first array
   */
  protected final void rememberParametersExcept(String[] complete, String[] part) {
    currentParameterArray = OptionUtil.parameterDifference(complete, part);
  }

  /**
   * Compatibility wrapper for not yet adapted code.
   * 
   * Depreciated: Renamed to {@link #rememberParametersExcept}
   * 
   * @param complete the complete array
   * @param part parameters to not set from the first array (only!)
   */
  @Deprecated
  protected final void setParameters(String[] complete, String[] part) {
    rememberParametersExcept(complete, part);
  }

  /*
   * See: {@link Parameterizable#getParameters()}
   */
  public final String[] getParameters() {
    String[] param = new String[currentParameterArray.length];
    System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
    return param;
  }

  /**
   * Returns the settings of all options assigned to the option handler.
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = new ArrayList<AttributeSettings>();
    AttributeSettings mySettings = new AttributeSettings(this);
    optionHandler.addOptionSettings(mySettings);
    settings.add(mySettings);
    return settings;
  }

  /**
   * Returns a description of the class and the required parameters by calling
   * {@code optionHandler.usage("", false)}. Subclasses may need to overwrite
   * this method for a more detailed description.
   * 
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler#usage
   */
  public String parameterDescription() {
    return optionHandler.usage("", false);
  }

  /**
   * @see OptionHandler#getOptions()
   */
  // TODO: remove - only used in guidraft1?
  public Option<?>[] getPossibleOptions() {
    return optionHandler.getOptions();
  }

  /**
   * @see OptionHandler#checkGlobalParameterConstraints()
   */
  // TODO: remove - only used in guidraft1?
  public void checkGlobalParameterConstraints() throws ParameterException {
    this.optionHandler.checkGlobalParameterConstraints();
  }

  /**
   * Get all possible options.
   * 
   * @param collection existing collection to add to.
   */
  // TODO: not yet used.
  public void collectOptions(List<Pair<Parameterizable, Option<?>>> collection) {
    Option<?>[] opts = this.optionHandler.getOptions();
    for(Option<?> o : opts) {
      collection.add(new Pair<Parameterizable, Option<?>>(this, o));
    }
  }
}
