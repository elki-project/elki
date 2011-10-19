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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * A parser to load term frequency data, which essentially are sparse vectors
 * with text keys.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has SparseFloatVector
 */
// TODO: add a flag to perform TF normalization when using term counts
@Title("Term frequency parser")
@Description("Parse a file containing term frequencies. The expected format is 'label term1 <freq> term2 <freq> ...'. Terms must not contain the separator character!")
public class TermFrequencyParser extends NumberVectorLabelParser<SparseFloatVector> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(TermFrequencyParser.class);

  /**
   * Maximum dimension used
   */
  int maxdim;

  /**
   * Map
   */
  HashMap<String, Integer> keymap;

  /**
   * Normalize
   */
  boolean normalize;

  /**
   * Constructor.
   * 
   * @param normalize Normalize
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public TermFrequencyParser(boolean normalize, Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices, SparseFloatVector.STATIC);
    this.normalize = normalize;
    this.maxdim = 0;
    this.keymap = new HashMap<String, Integer>();
  }

  @Override
  protected void parseLineInternal(String line, List<SparseFloatVector> vectors, List<LabelList> labellist) {
    List<String> entries = tokenize(line);

    double len = 0;
    Map<Integer, Float> values = new TreeMap<Integer, Float>();
    LabelList labels = new LabelList();

    String curterm = null;
    for(int i = 0; i < entries.size(); i++) {
      if(curterm == null) {
        curterm = entries.get(i);
      }
      else {
        try {
          float attribute = Float.valueOf(entries.get(i));
          Integer curdim = keymap.get(curterm);
          if(curdim == null) {
            curdim = maxdim + 1;
            keymap.put(curterm, curdim);
            maxdim += 1;
          }
          values.put(curdim, attribute);
          len += attribute;
          curterm = null;
        }
        catch(NumberFormatException e) {
          if(curterm != null) {
            labels.add(curterm);
          }
          curterm = entries.get(i);
        }
      }
    }
    if(curterm != null) {
      labels.add(curterm);
    }
    if(normalize) {
      if(Math.abs(len - 1.0) > 1E-10 && len > 1E-10) {
        for(Entry<Integer, Float> ent : values.entrySet()) {
          ent.setValue((float) (ent.getValue() / len));
        }
      }
    }

    vectors.add(new SparseFloatVector(values, maxdim));
    labellist.add(labels);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    List<SparseFloatVector> vectors = new ArrayList<SparseFloatVector>();
    List<LabelList> lblc = new ArrayList<LabelList>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          parseLineInternal(line, vectors, lblc);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    // Set maximum dimensionality
    for(int i = 0; i < vectors.size(); i++) {
      vectors.get(i).setDimensionality(maxdim);
    }
    return MultipleObjectsBundle.makeSimple(getTypeInformation(maxdim), vectors, TypeUtil.LABELLIST, lblc);
  }

  @Override
  protected VectorFieldTypeInformation<SparseFloatVector> getTypeInformation(int dimensionality) {
    final Map<Integer, Float> emptyMap = Collections.emptyMap();
    return new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class, dimensionality, new SparseFloatVector(emptyMap, dimensionality));
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
    /**
     * Option ID for normalization
     */
    public static final OptionID NORMALIZE_FLAG = OptionID.getOrCreateOptionID("tf.normalize", "Normalize vectors to manhattan length 1 (convert term counts to term frequencies)");

    /**
     * Normalization flag
     */
    boolean normalize = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag normF = new Flag(NORMALIZE_FLAG);
      if(config.grab(normF)) {
        normalize = normF.getValue();
      }
    }

    @Override
    protected TermFrequencyParser makeInstance() {
      return new TermFrequencyParser(normalize, colSep, quoteChar, labelIndices);
    }
  }
}