package de.lmu.ifi.dbs.elki.data.projection;
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

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.SubsetArrayAdapter;

/**
 * Projection class for number vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <F> Feature type
 */
public class FeatureSelection<V extends FeatureVector<V, F>, F> implements Projection<V, V> {
  /**
   * Minimum dimensionality required for projection.
   */
  private int mindim;

  /**
   * Object factory.
   */
  private V factory;

  /**
   * Output dimensionality.
   */
  private int dimensionality;

  /**
   * Array adapter.
   */
  protected ArrayAdapter<F, V> adapter;
  
  /**
   * Constructor.
   * 
   * @param dims Dimensions
   * @param factory Object factory
   */
  public FeatureSelection(int[] dims, V factory) {
    this.adapter = new SubsetArrayAdapter<F, V>(getAdapter(factory), dims);
    this.factory = factory;
    this.dimensionality = dims.length;

    int mind = 0;
    for(int dim : dims) {
      mind = Math.max(mind, dim + 1);
    }
    this.mindim = mind;
  }

  @Override
  public V project(V data) {
    return data.newFeatureVector(data, adapter);
  }
  
  /**
   * Choose the best adapter for this.
   * 
   * @param factory Object factory, for type inference
   * @param <V> Vector type
   * @param <F> Value type
   * @return Adapter
   */
  @SuppressWarnings("unchecked")
  private static <V extends FeatureVector<V, F>, F> ArrayAdapter<F, ? super V> getAdapter(V factory) {
    if(factory instanceof NumberVector) {
      ArrayAdapter<?, ?> ret = ArrayLikeUtil.numberVectorAdapter((NumberVector<?, ?>) factory);
      return (ArrayAdapter<F, ? super V>) ret;
    }
    return ArrayLikeUtil.featureVectorAdapter(factory);
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    @SuppressWarnings("unchecked")
    final Class<V> cls = (Class<V>) factory.getClass();
    return new VectorFieldTypeInformation<V>(cls, dimensionality);
  }

  @Override
  public TypeInformation getInputDataTypeInformation() {
    @SuppressWarnings("unchecked")
    final Class<V> cls = (Class<V>) factory.getClass();
    return new VectorTypeInformation<V>(cls, mindim, Integer.MAX_VALUE);
  }
}