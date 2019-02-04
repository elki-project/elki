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
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Continuous uncertain object model using a uniform distribution on the
 * bounding box.
 * <p>
 * This is a continuous version of the U-Model in:
 * <p>
 * L. Antova, T. Jansen, C. Koch, D. Olteanu<br>
 * Fast and simple relational processing of uncertain data<br>
 * In IEEE 24th International Conference on Data Engineering (ICDE) 2008.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 * @since 0.7.0
 */
public class UniformContinuousUncertainObject extends AbstractUncertainObject {
  /**
   * Vector factory.
   */
  public static final FeatureVector.Factory<UniformContinuousUncertainObject, ?> FACTORY = new Factory();

  /**
   * Constructor.
   *
   * @param bounds Bounding box.
   */
  public UniformContinuousUncertainObject(SpatialComparable bounds) {
    super();
    this.bounds = bounds;
  }

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

    for(int i = 0; i < dim; i++) {
      double w = bounds.getMax(i) - bounds.getMin(i);
      assert (w < Double.POSITIVE_INFINITY);
      values[i] = rand.nextDouble() * w + bounds.getMin(i);
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
  private static class Factory implements FeatureVector.Factory<UniformContinuousUncertainObject, Number> {
    @Override
    public <A> UniformContinuousUncertainObject newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ByteBufferSerializer<UniformContinuousUncertainObject> getDefaultSerializer() {
      return null; // No serializer available.
    }

    @Override
    public Class<? super UniformContinuousUncertainObject> getRestrictionClass() {
      return UniformContinuousUncertainObject.class;
    }
  }
}
