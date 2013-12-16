package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.logging.Logging;
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
public class NumberVectorLabelParser<V extends NumberVector<?>> extends AbstractStreamingParser {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(NumberVectorLabelParser.class);

  /**
   * Keeps the indices of the attributes to be treated as a string label.
   */
  protected BitSet labelIndices;

  /**
   * Vector factory class.
   */
  protected NumberVector.Factory<V, ?> factory;

  /**
   * Buffer reader.
   */
  private BufferedReader reader;

  /**
   * Current line number.
   */
  protected int lineNumber;

  /**
   * Dimensionality reported.
   */
  protected int mindim, maxdim;

  /**
   * Metadata.
   */
  protected BundleMeta meta = null;

  /**
   * Column names.
   */
  protected List<String> columnnames = null;

  /**
   * Bitset to indicate which columns are not numeric.
   */
  protected BitSet labelcolumns = null;

  /**
   * Whether or not the data set has labels.
   */
  protected boolean haslabels = false;

  /**
   * Current vector.
   */
  protected V curvec = null;

  /**
   * Current labels.
   */
  protected LabelList curlbl = null;

  /**
   * (Reused) store for numerical attributes.
   */
  final TDoubleArrayList attributes = new TDoubleArrayList();

  /**
   * (Reused) store for labels.
   */
  final ArrayList<String> labels = new ArrayList<>();

  /**
   * For String unification.
   */
  HashMap<String, String> unique = new HashMap<>();

  /**
   * Event to report next.
   */
  Event nextevent = null;

  /**
   * Constructor with defaults.
   * 
   * @param factory Vector factory
   */
  public NumberVectorLabelParser(NumberVector.Factory<V, ?> factory) {
    this(Pattern.compile(DEFAULT_SEPARATOR), QUOTE_CHARS, Pattern.compile(COMMENT_PATTERN), null, factory);
  }

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChars Quote character
   * @param comment Comment pattern
   * @param labelIndices Column indexes that are numeric.
   * @param factory Vector factory
   */
  public NumberVectorLabelParser(Pattern colSep, String quoteChars, Pattern comment, BitSet labelIndices, NumberVector.Factory<V, ?> factory) {
    super(colSep, quoteChars, comment);
    this.labelIndices = labelIndices;
    this.factory = factory;
  }

  @Override
  public void initStream(InputStream in) {
    reader = new BufferedReader(new InputStreamReader(in));
    lineNumber = 1;
    mindim = Integer.MAX_VALUE;
    maxdim = 0;
    columnnames = null;
    haslabels = false;
    labelcolumns = new BitSet();
    if(labelIndices != null) {
      labelcolumns.or(labelIndices);
    }
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
        // Skip empty lines and comments
        if(line.length() <= 0 || (comment != null && comment.matcher(line).matches())) {
          continue;
        }
        parseLineInternal(line);
        // Maybe a header column?
        if(curvec == null) {
          continue;
        }
        final int curdim = curvec.getDimensionality();
        if(curdim > maxdim || mindim > curdim) {
          mindim = Math.min(mindim, curdim);
          maxdim = Math.max(maxdim, curdim);
          buildMeta();
          nextevent = Event.NEXT_OBJECT;
          return Event.META_CHANGED;
        }
        else if(curlbl != null && meta != null && meta.size() == 1) {
          buildMeta();
          nextevent = Event.NEXT_OBJECT;
          return Event.META_CHANGED;
        }
        return Event.NEXT_OBJECT;
      }
      reader.close();
      reader = null;
      unique.clear();
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
    if(haslabels) {
      meta = new BundleMeta(2);
      meta.add(getTypeInformation(mindim, maxdim));
      meta.add(TypeUtil.LABELLIST);
    }
    else {
      meta = new BundleMeta(1);
      meta.add(getTypeInformation(mindim, maxdim));
    }
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
    attributes.reset();
    labels.clear();

