package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
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
   * @param flag the flag to be added
   */
  public static void addFlag(List<String> parameters, Flag flag) {
    parameters.add(0, OptionHandler.OPTION_PREFIX + flag.getName());
  }

  /**
   * Adds the specified optionID of a flag to the beginning of the given
   * parameter list.
   * 
   * @param parameters the list of parameters
   * @param optionID the optionID to be added
   */
  public static void addFlag(List<String> parameters, OptionID optionID) {
    parameters.add(0, OptionHandler.OPTION_PREFIX + optionID.getName());
  }

  /**
   * Adds the specified optionID and its value to the beginning of the given
   * parameter list.
   * 
   * @param parameters the list of parameters
   * @param optionID the optionID of the parameter to be added
   * @param value the value of the parameter to be added
   */
  public static void addParameter(List<String> parameters, OptionID optionID, String value) {
    parameters.add(0, OptionHandler.OPTION_PREFIX + optionID.getName());
    parameters.add(1, value);
  }

  /**
   * Adds the specified parameter and the specified value to the beginning of
   * the given parameter list.
   * 
   * @param parameters the list of parameters
   * @param parameter the parameter to be added
   * @param value the value of the parameter to be added
   */
  public static void addParameter(List<String> parameters, Parameter<?, ?> parameter, String value) {
    parameters.add(0, OptionHandler.OPTION_PREFIX + parameter.getName());
    parameters.add(1, value);
  }

  /**
   * Adds the specified optionID and its value to the beginning of the given
   * parameter array.
   * 
   * @param parameters the array of parameters
   * @param optionID the optionID to be added
   * @param value the value of the optionID to be added
   * @return a new parameter array containing the values of
   *         <code>parameters</code> and the specified <code>optionID</code> and
   *         its <code>value</code>.
   */
  public static String[] addParameter(String[] parameters, OptionID optionID, String value) {
    String[] newParameters = new String[parameters.length + 2];
    System.arraycopy(parameters, 0, newParameters, 2, parameters.length);
    newParameters[0] = OptionHandler.OPTION_PREFIX + optionID.getName();
    newParameters[1] = value;
    return newParameters;
  }

  /**
   * Appends the given options to the parameter list.
   * 
   * @param parameters the list of parameters
   * @param append Parameters to append.
   */
  public static void addParameters(List<String> parameters, Iterable<String> append) {
    for(String par : append) {
      parameters.add(par);
    }
  }

  /**
   * Appends the given options to the parameter list.
   * 
   * @param parameters the list of parameters
   * @param append Parameters to append.
   */
  public static void addParameters(List<String> parameters, String[] append) {
    for(String par : append) {
      parameters.add(par);
    }
  }

  /**
   * Returns a string representation of the specified list of options containing
   * the names of the options.
   * 
   * @param <O> Option type
   * @param options the list of options
   * @return the names of the options
   */
  public static <O extends Option<?>> String optionsNamesToString(List<O> options) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[");
    for(int i = 0; i < options.size(); i++) {
      buffer.append(options.get(i).getName());
      if(i != options.size() - 1) {
        buffer.append(",");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Returns a string representation of the specified list of options containing
   * the names of the options.
   * 
   * @param <O> Option type
   * @param options the list of options
   * @return the names of the options
   */
  public static <O extends Option<?>> String optionsNamesToString(O[] options) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[");
    for(int i = 0; i < options.length; i++) {
      buffer.append(options[i].getName());
      if(i != options.length - 1) {
        buffer.append(",");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Returns an array that contains all elements of the first parameter array
   * that are not contained by the second parameter array. The first parameter
   * array must at least be as long as the second. The second must not contain
   * entries that are not contained by the first.
   * 
   * @param complete the complete array
   * @param part an array that contains only elements of the first parameter
   *        array
   * @return an array that contains all elements of the first parameter array
   *         that are not contained by the second parameter array
   * @throws IllegalArgumentException if the first array, <code>complete</code>
   *         is not as long as the second array, <code>part</code> or the
   *         second, <code>part</code>, contains entries that are not contained
   *         by the first, <code>complete</code>
   */
  public static List<String> parameterDifference(List<String> complete, List<String> part) throws IllegalArgumentException {
    if(complete.size() < part.size()) {
      throw new IllegalArgumentException("First array must be at least as long as second array.\n" + "First array:  " + complete + "\n" + "Second array: " + part);
    }

    if(complete.size() == 0) {
      return new ArrayList<String>(0);
    }

    List<String> completeArray = new ArrayList<String>();
    for(int i = 0; i < complete.size(); i++) {
      String param = complete.get(i);
      if(param.startsWith(OptionHandler.OPTION_PREFIX)) {
        if(i < complete.size() - 1) {
          String key = complete.get(i + 1);
          if(!key.startsWith(OptionHandler.OPTION_PREFIX)) {
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
    for(int i = 0; i < part.size(); i++) {
      String param = part.get(i);
      if(param.startsWith(OptionHandler.OPTION_PREFIX)) {
        if(i < part.size() - 1) {
          String key = part.get(i + 1);
          if(!key.startsWith(OptionHandler.OPTION_PREFIX)) {
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
    ArrayList<String> result = new ArrayList<String>();
    int first = 0;
    int second = 0;
    while(first < completeArray.size() && second < partArray.size()) {
      if(completeArray.get(first).equals(partArray.get(second))) {
        first++;
        second++;
      }
      else {
        String[] params = pattern.split(completeArray.get(first));
        for(String p : params) {
          result.add(p);
        }
        first++;
      }
    }
    if(second < partArray.size()) {
      throw new IllegalArgumentException("second array contains entries that are not " + "contained in the first array.\n" + "First array:  " + complete + "\n" + "Second array: " + part);
    }
    while(first < completeArray.size()) {
      String[] params = pattern.split(completeArray.get(first));
      for(String p : params) {
        result.add(p);
      }
      first++;
    }

    return result;
  }

  /**
   * Returns a string representation of the list of number parameters containing
   * the names and the values of the parameters.
   * 
   * @param <N> Parameter type
   * @param parameters the list of number parameters
   * @return the names and the values of the parameters
   */
  public static <N extends NumberParameter<?>> String parameterNamesAndValuesToString(List<N> parameters) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[");
    for(int i = 0; i < parameters.size(); i++) {
      buffer.append(parameters.get(i).getName());
      buffer.append(":");
      buffer.append(parameters.get(i).getNumberValue().doubleValue());
      if(i != parameters.size() - 1) {
        buffer.append(", ");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Format a list of options (and associated owning objects) for console help
   * output.
   * 
   * @param buf Serialization buffer
   * @param width Screen width
   * @param indent Indentation string
   * @param options List of options
   */
  public static void formatForConsole(StringBuffer buf, int width, String indent, List<Pair<Parameterizable, Option<?>>> options) {
    for(Pair<Parameterizable, Option<?>> pair : options) {
      String currentOption = pair.getSecond().getName();
      String syntax = pair.getSecond().getSyntax();
      String longDescription = pair.getSecond().getFullDescription();

      buf.append(OptionHandler.OPTION_PREFIX);
      buf.append(currentOption);
      buf.append(" ");
      buf.append(syntax);
      buf.append(OptionHandler.NEWLINE);
      for(String line : FormatUtil.splitAtLastBlank(longDescription, width - indent.length())) {
        buf.append(indent);
        buf.append(line);
        if(!line.endsWith(OptionHandler.NEWLINE)) {
          buf.append(OptionHandler.NEWLINE);
        }
      }
    }
  }

  /**
   * Produce a description of a Parameterizable instance (including recursive
   * options).
   * 
   * @param p Parameterizable to describe
   * @return Formatted description
   */
  public static String describeParameterizable(Parameterizable p) {
    StringBuffer usage = new StringBuffer();
    describeParameterizable(usage, p, 77, "   ");
    return usage.toString();
  }

  /**
   * Format a description of a Parameterizable (including recursive options).
   * 
   * @param buf Buffer to append to.
   * @param p Parameterizable to describe
   * @param width Width
   * @param indent Text indent
   * @return Formatted description
   */
  public static StringBuffer describeParameterizable(StringBuffer buf, Parameterizable p, int width, String indent) {
    buf.append("Description for class ");
    buf.append(p.getClass().getName());
    buf.append(":\n");

    if(p instanceof Algorithm<?, ?>) {
      Algorithm<?, ?> a = (Algorithm<?, ?>) p;
      buf.append(a.getDescription().toString());
      buf.append("\n");
    }

    String shortdesc = p.shortDescription();
    if(shortdesc != null) {
      buf.append(shortdesc);
      buf.append("\n");
    }

    // Collect options
    ArrayList<Pair<Parameterizable, Option<?>>> options = p.collectOptions();
    OptionUtil.formatForConsole(buf, width, indent, options);
    return buf;
  }

}
