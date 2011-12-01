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

import gnu.trove.map.hash.TIntFloatHashMap;

import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

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
// FIXME: Maxdim!
@Title("Sparse Float Vector Label Parser")
@Description("Parser for the following line format:\n" + "A single line provides a single point. Entries are separated by whitespace. " + "The values will be parsed as floats (resulting in a set of SparseFloatVectors). A line is expected in the following format: The first entry of each line is the number of attributes with coordinate value not zero. Subsequent entries are of the form (index, value), where index is the number of the corresponding dimension, and value is the value of the corresponding attribute." + "Any pair of two subsequent substrings not containing whitespace is tried to be read as int and float. If this fails for the first of the pair (interpreted ans index), it will be appended to a label. (Thus, any label must not be parseable as Integer.) If the float component is not parseable, an exception will be thrown. Empty lines and lines beginning with \"#\" will be ignored. Having the file parsed completely, the maximum occuring dimensionality is set as dimensionality to all created SparseFloatvectors.")
public class SparseFloatVectorLabelParser extends NumberVectorLabelParser<SparseFloatVector> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(SparseFloatVectorLabelParser.class);

  /**
   * Holds the dimensionality of the parsed data which is the maximum occurring
   * index of any attribute.
   */
  private int maxdim = -1;

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public SparseFloatVectorLabelParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices, SparseFloatVector.STATIC);
  }

  @Override
  protected void parseLineInternal(String line) {
    List<String> entries = tokenize(line);
    int cardinality = Integer.parseInt(entries.get(0));

    TIntFloatHashMap values = new TIntFloatHashMap(cardinality, 1);
    LabelList labels = null;

    for(int i = 1; i < entries.size() - 1; i++) {
      if(!labelIndices.get(i)) {
        try {
          int index = Integer.valueOf(entries.get(i));
          if(index > maxdim) {
            maxdim = index;
          }
          float attribute = Float.valueOf(entries.get(i));
          values.put(index, attribute);
          i++;
        }
        catch(NumberFormatException e) {
          if(labels == null) {
            labels = new LabelList(1);
          }
          labels.add(entries.get(i));
          continue;
        }
      }
      else {
        if(labels == null) {
          labels = new LabelList(1);
        }
        labels.add(entries.get(i));
      }
    }
    curvec = new SparseFloatVector(values, maxdim);
    curlbl = labels;
  }

  @Override
  protected VectorFieldTypeInformation<SparseFloatVector> getTypeInformation(int dimensionality) {
    return new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class, dimensionality, new SparseFloatVector(SparseFloatVector.EMPTYMAP, dimensionality));
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