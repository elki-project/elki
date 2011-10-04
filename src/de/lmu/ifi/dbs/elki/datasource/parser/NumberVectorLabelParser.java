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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
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
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * @apiviz.has NumberVector
 * 
 * @param <V> the type of NumberVector used
 */
public class NumberVectorLabelParser<V extends NumberVector<V, ?>> extends AbstractParser implements LinebasedParser, Parser {
  /**
   * Logging class.
   */
  private static final Logging logger = Logging.getLogger(NumberVectorLabelParser.class);

  /**
   * A comma separated list of the indices of labels (may be numeric), counting
   * whitespace separated entries in a line starting with 0. The corresponding
   * entries will be treated as a label.
   * <p>
   * Key: {@code -parser.labelIndices}
   * </p>
   */
  public static final OptionID LABEL_INDICES_ID = OptionID.getOrCreateOptionID("parser.labelIndices", "A comma separated list of the indices of labels (may be numeric), counting whitespace separated entries in a line starting with 0. The corresponding entries will be treated as a label.");

  /**
   * Parameter to specify the type of vectors to produce.
   * <p>
   * Key: {@code -parser.vector-type}<br />
   * Default: DoubleVector
   * </p>
   */
  public static final OptionID VECTOR_TYPE_ID = OptionID.getOrCreateOptionID("parser.vector-type", "The type of vectors to create for numerical attributes.");

  /**
   * Keeps the indices of the attributes to be treated as a string label.
   */
  protected BitSet labelIndices;

  /**
   * Vector factory class
   */
  protected V factory;

  /**
   * Constructor
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   * @param factory Vector factory
   */
  public NumberVectorLabelParser(Pattern colSep, char quoteChar, BitSet labelIndices, V factory) {
    super(colSep, quoteChar);
    this.labelIndices = labelIndices;
    this.factory = factory;
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    int dimensionality = -1;
    List<V> vectors = new ArrayList<V>();
    List<LabelList> labels = new ArrayList<LabelList>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          parseLineInternal(line, vectors, labels);
          V newvec = vectors.get(vectors.size());
          if(dimensionality < 0) {
            dimensionality = newvec.getDimensionality();
          }
          else {
            if(dimensionality != newvec.getDimensionality()) {
              throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ":" + newvec.getDimensionality() + " != " + dimensionality);
            }
          }
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    return MultipleObjectsBundle.makeSimple(getTypeInformation(dimensionality), vectors, TypeUtil.LABELLIST, labels);
  }

  @Override
  public SingleObjectBundle parseLine(String line) {
    // TODO: code duplication with parseLineInternal below.
    List<String> entries = tokenize(line);
    // Split into numerical attributes and labels
    List<Double> attributes = new ArrayList<Double>(entries.size());
    LabelList labels = new LabelList();

    Iterator<String> itr = entries.iterator();
    for(int i = 0; itr.hasNext(); i++) {
      String ent = itr.next();
      if(!labelIndices.get(i)) {
        try {
          Double attribute = Double.valueOf(ent);
          attributes.add(attribute);
          continue;
        }
        catch(NumberFormatException e) {
          // Ignore attempt, add to labels below.
        }
      }
      labels.add(ent);
    }
    V vec = createDBObject(attributes, ArrayUtil.numberListAdapter(attributes));
    SingleObjectBundle pkg = new SingleObjectBundle();
    pkg.append(getTypeInformation(vec.getDimensionality()), vec);
    pkg.append(TypeUtil.LABELLIST, labels);
    return pkg;
  }

  /**
   * Internal method for parsing a single line. Used by both line based parsig
   * as well as block parsing. This saves the building of meta data for each
   * line.
   * 
   * @param line Line to process
   * @param vectors Vectors
   * @param labellist Labels
   */
  protected void parseLineInternal(String line, List<V> vectors, List<LabelList> labellist) {
    List<String> entries = tokenize(line);
    // Split into numerical attributes and labels
    List<Double> attributes = new ArrayList<Double>(entries.size());
    LabelList labels = new LabelList();

    Iterator<String> itr = entries.iterator();
    for(int i = 0; itr.hasNext(); i++) {
      String ent = itr.next();
      if(!labelIndices.get(i)) {
        try {
          Double attribute = Double.valueOf(ent);
          attributes.add(attribute);
          continue;
        }
        catch(NumberFormatException e) {
          // Ignore attempt, add to labels below.
        }
      }
      labels.add(ent);
    }

    vectors.add(createDBObject(attributes, ArrayUtil.numberListAdapter(attributes)));
    labellist.add(labels);
  }

  /**
   * <p>
   * Creates a database object of type V.
   * </p>
   * 
   * @param attributes the attributes of the vector to create.
   * @return a RalVector of type V containing the given attribute values
   */
  protected <A> V createDBObject(A attributes, NumberArrayAdapter<?, A> adapter) {
    return factory.newInstance(attributes, adapter);
  }

  /**
   * Get a prototype object for the given dimensionality.
   * 
   * @param dimensionality Dimensionality
   * @return Prototype object
   */
  VectorFieldTypeInformation<V> getTypeInformation(int dimensionality) {
    @SuppressWarnings("unchecked")
    Class<V> cls = (Class<V>) factory.getClass();
    return new VectorFieldTypeInformation<V>(cls, dimensionality, factory);
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
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParser.Parameterizer {
    /**
     * Keeps the indices of the attributes to be treated as a string label.
     */
    protected BitSet labelIndices = null;

    /**
     * Factory
     */
    protected V factory;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      getLabelIndices(config);
      getFactory(config);
    }

    protected void getFactory(Parameterization config) {
      ObjectParameter<V> factoryP = new ObjectParameter<V>(VECTOR_TYPE_ID, NumberVector.class, DoubleVector.class);
      if(config.grab(factoryP)) {
        factory = factoryP.instantiateClass(config);
      }
    }

    protected void getLabelIndices(Parameterization config) {
      IntListParameter labelIndicesP = new IntListParameter(LABEL_INDICES_ID, true);

      labelIndices = new BitSet();
      if(config.grab(labelIndicesP)) {
        List<Integer> labelcols = labelIndicesP.getValue();
        for(Integer idx : labelcols) {
          labelIndices.set(idx);
        }
      }
    }

    @Override
    protected NumberVectorLabelParser<V> makeInstance() {
      return new NumberVectorLabelParser<V>(colSep, quoteChar, labelIndices, factory);
    }
  }
}