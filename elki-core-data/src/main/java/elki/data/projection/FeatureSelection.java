/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.data.projection;

import java.util.function.Function;

import elki.data.FeatureVector;
import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.VectorFieldTypeInformation;
import elki.data.type.VectorTypeInformation;
import elki.utilities.datastructures.arraylike.ArrayAdapter;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Projection class for number vectors.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @assoc - - - FeatureVector
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
   * Dimensions to select.
   */
  private int[] dims;

  /**
   * Object factory.
   */
  private FeatureVector.Factory<V, F> factory;

  /**
   * Projection lambda.
   */
  private Function<V, V> project;

  /**
   * Constructor.
   * 
   * @param dims Dimensions
   */
  public FeatureSelection(int[] dims) {
    this.dims = dims;
    int mind = 0;
    for(int dim : dims) {
      mind = Math.max(mind, dim + 1);
    }
    this.mindim = mind;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(SimpleTypeInformation<? extends V> in) {
    final VectorFieldTypeInformation<V> vin = (VectorFieldTypeInformation<V>) in;
    factory = (FeatureVector.Factory<V, F>) vin.getFactory();
    if(factory instanceof NumberVector.Factory) {
      NumberVector.Factory<?> vfactory = (NumberVector.Factory<?>) factory;
      ProjectedNumberFeatureVectorAdapter proj = new ProjectedNumberFeatureVectorAdapter();
      project = v -> (V) vfactory.newNumberVector((NumberVector) v, proj);
    }
    else {
      ProjectedFeatureVectorAdapter proj = new ProjectedFeatureVectorAdapter();
      project = v -> factory.newFeatureVector(v, proj);
    }
    if(vin.getDimensionality() < mindim) {
      throw new AbortException("Data does not have enough dimensions for this projection!");
    }
  }

  @Override
  public V project(V data) {
    return project.apply(data);
  }

  /**
   * Generic projection function
   *
   * @author Erich Schubert
   */
  private class ProjectedFeatureVectorAdapter implements ArrayAdapter<F, V> {
    @Override
    public int size(V array) {
      return dims.length;
    }

    @Override
    public F get(V array, int off) throws IndexOutOfBoundsException {
      return array.getValue(dims[off]);
    }
  }

  /**
   * Adapter for generating number vectors without reboxing.
   *
   * @author Erich Schubert
   */
  private class ProjectedNumberFeatureVectorAdapter implements NumberArrayAdapter<Double, NumberVector> {
    @Override
    public int size(NumberVector array) {
      return dims.length;
    }

    @Override
    public Double get(NumberVector array, int off) throws IndexOutOfBoundsException {
      return array.doubleValue(dims[off]);
    }

    @Override
    public double getDouble(NumberVector array, int off) throws IndexOutOfBoundsException {
      return array.doubleValue(dims[off]);
    }

    @Override
    public long getLong(NumberVector array, int off) throws IndexOutOfBoundsException {
      return array.longValue(dims[off]);
    }
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(factory, dims.length);
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
   */
  public static class Par<V extends FeatureVector<F>, F> implements Parameterizer {
    /**
     * Selected attributes parameter.
     */
    public static final OptionID SELECTED_ATTRIBUTES_ID = new OptionID("projectionfilter.selectedattributes", "a comma separated array of integer values d_i, where 0 <= d_i < the dimensionality of the feature space specifying the dimensions to be considered for projection. If this parameter is not set, no dimensions will be considered, i.e. the projection is a zero-dimensional feature space");

    /**
     * Dimensions to select.
     */
    int[] dims;

    @Override
    public void configure(Parameterization config) {
      new IntListParameter(SELECTED_ATTRIBUTES_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT_LIST) //
          .grab(config, x -> dims = x.clone());
    }

    @Override
    public FeatureSelection<V, F> make() {
      return new FeatureSelection<>(dims);
    }
  }
}
