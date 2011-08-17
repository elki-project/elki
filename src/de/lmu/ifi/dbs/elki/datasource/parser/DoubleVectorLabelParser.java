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

import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * <p>
 * Provides a parser for parsing one point per line, attributes separated by
 * whitespace.
 * </p>
 * <p>
 * Several labels may be given per point. A label must not be parseable as
 * double. Lines starting with &quot;#&quot; will be ignored.
 * </p>
 * <p/>
 * <p>
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * 
 * @apiviz.has DoubleVector
 */
public class DoubleVectorLabelParser extends NumberVectorLabelParser<DoubleVector> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(DoubleVectorLabelParser.class);

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public DoubleVectorLabelParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices);
  }

  /**
   * Constructor with default values.
   */
  public DoubleVectorLabelParser() {
    this(Pattern.compile(WHITESPACE_PATTERN), QUOTE_CHAR.charAt(0), new BitSet());
  }

  /**
   * Creates a DoubleVector out of the given attribute values.
   * 
   * @see de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser#createDBObject(java.util.List)
   */
  @Override
  public DoubleVector createDBObject(List<Double> attributes) {
    return new DoubleVector(attributes);
  }

  @Override
  protected VectorFieldTypeInformation<DoubleVector> getTypeInformation(int dimensionality) {
    return new VectorFieldTypeInformation<DoubleVector>(DoubleVector.class, dimensionality, new DoubleVector(new double[dimensionality]));
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
  public static class Parameterizer extends NumberVectorLabelParser.Parameterizer<DoubleVector> {
    @Override
    protected DoubleVectorLabelParser makeInstance() {
      return new DoubleVectorLabelParser(colSep, quoteChar, labelIndices);
    }
  }
}