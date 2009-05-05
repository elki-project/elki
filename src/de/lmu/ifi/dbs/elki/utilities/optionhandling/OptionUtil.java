package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Utility functions related to Option handling.
 *
 */
public final class OptionUtil {

  /**
   * Adds the specified flag to the beginning of the given parameter list.
   *
   * @param parameters the list of parameters
   * @param flag       the flag to be added
   */
  public static void addFlag(List<String> parameters, Flag flag) {
      parameters.add(0, OptionHandler.OPTION_PREFIX + flag.getName());
  }

  /**
   * Adds the specified optionID of a flag to the beginning of the given parameter list.
   *
   * @param parameters the list of parameters
   * @param optionID   the optionID to be added
   */
  public static void addFlag(List<String> parameters, OptionID optionID) {
      parameters.add(0, OptionHandler.OPTION_PREFIX + optionID.getName());
  }

  /**
   * Adds the specified optionID and its value to the beginning of the given parameter list.
   *
   * @param parameters the list of parameters
   * @param optionID   the optionID of the parameter to be added
   * @param value      the value of the parameter to be added
   */
  public static void addParameter(List<String> parameters, OptionID optionID, String value) {
      parameters.add(0, OptionHandler.OPTION_PREFIX + optionID.getName());
      parameters.add(1, value);
  }

  /**
   * Adds the specified parameter and the specified value to the beginning of the given parameter list.
   *
   * @param parameters the list of parameters
   * @param parameter  the parameter to be added
   * @param value      the value of the parameter to be added
   */
  public static void addParameter(List<String> parameters, Parameter<?, ?> parameter, String value) {
      parameters.add(0, OptionHandler.OPTION_PREFIX + parameter.getName());
      parameters.add(1, value);
  }

  /**
   * Adds the specified optionID and its value to the beginning of the given parameter array.
   *
   * @param parameters the array of parameters
   * @param optionID   the optionID to be added
   * @param value      the value of the optionID to be added
   * @return a new parameter array containing the values of <code>parameters</code> and
   *         the specified <code>optionID</code> and its <code>value</code>.
   */
  public static String[] addParameter(String[] parameters, OptionID optionID, String value) {
      String[] newParameters = new String[parameters.length + 2];
      System.arraycopy(parameters, 0, newParameters, 2, parameters.length);
      newParameters[0] = OptionHandler.OPTION_PREFIX + optionID.getName();
      newParameters[1] = value;
      return newParameters;
  }

  /**
   * Returns a string representation of the specified list of
   * options containing the names of the options.
   *
   * @param <O> Option type
   * @param options the list of options
   * @return the names of the options
   */
  public static <O extends Option<?>> String optionsNamesToString(List<O> options) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[");
      for (int i = 0; i < options.size(); i++) {
          buffer.append(options.get(i).getName());
          if (i != options.size() - 1) {
              buffer.append(",");
          }
      }
      buffer.append("]");
      return buffer.toString();
  }

  /**
   * Returns a string representation of the specified list of
   * options containing the names of the options.
   *
   * @param <O> Option type
   * @param options the list of options
   * @return the names of the options
   */
  public static <O extends Option<?>> String optionsNamesToString(O[] options) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[");
      for (int i = 0; i < options.length; i++) {
          buffer.append(options[i].getName());
          if (i != options.length - 1) {
              buffer.append(",");
          }
      }
      buffer.append("]");
      return buffer.toString();
  }

