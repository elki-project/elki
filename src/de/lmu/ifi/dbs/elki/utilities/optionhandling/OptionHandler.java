package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;

/**
 * Provides an OptionHandler for holding the given options.
 * <p/>
 * <p/>
 * The options specified are stored in a &lt;String,Option&gt;-Map (
 * {@link java.util.Map}) with the names of the options being the keys. New
 * options can be added by using the method {@link #put(Option)}.
 * <p/>
 * 
 * @author Arthur Zimek
 */
public class OptionHandler extends AbstractLoggable {
  /**
   * The newline-String dependent on the system.
   */
  public final static String NEWLINE = System.getProperty("line.separator");

  /**
   * Prefix of option markers on the command line.
   * <p/>
   * The option markers are supposed to be given on the command line with leading
   * -.
   */
  public static final String OPTION_PREFIX = "-";

  /**
   * Holds the parameter array as given to the last call of
   * {@link #grabOptions(String[]) grabOptions(String[])}.
   */
  private String[] currentParameters = new String[0];

  /**
   * Contains the optionHandler's options, the option names are used as the
   * map's keys
   */
  private Map<String, Option<?>> parameters = new TreeMap<String, Option<?>>();

  /**
   * Contains constraints addressing several parameters
   */
  private List<GlobalParameterConstraint> globalParameterConstraints = new ArrayList<GlobalParameterConstraint>();

  /**
   * Provides an OptionHandler.
   * <p/>
   * The options are specified in the given TreeMap with the option names being
   * as keys. Leading &quot;-&quot; do not have to be specified since
   * OptionHandler will provide them.
   */
  public OptionHandler() {
    super(false);
  }

  /**
   * Reads the options out of a given String-array (usually the args of any
   * main-method).
   * 
   * @param currentOptions an array of given options, flags without values. E.g.
   *        the args of some main-method. In this array every option should have
   *        a leading &quot;-&quot;.
   * @return String[] an array containing the unexpected parameters in the given
   *         order. Parameters are treated as unexpected if they are not known
   *         to the OptionHandler or if they were already read.
   * @throws ParameterException if a parameter, for which a value is
   *         required, has none (e.g. because the next value is itself some
   *         option)
   */
  public List<String> grabOptions(List<String> currentOptions) throws ParameterException {
    ArrayList<String> unexpectedParameters = new ArrayList<String>();
    ArrayList<String> parameterArray = new ArrayList<String>();

    for(int i = 0; i < currentOptions.size(); i++) {
      if(!currentOptions.get(i).startsWith(OPTION_PREFIX)) {
        throw new NoParameterValueException(currentOptions.get(i) + " is no parameter!");
      }

      // get the option without the option prefix -
      String noPrefixOption = currentOptions.get(i).substring(OPTION_PREFIX.length());

      // check if parameter-Map contains the current option
      if(parameters.containsKey(noPrefixOption)) {

        Option<?> current = parameters.get(noPrefixOption);
        // check if the option is a parameter or a flag
        if(current instanceof Parameter) {

          // check if there is a next element in the option array and
          // if it's indeed an option value
          if(i + 1 < currentOptions.size() /*
                                            * && !currentOptions[i +
                                            * 1].startsWith(OPTION_PREFIX)
                                            */) {

            // set the parameter value if the value is not already set
            if(!current.isSet()) {
              current.setValue(currentOptions.get(i + 1));
              parameterArray.add(currentOptions.get(i));
              parameterArray.add(currentOptions.get(i + 1));
              i++;
            }
            else { // parameter is already set!!
              unexpectedParameters.add(currentOptions.get(i));
              unexpectedParameters.add(currentOptions.get(i + 1));
              i++;
            }
          }
          // next element seems to be a parameter of flag identifier!
          else {
            throw new NoParameterValueException("Parameter " + currentOptions.get(i) + " requires a parameter value!");
          }
        }
        // option is of type flag
        else if(current instanceof Flag) {
          // set the flag if it's not already set to true
          if(!current.isSet()) {

            // check if the next element is wrongly a parameter
            // value
            if(i + 1 < currentOptions.size() && !currentOptions.get(i + 1).startsWith(OPTION_PREFIX)) {
              throw new NoParameterValueException("Flag " + currentOptions.get(i) + " requires no parameter-value! " + "(read parameter-value: " + currentOptions.get(i + 1) + ")");
            }
            // set the flag
            current.setValue(Flag.SET);
            parameterArray.add(currentOptions.get(i));

          }
          else { // flag was already set!
            unexpectedParameters.add(currentOptions.get(i));

          }
        }

        // unexpected option type
        else {
          // FIXME unexpected option type
        }

      }

      // unexpected option
      else {
        unexpectedParameters.add(currentOptions.get(i));
        if(i + 1 < currentOptions.size() && !currentOptions.get(i + 1).startsWith(OPTION_PREFIX)) {
          unexpectedParameters.add(currentOptions.get(i + 1));
          i++;
        }
      }
    }

    currentParameters = new String[parameterArray.size()];
    currentParameters = parameterArray.toArray(currentParameters);

    if(logger.isDebuggingFiner()) {
      for(Map.Entry<String, Option<?>> option : parameters.entrySet()) {
        logger.debugFiner("option " + option.getKey() + " has value " + option.getValue().getValue());
      }
    }

    setDefaultValues();

    checkNonOptionalParameters();
    checkGlobalParameterConstraints();

    return unexpectedParameters;
  }

