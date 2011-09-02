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
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;

/**
 * Normalization for text frequency vectors, using the inverse document
 * frequency.
 * 
 * @author Erich Schubert
 */
public class InverseDocumentFrequencyNormalization extends AbstractNormalization<SparseFloatVector> {
  /**
   * The IDF storage
   */
  Map<Integer, Number> idf = new HashMap<Integer, Number>();

  /**
   * The number of objects in the dataset
   */
  int objcnt = 0;

  /**
   * Constructor.
   */
  public InverseDocumentFrequencyNormalization() {
    super();
  }

  @Override
  protected boolean prepareStart(@SuppressWarnings("unused") SimpleTypeInformation<SparseFloatVector> in) {
    if(idf.size() > 0) {
      throw new UnsupportedOperationException("This normalization may only be used once!");
    }
    objcnt = 0;
    return true;
  }

  @Override
  protected void prepareProcessInstance(SparseFloatVector featureVector) {
    BitSet b = featureVector.getNotNullMask();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      if(featureVector.doubleValue(i) >= 0.0) {
        Number c = idf.get(i);
        if(c == null) {
          idf.put(i, 1);
        }
        else {
          idf.put(i, c.intValue() + 1);
        }
      }
    }
    objcnt += 1;
  }

  @Override
  protected void prepareComplete() {
    final double dbsize = objcnt;
    // Compute IDF values
    for(Entry<Integer, Number> ent : idf.entrySet()) {
      // Note: dbsize is a double!
      ent.setValue(Math.log(dbsize / ent.getValue().intValue()));
    }
  }

  @Override
  protected SparseFloatVector filterSingleObject(SparseFloatVector featureVector) {
    BitSet b = featureVector.getNotNullMask();
    Map<Integer, Float> vals = new HashMap<Integer, Float>();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      vals.put(i, (float) (featureVector.doubleValue(i) * idf.get(i).doubleValue()));
    }
    return new SparseFloatVector(vals, featureVector.getDimensionality());
  }

  @Override
  public SparseFloatVector restore(SparseFloatVector featureVector) {
    BitSet b = featureVector.getNotNullMask();
    Map<Integer, Float> vals = new HashMap<Integer, Float>();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      vals.put(i, (float) (featureVector.doubleValue(i) / idf.get(i).doubleValue()));
    }
    return new SparseFloatVector(vals, featureVector.getDimensionality());
  }

  @Override
  protected SimpleTypeInformation<? super SparseFloatVector> getInputTypeRestriction() {
    return TypeUtil.SPARSE_FLOAT_FIELD;
  }
}