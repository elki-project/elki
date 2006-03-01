package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DoubleVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser reads points transposed. Line n gives the n-th attribute for all points.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DoubleVectorLabelTransposingParser extends DoubleVectorLabelParser {

  /**
   * Provides a parser to read points transposed (per column).
   */
  public DoubleVectorLabelTransposingParser() {
    super();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  @Override
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(DoubleVectorLabelTransposingParser.class.getName());
    description.append(" expects following format of parsed lines:\n");
    description.append("A single line provides an attribute for each point. Attributes of different points are separated by whitespace (");
    description.append(WHITESPACE_PATTERN.pattern());
    description.append("). Any substring not containing whitespace is tried to be read as double. If this fails, it will be appended to a label of the respective column. (Thus, any label must not be parseable as double.) Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored. If any point differs in its dimensionality from other points, the parse method will fail with an Exception.\n");

    return usage(description.toString());
  }

  /**
   * @see Parser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<DoubleVector> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    List<Double>[] data = null;
    List<String>[] labels = null;

    int dimensionality = -1;

    try {
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          if (dimensionality == -1) {
            dimensionality = entries.length;
          }
          else if (entries.length != dimensionality) {
            throw new IllegalArgumentException("Differing dimensionality in line " + (lineNumber) + ".");
          }

          if (data == null) {
            //noinspection unchecked
            data = new ArrayList[dimensionality];
            for (int i = 0; i < data.length; i++) {
              data[i] = new ArrayList<Double>();
            }
            //noinspection unchecked
            labels = new ArrayList[dimensionality];
            for (int i = 0; i < labels.length; i++) {
              labels[i] = new ArrayList<String>();
            }
          }

          for (int i = 0; i < entries.length; i++) {
            try {
              Double attribute = Double.valueOf(entries[i]);
              data[i].add(attribute);
            }
            catch (NumberFormatException e) {
              labels[i].add(entries[i]);
            }
          }
        }
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    List<ObjectAndLabels<DoubleVector>> objectAndLabelList = new ArrayList<ObjectAndLabels<DoubleVector>>(data.length);
    for (int i = 0; i < data.length; i++) {
      List<String> label = new ArrayList<String>();
      label.add(labels[i].toString());

      ObjectAndLabels<DoubleVector> objectAndLabels = new ObjectAndLabels<DoubleVector>(new DoubleVector(data[i]), label);
      objectAndLabelList.add(objectAndLabels);
    }

    return new ParsingResult<DoubleVector>(objectAndLabelList);
  }


}