    // Split into numerical attributes and labels
    int i = 0;
    for(tokenizer.initialize(line, 0, lengthWithoutLinefeed(line)); tokenizer.valid(); tokenizer.advance(), i++) {
      if(labelIndices == null || !labelIndices.get(i)) {
        try {
          double attribute = tokenizer.getDouble();
          attributes.add(attribute);
          continue;
        }
        catch(NumberFormatException e) {
          // Ignore attempt, add to labels below.
          labelcolumns.set(i);
        }
      }
      // Else: labels.
      haslabels = true;
      final String lbl = tokenizer.getSubstring();
      String u = unique.get(lbl);
      if(u == null) {
        u = lbl;
        unique.put(u, u);
      }
      labels.add(u);
    }
    // Maybe a label row?
    if(lineNumber == 1 && attributes.size() == 0) {
      columnnames = new ArrayList<>(labels);
      labelcolumns.clear();
      if(labelIndices != null) {
        labelcolumns.or(labelIndices);
      }
      curvec = null;
      curlbl = null;
      haslabels = false;
      return;
    }
    // Pass outside via class variables
    curvec = createDBObject(attributes, ArrayLikeUtil.TDOUBLELISTADAPTER);
    curlbl = LabelList.make(labels);
  }

  /**
   * Creates a database object of type V.
   * 
   * @param attributes the attributes of the vector to create.
   * @param adapter Array adapter
   * @param <A> attribute type
   * @return a RalVector of type V containing the given attribute values
   */
  protected <A> V createDBObject(A attributes, NumberArrayAdapter<?, A> adapter) {
    return factory.newNumberVector(attributes, adapter);
  }

  /**
   * Get a prototype object for the given dimensionality.
   * 
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @return Prototype object
   */
  SimpleTypeInformation<V> getTypeInformation(int mindim, int maxdim) {
    if(mindim == maxdim) {
      String[] colnames = null;
      if(columnnames != null) {
        if(columnnames.size() - labelcolumns.cardinality() == mindim) {
          colnames = new String[mindim];
          for(int i = 0, j = 0; i < columnnames.size(); i++) {
            if(!labelcolumns.get(i)) {
              colnames[j] = columnnames.get(i);
              j++;
            }
          }
        }
      }
      return new VectorFieldTypeInformation<>(factory, mindim, colnames);
    }
    else if(mindim < maxdim) {
      // Variable dimensionality - return non-vector field type
      return new VectorTypeInformation<>(factory.getRestrictionClass(), factory.getDefaultSerializer(), mindim, maxdim);
    }
    else {
      throw new AbortException("No vectors were read from the input file - cannot determine vector data type.");
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParser.Parameterizer {
    /**
     * A comma separated list of the indices of labels (may be numeric),
     * counting whitespace separated entries in a line starting with 0. The
     * corresponding entries will be treated as a label.
     * <p>
     * Key: {@code -parser.labelIndices}
     * </p>
     */
    public static final OptionID LABEL_INDICES_ID = new OptionID("parser.labelIndices", "A comma separated list of the indices of labels (may be numeric), counting whitespace separated entries in a line starting with 0. The corresponding entries will be treated as a label.");

    /**
     * Parameter to specify the type of vectors to produce.
     * <p>
     * Key: {@code -parser.vector-type}<br />
     * Default: DoubleVector
     * </p>
     */
    public static final OptionID VECTOR_TYPE_ID = new OptionID("parser.vector-type", "The type of vectors to create for numerical attributes.");

    /**
     * Keeps the indices of the attributes to be treated as a string label.
     */
    protected BitSet labelIndices = null;

    /**
     * Factory object.
     */
    protected NumberVector.Factory<V, ?> factory;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      getLabelIndices(config);
      getFactory(config);
    }

    /**
     * Get the object factory.
     * 
     * @param config Parameterization
     */
    protected void getFactory(Parameterization config) {
      ObjectParameter<NumberVector.Factory<V, ?>> factoryP = new ObjectParameter<>(VECTOR_TYPE_ID, NumberVector.Factory.class, DoubleVector.Factory.class);
      if(config.grab(factoryP)) {
        factory = factoryP.instantiateClass(config);
      }
    }

    /**
     * Get the label indices.
     * 
     * @param config Parameterization
     */
    protected void getLabelIndices(Parameterization config) {
      IntListParameter labelIndicesP = new IntListParameter(LABEL_INDICES_ID, true);

      if(config.grab(labelIndicesP)) {
        labelIndices = new BitSet();
        List<Integer> labelcols = labelIndicesP.getValue();
        for(Integer idx : labelcols) {
          labelIndices.set(idx.intValue());
        }
      }
    }

    @Override
    protected NumberVectorLabelParser<V> makeInstance() {
      return new NumberVectorLabelParser<>(colSep, quoteChars, comment, labelIndices, factory);
    }
  }
}
