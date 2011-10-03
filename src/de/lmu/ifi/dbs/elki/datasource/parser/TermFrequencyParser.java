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
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public TermFrequencyParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices, SparseFloatVector.STATIC);
    this.maxdim = 0;
    this.keymap = new HashMap<String, Integer>();
  }

  @Override
  public Pair<SparseFloatVector, LabelList> parseLineInternal(String line) {
    List<String> entries = tokenize(line);

    Map<Integer, Float> values = new TreeMap<Integer, Float>();
    LabelList labels = new LabelList();

    String curterm = null;
    for(int i = 0; i < entries.size(); i++) {
      if(curterm == null) {
        curterm = entries.get(i);
      }
      else {
        try {
          Float attribute = Float.valueOf(entries.get(i));
          Integer curdim = keymap.get(curterm);
          if(curdim == null) {
            curdim = maxdim + 1;
            keymap.put(curterm, curdim);
            maxdim += 1;
          }
          values.put(curdim, attribute);
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

    return new Pair<SparseFloatVector, LabelList>(new SparseFloatVector(values, maxdim), labels);
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
          Pair<SparseFloatVector, LabelList> pair = parseLineInternal(line);
          vectors.add(pair.first);
          lblc.add(pair.second);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    // Set maximum dimensionality
    for(int i = 0; i < vectors.size(); i ++) {
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
    @Override
    protected TermFrequencyParser makeInstance() {
      return new TermFrequencyParser(colSep, quoteChar, labelIndices);
    }
  }
}