  /**
   * Returns a copy of the parameter array as given to the last call of
   * {@link #grabOptions(String[]) grabOptions(String[])}. The resulting array
   * will contain only those values that were recognized and needed by this
   * OptionHandler.
   * 
   * @return a copy of the parameter array as given to the last call of
   *         {@link #grabOptions(String[]) grabOptions(String[])}
   */
  public String[] getParameterArray() {
    String[] parameterArray = new String[currentParameters.length];
    System.arraycopy(currentParameters, 0, parameterArray, 0, currentParameters.length);
    return parameterArray;
  }

  /**
   * Adds the given option to the OptionHandler's current parameter map.
   * 
   * @param option Option to be added.
   */
  public void put(Option<?> option) {
    Option<?> put = this.parameters.put(option.getName(), option);
    if(put != null) {
      try {
        logger.warning("Parameter " + option.getName() + " has been already set before, overwrite old value. " + "(old value: " + put.getValue().toString() + ", new value: " + option.getValue().toString() + ")");
      }
      catch(UnusedParameterException e) {
        logger.exception(e);
      }
    }
  }

  /**
   * Add a global parameter constraint.
   * 
   * @param gpc constraint
   */
  public void setGlobalParameterConstraint(GlobalParameterConstraint gpc) {
    globalParameterConstraints.add(gpc);
  }

  /**
   * Removes the given option from the OptionHandler's parameter map.
   * 
   * @param optionName Option to be removed.
   * @throws UnusedParameterException If there is no such option.
   */
  public void remove(String optionName) throws UnusedParameterException {
    Option<?> removed = this.parameters.remove(optionName);
    Logger.getLogger(this.getClass().getName()).finer("removed " + removed.getName());
    if(removed == null) {
      throw new UnusedParameterException("Cannot remove parameter " + optionName + " because it has not been set before.");
    }
  }

  /**
   * Get the available options of this handler.
   * 
   * @return new array of options.
   */
  public Option<?>[] getOptions() {
    return parameters.values().toArray(new Option<?>[] {});
  }

  /**
   * Checks all parameters not specified in currentParameters for default values
   * and sets them, if existing
   */
  private void setDefaultValues() {

    for(Option<?> opt : parameters.values()) {

      if(opt instanceof Parameter && !opt.isSet() && ((Parameter<?, ?>) opt).hasDefaultValue()) {

        ((Parameter<?, ?>) opt).setDefaultValueToValue();
      }
    }
  }

  private void checkNonOptionalParameters() throws ParameterException {

    Vector<Option<?>> notOptional = new Vector<Option<?>>();

    for(Option<?> opt : parameters.values()) {
      if(opt instanceof Parameter && !opt.isSet() && !((Parameter<?, ?>) opt).isOptional()) {
        notOptional.add(opt);
      }
    }
    if(!notOptional.isEmpty()) {

      if(notOptional.size() == 1) {
        throw new UnspecifiedParameterException("Aborted. Parameter " + notOptional.get(0).getName() + " requires parameter value.");
      }
      StringBuffer buffer = new StringBuffer();
      for(int i = 0; i < notOptional.size(); i++) {
        buffer.append(notOptional.get(i).getName());
        if(i != notOptional.size() - 1) {
          buffer.append(",");
        }
      }
      throw new UnspecifiedParameterException("Aborted. Parameters " + buffer.toString() + " require parameter values.");
    }
  }

  protected void checkGlobalParameterConstraints() throws ParameterException {

    for(GlobalParameterConstraint gbc : globalParameterConstraints) {
      gbc.test();
    }
  }

  // TODO: immutable access?
  public List<GlobalParameterConstraint> getGlobalParameterConstraints() {
    return globalParameterConstraints;
  }
}