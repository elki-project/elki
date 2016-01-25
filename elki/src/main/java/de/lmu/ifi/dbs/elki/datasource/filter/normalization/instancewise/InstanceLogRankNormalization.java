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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.AbstractStreamNormalization;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Normalize vectors such that the smallest value of each instance is 0, the
 * largest is 1, but using log_2(1+x).
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> vector type
 */
public class InstanceLogRankNormalization<V extends NumberVector> extends AbstractStreamNormalization<V> {
  /**
   * Average value use for NaNs
   */
  private static final double CENTER = Math.log1p(.5) * MathUtil.ONE_BY_LOG2;

  /**
   * Constructor.
   */
  public InstanceLogRankNormalization() {
    super();
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    double[] raw = featureVector.getColumnVector().getArrayRef();
    // TODO: reduce memory consumption?
    double[] tmp = raw.clone();
    Arrays.sort(tmp);
    double scale = .5 / (raw.length - 1);
    for(int i = 0; i < raw.length; ++i) {
      final double v = raw[i];
      if(v != v) { // NaN guard
        raw[i] = CENTER;
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
      raw[i] = Math.log1p((first + last - 1) * scale) * MathUtil.ONE_BY_LOG2;
    }
    return factory.newNumberVector(raw);
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
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected InstanceLogRankNormalization<NumberVector> makeInstance() {
      return new InstanceLogRankNormalization<>();
    }
  }
}