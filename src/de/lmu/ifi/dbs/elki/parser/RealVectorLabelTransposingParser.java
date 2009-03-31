package de.lmu.ifi.dbs.elki.parser;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FloatVector;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser reads points transposed. Line n gives the n-th attribute for all points.
 *
 * @author Arthur Zimek
 */
public class RealVectorLabelTransposingParser<V extends RealVector<V, ?>> extends RealVectorLabelParser<V> {

  /**
   * Provides a parser to read points transposed (per column).
   */
  public RealVectorLabelTransposingParser() {
    super();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ParsingResult<V> parse(InputStream in) {
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
            data = ClassGenericsUtil.newArrayOfArrayList(dimensionality);
            for (int i = 0; i < data.length; i++) {
              data[i] = new ArrayList<Double>();
            }
            labels = ClassGenericsUtil.newArrayOfArrayList(dimensionality);
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

    List<Pair<V, List<String>>> objectAndLabelList = new ArrayList<Pair<V, List<String>>>(data.length);
    for (int i = 0; i < data.length; i++) {
      List<String> label = new ArrayList<String>();
      label.add(labels[i].toString());

      V featureVector;
      if (parseFloat) {
        featureVector = (V) new FloatVector(Util.convertToFloat(data[i]));
      }
      else {
        featureVector = (V) new DoubleVector(data[i]);
      }
      Pair<V, List<String>> objectAndLabels = new Pair<V, List<String>>(featureVector, label);
      objectAndLabelList.add(objectAndLabels);
    }

    return new ParsingResult(objectAndLabelList);
  }
}
