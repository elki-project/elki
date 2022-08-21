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
package elki.datasource.filter.normalization.instancewise;

import elki.data.NumberVector;
import elki.data.SparseNumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.datasource.filter.AbstractVectorStreamConversionFilter;
import elki.datasource.filter.normalization.Normalization;
import elki.distance.Norm;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.ManhattanDistance;
import elki.distance.minkowski.SparseEuclideanDistance;
import elki.distance.minkowski.SparseManhattanDistance;
import elki.math.linearalgebra.VMath;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Class to perform a normalization on vectors to norm 1.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <V> vector type
 */
public class LengthNormalization<V extends NumberVector> extends AbstractVectorStreamConversionFilter<V, V> implements Normalization<V> {
  /**
   * Norm to use.
   */
  Norm<? super V> norm;

  /**
   * Map used if converting sparse vectors.
   */
  Int2DoubleOpenHashMap map;

  /**
   * Constructor.
   * 
   * @param norm Norm to use
   */
  public LengthNormalization(Norm<? super V> norm) {
    super();
    this.norm = norm;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected V filterSingleObject(V featureVector) {
    // generic casts here are not safe, but will do:
    Norm<? super V> norm = this.norm;
    if(featureVector instanceof SparseNumberVector) {
      // Special override, as it will be easily used inefficiently otherwise
      if(EuclideanDistance.STATIC.equals(norm)) {
        norm = (Norm<? super V>) SparseEuclideanDistance.STATIC;
      }
      else if(ManhattanDistance.STATIC.equals(norm)) {
        norm = (Norm<? super V>) SparseManhattanDistance.STATIC;
      }
    }
    final double d = norm.norm(featureVector);
    final double factor = d > 0 ? 1 / d : 1.;
    // Optimized codepath when converting sparse vectors
    if(featureVector instanceof SparseNumberVector && factory instanceof SparseNumberVector.Factory) {
      SparseNumberVector fv = (SparseNumberVector) featureVector;
      SparseNumberVector.Factory<?> sfact = (SparseNumberVector.Factory<?>) factory;
      if(map == null) {
        map = new Int2DoubleOpenHashMap();
      }
      map.clear();
      for(int j = fv.iter(); fv.iterValid(j); j = fv.iterAdvance(j)) {
        map.put(fv.iterDim(j), fv.iterDoubleValue(j) * factor);
      }
      return (V) sfact.newNumberVector(map, fv.getDimensionality());
    }
    return factory.newNumberVector(VMath.timesEquals(featureVector.toArray(), factor));
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return in;
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Option ID for normalization norm.
     */
    public static final OptionID NORM_ID = new OptionID("normalization.norm", "Norm (length function) to use for computing the vector length.");

    /**
     * Norm to use.
     */
    Norm<? super V> norm;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Norm<? super V>>(NORM_ID, Norm.class, EuclideanDistance.class) //
          .grab(config, x -> norm = x);
    }

    @Override
    public LengthNormalization<V> make() {
      return new LengthNormalization<>(norm);
    }
  }
}
