package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.output.PrettyPrinter;

/**
 * Provides an OptionHandler for holding the given options.<p/>
 * 
 * The options
 * specified are stored in a &lt;String,Option&gt;-Map ({@link java.util.Map})
 * with the names of the options being the keys. New options can be added by
 * using the method {@link #put(Option)}.
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
   * Prefix of optionmarkers on the commandline. <p/> The optionmarkers are
   * supposed to be given on the commandline with leading -.
   */
  public static final String OPTION_PREFIX = "-";

  /**
   * The programCall as it should be denoted in an eventual usage-message.
   */
  private String programCall;

  /**
   * Holds the parameter array as given to the last call of
   * {@link #grabOptions(String[]) grabOptions(String[])}.
   */
  private String[] currentParameters = new String[0];

  /**
   * Contains the optionHandler's options, the option names are used as the
   * map's keys
   */
  private Map<String, Option<?>> parameters;

  /**
   * Contains constraints addressing several parameters
   */
  private List<GlobalParameterConstraint> globalParameterConstraints;

  /**
   * Provides an OptionHandler. <p/> The options are specified in the given
   * TreeMap with the option names being as keys. Leading &quot;-&quot; do not
   * have to be specified since OptionHandler will provide them.
   *
   * @param parameters  Map containing the options
   * @param programCall String for the program-call using this OptionHandler (for
   *                    usage in usage(String))
   */
  public OptionHandler(Map<String, Option<?>> parameters, String programCall) {
    super(LoggingConfiguration.DEBUG);
    this.parameters = parameters;
    this.programCall = programCall;
    this.globalParameterConstraints = new ArrayList<GlobalParameterConstraint>();
  }

  /**
   * Reads the options out of a given String-array (usually the args of any
   * main-method).
   *
   * @param currentOptions an array of given options, flags without values. E.g. the args
   *                       of some main-method. In this array every option should have a
   *                       leading &quot;-&quot;.
   * @return String[] an array containing the unexpected parameters in the
   *         given order. Parameters are treated as unexpected if they are not
   *         known to the optionhandler or if they were already read.
   * @throws NoParameterValueException if a parameter, for which a value is required, has none (e.g.
   *                                   because the next value is itself some option)
   */
  public String[] grabOptions(String[] currentOptions) throws NoParameterValueException, ParameterException {

    List<String> unexpectedParameters = new ArrayList<String>();
    List<String> parameterArray = new ArrayList<String>();

    for (int i = 0; i < currentOptions.length; i++) {
      if (!currentOptions[i].startsWith(OPTION_PREFIX)) {
        throw new NoParameterValueException(currentOptions[i] + " is no parameter!");
      }

      // get the option without the option prefix -
      String noPrefixOption = currentOptions[i].substring(1);

      // check if parameter-Map contains the current option
      if (parameters.containsKey(noPrefixOption)) {

        Option<?> current = parameters.get(noPrefixOption);
        // check if ithe option is a parameter or a flag
        if (current instanceof Parameter) {

          // check if there is a next element in the option array and
          // if it's indeed an option value
          if (i + 1 < currentOptions.length /*&& !currentOptions[i + 1].startsWith(OPTION_PREFIX)*/) {

            // set the parameter value if the value is not already set
            if (!current.isSet()) {
              current.setValue(currentOptions[i + 1]);
              parameterArray.add(currentOptions[i]);
              parameterArray.add(currentOptions[i + 1]);
              i++;                           
            }
            else { // parameter is already set!!
              unexpectedParameters.add(currentOptions[i]);
              unexpectedParameters.add(currentOptions[i + 1]);
              i++;
            }
          }
          // next element seems to be a paramter of flag identifier!
          else {
            throw new NoParameterValueException("Parameter " + currentOptions[i] + " requires a parameter value!");
          }
        }
        // option is of type flag
        else if (current instanceof Flag) {
          // set the flag if it's not already set to true
          if (!current.isSet()) {

            // check if the next element is wrongly a parameter
            // value
            if (i + 1 < currentOptions.length && !currentOptions[i + 1].startsWith(OPTION_PREFIX)) {
              throw new NoParameterValueException("Flag " + currentOptions[i] + " requires no parameter-value! "
                                                  + "(read parameter-value: " + currentOptions[i + 1] + ")");
            }
            // set the flag
            current.setValue(Flag.SET);
            parameterArray.add(currentOptions[i]);

          }
          else { // flag was already set!
            unexpectedParameters.add(currentOptions[i]);

          }
        }

        // unexpected option type
        else {
          // TODO unexpected option type
        }

      }

      // unexpected option
      else {
        unexpectedParameters.add(currentOptions[i]);
        if (i + 1 < currentOptions.length && !currentOptions[i + 1].startsWith(OPTION_PREFIX)) {
          unexpectedParameters.add(currentOptions[i + 1]);
          i++;
        }
      }
    }

    currentParameters = new String[parameterArray.size()];
    currentParameters = parameterArray.toArray(currentParameters);
    String[] remain = new String[unexpectedParameters.size()];
    unexpectedParameters.toArray(remain);

//    if (this.debug) { // TODO doesn't work!! has to be checked!
//      for (Map.Entry<String, Option<?>> option : parameters.entrySet()) {
//        debugFine("option " + option.getKey() + " has value " + option.getValue().getValue());
//
//      }
//
//    }

    setDefaultValues();

    checkNonOptionalParameters();
    checkGlobalParameterConstraints();

    return remain;
  }

  /**
   * Returns the value of the given option, if there is one.
   *
   * @param option option to get value of. The option should be asked for without
   *               leading &quot;-&quot; or closing &quot;:&quot;.
   * @return String value of given option
   * @throws UnusedParameterException  if the given option is not used
   * @throws NoParameterValueException if the given option is only a flag and should therefore have
   *                                   no value
   */
