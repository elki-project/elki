package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.FloatVector;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.Util;

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
public class RealVectorLabelTransposingParser extends RealVectorLabelParser {

  /**
   * Provides a parser to read points transposed (per column).
   */
  public RealVectorLabelTransposingParser() {
    super();
  }

  /**
   * @see Parser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<RealVector> parse(InputStream in) {
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
            throw new IllegalArgumentException("Differing dimensionality in line " + (lineNumber) + ", " +
                                               "expected: " + dimensionality + ", read: " + entries.length);
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

    List<ObjectAndLabels<RealVector>> objectAndLabelList = new ArrayList<ObjectAndLabels<RealVector>>(data.length);
    for (int i = 0; i < data.length; i++) {
      List<String> label = new ArrayList<String>();
      label.add(labels[i].toString());

      RealVector featureVector;
      if (parseFloat) {
        featureVector = new FloatVector(Util.convertToFloat(data[i]));
      }
      else {
        featureVector = new DoubleVector(data[i]);
      }
      ObjectAndLabels<RealVector> objectAndLabels = new ObjectAndLabels<RealVector>(featureVector, label);
      objectAndLabelList.add(objectAndLabels);
    }

    return new ParsingResult<RealVector>(objectAndLabelList);
  }
}
