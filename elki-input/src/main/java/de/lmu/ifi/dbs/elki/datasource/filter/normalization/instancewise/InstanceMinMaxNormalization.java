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
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorStreamConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Normalize vectors with respect to a given minimum and maximum in each
 * dimension. By default, minimum 0 and maximum 1 is used. This class
 * performs a linear scaling on the data.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> vector type
 */
public class InstanceMinMaxNormalization<V extends NumberVector> extends AbstractVectorStreamConversionFilter<V, V> implements Normalization<V> {
  /**
   * Minimum and maximum values.
   */
  private double min, max;

  /**
   * Multiplicity of the vector.
   */
  private int multiplicity;

  /**
   * Constructor.
   * 
   * @param min Desired minimum value
   * @param max Desired maximum value
   */
  public InstanceMinMaxNormalization(double min, double max) {
    super();
    this.min = min;
    this.max = max;
  }

  /**
   * Constructor, normalizing to {@code [0;1]}
   */
  public InstanceMinMaxNormalization() {
    this(0., 1.);
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    double[] raw = featureVector.toArray();
    // Multivariate codepath:
    if(multiplicity > 1) {
      assert (raw.length % multiplicity == 0) : "Vector length is not divisible by multiplicity?";
      double[] mi = new double[multiplicity], ma = new double[multiplicity];
      for(int i = 0; i < multiplicity; i++) {
        mi[i] = Double.POSITIVE_INFINITY;
        ma[i] = Double.NEGATIVE_INFINITY;
      }
      for(int i = 0, j = 0; i < raw.length; ++i, j = ++j % multiplicity) {
        final double v = raw[i];
        if(v != v) { // NaN guard
          continue;
        }
        mi[j] = (mi[j] < v) ? mi[j] : v;
        ma[j] = (ma[j] > v) ? ma[j] : v;
      }
      for(int j = 0; j < multiplicity; j++) {
        if(mi[j] < ma[j]) {
          final double s = (max - min) / (ma[j] - mi[j]);
          for(int i = 0; i < raw.length; i += multiplicity) {
            raw[i] = (raw[i] - mi[j]) * s + min;
          }
        }
      }
      return factory.newNumberVector(raw);
    }
    // Default codepath
    double mi = Double.POSITIVE_INFINITY, ma = Double.NEGATIVE_INFINITY;
    for(int i = 0; i < raw.length; ++i) {
      final double v = raw[i];
      if(v != v) { // NaN guard
        continue;
      }
      mi = (mi < v) ? mi : v;
      ma = (ma > v) ? ma : v;
    }
    if(mi < ma) {
      final double s = (max - min) / (ma - mi);
      for(int i = 0; i < raw.length; ++i) {
        raw[i] = (raw[i] - mi) * s + min;
      }
    }
    return factory.newNumberVector(raw);
  }

  @Override
  protected void initializeOutputType(SimpleTypeInformation<V> type) {
    super.initializeOutputType(type);
    multiplicity = ((VectorTypeInformation<?>) type).getMultiplicity();
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
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Option ID for minimum value.
     */
    public static final OptionID MIN_ID = new OptionID("normalization.min", "Minimum value to assign to objects.");

    /**
     * Option ID for maximum value.
     */
    public static final OptionID MAX_ID = new OptionID("normalization.max", "Maximum value to assign to objects.");

    /**
     * Minimum and maximum values.
     */
    private double min, max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minP = new DoubleParameter(MIN_ID, 0.);
      if(config.grab(minP)) {
        min = minP.doubleValue();
      }
      DoubleParameter maxP = new DoubleParameter(MAX_ID, 1.);
      if(config.grab(maxP)) {
        max = maxP.doubleValue();
      }
      // Non-formalized parameter constraint: min < max
      if(min >= max) {
        config.reportError(new WrongParameterValueException(minP, "must be less than", maxP, ""));
      }
    }

    @Override
    protected InstanceMinMaxNormalization<V> makeInstance() {
      return new InstanceMinMaxNormalization<>(min, max);
    }
  }
}
