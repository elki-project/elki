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
    description.append(WHITESPACE.pattern());
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
    StringBuffer[] labels = null;
    try {
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE.split(line);
          if (data == null) {
            //noinspection unchecked
            data = new ArrayList[entries.length];
            for (int i = 0; i < data.length; i++) {
              data[i] = new ArrayList<Double>();
            }
          }
          if (labels == null) {
            labels = new StringBuffer[entries.length];
            for (int i = 0; i < labels.length; i++) {
              labels[i] = new StringBuffer();
            }
          }
          for (int i = 0; i < entries.length; i++) {
            try {
              Double attribute = Double.valueOf(entries[i]);
              data[i].add(attribute);
            }
            catch (NumberFormatException e) {
              if (labels[i].length() > 0) {
                labels[i].append(LABEL_CONCATENATION);
              }
              labels[i].append(entries[i]);
            }
          }
        }
      }

      int dimensionality = -1;
      for (int i = 0; i < data.length; i++) {
        if (dimensionality == -1) {
          dimensionality = data[i].size();
        }
        else {
          if (dimensionality != data[i].size()) {
            throw new IllegalArgumentException("Differing dimensionality in column " + (i + 1) + ".");
          }
        }
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    List<DoubleVector> objects = new ArrayList<DoubleVector>(data.length);
    List<String> labelList = new ArrayList<String>();
    for (int i = 0; i < data.length; i++) {
      objects.add(new DoubleVector(data[i]));
      labelList.add(labels[i].toString());
    }

    return new ParsingResult<DoubleVector>(objects, labelList);
  }


}
