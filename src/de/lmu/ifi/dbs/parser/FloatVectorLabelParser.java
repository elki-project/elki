package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.FloatVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a parser for parsing one point per line, attributes separated by whitespace.
 * <p/>
 * Several labels may be given per point. A label must not be parseable as float.
 * Lines starting with &quot;#&quot; will be ignored.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FloatVectorLabelParser extends AbstractParser<FloatVector> {

  /**
   * Provides a parser for parsing one point per line, attributes separated by whitespace.
   * <p/>
   * Several labels may be given per point. A label must not be parseable as float.
   * Lines starting with &quot;#&quot; will be ignored.
   */
  public FloatVectorLabelParser() {
    super();
  }

  /**
   * @see de.lmu.ifi.dbs.parser.Parser#parse(java.io.InputStream)
   */
  public ParsingResult<FloatVector> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<ObjectAndLabels<FloatVector>> objectAndLabelsList = new ArrayList<ObjectAndLabels<FloatVector>>();
    try {
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          List<Float> attributes = new ArrayList<Float>();
          List<String> labels = new ArrayList<String>();
          for (String entry : entries) {
            try {
              Float attribute = Float.valueOf(entry);
              attributes.add(attribute);
            }
            catch (NumberFormatException e) {
              labels.add(entry);
            }
          }

          if (dimensionality < 0) {
            dimensionality = attributes.size();
          }
          else if (dimensionality != attributes.size()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ".");
          }

          ObjectAndLabels<FloatVector> objectAndLabel = new ObjectAndLabels<FloatVector>(new FloatVector(attributes), labels);
          objectAndLabelsList.add(objectAndLabel);
        }
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<FloatVector>(objectAndLabelsList);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(FloatVectorLabelParser.class.getName());
    description.append(" expects following format of parsed lines:\n");
    description.append("A single line provides a single point. Attributes are separated by whitespace (");
    description.append(WHITESPACE_PATTERN.pattern());
    description.append("). Any substring not containing whitespace is tried to be read as float. If this fails, it will be appended to a label. (Thus, any label must not be parseable as float.) Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored. If any point differs in its dimensionality from other points, the parse method will fail with an Exception.\n");

    return usage(description.toString());
  }
}
