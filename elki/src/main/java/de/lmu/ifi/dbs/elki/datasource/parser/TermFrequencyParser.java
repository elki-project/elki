package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;

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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A parser to load term frequency data, which essentially are sparse vectors
 * with text keys.
 * 
 * If your data does not contain frequencies, you can maybe use
 * {@link SimpleTransactionParser} instead.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.has SparseNumberVector
 */
@Title("Term frequency parser")
@Description("Parse a file containing term frequencies. The expected format is 'label term1 <freq> term2 <freq> ...'. Terms must not contain the separator character!")
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
  TObjectIntMap<String> keymap;

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
  TIntDoubleHashMap values = new TIntDoubleHashMap();

  /**
   * (Reused) label buffer.
   */
  ArrayList<String> labels = new ArrayList<>();

  /**
   * Constructor.
   * 
   * @param normalize Normalize
   * @param format Input format
   * @param labelIndices Indices to use as labels
   */
  public TermFrequencyParser(boolean normalize, CSVReaderFormat format, long[] labelIndices, SparseNumberVector.Factory<V> factory) {
    super(format, labelIndices, factory);
    this.normalize = normalize;
    this.keymap = new TObjectIntHashMap<>(1001, .5f, -1);
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
        int curdim = keymap.get(curterm);
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
        if(curterm != null) {
          labels.add(curterm);
        }
        curterm = tokenizer.getSubstring();
      }
    }
    if(curterm != null) {
      labels.add(curterm);
    }
    haslabels |= (labels.size() > 0);
    if(normalize) {
      if(Math.abs(len - 1.0) > Double.MIN_NORMAL) {
        for(TIntDoubleIterator iter = values.iterator(); iter.hasNext();) {
          iter.advance();
          iter.setValue(iter.value() / len);
        }
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
   * 
   * @apiviz.exclude
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
