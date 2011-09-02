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
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p>
 * Provides a parser for parsing one point per line, attributes separated by
 * whitespace.
 * </p>
 * <p>
 * Several labels may be given per point. A label must not be parseable as
 * double. Lines starting with &quot;#&quot; will be ignored.
 * </p>
 * <p>
 * A line is expected in the following format: The first entry of each line is
 * the number of attributes with coordinate value not zero. Subsequent entries
 * are of the form (index, value), where index is the number of the
 * corresponding dimension, and value is the value of the corresponding
 * attribute.
 * </p>
 * <p>
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has SparseFloatVector
 */
@Title("Sparse Float Vector Label Parser")
@Description("Parser for the following line format:\n" + "A single line provides a single point. Entries are separated by whitespace. " + "The values will be parsed as floats (resulting in a set of SparseFloatVectors). A line is expected in the following format: The first entry of each line is the number of attributes with coordinate value not zero. Subsequent entries are of the form (index, value), where index is the number of the corresponding dimension, and value is the value of the corresponding attribute." + "Any pair of two subsequent substrings not containing whitespace is tried to be read as int and float. If this fails for the first of the pair (interpreted ans index), it will be appended to a label. (Thus, any label must not be parseable as Integer.) If the float component is not parseable, an exception will be thrown. Empty lines and lines beginning with \"#\" will be ignored. Having the file parsed completely, the maximum occuring dimensionality is set as dimensionality to all created SparseFloatvectors.")
public class SparseFloatVectorLabelParser extends NumberVectorLabelParser<SparseFloatVector> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(SparseFloatVectorLabelParser.class);

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public SparseFloatVectorLabelParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices);
  }

  /**
   * Holds the dimensionality of the parsed data which is the maximum occurring
   * index of any attribute.
   */
  private int dimensionality = -1;

  @Override
  public SparseFloatVector createDBObject(List<Double> attributes) {
    throw new UnsupportedOperationException("This method should never be reached.");
  }

  @Override
  public Pair<SparseFloatVector, LabelList> parseLineInternal(String line) {
    List<String> entries = tokenize(line);
    int cardinality = Integer.parseInt(entries.get(0));

    Map<Integer, Float> values = new HashMap<Integer, Float>(cardinality, 1);
    LabelList labels = new LabelList();

    for(int i = 1; i < entries.size() - 1; i++) {
      if(!labelIndices.get(i)) {
        Integer index;
        Float attribute;
        try {
          index = Integer.valueOf(entries.get(i));
          if(index > dimensionality) {
            dimensionality = index;
          }
          i++;
        }
        catch(NumberFormatException e) {
          labels.add(entries.get(i));
          continue;
        }
        attribute = Float.valueOf(entries.get(i));
        values.put(index, attribute);
      }
      else {
        labels.add(entries.get(i));
      }
    }
    return new Pair<SparseFloatVector, LabelList>(new SparseFloatVector(values, dimensionality), labels);
  }

  /**
   * 
   * @see de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser#parse(java.io.InputStream)
   */
  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    dimensionality = -1;
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    List<SparseFloatVector> vectors = new ArrayList<SparseFloatVector>();
    List<LabelList> lblc = new ArrayList<LabelList>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          Pair<SparseFloatVector, LabelList> pair = parseLineInternal(line);
          vectors.add(pair.first);
          lblc.add(pair.second);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    // Set maximum dimensionality
    for(int i = 0; i < vectors.size(); i++) {
      vectors.get(i).setDimensionality(dimensionality);
    }
    return MultipleObjectsBundle.makeSimple(getTypeInformation(dimensionality), vectors, TypeUtil.LABELLIST, lblc);
  }

  @Override
  protected VectorFieldTypeInformation<SparseFloatVector> getTypeInformation(int dimensionality) {
    final Map<Integer, Float> emptyMap = Collections.emptyMap();
    return new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class, dimensionality, new SparseFloatVector(emptyMap, dimensionality));
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
  public static class Parameterizer extends NumberVectorLabelParser.Parameterizer<SparseFloatVector> {
    @Override
    protected SparseFloatVectorLabelParser makeInstance() {
      return new SparseFloatVectorLabelParser(colSep, quoteChar, labelIndices);
    }
  }
}