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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorStreamConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Normalize vectors such that the smallest value of each instance is 0, the
 * largest is 1.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> vector type
 */
public class InstanceRankNormalization<V extends NumberVector> extends AbstractVectorStreamConversionFilter<V, V> implements Normalization<V> {
  /**
   * Constructor.
   */
  public InstanceRankNormalization() {
    super();
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    double[] raw = featureVector.toArray();
    // TODO: reduce memory consumption?
    double[] tmp = raw.clone();
    Arrays.sort(tmp);
    double scale = .5 / (raw.length - 1);
    for(int i = 0; i < raw.length; ++i) {
      final double v = raw[i];
      if(v != v) { // NaN guard
        raw[i] = 0.5;
        continue;
      }
      int first = Arrays.binarySearch(tmp, v), last = first + 1;
      assert (first >= 0);
      while(first > 0 && tmp[first - 1] >= v) {
        --first;
      }
      while(last < tmp.length && tmp[last] <= v) {
        ++last;
      }
      raw[i] = (first + last - 1) * scale;
    }
    return factory.newNumberVector(raw);
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
    protected InstanceRankNormalization<NumberVector> makeInstance() {
      return new InstanceRankNormalization<>();
    }
  }
}
