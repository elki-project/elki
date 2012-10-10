package de.lmu.ifi.dbs.elki.datasource.filter.normalization;

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

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;

/**
 * Normalization for text frequency vectors, using the inverse document
 * frequency.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses SparseNumberVector
 * 
 * @param <V> Vector type
 */
public class InverseDocumentFrequencyNormalization<V extends SparseNumberVector<?>> extends AbstractNormalization<V> {
  /**
   * The IDF storage.
   */
  TIntDoubleMap idf = new TIntDoubleHashMap();

  /**
   * The number of objects in the dataset.
   */
  int objcnt = 0;

  /**
   * Constructor.
   */
  public InverseDocumentFrequencyNormalization() {
    super();
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<V> in) {
    if(idf.size() > 0) {
      throw new UnsupportedOperationException("This normalization may only be used once!");
    }
    objcnt = 0;
    return true;
  }

  @Override
  protected void prepareProcessInstance(V featureVector) {
    BitSet b = featureVector.getNotNullMask();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      if(featureVector.doubleValue(i) >= 0.0) {
        idf.put(i, idf.get(i) + 1);
      }
    }
    objcnt += 1;
  }

  @Override
  protected void prepareComplete() {
    final double dbsize = objcnt;
    // Compute IDF values
    for(TIntDoubleIterator iter = idf.iterator(); iter.hasNext();) {
      iter.advance();
      // Note: dbsize is a double!
      iter.setValue(Math.log(dbsize / iter.value()));
    }
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    BitSet b = featureVector.getNotNullMask();
    TIntDoubleHashMap vals = new TIntDoubleHashMap();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      vals.put(i, (float) (featureVector.doubleValue(i) * idf.get(i)));
    }
    return ((SparseNumberVector.Factory<V, ?>) factory).newNumberVector(vals, featureVector.getDimensionality());
  }

  @Override
  public V restore(V featureVector) {
    BitSet b = featureVector.getNotNullMask();
    TIntDoubleHashMap vals = new TIntDoubleHashMap();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      vals.put(i, (float) (featureVector.doubleValue(i) / idf.get(i)));
    }
    return ((SparseNumberVector.Factory<V, ?>) factory).newNumberVector(vals, featureVector.getDimensionality());
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.SPARSE_VECTOR_FIELD;
  }
}