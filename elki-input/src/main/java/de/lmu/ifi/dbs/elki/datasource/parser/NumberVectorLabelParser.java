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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.NumberVector.Factory;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Parser for a simple CSV type of format, with columns separated by the given
 * pattern (default: whitespace).
 *
 * Several labels may be given per point. A label must not be parseable as
 * double. Lines starting with &quot;#&quot; will be ignored.
 *
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 *
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.1
 *
 * @opt nodefillcolor LemonChiffon
 * @has - - - NumberVector
 *
 * @param <V> the type of NumberVector used
 */
@Alias({ "de.lmu.ifi.dbs.elki.parser.NumberVectorLabelParser", //
    "de.lmu.ifi.dbs.elki.parser.RealVectorLabelParser" })
public class NumberVectorLabelParser<V extends NumberVector> extends AbstractStreamingParser {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(NumberVectorLabelParser.class);

  /**
   * Keeps the indices of the attributes to be treated as a string label.
   */
  private long[] labelIndices;

  /**
   * Vector factory class.
   */
  protected NumberVector.Factory<V> factory;

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
   * Double array storing the numerical attributes during parsing.
   */
  protected DoubleArray attributes = new DoubleArray(11);

  /**
   * (Reused) store for labels.
   */
  final ArrayList<String> labels = new ArrayList<>();

  /**
   * For String unification.
   */
  ObjectOpenHashSet<String> unique = new ObjectOpenHashSet<>();

  /**
   * Event to report next.
   */
  Event nextevent = null;

  /**
   * Emit a double-precision limit warning once.
   */
  boolean warnedPrecision = false;

  /**
   * Constructor.
   *
   * @param format Input format
   * @param labelIndices Column indexes that are not numeric.
   * @param factory Vector factory
   */
  public NumberVectorLabelParser(CSVReaderFormat format, long[] labelIndices, Factory<V> factory) {
    super(format);
    this.labelIndices = labelIndices;
    this.factory = factory;
  }

  /**
   * Constructor with defaults.
   *
   * @param factory Vector factory
   */
  public NumberVectorLabelParser(NumberVector.Factory<V> factory) {
    this(CSVReaderFormat.DEFAULT_FORMAT, null, factory);
  }

  /**
   * Constructor.
   *
   * @param colSep Column separator
   * @param quoteChars Quote character
   * @param comment Comment pattern
   * @param labelIndices Column indexes that are not numeric.
   * @param factory Vector factory
   */
  public NumberVectorLabelParser(Pattern colSep, String quoteChars, Pattern comment, long[] labelIndices, NumberVector.Factory<V> factory) {
    this(new CSVReaderFormat(colSep, quoteChars, comment), labelIndices, factory);
  }

  /**
   * Test if the current column is marked as label column.
   *
   * @param col Column number
   * @return {@code true} when a label column.
   */
  protected boolean isLabelColumn(int col) {
    return labelIndices != null && BitsUtil.get(labelIndices, col);
  }

