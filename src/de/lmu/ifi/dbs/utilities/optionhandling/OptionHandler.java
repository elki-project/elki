package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.utilities.output.PrettyPrinter;

import java.util.*;

/**
 * Provides an OptionHandler, which is able to read the specified options. <p/>
 * The specified options in the given String-array should be single Strings.
 * There are no leading &quot;-&quot; to denote, because OptionHandler will
 * provide them. If there is a value for any option required, this should be
 * denoted by a &quot;:&quot; at the end of the String, which value is provided
 * by {@link #EXPECTS_VALUE OptionHandler.EXPECTS_VALUE}. E.g. there are three
 * options expected for some main-method:<br>
 * -i &lt;inputfile&gt; -o &lt;outputfile&gt; -v <br>
 * Then the given array should contain &quot;i:&quot;, &quot;o:&quot; and
 * &quot;v&quot;. <br>
 * <br>
 * <b>Example for usage</b><br>
 * <p/>
 * <p/>
 * <pre>
 *     public static void main(String[] args)
 *      {
 *          final String FILE = &quot;f&quot;;
 *          final String MINSUPPORT = &quot;ms&quot;;
 *          final String MINCONFIDENCE = &quot;mc&quot;;
 *          final String NUMBER_OF_ITEMS = &quot;i&quot;;
 *          final String DBSCAN_EPSILON = &quot;eps&quot;;
 *          final String DBSCAN_MINPTS = &quot;minPts&quot;;
 *          final String VERBOSE = &quot;v&quot;;
 *     &lt;p/&gt;
 *          Hashtable options = new Hashtable();
 *          options.put(FILE+OptionHandler.EXPECTS_VALUE, &quot;&lt;inputfile&gt; datafile&quot;);
 *          options.put(MINSUPPORT+OptionHandler.EXPECTS_VALUE, &quot;&lt;minsupport&gt; percent&quot;);
 *          options.put(MINCONFIDENCE+OptionHandler.EXPECTS_VALUE, &quot;&lt;minConfidence&gt; percent&quot;);
 *          options.put(NUMBER_OF_ITEMS+OptionHandler.EXPECTS_VALUE, &quot;&lt;numberOfItems&gt; number of items in the datafile&quot;);
 *          options.put(DBSCAN_EPSILON+OptionHandler.EXPECTS_VALUE, &quot;&lt;epsilon&gt; epsilon for ModeDBSCAN\n(should be very small, recommended is at most 0.2).&quot;);
 *          options.put(DBSCAN_MINPTS+OptionHandler.EXPECTS_VALUE, &quot;&lt;minPts&gt; minPts for ModeDBSCAN&quot;);
 *          options.put(VERBOSE, &quot;flag causes full output&quot;);
 *          OptionHandler optionHandler = new OptionHandler(options, &quot;java myPackage.myProgram&quot;);
 *          try
 *          {
 *              optionHandler.grabOptions(args);
 *          }
 *          catch(NoParameterValueException npve)
 *          {
 *              System.err.println(optionHandler.usage(npve.getMessage()));
 *              System.exit(1);
 *          }
 *     &lt;p/&gt;
 *          String filename = optionHandler.getString(FILE);
 *          float minSupport = optionHandler.getFloat(MINSUPPORT);
 *          float minConfidence = optionHandler.getFloat(MINCONFIDENCE);
 *          int numberOfItems = optionHandler.getInt(NUMBER_OF_ITEMS);
 *          float dbscanEpsilon = optionHandler.getFloat(DBSCAN_EPSILON);
 *          int dbscanMinPts = optionHandler.getInt(DBSCAN_MINPTS);
 *          boolean verbose = optionHandler.isSet(VERBOSE);
 *          ...
 *          ...
 *      }
 * </pre>
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 * @version 1.0 gamma (2005-07-28)
 */
public class OptionHandler {
  /**
   * The newline-String dependent on the system.
   */
  public final static String NEWLINE = System.getProperty("line.separator");

  /**
   * Value to denote at the end of an optionmarker that a value is supposed to
   * be given to the option.
   */
  public static final String EXPECTS_VALUE = ":";