  /**
   * Returns an array that contains all elements of the first parameter array that
   * are not contained by the second parameter array. The first parameter array must at
   * least be as long as the second. The second must not contain entries that
   * are not contained by the first.
   *
   * @param complete the complete array
   * @param part     an array that contains only elements of the first parameter array
   * @return an array that contains all elements of the first parameter array that
   *         are not contained by the second parameter array
   * @throws IllegalArgumentException if the first array, <code>complete</code> is not as long as
   *                                  the second array, <code>part</code> or the second,
   *                                  <code>part</code>, contains entries that are not contained
   *                                  by the first, <code>complete</code>
   */
  public static String[] parameterDifference(String[] complete, String[] part) throws IllegalArgumentException {
      if (complete.length < part.length) {
          throw new IllegalArgumentException("First array must be at least as long as second array.\n" +
              "First array:  " + Arrays.asList(complete) + "\n" +
              "Second array: " + Arrays.asList(part));
      }
  
      if (complete.length == 0) {
          return new String[0];
      }
  
      List<String> completeArray = new ArrayList<String>();
      for (int i = 0; i < complete.length; i++) {
          String param = complete[i];
          if (param.startsWith(OptionHandler.OPTION_PREFIX)) {
              if (i < complete.length - 1) {
                  String key = complete[i + 1];
                  if (!key.startsWith(OptionHandler.OPTION_PREFIX)) {
                      completeArray.add(param + " " + key);
                      i++;
                  }
                  else {
                      completeArray.add(param);
                  }
              }
          }
      }
  
      List<String> partArray = new ArrayList<String>();
      for (int i = 0; i < part.length; i++) {
          String param = part[i];
          if (param.startsWith(OptionHandler.OPTION_PREFIX)) {
              if (i < part.length - 1) {
                  String key = part[i + 1];
                  if (!key.startsWith(OptionHandler.OPTION_PREFIX)) {
                      partArray.add(param + " " + key);
                      i++;
                  }
                  else {
                      partArray.add(param);
                  }
              }
          }
      }
  
      Pattern pattern = Pattern.compile(" ");
      List<String> result = new ArrayList<String>();
      int first = 0;
      int second = 0;
      while (first < completeArray.size() && second < partArray.size()) {
          if (completeArray.get(first).equals(partArray.get(second))) {
              first++;
              second++;
          }
          else {
              String[] params = pattern.split(completeArray.get(first));
              for (String p : params) {
                  result.add(p);
              }
              first++;
          }
      }
      if (second < partArray.size()) {
          throw new IllegalArgumentException("second array contains entries that are not " +
              "contained in the first array.\n" +
              "First array:  " + Arrays.asList(complete) + "\n" +
              "Second array: " + Arrays.asList(part));
      }
      while (first < completeArray.size()) {
          String[] params = pattern.split(completeArray.get(first));
          for (String p : params) {
              result.add(p);
          }
          first++;
      }
  
  
      String[] resultArray = new String[result.size()];
      return result.toArray(resultArray);
  }

  /**
   * Returns a string representation of the list of number
   * parameters containing the names and the values of the parameters.
   *
   * @param <N> Parameter type
   * @param parameters the list of number parameters
   * @return the names and the values of the parameters
   */
  public static <N extends NumberParameter<?>> String parameterNamesAndValuesToString(List<N> parameters) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[");
      for (int i = 0; i < parameters.size(); i++) {
          buffer.append(parameters.get(i).getName());
          buffer.append(":");
          buffer.append(parameters.get(i).getNumberValue().doubleValue());
          if (i != parameters.size() - 1) {
              buffer.append(", ");
          }
      }
      buffer.append("]");
      return buffer.toString();
  }

  /**
   * Format a list of options (and associated owning objects) for console help output.
   * 
   * @param buf Serialization buffer
   * @param width Screen width
   * @param indent Indentation string
   * @param options List of options
   */
  public static void formatForConsole(StringBuffer buf, int width, String indent, List<Pair<Parameterizable, Option<?>>> options) {
    for (Pair<Parameterizable, Option<?>> pair : options) {
      String currentOption = pair.getSecond().getName();
      String syntax = pair.getSecond().getSyntax();
      String longDescription = pair.getSecond().getDescription();
      
      buf.append(OptionHandler.OPTION_PREFIX);
      buf.append(currentOption);
      buf.append(" ");
      buf.append(syntax);
      buf.append(OptionHandler.NEWLINE);
      for (String line : FormatUtil.splitAtLastBlank(longDescription, width - indent.length())) {
        buf.append(indent);
        buf.append(line);
        if (! line.endsWith(OptionHandler.NEWLINE)) {
          buf.append(OptionHandler.NEWLINE);
        }
      }
    }
  }

}
