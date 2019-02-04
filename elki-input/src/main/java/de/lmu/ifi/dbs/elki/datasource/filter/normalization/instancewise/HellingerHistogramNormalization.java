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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.instancewise;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorStreamConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import net.jafama.FastMath;

/**
 * Normalize histograms by scaling them to unit absolute sum, then taking the
 * square root of the absolute value in each attribute, times the normalization
 * constant \(1/\sqrt{2}\).
 * <p>
 * \[ H(x_i)=\tfrac{\sqrt{|x_i|/\Sigma}}{\sqrt{2}}
 * \quad\text{ with } \Sigma=\sum\nolimits_i |x_i| \]
 * <p>
 * Using Euclidean distance (linear kernel) and this transformation is the same
 * as using Hellinger distance:
 * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic.HellingerDistanceFunction}
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> vector type
 */
public class HellingerHistogramNormalization<V extends NumberVector> extends AbstractVectorStreamConversionFilter<V, V> implements Normalization<V> {
  /**
   * Static instance.
   */
  public static final HellingerHistogramNormalization<NumberVector> STATIC = new HellingerHistogramNormalization<>();

  /**
   * Constructor.
   */
  public HellingerHistogramNormalization() {
    super();
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    double[] data = new double[featureVector.getDimensionality()];
    double sum = 0.;
    for(int d = 0; d < data.length; ++d) {
      double v = featureVector.doubleValue(d);
      sum += (data[d] = v > 0 ? v : -v);
    }
    // Normalize and sqrt:
    if(sum > 0.) {
      for(int d = 0; d < data.length; ++d) {
        double v = data[d];
        data[d] = v > 0 ? FastMath.sqrt(v / sum) * MathUtil.SQRTHALF : 0.;
      }
    }
    return factory.newNumberVector(data);
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
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected HellingerHistogramNormalization<NumberVector> makeInstance() {
      return STATIC;
    }
  }
}
