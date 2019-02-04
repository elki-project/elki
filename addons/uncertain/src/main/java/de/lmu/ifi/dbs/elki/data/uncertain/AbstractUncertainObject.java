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
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Abstract base implementation for {@link UncertainObject}s, providing shared
 * functionality such as bounding box access and random generation.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractUncertainObject implements UncertainObject {
  /**
   * Default retry limit for sampling, to guard against bad parameters.
   */
  public final static int DEFAULT_TRY_LIMIT = 1000;

  /**
   * Compute the bounding box for some samples.
   *
   * @param samples Samples
   * @return Bounding box.
   */
  protected static HyperBoundingBox computeBounds(NumberVector[] samples) {
    assert(samples.length > 0) : "Cannot compute bounding box of empty set.";
    // Compute bounds:
    final int dimensions = samples[0].getDimensionality();
    final double[] min = new double[dimensions];
    final double[] max = new double[dimensions];
    NumberVector first = samples[0];
    for(int d = 0; d < dimensions; d++) {
      min[d] = max[d] = first.doubleValue(d);
    }
    for(int i = 1; i < samples.length; i++) {
      NumberVector v = samples[i];
      for(int d = 0; d < dimensions; d++) {
        final double c = v.doubleValue(d);
        min[d] = c < min[d] ? c : min[d];
        max[d] = c > max[d] ? c : max[d];
      }
    }
    return new HyperBoundingBox(min, max);
  }

  /**
   * Bounding box of the object.
   */
  protected SpatialComparable bounds;

  @Override
  public abstract DoubleVector drawSample(Random rand);

  @Override
  public int getDimensionality() {
    return bounds.getDimensionality();
  }

  @Override
  public double getMin(final int dimension) {
    return bounds.getMin(dimension);
  }

  @Override
  public double getMax(final int dimension) {
    return bounds.getMax(dimension);
  }

  @Override
  public Double getValue(int dimension) {
    // Center of bounding box.
    // Note: currently not used, part of the FeatureVector<Double> API.
    // But we are currently not implementing NumberVector!
    return (bounds.getMax(dimension) + bounds.getMin(dimension)) * .5;
  }

  @Override
  public abstract DoubleVector getCenterOfMass();
}
