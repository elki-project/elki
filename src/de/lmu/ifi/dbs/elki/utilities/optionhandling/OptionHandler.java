package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.output.PrettyPrinter;

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
   * Provides an OptionHandler.
   * <p/>
   * The options are specified in the given TreeMap with the option names being
   * as keys. Leading &quot;-&quot; do not have to be specified since
   * OptionHandler will provide them.
   * 
   * @param parameters Map containing the options
   * @param programCall String for the program-call using this OptionHandler
   *        (for usage in usage(String))
   */
  public OptionHandler(Map<String, Option<?>> parameters, String programCall) {
    super(false);
    this.parameters = parameters;
    this.programCall = programCall;
    this.globalParameterConstraints = new ArrayList<GlobalParameterConstraint>();
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
   * @throws NoParameterValueException if a parameter, for which a value is
   *         required, has none (e.g. because the next value is itself some
   *         option)
   */
  public String[] grabOptions(String[] currentOptions) throws ParameterException {
    List<String> unexpectedParameters = new ArrayList<String>();
    List<String> parameterArray = new ArrayList<String>();

    for(int i = 0; i < currentOptions.length; i++) {
      if(!currentOptions[i].startsWith(OPTION_PREFIX)) {
        throw new NoParameterValueException(currentOptions[i] + " is no parameter!");
      }

      // get the option without the option prefix -
      String noPrefixOption = currentOptions[i].substring(OPTION_PREFIX.length());

      // check if parameter-Map contains the current option
      if(parameters.containsKey(noPrefixOption)) {

        Option<?> current = parameters.get(noPrefixOption);
        // check if the option is a parameter or a flag
        if(current instanceof Parameter) {

          // check if there is a next element in the option array and
          // if it's indeed an option value
          if(i + 1 < currentOptions.length /*
                                            * && !currentOptions[i +
                                            * 1].startsWith(OPTION_PREFIX)
                                            */) {

            // set the parameter value if the value is not already set
            if(!current.isSet()) {
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
          // next element seems to be a parameter of flag identifier!
          else {
            throw new NoParameterValueException("Parameter " + currentOptions[i] + " requires a parameter value!");
          }
        }
        // option is of type flag
        else if(current instanceof Flag) {
          // set the flag if it's not already set to true
          if(!current.isSet()) {

            // check if the next element is wrongly a parameter
            // value
            if(i + 1 < currentOptions.length && !currentOptions[i + 1].startsWith(OPTION_PREFIX)) {
              throw new NoParameterValueException("Flag " + currentOptions[i] + " requires no parameter-value! " + "(read parameter-value: " + currentOptions[i + 1] + ")");
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
          // FIXME unexpected option type
        }

      }

      // unexpected option
      else {
        unexpectedParameters.add(currentOptions[i]);
        if(i + 1 < currentOptions.length && !currentOptions[i + 1].startsWith(OPTION_PREFIX)) {
          unexpectedParameters.add(currentOptions[i + 1]);
          i++;
        }
      }
    }

    currentParameters = new String[parameterArray.size()];
    currentParameters = parameterArray.toArray(currentParameters);
    String[] remain = new String[unexpectedParameters.size()];
    unexpectedParameters.toArray(remain);

    if(logger.isDebuggingFiner()) {
      for(Map.Entry<String, Option<?>> option : parameters.entrySet()) {
        logger.debugFiner("option " + option.getKey() + " has value " + option.getValue().getValue());
      }
    }

    setDefaultValues();

    checkNonOptionalParameters();
    checkGlobalParameterConstraints();

    return remain;
  }

  public Option<?> getOption(String name) throws UnusedParameterException {
    if(parameters.containsKey(name)) {
      return parameters.get(name);
    }
    throw new UnusedParameterException("Parameter " + name + " is not assigned to the option handler.");
  }

  /**
   * Returns true if the value of the given option is set, false otherwise.
   * 
   * @param option The option should be asked for without leading &quot;-&quot;
   *        or closing &quot;:&quot;.
   * @return boolean true if the value of the given option is set, false
   *         otherwise
   */
  public boolean isSet(String option) {
    if(parameters.containsKey(option)) {
      return parameters.get(option).isSet();
    }
    return false;
  }

  /**
   * Returns true if the value of the given option is set, false otherwise.
   * 
   * @param option The option should be asked for without leading &quot;-&quot;
   *        or closing &quot;:&quot;.
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
   * @return an usage-String according to the descriptions given in the
   *         constructor.
   */
  public String usage(String message) {
    return usage(message, true);
  }

  /**
   * Returns an usage-String according to the descriptions given in the
   * constructor.
   * 
   * @param message some error-message, if needed (may be null or empty String)
   * @param standalone whether the class using this OptionHandler provides a
   *        main method
   * @return an usage-String according to the descriptions given in the
   *         constructor.
   */
  public String usage(String message, boolean standalone) {
    final String space = " ";
    int lineLength = 80;
    String paramLineIndent = "    ";
    StringBuffer messageBuffer = new StringBuffer();
    if(!(message == null || message.equals(""))) {
      messageBuffer.append(message).append(NEWLINE);
    }

    String[] options = new String[parameters.size()];

    String[] longDescriptions = new String[options.length];

    int longestShortline = 0;
    StringBuffer paramLine = new StringBuffer();
    int currentLength = programCall.length();

    int counter = 0;
    for(Map.Entry<String, Option<?>> option : parameters.entrySet()) {

      String currentOption = option.getKey();

      String longDescription = option.getValue().getDescription();

      if (false) {
        if(option.getValue() instanceof ClassParameter) {
          ClassParameter<?> c = (ClassParameter<?>) option.getValue();
          longDescription = longDescription + "\n" + Properties.ELKI_PROPERTIES.restrictionString(c.getRestrictionClass());
          if (c.getDefaultValue() != null) {
            longDescription = longDescription + "\n" + "Default:" + Properties.NONBREAKING_SPACE + c.getDefaultValue();
          }
        }
      }
      currentOption = OPTION_PREFIX + currentOption;
      options[counter] = currentOption;
      longDescriptions[counter] = longDescription;
      longestShortline = Math.max(longestShortline, currentOption.length() + 1);
      currentLength = currentLength + currentOption.length() + 1;
      if(currentLength > lineLength) {
        paramLine.append(NEWLINE);
        paramLine.append(paramLineIndent);
        currentLength = paramLineIndent.length();
      }
      paramLine.append(currentOption);
      paramLine.append(space);

      counter++;
    }

    String indent = "  ";
    int firstCol = indent.length() + longestShortline;
    StringBuffer descriptionIndent = new StringBuffer();
    for(int i = 0; i < firstCol; i++) {
      descriptionIndent.append(space);
    }
    int thirdCol = lineLength - firstCol;
    // if the column would be zero-width, give up...
    if(thirdCol < 0) {
      thirdCol = lineLength;
    }
    int[] cols = { firstCol, thirdCol };
    PrettyPrinter prettyPrinter = new PrettyPrinter(cols, "");
    char fillchar = ' ';

    if(standalone) {
      messageBuffer.append("Usage: ");
      messageBuffer.append(NEWLINE);
    }
    messageBuffer.append(programCall);
    if(standalone) {
      messageBuffer.append(space);
      messageBuffer.append(paramLine);
    }
    messageBuffer.append(NEWLINE);

    for(int i = 0; i < options.length; i++) {
      StringBuffer option = new StringBuffer();
      option.append(indent);
      option.append(options[i]);
      Vector<String> lines = prettyPrinter.breakLine(longDescriptions[i], 1);
      String[] firstline = { option.toString(), lines.firstElement() };
      messageBuffer.append(prettyPrinter.formattedLine(firstline, fillchar)).append(NEWLINE);
      for(int l = 1; l < lines.size(); l++) {
        messageBuffer.append(descriptionIndent).append(lines.get(l)).append(NEWLINE);
      }
    }

    // global constraints
    // todo arthur bitte richtig formatieren mit pretty printer
    if(!globalParameterConstraints.isEmpty()) {
      messageBuffer.append(NEWLINE).append("  Global parameter constraints:");
      for(GlobalParameterConstraint gpc : globalParameterConstraints) {
        messageBuffer.append(NEWLINE).append("  - ");
        messageBuffer.append(gpc.getDescription());
      }
    }

    return messageBuffer.toString();
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

  public void setGlobalParameterConstraint(GlobalParameterConstraint gpc) {
    globalParameterConstraints.add(gpc);
  }

  /**
   * Sets the OptionHandler's programmCall (@link #programCall} to the given
   * call.
   * 
   * @param call The new program call.
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
    Logger.getLogger(this.getClass().getName()).finer("removed " + removed.getName());
    if(removed == null) {
      throw new UnusedParameterException("Cannot remove parameter " + optionName + " because it has not been set before.");
    }
  }

  public Option<?>[] getOptions() {
    return parameters.values().toArray(new Option[] {});
  }

  /**
   * Adds the settings of the options assigned to this option handler to the
   * specified attribute settings.
   * 
   * @param settings the attribute settings to add the settings of the options
   *        assigned to this option handler to
   * @throws UnusedParameterException todo
   */
  public void addOptionSettings(AttributeSettings settings) throws UnusedParameterException {
    for(Option<?> option : parameters.values()) {
      if(option instanceof Flag) {
        settings.addSetting(option.getName(), Boolean.toString(isSet(option.getName())));
      }
      else {
        Object value = option.getValue();
        if(value != null) {
          settings.addSetting(option.getName(), value.toString());
        }
        else {
          settings.addSetting(option.getName(), "null");
        }
      }
    }
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
        throw new WrongParameterValueException("Aborted. Parameter " + notOptional.get(0).getName() + " requires parameter value.");
      }
      StringBuffer buffer = new StringBuffer();
      for(int i = 0; i < notOptional.size(); i++) {
        buffer.append(notOptional.get(i).getName());
        if(i != notOptional.size() - 1) {
          buffer.append(",");
        }
      }
      throw new WrongParameterValueException("Aborted. Parameters " + buffer.toString() + " require parameter values.");
    }
  }

  protected void checkGlobalParameterConstraints() throws ParameterException {

    for(GlobalParameterConstraint gbc : globalParameterConstraints) {
      gbc.test();
    }
  }
}