@SuppressWarnings("unchecked")
public <T> T getOptionValue(String option) throws UnusedParameterException, NoParameterValueException {

    if (parameters.containsKey(option)) {
      try {
        return (T) parameters.get(option).getValue();
      }
      catch (ClassCastException e) {
        throw new NoParameterValueException("Parameter " + option + " is flag which has no value.", e);
      }
    }
    else {
      throw new UnusedParameterException("Parameter " + option + " is not assigned to the option handler.");
    }
  }

  /**
   * Returns the value of the given parameter, if there is one.
   *
   * @param parameter parameter to get value of
   * @return value of given option
   * @throws UnusedParameterException if the given option is not used
   */
@SuppressWarnings("unchecked")
public <T> T getParameterValue(Parameter<T,?> parameter) throws UnusedParameterException {
    if (parameters.containsKey(parameter.getName())) {
      return (T) parameters.get(parameter.getName()).getValue();
    }
    else {
      throw new UnusedParameterException("Parameter " + parameter + " is not assigned to the option handler.");
    }
  }

  public Option<?> getOption(String name) throws UnusedParameterException {
    if (parameters.containsKey(name)) {
      return parameters.get(name);
    }
    throw new UnusedParameterException("Parameter " + name + " is not assigned to the option handler.");
  }

  /**
   * Returns true if the value of the given option is set, false otherwise.
   *
   * @param option The option should be asked for without leading &quot;-&quot;
   *               or closing &quot;:&quot;.
   * @return boolean true if the value of the given option is set, false
   *         otherwise
   */
  public boolean isSet(String option) {
    if (parameters.containsKey(option)) {
      return parameters.get(option).isSet();
    }
    return false;
  }

  /**
   * Returns true if the value of the given option is set, false otherwise.
   *
   * @param option The option should be asked for without leading &quot;-&quot;
   *               or closing &quot;:&quot;.
   * @return boolean true if the value of the given option is set, false
   *         otherwise
   */
  public boolean isSet(Option<?> option) {
    return isSet(option.getName());
  }

  /**
   * Returns an usage-String according to the descriptions given in the
   * constructor. Same as <code>usage(message,true)</code>.
   *
   * @param message some error-message, if needed (may be null or empty String)
   * @return String an usage-String according to the descriptions given in the
   *         constructor.
   */
  public String usage(String message) {
    return usage(message, true);
  }

  /**
   * Returns an usage-String according to the descriptions given in the
   * constructor.
   *
   * @param message    some error-message, if needed (may be null or empty String)
   * @param standalone whether the class using this OptionHandler provides a main
   *                   method
   * @return String an usage-String according to the descriptions given in the
   *         constructor.
   */
  public String usage(String message, boolean standalone) {
    String empty = "";
    String space = " ";
    int lineLength = 80;
    String paramLineIndent = "        ";
    StringBuffer messageBuffer = new StringBuffer();
    if (!(message == null || message.equals(empty))) {
      messageBuffer.append(message).append(NEWLINE);
    }

    String[] options = new String[parameters.size()];

    String[] shortDescriptions = new String[options.length];

    String[] longDescriptions = new String[options.length];

    int longestShortline = 0;
    StringBuffer paramLine = new StringBuffer();
    int currentLength = programCall.length();

    int counter = 0;
    for (Map.Entry<String, Option<?>> option : parameters.entrySet()) {

      String currentOption = option.getKey();

      String desc = option.getValue().getDescription();

      String shortDescription = empty;
      String longDescription = desc;

      if (option.getValue() instanceof Parameter) {
        // shortDescription = desc.substring(desc.indexOf("<"), desc
        // .indexOf(">") + 1);
        // longDescription = desc.substring(desc.indexOf(">") + 1);
        currentOption = currentOption.substring(0);
      }
      currentOption = OPTION_PREFIX + currentOption;
      options[counter] = currentOption;
      shortDescriptions[counter] = shortDescription;
      longDescriptions[counter] = longDescription;
      longestShortline = Math.max(longestShortline, currentOption.length() + shortDescription.length() + 1);
      currentLength = currentLength + currentOption.length() + 2 + shortDescription.length();
      if (currentLength > lineLength) {
        paramLine.append(NEWLINE);
        paramLine.append(paramLineIndent);
        currentLength = paramLineIndent.length();
      }
      paramLine.append(currentOption);
      paramLine.append(space);
      paramLine.append(shortDescription);
      paramLine.append(space);

      counter++;
    }

    String mark = " : ";
    String indent = "  ";
    int firstCol = indent.length() + longestShortline;
    int secondCol = mark.length();
    StringBuffer descriptionIndent = new StringBuffer();
    for (int i = 0; i < firstCol + secondCol; i++) {
      descriptionIndent.append(space);
    }
    int thirdCol = lineLength - (firstCol + secondCol);
    int[] cols = {firstCol, secondCol, thirdCol};
    PrettyPrinter prettyPrinter = new PrettyPrinter(cols, empty);
    char fillchar = ' ';

    if (standalone) {
      messageBuffer.append("Usage: ");
      messageBuffer.append(NEWLINE);
    }
    messageBuffer.append(programCall);
    if (standalone) {
      messageBuffer.append(space);
      messageBuffer.append(paramLine);
    }
    messageBuffer.append(NEWLINE);

    for (int i = 0; i < options.length; i++) {
      StringBuffer option = new StringBuffer();
      option.append(indent);
      option.append(options[i]);
      option.append(space);
      option.append(shortDescriptions[i]);
      Vector<String> lines = prettyPrinter.breakLine(longDescriptions[i], 2);
      String[] firstline = {option.toString(), mark, lines.firstElement()};
      messageBuffer.append(prettyPrinter.formattedLine(firstline, fillchar)).append(NEWLINE);
      for (int l = 1; l < lines.size(); l++) {
        messageBuffer.append(descriptionIndent).append(lines.get(l)).append(NEWLINE);
      }
    }
    return messageBuffer.toString();
  }

  /**
   * Returns a copy of the parameter array as given to the last call of
   * {@link #grabOptions(String[]) grabOptions(String[])}. The resulting
   * array will contain only those values that were recognized and needed by
   * this OptionHandler.
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
   * Adds the given parameter map to the OptionHandler's current parameter
   * map.
   *
   * @param params Parameter map to be added.
   * @deprecated
   */
  @Deprecated
