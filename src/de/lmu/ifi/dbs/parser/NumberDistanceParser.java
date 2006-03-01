package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.ExternalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.utilities.IDPair;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Provides a parser for parsing one distance value per line.
 * <p/>
 * A line must have the follwing format: id1 id2 distanceValue,
 * where id1 and is2 are integers representing the two ids belonging to the distance value.
 * Lines starting with &quot;#&quot; will be ignored.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class NumberDistanceParser extends AbstractParser<ExternalObject> implements DistanceParser<ExternalObject, NumberDistance> {
  /**
   * Parameter for distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "<classname>the distance function - must implement " + DistanceFunction.class.getName() + ".";

  /**
   * The distance function.
   */
  private DistanceFunction<ExternalObject, NumberDistance> distanceFunction;

  /**
   * Provides a parser for parsing one double distance per line.
   * A line must have the follwing format: id1 id2 distanceValue,
   * where id1 and is2 are integers representing the two ids belonging to the distance value,
   * the distance value is a double value.
   * Lines starting with &quot;#&quot; will be ignored.
   */
  public NumberDistanceParser() {
    super();
    parameterToDescription.put(DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTION_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see Parser#parse(java.io.InputStream)
   */
  public ParsingResult<ExternalObject> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    List<ObjectAndLabels<ExternalObject>> objectAndLabelsList = new ArrayList<ObjectAndLabels<ExternalObject>>();

    Set<Integer> ids = new HashSet<Integer>();
    Map<IDPair, NumberDistance> distanceMap = new HashMap<IDPair, NumberDistance>();
    try {
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          if (entries.length != 3)
            throw new IllegalArgumentException("Line " + lineNumber + " does not have the " +
                                               "required input format: id1 id2 distanceValue! " + line);

          Integer id1, id2;
          try {
            id1 = Integer.parseInt(entries[0]);
          }
          catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in line " + lineNumber + ": id1 is no integer!");
          }

          try {
            id2 = Integer.parseInt(entries[1]);
          }
          catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in line " + lineNumber + ": id2 is no integer!");
          }

          try {
            NumberDistance distance = distanceFunction.valueOf(entries[2]);
            distanceMap.put(new IDPair(id1, id2), distance);
            ids.add(id1);
            ids.add(id2);
          }
          catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error in line " + lineNumber + ":" + e.getMessage());
          }
        }
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    // check if all distance values are specified
    for (Integer id1 : ids) {
      for (Integer id2 : ids) {
        if (id2 < id1) continue;
        IDPair pair = new IDPair(id1, id2);
        if (! distanceMap.containsKey(pair))
          throw new IllegalArgumentException("Distance value for " + id1 + " - " + id2 + " is missing!");
      }
    }

    for (Integer id : ids) {
      objectAndLabelsList.add(new ObjectAndLabels<ExternalObject>(new ExternalObject(id), new ArrayList<String>()));
    }

    return new DistanceParsingResult<ExternalObject, NumberDistance>(objectAndLabelsList, distanceMap);
  }

  /**
   * Returns the distance function of this parser.
   *
   * @return the distance function of this parser
   */
  public DistanceFunction<ExternalObject, NumberDistance> getDistanceFunction() {
    return distanceFunction;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(NumberDistanceParser.class.getName());
    description.append(" expects following format of parsed lines:\n");
    description.append("id1 id2 distanceValue, where id1 and is2 are integers representing " +
                       "the two ids belonging to the distance value.\n" +
                       " The ids and the distance value are separated by whitespace (");
    description.append(WHITESPACE_PATTERN.pattern());
    description.append("). Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored.\n");

    return usage(description.toString());
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      String className = optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
      //noinspection unchecked
      distanceFunction = Util.instantiate(DistanceFunction.class, className);
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings setting = result.get(0);
    setting.addSetting(DISTANCE_FUNCTION_P, distanceFunction.getClass().getSimpleName());

    return result;
  }

}
