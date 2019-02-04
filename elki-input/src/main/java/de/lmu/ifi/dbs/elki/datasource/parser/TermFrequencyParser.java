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

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * A parser to load term frequency data, which essentially are sparse vectors
 * with text keys.
 * <p>
 * Parse a file containing term frequencies. The expected format is:
 * 
 * <pre>
 * rowlabel1 term1 &lt;freq&gt; term2 &lt;freq&gt; ...
 * rowlabel2 term1 &lt;freq&gt; term3 &lt;freq&gt; ...
 * </pre>
 * 
 * Terms must not contain the separator character!
 * <p>
 * If your data does not contain frequencies, you can maybe use
 * {@link SimpleTransactionParser} instead.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - SparseNumberVector
 */
public class TermFrequencyParser<V extends SparseNumberVector> extends NumberVectorLabelParser<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(TermFrequencyParser.class);

  /**
   * Number of different terms observed.
   */
  int numterms;

  /**
   * Map.
   */
  Object2IntOpenHashMap<String> keymap;

  /**
   * Normalize.
   */
  boolean normalize;

  /**
   * Same as {@link #factory}, but subtype.
   */
  private SparseNumberVector.Factory<V> sparsefactory;

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
   * @param normalize Normalize
   * @param factory Vector type
   */
  public TermFrequencyParser(boolean normalize, SparseNumberVector.Factory<V> factory) {
    this(normalize, CSVReaderFormat.DEFAULT_FORMAT, null, factory);
  }

  /**
   * Constructor.
   * 
   * @param normalize Normalize
   * @param format Input format
   * @param labelIndices Indices to use as labels
   * @param factory Vector type
   */
  public TermFrequencyParser(boolean normalize, CSVReaderFormat format, long[] labelIndices, SparseNumberVector.Factory<V> factory) {
    super(format, labelIndices, factory);
    this.normalize = normalize;
    this.keymap = new Object2IntOpenHashMap<>();
    this.keymap.defaultReturnValue(-1);
    this.sparsefactory = factory;
  }

  @Override
  protected boolean parseLineInternal() {
    double len = 0;

    String curterm = null;
    int c = 0;
    for(/* initialized by nextLineExceptComments() */; tokenizer.valid(); tokenizer.advance()) {
      if(isLabelColumn(c++)) {
        labels.add(tokenizer.getSubstring());
        continue;
      }
      if(curterm == null) {
        curterm = tokenizer.getSubstring();
        continue;
      }
      try {
        double attribute = tokenizer.getDouble();
        int curdim = keymap.getInt(curterm);
        if(curdim < 0) {
          curdim = numterms;
          keymap.put(curterm, curdim);
          ++numterms;
        }
        values.put(curdim, attribute);
        len += attribute;
        curterm = null;
      }
      catch(NumberFormatException e) {
        if(!warnedPrecision && (e == ParseUtil.PRECISION_OVERFLOW || e == ParseUtil.EXPONENT_OVERFLOW)) {
          getLogger().warning("Too many digits in what looked like a double number - treating as string: " + tokenizer.getSubstring());
          warnedPrecision = true;
        }
        labels.add(curterm);
        curterm = tokenizer.getSubstring();
      }
    }
    if(curterm != null) {
      labels.add(curterm);
    }
    haslabels |= !labels.isEmpty();
    if(normalize && Math.abs(len - 1.0) > Double.MIN_NORMAL) {
      for(ObjectIterator<Int2DoubleMap.Entry> iter = values.int2DoubleEntrySet().fastIterator(); iter.hasNext();) {
        Int2DoubleMap.Entry entry = iter.next();
        entry.setValue(entry.getDoubleValue() / len);
      }
    }

    curvec = sparsefactory.newNumberVector(values, numterms);
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
    /**
     * Option ID for normalization.
     */
    public static final OptionID NORMALIZE_FLAG = new OptionID("tf.normalize", "Normalize vectors to manhattan length 1 (convert term counts to term frequencies)");

    /**
     * Normalization flag.
     */
    boolean normalize = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag normF = new Flag(NORMALIZE_FLAG);
      if(config.grab(normF)) {
        normalize = normF.isTrue();
      }
    }

    @Override
    protected void getFactory(Parameterization config) {
      ObjectParameter<SparseNumberVector.Factory<V>> factoryP = new ObjectParameter<>(VECTOR_TYPE_ID, SparseNumberVector.Factory.class, SparseFloatVector.Factory.class);
      if(config.grab(factoryP)) {
        factory = factoryP.instantiateClass(config);
      }
    }

    @Override
    protected TermFrequencyParser<V> makeInstance() {
      return new TermFrequencyParser<>(normalize, format, labelIndices, (SparseNumberVector.Factory<V>) factory);
    }
  }
}
