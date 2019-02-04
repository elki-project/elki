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
package de.lmu.ifi.dbs.elki.data.uncertain;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Gaussian model for uncertain objects, sampled from a 3-sigma bounding box.
 *
 * This model does not support covariance, but all distributions are
 * axis-aligned.
 *
 * TODO: currently, only a 3 sigma bounding box is supported.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SimpleGaussianContinuousUncertainObject extends AbstractUncertainObject {
  /**
   * Vector factory.
   */
  public static final FeatureVector.Factory<SimpleGaussianContinuousUncertainObject, ?> FACTORY = new Factory();

  /**
   * Scaling factor from bounding box width to standard deviations. Bounding box
   * is 6 standard deviations in width (three on each side)!
   */
  private static final double DIV = 1. / 6.;

  /**
   * Constructor.
   *
   * @param bounds Bounding box (3 sigma)
   */
  public SimpleGaussianContinuousUncertainObject(SpatialComparable bounds) {
    super();
    this.bounds = bounds;
  }

  // TODO: move to an abstract superclass?
  @Override
  public DoubleVector getCenterOfMass() {
    final int dim = bounds.getDimensionality();
    double[] mean = new double[dim];
    for(int d = 0; d < dim; d++) {
      mean[d] = (bounds.getMin(d) + bounds.getMax(d)) * .5;
    }
    return DoubleVector.wrap(mean);
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    final int dim = bounds.getDimensionality();
    double[] values = new double[dim];

    for(int i = 0, maxtries = DEFAULT_TRY_LIMIT; i < dim;) {
      final double l = bounds.getMin(i), u = bounds.getMax(i);
      final double s = (u - l) * DIV;
      assert(s < Double.POSITIVE_INFINITY);
      final double v = rand.nextGaussian() * s + (l + u) * .5;
      if(v < l || v > u) {
        if(--maxtries == 0) {
          throw new AbortException("Could not satisfy bounding box!");
        }
        continue;
      }
      values[i++] = v; // Success.
    }
    return DoubleVector.wrap(values);
  }

  /**
   * Factory class for this data type. Not for public use, use
   * {@link de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier.Uncertainifier} to
   * derive uncertain objects from certain vectors.
   *
   * TODO: provide serialization functionality.
   *
   * @author Erich Schubert
   */
  private static class Factory implements FeatureVector.Factory<SimpleGaussianContinuousUncertainObject, Number> {
    @Override
    public <A> SimpleGaussianContinuousUncertainObject newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ByteBufferSerializer<SimpleGaussianContinuousUncertainObject> getDefaultSerializer() {
      return null; // No serializer available.
    }

    @Override
    public Class<? super SimpleGaussianContinuousUncertainObject> getRestrictionClass() {
      return SimpleGaussianContinuousUncertainObject.class;
    }
  }
}
