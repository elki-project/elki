package de.lmu.ifi.dbs.elki.datasource.filter;

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
import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;

/**
 * Perform full TF-IDF Normalization as commonly used in text mining.
 * 
 * Each record is first normalized using "term frequencies" to sum up to 1. Then
 * it is globally normalized using the Inverse Document Frequency, so rare terms
 * are weighted stronger than common terms.
 * 
 * Restore will only undo the IDF part of the normalization!
 * 
 * @author Erich Schubert
 */
public class TFIDFNormalization extends InverseDocumentFrequencyNormalization {
  /**
   * Constructor.
   */
  public TFIDFNormalization() {
    super();
  }

  @Override
  protected SparseFloatVector filterSingleObject(SparseFloatVector featureVector) {
    BitSet b = featureVector.getNotNullMask();
    double sum = 0.0;
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      sum += featureVector.doubleValue(i);
    }
    if(sum <= 0) {
      sum = 1.0;
    }
    Map<Integer, Float> vals = new HashMap<Integer, Float>();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      vals.put(i, (float) (featureVector.doubleValue(i) / sum * idf.get(i).doubleValue()));
    }
    return new SparseFloatVector(vals, featureVector.getDimensionality());
  }
}