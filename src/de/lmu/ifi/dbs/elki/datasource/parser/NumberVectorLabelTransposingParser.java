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
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayUtil;

/**
 * Parser reads points transposed. Line n gives the n-th attribute for all
 * points.
 * 
 * @author Arthur Zimek
 * 
 * @param <V> Vector type
 */
public class NumberVectorLabelTransposingParser<V extends NumberVector<V, ?>> extends NumberVectorLabelParser<V> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(NumberVectorLabelTransposingParser.class);

  /**
   * Constructor.
   * 
   * @param colSep Column separator pattern
   * @param quoteChar Quote character
   * @param labelIndices Indices of columns to use as labels
   * @param factory Factory class
   */
  public NumberVectorLabelTransposingParser(Pattern colSep, char quoteChar, BitSet labelIndices, V factory) {
    super(colSep, quoteChar, labelIndices, factory);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    List<Double>[] data = null;
    LabelList[] labels = null;

    int dimensionality = -1;

    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          List<String> entries = tokenize(line);
          if(dimensionality == -1) {
            dimensionality = entries.size();
          }
          else if(entries.size() != dimensionality) {
            throw new IllegalArgumentException("Differing dimensionality in line " + (lineNumber) + ", " + "expected: " + dimensionality + ", read: " + entries.size());
          }

          if(data == null) {
            data = ClassGenericsUtil.newArrayOfEmptyArrayList(dimensionality);
            /*
             * for (int i = 0; i < data.length; i++) { data[i] = new
             * ArrayList<Double>(); }
             */
            labels = ClassGenericsUtil.newArrayOfNull(dimensionality, LabelList.class);
            for(int i = 0; i < labels.length; i++) {
              labels[i] = new LabelList();
            }
          }

          for(int i = 0; i < entries.size(); i++) {
            try {
              Double attribute = Double.valueOf(entries.get(i));
              data[i].add(attribute);
            }
            catch(NumberFormatException e) {
              labels[i].add(entries.get(i));
            }
          }
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    List<V> vectors = new ArrayList<V>();
    List<LabelList> lblc = new ArrayList<LabelList>();
    for(int i = 0; i < data.length; i++) {
      V featureVector = createDBObject(data[i], ArrayUtil.numberListAdapter(data[i]));
      vectors.add(featureVector);
      lblc.add(labels[i]);
    }
    return MultipleObjectsBundle.makeSimple(getTypeInformation(dimensionality), vectors, TypeUtil.LABELLIST, lblc);
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
  public static class Parameterizer<V extends NumberVector<V, ?>> extends NumberVectorLabelParser.Parameterizer<V> {
    @Override
    protected NumberVectorLabelTransposingParser<V> makeInstance() {
      return new NumberVectorLabelTransposingParser<V>(colSep, quoteChar, labelIndices, factory);
    }
  }
}