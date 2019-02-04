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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise;

import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.jafama.FastMath;

/**
 * Normalization for text frequency (TF) vectors, using the inverse document
 * frequency (IDF). See also: TF-IDF for text analysis.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - SparseNumberVector
 *
 * @param <V> Vector type
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.normalization.InverseDocumentFrequencyNormalization", //
    "de.lmu.ifi.dbs.elki.datasource.filter.InverseDocumentFrequencyNormalization" })
public class InverseDocumentFrequencyNormalization<V extends SparseNumberVector> extends AbstractVectorConversionFilter<V, V> implements Normalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(InverseDocumentFrequencyNormalization.class);

  /**
   * The IDF storage.
   */
  Int2DoubleOpenHashMap idf = new Int2DoubleOpenHashMap();

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
    for(int it = featureVector.iter(); featureVector.iterValid(it); it = featureVector.iterAdvance(it)) {
      if(featureVector.iterDoubleValue(it) >= 0.) {
        final int dim = featureVector.iterDim(it);
        idf.put(dim, idf.get(dim) + 1);
      }
    }
    objcnt += 1;
  }

  @Override
  protected void prepareComplete() {
    final double dbsize = objcnt;
    // Compute IDF values
    for(ObjectIterator<Int2DoubleMap.Entry> iter = idf.int2DoubleEntrySet().fastIterator(); iter.hasNext();) {
      Int2DoubleMap.Entry entry = iter.next();
      entry.setValue(FastMath.log(dbsize / entry.getDoubleValue()));
    }
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    Int2DoubleOpenHashMap vals = new Int2DoubleOpenHashMap();
    for(int it = featureVector.iter(); featureVector.iterValid(it); it = featureVector.iterAdvance(it)) {
      final int dim = featureVector.iterDim(it);
      vals.put(dim, featureVector.iterDoubleValue(it) * idf.get(dim));
    }
    return ((SparseNumberVector.Factory<V>) factory).newNumberVector(vals, featureVector.getDimensionality());
  }

  @Override
  public V restore(V featureVector) {
    Int2DoubleOpenHashMap vals = new Int2DoubleOpenHashMap();
    for(int it = featureVector.iter(); featureVector.iterValid(it); it = featureVector.iterAdvance(it)) {
      final int dim = featureVector.iterDim(it);
      vals.put(dim, featureVector.iterDoubleValue(it) / idf.get(dim));
    }
    return ((SparseNumberVector.Factory<V>) factory).newNumberVector(vals, featureVector.getDimensionality());
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return in;
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.SPARSE_VECTOR_VARIABLE_LENGTH;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