  @Override
  public void initStream(InputStream in) {
    super.initStream(in);
    mindim = Integer.MAX_VALUE;
    maxdim = 0;
    columnnames = null;
    haslabels = false;
    nextevent = null;
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
      while(reader.nextLineExceptComments()) {
        if(parseLineInternal()) {
          final int curdim = curvec.getDimensionality();
          if(curdim > maxdim || mindim > curdim) {
            mindim = (curdim < mindim) ? curdim : mindim;
            maxdim = (curdim > maxdim) ? curdim : maxdim;
            buildMeta();
            nextevent = Event.NEXT_OBJECT;
            return Event.META_CHANGED;
          }
          else if(curlbl != null && meta != null && haslabels && meta.size() == 1) {
            buildMeta();
            nextevent = Event.NEXT_OBJECT;
            return Event.META_CHANGED;
          }
          return Event.NEXT_OBJECT;
        }
      }
      if(maxdim == 0) {
        throw new AbortException("No numeric data was read in line " + reader.getLineNumber() + ". Verify the column separator; for textual data use other parsers.");
      }
      return Event.END_OF_STREAM;
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + reader.getLineNumber() + ".");
    }
  }

  @Override
  public void cleanup() {
    super.cleanup();
    unique.clear();
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
    if(rnum > 1) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return (rnum == 0) ? curvec : curlbl;
  }

  /**
   * Internal method for parsing a single line. Used by both line based parsing
   * as well as block parsing. This saves the building of meta data for each
   * line.
   *
   * @return {@code true} when a valid line was read, {@code false} on a label
   *         row.
   */
  protected boolean parseLineInternal() {
    // Split into numerical attributes and labels
    int i = 0;
    for(/* initialized by nextLineExceptComents()! */; tokenizer.valid(); tokenizer.advance(), i++) {
      if(!isLabelColumn(i) && !tokenizer.isQuoted()) {
        try {
          attributes.add(tokenizer.getDouble());
          continue;
        }
        catch(NumberFormatException e) {
          if(!warnedPrecision && (e == ParseUtil.PRECISION_OVERFLOW || e == ParseUtil.EXPONENT_OVERFLOW)) {
            getLogger().warning("Too many digits in what looked like a double number - treating as string: " + tokenizer.getSubstring());
            warnedPrecision = true;
          }
          // Ignore attempt, add to labels below.
        }
      }
      // Else: labels.
      String lbl = tokenizer.getStrippedSubstring();
      if(lbl.length() > 0) {
        haslabels = true;
        lbl = unique.addOrGet(lbl);
        labels.add(lbl);
      }
    }
    // Maybe a label row?
    if(curvec == null && attributes.size == 0) {
      columnnames = new ArrayList<>(labels);
      haslabels = false;
      curvec = null;
      curlbl = null;
      labels.clear();
      return false;
    }
    // Pass outside via class variables
    curvec = createVector();
    curlbl = LabelList.make(labels);
    attributes.clear();
    labels.clear();
    return true;
  }

  /**
   * Creates a database object of type V.
   *
   * @return a vector of type V containing the given attribute values
   */
  protected V createVector() {
    return factory.newNumberVector(attributes, attributes);
  }

  /**
   * Get a prototype object for the given dimensionality.
   *
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @return Prototype object
   */
  SimpleTypeInformation<V> getTypeInformation(int mindim, int maxdim) {
    if(mindim > maxdim) {
      throw new AbortException("No vectors were read from the input file - cannot determine vector data type.");
    }
    if(mindim == maxdim) {
      String[] colnames = null;
      if(columnnames != null && mindim <= columnnames.size()) {
        colnames = new String[mindim];
        int j = 0;
        for(int i = 0; i < mindim; i++) {
          if(isLabelColumn(i)) {
            continue;
          }
          colnames[j] = columnnames.get(i);
          j++;
        }
        if(j != mindim) {
          colnames = null; // Did not work
        }
      }
      return new VectorFieldTypeInformation<>(factory, mindim, colnames);
    }
    // Variable dimensionality - return non-vector field type
    return new VectorTypeInformation<>(factory, factory.getDefaultSerializer(), mindim, maxdim);
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
  public static class Parameterizer<V extends NumberVector> extends AbstractStreamingParser.Parameterizer {
    /**
     * A comma separated list of the indices of labels (may be numeric),
     * counting whitespace separated entries in a line starting with 0. The
     * corresponding entries will be treated as a label.
     */
    public static final OptionID LABEL_INDICES_ID = new OptionID("parser.labelIndices", "A comma separated list of the indices of labels (may be numeric), counting whitespace separated entries in a line starting with 0. The corresponding entries will be treated as a label.");

    /**
     * Parameter to specify the type of vectors to produce.
     */
    public static final OptionID VECTOR_TYPE_ID = new OptionID("parser.vector-type", "The type of vectors to create for numerical attributes.");

    /**
     * Keeps the indices of the attributes to be treated as a string label.
     */
    protected long[] labelIndices;

    /**
     * Factory object.
     */
    protected NumberVector.Factory<V> factory;

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
      ObjectParameter<NumberVector.Factory<V>> factoryP = new ObjectParameter<>(VECTOR_TYPE_ID, NumberVector.Factory.class, DoubleVector.Factory.class);
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
        labelIndices = labelIndicesP.getValueAsBitSet();
      }
    }

    @Override
    protected NumberVectorLabelParser<V> makeInstance() {
      return new NumberVectorLabelParser<>(format, labelIndices, factory);
    }
  }
}
