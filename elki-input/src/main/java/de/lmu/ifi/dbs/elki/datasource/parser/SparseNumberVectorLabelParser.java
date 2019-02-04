/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource.parser;

import java.util.ArrayList;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Parser for parsing one point per line, attributes separated by whitespace.
 * <p>
 * Several labels may be given per point. A label must not be parseable as
 * double. Lines starting with &quot;#&quot; will be ignored.
 * <p>
 * A line is expected in the following format: The first entry of each line is
 * the number of attributes with coordinate value not zero. Subsequent entries
 * are of the form <code>index value </code> each, where index is the number of
 * the corresponding dimension, and value is the value of the corresponding
 * attribute. A complete line then could look like this:
 *
 * <pre>
 * 3 7 12.34 8 56.78 11 1.234 objectlabel
 * </pre>
 *
 * where <code>3</code> indicates there are three attributes set,
 * <code>7,8,11</code> are the attributes indexes and there is a non-numerical
 * object label.
 * <p>
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 *
 * @author Arthur Zimek
 * @since 0.2
 *
 * @has - - - SparseNumberVector
 *
 * @param <V> vector type
 */
@Title("Sparse Vector Label Parser")
@Description("Parser for the following line format:\n" //
    + "A single line provides a single point. Entries are separated by whitespace. " //
    + "The values will be parsed as floats (resulting in a set of SparseFloatVectors).\n"//
    + "A line is expected in the following format:\n"//
    + "The first entry of each line is the number of attributes with coordinate value not zero. "//
    + "Subsequent entries are of the form (index, value), where index is the number of the corresponding dimension, "//
    + "and value is the value of the corresponding attribute. " //
    + "Any pair of two subsequent substrings not containing whitespace is tried to be read as int and float. "//
    + "If this fails for the first of the pair (interpreted ans index), it will be appended to a label. "//
    + "(Thus, any label must not be parseable as Integer.) "//
    + "If the float component is not parseable, an exception will be thrown. "//
    + "Empty lines and lines beginning with \"#\" will be ignored.")
public class SparseNumberVectorLabelParser<V extends SparseNumberVector> extends NumberVectorLabelParser<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SparseNumberVectorLabelParser.class);

  /**
   * Same as {@link #factory}, but subtype.
   */
  protected SparseNumberVector.Factory<V> sparsefactory;

  /**
   * (Reused) set of values for the number vector.
   */
  Int2DoubleOpenHashMap values = new Int2DoubleOpenHashMap();

  /**
   * (Reused) label buffer.
   */
  ArrayList<String> labels = new ArrayList<>();

  /**
   * Constructor.
   *
   * @param format Input format
   * @param labelIndices Indices to use as labels
   * @param factory Vector factory
   */
  public SparseNumberVectorLabelParser(CSVReaderFormat format, long[] labelIndices, SparseNumberVector.Factory<V> factory) {
    super(format, labelIndices, factory);
    this.sparsefactory = factory;
  }

  /**
   * Constructor.
   *
   * @param colSep Column separator
   * @param quoteChars Quotation character
   * @param comment Comment pattern
   * @param labelIndices Indices to use as labels
   * @param factory Vector factory
   */
  public SparseNumberVectorLabelParser(Pattern colSep, String quoteChars, Pattern comment, long[] labelIndices, SparseNumberVector.Factory<V> factory) {
    super(colSep, quoteChars, comment, labelIndices, factory);
    this.sparsefactory = factory;
  }

  @Override
  protected boolean parseLineInternal() {
    /* tokenizer initialized by nextLineExceptComments() */
    int cardinality;
    try {
      cardinality = tokenizer.getIntBase10();
    }
    catch(NumberFormatException e) {
      throw new NumberFormatException("Expected the number of values at the beginning of line " + reader.getLineNumber() + ", read '" + tokenizer.getSubstring() + "'");
    }
    tokenizer.advance();

    int thismax = 0, index = -1;

    while(tokenizer.valid()) {
      if(values.size() < cardinality) {
        try {
          // Try reading the next index:
          if(index < 0) {
            index = tokenizer.getIntBase10();
            tokenizer.advance();
            continue;
          }
          // Read the next value, but respect labelIndices.
          if(!isLabelColumn(index)) {
            double attribute = tokenizer.getDouble();
            thismax = index >= thismax ? index + 1 : thismax;
            values.put(index, attribute);
            tokenizer.advance();
            index = -1;
            continue;
          }
        }
        catch(NumberFormatException e) {
          if(!warnedPrecision && (e == ParseUtil.PRECISION_OVERFLOW || e == ParseUtil.EXPONENT_OVERFLOW)) {
            getLogger().warning("Too many digits in what looked like a double number - treating as string: " + tokenizer.getSubstring());
            warnedPrecision = true;
          }
          // continue with fallback below.
        }
      }
      // Fallback: treat as label
      haslabels = true;
      labels.add(tokenizer.getSubstring());
      tokenizer.advance();
    }
    if(index >= 0 && !tokenizer.valid()) {
      throw new IllegalArgumentException("Parser expected double value, but line ended too early: " + reader.getLineNumber());
    }
    curvec = sparsefactory.newNumberVector(values, thismax);
    curlbl = LabelList.make(labels);
    values.clear();
    labels.clear();
    return true;
  }

  @Override
  protected SimpleTypeInformation<V> getTypeInformation(int mindim, int maxdim) {
    if(mindim == maxdim) {
      return new VectorFieldTypeInformation<>(factory, mindim);
    }
    else if(mindim < maxdim) {
      return new VectorTypeInformation<>(factory, factory.getDefaultSerializer(), mindim, maxdim);
    }
    throw new AbortException("No vectors were read from the input file - cannot determine vector data type.");
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends SparseNumberVector> extends NumberVectorLabelParser.Parameterizer<V> {
    @Override
    protected void getFactory(Parameterization config) {
      ObjectParameter<SparseNumberVector.Factory<V>> factoryP = new ObjectParameter<>(VECTOR_TYPE_ID, SparseNumberVector.Factory.class, SparseFloatVector.Factory.class);
      if(config.grab(factoryP)) {
        factory = factoryP.instantiateClass(config);
      }
    }

    @Override
    protected SparseNumberVectorLabelParser<V> makeInstance() {
      return new SparseNumberVectorLabelParser<>(format, labelIndices, (SparseNumberVector.Factory<V>) factory);
    }
  }
}
