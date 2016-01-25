package de.lmu.ifi.dbs.elki.data.projection;

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

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector.Factory;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.transform.NumberVectorFeatureSelectionFilter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.SubsetArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Projection class for number vectors.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @apiviz.uses FeatureVector
 * 
 * @param <V> Vector type
 * @param <F> Feature type
 */
public class FeatureSelection<V extends FeatureVector<F>, F> implements Projection<V, V> {
  /**
   * Minimum dimensionality required for projection.
   */
  private int mindim;

  /**
   * Object factory.
   */
  private FeatureVector.Factory<V, F> factory;

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
   */
  public FeatureSelection(int[] dims) {
    this.adapter = new SubsetArrayAdapter<>(getAdapter(factory), dims);
    this.dimensionality = dims.length;

    int mind = 0;
    for(int dim : dims) {
      mind = Math.max(mind, dim + 1);
    }
    this.mindim = mind;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(SimpleTypeInformation<V> in) {
    final VectorFieldTypeInformation<V> vin = (VectorFieldTypeInformation<V>) in;
    factory = (FeatureVector.Factory<V, F>) vin.getFactory();
    if(vin.getDimensionality() < mindim) {
      throw new AbortException("Data does not have enough dimensions for this projection!");
    }
  }

  @Override
  public V project(V data) {
    return factory.newFeatureVector(data, adapter);
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
  private static <V extends FeatureVector<F>, F> ArrayAdapter<F, ? super V> getAdapter(Factory<V, F> factory) {
    if(factory instanceof NumberVector.Factory) {
      return (ArrayAdapter<F, ? super V>) ArrayLikeUtil.NUMBERVECTORADAPTER;
    }
    return (ArrayAdapter<F, ? super V>) ArrayLikeUtil.FEATUREVECTORADAPTER;
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(factory, dimensionality);
  }

  @Override
  public TypeInformation getInputDataTypeInformation() {
    @SuppressWarnings("unchecked")
    final Class<V> cls = (Class<V>) factory.getRestrictionClass();
    return VectorTypeInformation.typeRequest(cls, mindim, Integer.MAX_VALUE);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends FeatureVector<F>, F> extends AbstractParameterizer {
    /**
     * Dimensions to select.
     */
    int[] dims = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntListParameter selectedAttributesP = new IntListParameter(NumberVectorFeatureSelectionFilter.Parameterizer.SELECTED_ATTRIBUTES_ID);
      selectedAttributesP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT_LIST);
      if(config.grab(selectedAttributesP)) {
        dims = selectedAttributesP.getValue().clone();
      }
    }

    @Override
    protected FeatureSelection<V, F> makeInstance() {
      return new FeatureSelection<>(dims);
    }
  }
}