  /**
   * Prefix of optionmarkers on the commandline. <p/> The optionmarkers are
   * supposed to be given on the commandline with leading -.
   */
  public static final String OPTION_PREFIX = "-";

  /**
   * A Map to collect possible parameters.
   */
  private Map<String, Boolean> possibleParameters;

  /**
   * A Map to collect possible flags.
   */
  private Map<String, Boolean> possibleFlags;

  /**
   * A Map to map options to values.
   */
  private Map<String, Object> optionToValue;

  /**
   * A Map to map parameters to their description.
   */
  private Map<String, String> parameterToDescription;

  /**
   * An array of possible options.
   */
  private String[] possibleOptions;

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
   * Provides an OptionHandler, which is able to read the specified options.
   * <p/> The specified options in the given Hashtable should be single
   * Strings as keys. There are no leading &quot;-&quot; to denote, because
   * OptionHandler will provide them. If there is a value for any option
   * required, this should be denoted by a description as value of the
   * according key, enclosed in leading &lt; and closing &gt;, followed
   * directly by a longer description at will, as well as a &quot;:&quot;
   * appended to the key. E.g. there are three options expected for some
   * main-method:<br>
   * -i &lt;inputfile&gt; -o &lt;outputfile&gt; -v <br>
   * Then the given Hashtable should contain<br>
   * &quot;i:&quot; =&gt; &quot;&lt;inputfile&gt;File containing
   * inputdata&quot;<br>
   * &quot;o:&quot; =&gt; &quot;&lt;outputfile&gt;Filename to write results
   * into&quot;<br>
   * &quot;v&quot; =&gt; &quot;verbose output&quot;.
   *
   * @param parameterToDescription options and flags to search for, mapped to a description
   * @param programCall            String for the program-call using this OptionHandler (for
   *                               usage in usage(String))
   */
  public OptionHandler(Map<String, String> parameterToDescription,
                       String programCall) {
    this.parameterToDescription = new Hashtable<String, String>(
    parameterToDescription);
    this.programCall = programCall;
    this.possibleParameters = new Hashtable<String, Boolean>();
    this.possibleFlags = new Hashtable<String, Boolean>();
    this.optionToValue = new Hashtable<String, Object>();
    Set<String> paramSet = parameterToDescription.keySet();
    this.possibleOptions = new String[paramSet.size()];
    paramSet.toArray(possibleOptions);
    Arrays.sort(possibleOptions);

    for (String possibleOption : possibleOptions) {
      String currentOption = OPTION_PREFIX + possibleOption;
      if (currentOption.endsWith(EXPECTS_VALUE)) {
        this.possibleParameters.put(currentOption.substring(0,
                                                            currentOption.length() - 1), true);
      }
      else {
        this.possibleFlags.put(currentOption, true);
      }
    }
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
  public String[] grabOptions(String[] currentOptions) throws NoParameterValueException {
    List<String> unexpectedParameters = new ArrayList<String>();
    List<String> parameterArray = new ArrayList<String>();
    for (int i = 0; i < currentOptions.length; i++) {
      if (! currentOptions[i].startsWith(OPTION_PREFIX)) {
        throw new NoParameterValueException(currentOptions[i] + " is no parameter!");
      }
      if (possibleParameters.containsKey(currentOptions[i])) {
        if (i + 1 < currentOptions.length
            && !possibleParameters.containsKey(currentOptions[i + 1])
            && !possibleFlags.containsKey(currentOptions[i + 1])) {
          if (!optionToValue.containsKey(currentOptions[i])) {
            optionToValue.put(currentOptions[i], currentOptions[i + 1]);
            parameterArray.add(currentOptions[i]);
            parameterArray.add(currentOptions[i + 1]);
            i++;
          }
          // option known, but already read and set
          else {
            unexpectedParameters.add(currentOptions[i]);
            unexpectedParameters.add(currentOptions[i + 1]);
            i++;
          }
        }
        // no option-value following the option - or the following
        // String is known as parameter or flag
        else {
          throw new NoParameterValueException("Parameter "
                                              + currentOptions[i]
                                              + " requires an parameter-value!");
        }
      }
      // flag
      else if (possibleFlags.containsKey(currentOptions[i]) &&
               !optionToValue.containsKey(currentOptions[i])) {

        // option-value is following
        if (i + 1 < currentOptions.length && ! currentOptions[i + 1].startsWith(OPTION_PREFIX)) {
          throw new NoParameterValueException("Parameter " + currentOptions[i] + " requires no parameter-value! " +
                                              "(read parameter-value: " + currentOptions[i + 1] + ")");
        }

        optionToValue.put(currentOptions[i], true);
        parameterArray.add(currentOptions[i]);
      }
      // flag not known or flag known but already set
      else {
        unexpectedParameters.add(currentOptions[i]);
        if (i + 1 < currentOptions.length && ! currentOptions[i + 1].startsWith(OPTION_PREFIX)) {
          unexpectedParameters.add(currentOptions[i + 1]);
          i++;
        }
      }
    }
    currentParameters = new String[parameterArray.size()];
    currentParameters = parameterArray.toArray(currentParameters);
    String[] remain = new String[unexpectedParameters.size()];
    unexpectedParameters.toArray(remain);
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
  public String getOptionValue(String option)
  throws UnusedParameterException, NoParameterValueException {

    if (optionToValue.containsKey(OPTION_PREFIX + option)) {
      try {
        return (String) optionToValue.get(OPTION_PREFIX + option);
      }
      catch (ClassCastException e) {
        throw new NoParameterValueException("Parameter " + option
                                            + " is flag which has no value!", e);
      }
    }
    else {
      throw new UnusedParameterException("Parameter " + option
                                         + " is not specified!");
    }
  }

  /**
   * Returns true if the given option is set, false otherwise.
   *
   * @param option The option should be asked for without leading &quot;-&quot;
   *               or closing &quot;:&quot;.
   * @return boolean true if the given option is set, false otherwise
   */
  public boolean isSet(String option) {
    return optionToValue.containsKey(OPTION_PREFIX + option);
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
    String[] options = new String[possibleOptions.length];
    String[] shortDescriptions = new String[possibleOptions.length];
    String[] longDescriptions = new String[possibleOptions.length];
    int longestShortline = 0;
    StringBuffer paramLine = new StringBuffer();
    int currentLength = programCall.length();
    for (int i = 0; i < possibleOptions.length; i++) {
      String currentOption = possibleOptions[i];
      String desc = parameterToDescription.get(currentOption);
      String shortDescription = empty;
      String longDescription = desc;
      if (currentOption.endsWith(EXPECTS_VALUE)) {
        shortDescription = desc.substring(desc.indexOf("<"), desc
        .indexOf(">") + 1);
        longDescription = desc.substring(desc.indexOf(">") + 1);
        currentOption = currentOption.substring(0, currentOption
        .length() - 1);
      }
      currentOption = OPTION_PREFIX + currentOption;
      options[i] = currentOption;
      shortDescriptions[i] = shortDescription;
      longDescriptions[i] = longDescription;
      longestShortline = Math.max(longestShortline, currentOption
      .length()
                                                    + shortDescription.length() + 1);
      currentLength = currentLength + currentOption.length() + 2
                      + shortDescription.length();
      if (currentLength > lineLength) {
        paramLine.append(NEWLINE);
        paramLine.append(paramLineIndent);
        currentLength = paramLineIndent.length();
      }
      paramLine.append(currentOption);
      paramLine.append(space);
      paramLine.append(shortDescription);
      paramLine.append(space);
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
      Vector<String> lines = prettyPrinter.breakLine(longDescriptions[i],
                                                     2);
      String[] firstline = {option.toString(), mark,
      lines.firstElement()};
      messageBuffer.append(
      prettyPrinter.formattedLine(firstline, fillchar)).append(
      NEWLINE);
      for (int l = 1; l < lines.size(); l++) {
        messageBuffer.append(descriptionIndent).append(lines.get(l))
        .append(NEWLINE);
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
    System.arraycopy(currentParameters, 0, parameterArray, 0,
                     currentParameters.length);
    return parameterArray;
  }
}
