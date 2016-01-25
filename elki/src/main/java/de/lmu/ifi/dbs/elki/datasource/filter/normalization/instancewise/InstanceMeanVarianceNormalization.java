package de.lmu.ifi.dbs.elki.datasource.filter.normalization.instancewise;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.AbstractStreamNormalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Normalize vectors such that they have zero mean and unit variance.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> vector type
 */
public class InstanceMeanVarianceNormalization<V extends NumberVector> extends AbstractStreamNormalization<V> {
  /**
   * Multiplicity of the vector.
   */
  private int multiplicity;

  /**
   * Constructor.
   */
  public InstanceMeanVarianceNormalization() {
    super();
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    double[] raw = featureVector.getColumnVector().getArrayRef();
    if(raw.length == 0) {
      return factory.newNumberVector(new double[] {});
    }
    if(raw.length == 1) {
      // Constant, but preserve NaNs
      return factory.newNumberVector(new double[] { raw[0] == raw[0] ? 0. : Double.NaN });
    }
    // Multivariate codepath:
    if(multiplicity > 1) {
      assert (raw.length % multiplicity == 0) : "Vector length is not divisible by multiplicity?";
      return factory.newNumberVector(multivariateStandardization(raw));
    }
    return factory.newNumberVector(univariateStandardization(raw));
  }

  protected double[] univariateStandardization(double[] raw) {
    // Two pass normalization is numerically most stable,
    // And Java should optimize this well enough.
    double sum = 0.;
    for(int i = 0; i < raw.length; ++i) {
      final double v = raw[i];
      if(v != v) { // NaN guard
        continue;
      }
      sum += v;
    }
    final double mean = sum / raw.length;
    double ssum = 0.;
    for(int i = 0; i < raw.length; ++i) {
      double v = raw[i] - mean;
      if(v != v) {
        continue;
      }
      ssum += v * v;
    }
    final double std = Math.sqrt(ssum) / (raw.length - 1);
    if(std > 0.) {
      for(int i = 0; i < raw.length; ++i) {
        raw[i] = (raw[i] - mean) / std;
      }
    }
    return raw;
  }

  protected double[] multivariateStandardization(double[] raw) {
    final int len = raw.length / multiplicity;
    if(len <= 1) {
      return raw;
    }
    // Two pass normalization is numerically most stable,
    // And Java should optimize this well enough.
    double[] mean = new double[multiplicity];
    for(int i = 0, j = 0; i < raw.length; ++i, j = ++j % multiplicity) {
      final double v = raw[i];
      if(v != v) { // NaN guard
        continue;
      }
      mean[j] += v;
    }
    for(int j = 0; j < multiplicity; ++j) {
      mean[j] /= len;
    }
    double[] std = new double[multiplicity];
    for(int i = 0, j = 0; i < raw.length; ++i, j = ++j % multiplicity) {
      double v = raw[i] - mean[j];
      if(v != v) {
        continue;
      }
      std[j] += v * v;
    }
    for(int j = 0; j < multiplicity; ++j) {
      std[j] = std[j] > 0. ? Math.sqrt(std[j]) / (len - 1) : 1;
    }
    for(int i = 0, j = 0; i < raw.length; ++i, j = ++j % multiplicity) {
      raw[i] = (raw[i] - mean[j]) / std[j];
    }
    return raw;
  }

  @Override
  protected void initializeOutputType(SimpleTypeInformation<V> type) {
    super.initializeOutputType(type);
    multiplicity = ((VectorTypeInformation<?>) type).getMultiplicity();
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    @Override
    protected InstanceMeanVarianceNormalization<V> makeInstance() {
      return new InstanceMeanVarianceNormalization<>();
    }
  }
}