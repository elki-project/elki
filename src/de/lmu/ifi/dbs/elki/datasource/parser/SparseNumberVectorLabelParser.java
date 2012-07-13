package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SparseDoubleVector;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * are of the form <code>index value </code> each, where index is the number of
 * the corresponding dimension, and value is the value of the corresponding
 * attribute. A complet line then could look like this:
 * 
 * <pre>
 * 3 7 12.34 8 56.78 11 1.234 objectlabel
 * </pre>
 * 
 * where <code>3</code> indicates there are three attributes set,
 * <code>7,8,11</code> are the attributes indexes and there is a non-numerical
 * object label.
 * </p>
 * <p>
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has SparseNumberVector
 */
// FIXME: Maxdim!
@Title("Sparse Vector Label Parser")
@Description("Parser for the following line format:\n" + "A single line provides a single point. Entries are separated by whitespace. " + "The values will be parsed as floats (resulting in a set of SparseFloatVectors). A line is expected in the following format: The first entry of each line is the number of attributes with coordinate value not zero. Subsequent entries are of the form (index, value), where index is the number of the corresponding dimension, and value is the value of the corresponding attribute." + "Any pair of two subsequent substrings not containing whitespace is tried to be read as int and float. If this fails for the first of the pair (interpreted ans index), it will be appended to a label. (Thus, any label must not be parseable as Integer.) If the float component is not parseable, an exception will be thrown. Empty lines and lines beginning with \"#\" will be ignored.")
public class SparseNumberVectorLabelParser<V extends SparseNumberVector<V, ?>> extends NumberVectorLabelParser<V> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(SparseNumberVectorLabelParser.class);

  /**
   * Holds the dimensionality of the parsed data which is the maximum occurring
   * index of any attribute.
   */
  private int maxdim = -1;

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChar Quotation character
   * @param labelIndices Label indexes
   * @param factory Vector factory
   */
  public SparseNumberVectorLabelParser(Pattern colSep, char quoteChar, BitSet labelIndices, V factory) {
    super(colSep, quoteChar, labelIndices, factory);
  }

  @Override
  protected void parseLineInternal(String line) {
    List<String> entries = tokenize(line);
    int cardinality = Integer.parseInt(entries.get(0));

    TIntDoubleHashMap values = new TIntDoubleHashMap(cardinality, 1);
    LabelList labels = null;

    for(int i = 1; i < entries.size() - 1; i++) {
      if(labelIndices == null || !labelIndices.get(i)) {
        try {
          int index = Integer.valueOf(entries.get(i));
          if(index >= maxdim) {
            maxdim = index + 1;
          }
          double attribute = Double.valueOf(entries.get(i));
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
    if(values.size() > maxdim) {
      throw new AbortException("Invalid sparse vector seen: " + line);
    }
    curvec = factory.newNumberVector(values, maxdim);
    curlbl = labels;
  }

  @Override
  protected SimpleTypeInformation<V> getTypeInformation(int dimensionality) {
    @SuppressWarnings("unchecked")
    Class<V> cls = (Class<V>) factory.getClass();
    if(dimensionality > 0) {
      return new VectorFieldTypeInformation<V>(cls, dimensionality, factory.newNumberVector(SparseDoubleVector.EMPTYMAP, dimensionality));
    }
    if(dimensionality == DIMENSIONALITY_VARIABLE) {
      return new SimpleTypeInformation<V>(cls);
    }
    throw new AbortException("No vectors were read from the input file - cannot determine vector data type.");
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
  public static class Parameterizer<V extends SparseNumberVector<V, ?>> extends NumberVectorLabelParser.Parameterizer<V> {
    @Override
    protected void getFactory(Parameterization config) {
      ObjectParameter<V> factoryP = new ObjectParameter<V>(VECTOR_TYPE_ID, SparseNumberVector.class, SparseFloatVector.class);
      if(config.grab(factoryP)) {
        factory = factoryP.instantiateClass(config);
      }
    }

    @Override
    protected SparseNumberVectorLabelParser<V> makeInstance() {
      return new SparseNumberVectorLabelParser<V>(colSep, quoteChar, labelIndices, factory);
    }
  }
}