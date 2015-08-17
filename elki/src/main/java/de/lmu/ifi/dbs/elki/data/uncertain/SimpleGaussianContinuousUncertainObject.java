package de.lmu.ifi.dbs.elki.data.uncertain;
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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Gaussian model for uncertain objects, sampled from a 3-sigma bounding box.
 *
 * This model does not support covariance, but all distributions are
 * axis-aligned.
 *
 * TODO: currently, only a 3 sigma bounding box is supported.
 *
 * @author Erich Schubert
 */
public class SimpleGaussianContinuousUncertainObject extends AbstractUncertainObject {
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
    return new DoubleVector(mean);
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
    return new DoubleVector(values);
  }

  /**
   * Vector factory
   *
   * @author Erich Schubert
   */
  public static class Factory extends AbstractUncertainObject.Factory<SimpleGaussianContinuousUncertainObject> {
    /**
     * Minimum and maximum allowed deviation.
     */
    double minDev, maxDev;

    /**
     * Generate symmetric distributions only.
     */
    boolean symmetric;

    /**
     * Random generator.
     */
    Random rand;

    /**
     * Constructor.
     *
     * @param minDev Minimum deviation
     * @param maxDev Maximum deviation
     * @param symmetric Generate symmetric distributions only
     * @param rand Random generator
     */
    public Factory(double minDev, double maxDev, boolean symmetric, RandomFactory rand) {
      super();
      this.minDev = minDev;
      this.maxDev = maxDev;
      this.rand = rand.getRandom();
    }

    @Override
    public <A> SimpleGaussianContinuousUncertainObject newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
      final int dim = adapter.size(array);
      double[] min = new double[dim], max = new double[dim];
      if(symmetric) {
        for(int i = 0; i < dim; ++i) {
          double v = adapter.getDouble(array, i);
          double width = rand.nextDouble() * (maxDev - minDev) + minDev;
          min[i] = v - width;
          max[i] = v + width;
        }
      }
      else {
        for(int i = 0; i < dim; ++i) {
          // Choose standard deviation
          final double s = rand.nextDouble() * (maxDev - minDev) + minDev;
          // Assume our center is off by a standard deviation of s.
          double v = adapter.getDouble(array, i) + rand.nextGaussian() * s;
          min[i] = v - s;
          max[i] = v + s;
        }
      }
      return new SimpleGaussianContinuousUncertainObject(new HyperBoundingBox(min, max));
    }

    @Override
    public Class<? super SimpleGaussianContinuousUncertainObject> getRestrictionClass() {
      return SimpleGaussianContinuousUncertainObject.class;
    }

    @Override
    public ByteBufferSerializer<SimpleGaussianContinuousUncertainObject> getDefaultSerializer() {
      return null; // TODO: not available
    }

    /**
     * Parameterizer class.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractUncertainObject.Factory.Parameterizer {
      /**
       * Parameter for minimum 3-sigma deviation.
       */
      public static final OptionID DEV_MIN_ID = new OptionID("uo.uncertainty.min3sigma", "Minimum 3-sigma deviation of uncertain region.");

      /**
       * Parameter for maximum 3-sigma deviation.
       */
      public static final OptionID DEV_MAX_ID = new OptionID("uo.uncertainty.max3sigma", "Maximum 3-sigma deviation of uncertain region.");

      /**
       * Minimum and maximum allowed deviation.
       */
      protected double minDev, maxDev;

      /**
       * Field to hold random for uncertainification.
       */
      protected RandomFactory rand;

      /**
       * Generate symmetric distributions only.
       */
      protected boolean symmetric;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final DoubleParameter pminDev = new DoubleParameter(DEV_MIN_ID, 0.);
        if(config.grab(pminDev)) {
          minDev = pminDev.getValue();
        }
        final DoubleParameter pmaxDev = new DoubleParameter(DEV_MAX_ID);
        if(config.grab(pmaxDev)) {
          maxDev = pmaxDev.getValue();
        }
        final RandomParameter pseed = new RandomParameter(SEED_ID);
        if(config.grab(pseed)) {
          rand = pseed.getValue();
        }
        Flag symmetricF = new Flag(SYMMETRIC_ID);
        if(config.grab(symmetricF)) {
          symmetric = symmetricF.isTrue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(minDev, maxDev, symmetric, rand);
      }
    }
  }
}