public void put(Map<String, Option<?>> params) {
    this.parameters.putAll(params);
  }

  /**
   * Adds the given option to the OptionHandler's current parameter map.
   *
   * @param option Option to be added.
   */
  public void put(Option<?> option) {
    Option<?> put = this.parameters.put(option.getName(), option);
    if (put != null) {
      try {
        warning("Parameter " + option.getName() + " has been already set before, overwrite old value. (old value: " + put.getValue().toString() +
                ", new value: " + option.getValue().toString());
      }
      catch (UnusedParameterException e) {
        this.exception(e.getMessage(),e);
      }
    }
  }

  /**
   * Adds the given option with the given name to the OptionHandler's current
   * parameter map.
   *
   * @param name   The name of the option to be added.
   * @param option The option to be added.
   * @deprecated
   */
  @Deprecated
public void put(String name, Option<?> option) {
    Option<?> put = this.parameters.put(name, option);
    if (put != null) {
      try {
        warning("Parameter " + option.getName() + " has been already set before, overwrite old value. (old value: " + put.getValue().toString() +
                ", new value: " + option.getValue().toString());
      }
      catch (UnusedParameterException e) {
        this.exception(e.getMessage(),e);
      }
    }
  }

  public void setGlobalParameterConstraint(GlobalParameterConstraint gpc) {
    globalParameterConstraints.add(gpc);
  }

  /**
   * Sets the OptionHandler's programmCall (@link #programCall} to the given
   * call.
   *
   * @param call The new programm call.
   */
  public void setProgrammCall(String call) {
    programCall = call;
  }

  /**
   * Removes the given option from the OptionHandler's parameter map.
   *
   * @param optionName Option to be removed.
   * @throws UnusedParameterException If there is no such option.
   */
  public void remove(String optionName) throws UnusedParameterException {
    Option<?> removed = this.parameters.remove(optionName);
    debugFiner("removed "+removed.getName());
    if (removed == null) {
      throw new UnusedParameterException("Cannot remove parameter " + optionName + " because it has not been set before.");
    }
  }

  
