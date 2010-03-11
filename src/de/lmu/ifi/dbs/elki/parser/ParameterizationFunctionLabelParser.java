package de.lmu.ifi.dbs.elki.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ParameterizationFunction;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a parser for parsing one point per line, attributes separated by
 * whitespace. The parser transforms each point into a parametrization function.
 * Several labels may be given per point. A label must not be parseable as
 * double (or float). Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Arthur Zimek
 */
@Title("Parameterization Function Label Parser")
@Description("Parser for the following line format:\n" + "A single line provides a single point. Attributes are separated by whitespace. The real values will be parsed as as doubles. Any substring not containing whitespace is tried to be read as double. If this fails, it will be appended to a label. (Thus, any label must not be parseable " + "as double.) Empty lines and lines beginning with \"#\" will be ignored. If any point differs in its dimensionality from other points, the parse method will fail with an Exception.")
public class ParameterizationFunctionLabelParser extends AbstractParser<ParameterizationFunction> implements Parameterizable {
  /**
   * Provides a parser for parsing one point per line, attributes separated by
   * whitespace. The parser transforms each point into a parametrization
   * function. Several labels may be given per point. A label must not be
   * parseable as double (or float). Lines starting with &quot;#&quot; will be
   * ignored.
   */
  public ParameterizationFunctionLabelParser() {
    super();
  }

  public ParsingResult<ParameterizationFunction> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    int dimensionality = -1;
    List<Pair<ParameterizationFunction, List<String>>> objectAndLabelsList = new ArrayList<Pair<ParameterizationFunction, List<String>>>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          List<Double> attributes = new ArrayList<Double>();
          List<String> labels = new ArrayList<String>();
          for(String entry : entries) {
            try {
              Double attribute = Double.valueOf(entry);
              attributes.add(attribute);
            }
            catch(NumberFormatException e) {
              labels.add(entry);
            }
          }

          if(dimensionality < 0) {
            dimensionality = attributes.size();
          }
          else if(dimensionality != attributes.size()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ":" + attributes.size() + " != " + dimensionality);
          }

          ParameterizationFunction function = new ParameterizationFunction(Util.convertToDoubles(attributes));
          Pair<ParameterizationFunction, List<String>> objectAndLabel = new Pair<ParameterizationFunction, List<String>>(function, labels);
          objectAndLabelsList.add(objectAndLabel);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<ParameterizationFunction>(objectAndLabelsList);
  }
}