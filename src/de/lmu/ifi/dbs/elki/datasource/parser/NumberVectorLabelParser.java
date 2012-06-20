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

import gnu.trove.list.array.TDoubleArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
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
public class NumberVectorLabelParser<V extends NumberVector<V, ?>> extends AbstractStreamingParser {
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
   * Constant used for unknown dimensionality (e.g. empty files)
   */
  public static final int DIMENSIONALITY_UNKNOWN = -1;

  /**
   * Constant used for records of variable dimensionality (e.g. time series)
   */
  public static final int DIMENSIONALITY_VARIABLE = -2;

  /**
   * Keeps the indices of the attributes to be treated as a string label.
   */
  protected BitSet labelIndices;

  /**
   * Vector factory class
   */
  protected V factory;

  /**
   * Buffer reader
   */
  private BufferedReader reader;

  /**
   * Current line number
   */
  protected int lineNumber;

  /**
   * Dimensionality reported
   */
  protected int dimensionality;

  /**
   * Metadata
   */
  protected BundleMeta meta = null;

  /**
   * Column names
   */
  protected List<String> columnnames = null;

  /**
   * Bitset to indicate which columns are numeric
   */
  protected BitSet labelcolumns = null;

  /**
   * Current vector
   */
  protected V curvec = null;

  /**
   * Current labels
   */
  protected LabelList curlbl = null;

  /**
   * Event to report next
   */
  Event nextevent = null;

  /**
   * Constructor with defaults
   * 
   * @param factory Vector factory
   */
  public NumberVectorLabelParser(V factory) {
    this(Pattern.compile(DEFAULT_SEPARATOR), QUOTE_CHAR, null, factory);
  }

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
  public void initStream(InputStream in) {
    reader = new BufferedReader(new InputStreamReader(in));
    lineNumber = 1;
    dimensionality = DIMENSIONALITY_UNKNOWN;
    columnnames = null;
    labelcolumns = new BitSet();
  }

  @Override
  public BundleMeta getMeta() {
    return meta;
  }

  @Override
  public Event nextEvent() {
    if(nextevent != null) {
      Event ret = nextevent;
      nextevent = null;
      return ret;
    }
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          parseLineInternal(line);
          // Maybe a header column?
          if(curvec == null) {
            continue;
          }
          if(dimensionality == DIMENSIONALITY_UNKNOWN) {
            dimensionality = curvec.getDimensionality();
            buildMeta();
            nextevent = Event.NEXT_OBJECT;
            return Event.META_CHANGED;
          }
          else if(dimensionality > 0) {
            if(dimensionality != curvec.getDimensionality()) {
              dimensionality = DIMENSIONALITY_VARIABLE;
              buildMeta();
              nextevent = Event.NEXT_OBJECT;
              return Event.META_CHANGED;
            }
          }
          return Event.NEXT_OBJECT;
        }
      }
      reader.close();
      reader = null;
      return Event.END_OF_STREAM;
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
  }

  /**
   * Update the meta element.
   */
  protected void buildMeta() {
    meta = new BundleMeta(2);
    meta.add(getTypeInformation(dimensionality));
    meta.add(TypeUtil.LABELLIST);
  }

  @Override
  public Object data(int rnum) {
    if(rnum == 0) {
      return curvec;
    }
    if(rnum == 1) {
      return curlbl;
    }
    throw new ArrayIndexOutOfBoundsException();
  }

  /**
   * Internal method for parsing a single line. Used by both line based parsing
   * as well as block parsing. This saves the building of meta data for each
   * line.
   * 
   * @param line Line to process
   */
  protected void parseLineInternal(String line) {
    List<String> entries = tokenize(line);
    // Split into numerical attributes and labels
    TDoubleArrayList attributes = new TDoubleArrayList(entries.size());
    LabelList labels = null;

    Iterator<String> itr = entries.iterator();
    for(int i = 0; itr.hasNext(); i++) {
      String ent = itr.next();
      if(!labelIndices.get(i)) {
        try {
          double attribute = Double.parseDouble(ent);
          attributes.add(attribute);
          continue;
        }
        catch(NumberFormatException e) {
          // Ignore attempt, add to labels below.
          labelcolumns.set(i);
        }
      }
      if(labels == null) {
        labels = new LabelList(1);
      }
      labels.add(ent);
    }
    // Maybe a label row?
    if(lineNumber == 1 && attributes.size() == 0) {
      columnnames = labels;
      labelcolumns.clear();
      curvec = null;
      curlbl = null;
      return;
    }
    // Pass outside via class variables
    curvec = createDBObject(attributes, ArrayLikeUtil.TDOUBLELISTADAPTER);
    curlbl = labels;
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
    return factory.newNumberVector(attributes, adapter);
  }

  /**
   * Get a prototype object for the given dimensionality.
   * 
   * @param dimensionality Dimensionality
   * @return Prototype object
   */
  SimpleTypeInformation<V> getTypeInformation(int dimensionality) {
    @SuppressWarnings("unchecked")
    Class<V> cls = (Class<V>) factory.getClass();
    if(dimensionality > 0) {
      String[] colnames = null;
      if(columnnames != null) {
        if(columnnames.size() - labelcolumns.cardinality() == dimensionality) {
          colnames = new String[dimensionality];
          for(int i = 0, j = 0; i < columnnames.size(); i++) {
            if(labelcolumns.get(i) == false) {
              colnames[j] = columnnames.get(i);
              j++;
            }
          }
        }
      }
      V f = factory.newNumberVector(new double[dimensionality]);
      if(f instanceof ByteBufferSerializer) {
        // TODO: Remove, once we have serializers for all types
        @SuppressWarnings("unchecked")
        final ByteBufferSerializer<V> ser = (ByteBufferSerializer<V>) f;
        return new VectorFieldTypeInformation<V>(cls, ser, dimensionality, colnames, f);
      }
      return new VectorFieldTypeInformation<V>(cls, dimensionality, colnames, f);
    }
    // Variable dimensionality - return non-vector field type
    if(dimensionality == DIMENSIONALITY_VARIABLE) {
      V f = factory.newNumberVector(new double[0]);
      if(f instanceof ByteBufferSerializer) {
        // TODO: Remove, once we have serializers for all types
        @SuppressWarnings("unchecked")
        final ByteBufferSerializer<V> ser = (ByteBufferSerializer<V>) f;
        return new SimpleTypeInformation<V>(cls, ser);
      }
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