public Option<?>[] getOptions() {
    return parameters.values().toArray(new Option[]{});
  }

  /**
   * Adds the settings of the options assigned to this option handler to the
   * specified attribute settings.
   *
   * @param settings the attribute settings to add
   *                 the settings of the options assigned to this option handler to
   * @throws UnusedParameterException
   */
  public void addOptionSettings(AttributeSettings settings) throws UnusedParameterException {
    for (Option<?> option : parameters.values()) {
      if (option instanceof Flag) {
        settings.addSetting(option.getName(), Boolean.toString(isSet(option.getName())));
      }
      else {
        Object value = option.getValue();
        if (value != null)
          settings.addSetting(option.getName(), value.toString());
        else
          settings.addSetting(option.getName(), "null");
      }
    }
  }


  /**
   * Checks all parameters not specified in currentParameters for default values
   * and sets them, if existing
   * 
   */
  private void setDefaultValues(){

    for (Option<?> opt : parameters.values()) {

      if (opt instanceof Parameter && !opt.isSet() && ((Parameter<?,?>) opt).hasDefaultValue()) {

        ((Parameter<?,?>) opt).setDefaultValueToValue();
      }
    }
  }


  private void checkNonOptionalParameters() throws ParameterException {

    Vector<Option<?>> notOptional = new Vector<Option<?>>();

    for (Option<?> opt : parameters.values()) {
      if (opt instanceof Parameter && !opt.isSet() && !((Parameter<?,?>) opt).isOptional()) {
        notOptional.add(opt);
      }
    }
    if (!notOptional.isEmpty()) {

      if (notOptional.size() == 1) {
        throw new WrongParameterValueException("Aborted. Parameter " + notOptional.get(0).getName() + " requires parameter value.");
      }
      StringBuffer buffer = new StringBuffer();
      for (int i = 0; i < notOptional.size(); i++) {
        buffer.append(notOptional.get(i).getName());
        if (i != notOptional.size() - 1) {
          buffer.append(",");
        }
      }
      throw new WrongParameterValueException("Aborted. Parameters " + buffer.toString() + " require parameter values.");
    }
  }

  protected void checkGlobalParameterConstraints() throws ParameterException {
	    
    for (GlobalParameterConstraint gbc : globalParameterConstraints) {
      gbc.test();
    }
  }
}