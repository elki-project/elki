package de.lmu.ifi.dbs.elki.datasource.parser;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.ParameterizationFunction;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Provides a parser for parsing one point per line, attributes separated by
 * whitespace. The parser transforms each point into a parameterization function.
 * Several labels may be given per point. A label must not be parseable as
 * double (or float). Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has ParameterizationFunction
 */
@Title("Parameterization Function Label Parser")
@Description("Parser for the following line format:\n" + "A single line provides a single point. Attributes are separated by whitespace. The real values will be parsed as as doubles. Any substring not containing whitespace is tried to be read as double. If this fails, it will be appended to a label. (Thus, any label must not be parseable " + "as double.) Empty lines and lines beginning with \"#\" will be ignored. If any point differs in its dimensionality from other points, the parse method will fail with an Exception.")
public class ParameterizationFunctionLabelParser extends AbstractParser implements Parser {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(ParameterizationFunctionLabelParser.class);

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   */
  public ParameterizationFunctionLabelParser(Pattern colSep, char quoteChar) {
    super(colSep, quoteChar);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    int dimensionality = -1;
    List<ParameterizationFunction> vectors = new ArrayList<ParameterizationFunction>();
    List<LabelList> labels = new ArrayList<LabelList>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          List<String> entries = tokenize(line);
          List<Double> attributes = new ArrayList<Double>(entries.size());
          LabelList labellist = new LabelList();
          for(String entry : entries) {
            try {
              Double attribute = Double.valueOf(entry);
              attributes.add(attribute);
            }
            catch(NumberFormatException e) {
              labellist.add(entry);
            }
          }

          if(dimensionality < 0) {
            dimensionality = attributes.size();
          }
          else if(dimensionality != attributes.size()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ":" + attributes.size() + " != " + dimensionality);
          }

          vectors.add(new ParameterizationFunction(Util.convertToDoubles(attributes)));
          labels.add(labellist);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return MultipleObjectsBundle.makeSimple(getTypeInformation(dimensionality), vectors, TypeUtil.LABELLIST, labels);
  }

  protected VectorFieldTypeInformation<ParameterizationFunction> getTypeInformation(int dimensionality) {
    return new VectorFieldTypeInformation<ParameterizationFunction>(ParameterizationFunction.class, dimensionality, new ParameterizationFunction(new double[dimensionality]));
  }
  
  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParser.Parameterizer {
    @Override
    protected ParameterizationFunctionLabelParser makeInstance() {
      return new ParameterizationFunctionLabelParser(colSep, quoteChar);
    }
  }
}