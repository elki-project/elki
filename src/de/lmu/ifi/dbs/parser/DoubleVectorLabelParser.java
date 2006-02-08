package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DoubleVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides a parser for parsing one point per line, attributes separated by whitespace.
 * <p/>
 * Several labels may be given per point. A label must not be parseable as double.
 * Lines starting with &quot;#&quot; will be ignored.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DoubleVectorLabelParser extends AbstractParser<DoubleVector> {
  /**
   * The comment character.
   */
  public static final String COMMENT = "#";

  /**
   * A sign to separate components of a label.
   */
  public static final String LABEL_CONCATENATION = " ";

  /**
   * A pattern defining whitespace.
   */
  public static final Pattern WHITESPACE = Pattern.compile("\\s+");

  /**
   * Provides a parser for parsing one point per line, attributes separated by whitespace.
   * <p/>
   * Several labels may be given per point. A label must not be parseable as double.
   * Lines starting with &quot;#&quot; will be ignored.
   */
  public DoubleVectorLabelParser() {
    super();
  }

  /**
   * @see Parser#parse(java.io.InputStream)
   */
  public ParsingResult<DoubleVector> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<DoubleVector> objects = new ArrayList<DoubleVector>();
    List<String> labels = new ArrayList<String>();
    try {
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE.split(line);
          List<Double> attributes = new ArrayList<Double>();
          StringBuffer label = new StringBuffer();
          for (String entry : entries) {
            try {
              Double attribute = Double.valueOf(entry);
              attributes.add(attribute);
            }
            catch (NumberFormatException e) {
              if (label.length() > 0) {
                label.append(LABEL_CONCATENATION);
              }
              label.append(entry);
            }
          }
          if (dimensionality < 0) {
            dimensionality = attributes.size();
          }
          if (dimensionality != attributes.size()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ".");
          }
          objects.add(new DoubleVector(attributes));
          labels.add(label.toString());
        }
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<DoubleVector>(objects, labels);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(DoubleVectorLabelParser.class.getName());
    description.append(" expects following format of parsed lines:\n");
    description.append("A single line provides a single point. Attributes are separated by whitespace (");
    description.append(WHITESPACE.pattern());
    description.append("). Any substring not containing whitespace is tried to be read as double. If this fails, it will be appended to a label. (Thus, any label must not be parseable as double.) Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored. If any point differs in its dimensionality from other points, the parse method will fail with an Exception.\n");

    return usage(description.toString());
  }
}
