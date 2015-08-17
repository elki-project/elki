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
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Continuous uncertain object model using a uniform distribution on the
 * bounding box.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
public class UniformContinuousUncertainObject extends AbstractUncertainObject {
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
    return new DoubleVector(mean);
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    final int dim = bounds.getDimensionality();
    double[] values = new double[dim];

    for(int i = 0; i < dim; i++) {
      double w = bounds.getMax(i) - bounds.getMin(i);
      assert(w < Double.POSITIVE_INFINITY);
      values[i] = rand.nextDouble() * w + bounds.getMin(i);
    }
    return new DoubleVector(values);
  }

  /**
   * Factory class.
   *
   * @author Erich Schubert
   */
  public static class Factory extends AbstractUncertainObject.Factory<UniformContinuousUncertainObject> {
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
    public <A> UniformContinuousUncertainObject newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
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
          double v = adapter.getDouble(array, i);
          min[i] = v - (rand.nextDouble() * (maxDev - minDev) + minDev);
          max[i] = v + (rand.nextDouble() * (maxDev - minDev) + minDev);
        }
      }
      return new UniformContinuousUncertainObject(new HyperBoundingBox(min, max));
    }

    @Override
    public Class<? super UniformContinuousUncertainObject> getRestrictionClass() {
      return UniformContinuousUncertainObject.class;
    }

    @Override
    public ByteBufferSerializer<UniformContinuousUncertainObject> getDefaultSerializer() {
      return null; // FIXME: not yet available.
    }

    /**
     * Parameterizer class.
     *
     * @author Alexander Koos
     * @author Erich Schubert
     */
    public final static class Parameterizer extends AbstractUncertainObject.Factory.Parameterizer {
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
