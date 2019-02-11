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
package de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.FeatureVector.Factory;
import de.lmu.ifi.dbs.elki.data.uncertain.SimpleGaussianContinuousUncertainObject;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Vector factory
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - SimpleGaussianContinuousUncertainObject
 */
public class SimpleGaussianUncertainifier implements Uncertainifier<SimpleGaussianContinuousUncertainObject> {
  /**
   * Minimum and maximum allowed deviation.
   */
  double minDev, maxDev;

  /**
   * Generate symmetric distributions only.
   */
  boolean symmetric;

  /**
   * Constructor.
   *
   * @param minDev Minimum deviation
   * @param maxDev Maximum deviation
   * @param symmetric Generate symmetric distributions only
   */
  public SimpleGaussianUncertainifier(double minDev, double maxDev, boolean symmetric) {
    super();
    this.minDev = minDev;
    this.maxDev = maxDev;
    this.symmetric = symmetric;
  }

  @Override
  public <A> SimpleGaussianContinuousUncertainObject newFeatureVector(Random rand, A array, NumberArrayAdapter<?, A> adapter) {
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
  public Factory<SimpleGaussianContinuousUncertainObject, ?> getFactory() {
    return SimpleGaussianContinuousUncertainObject.FACTORY;
  }

  /**
   * Parameterizer class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
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
     * Generate symmetric distributions only.
     */
    protected boolean symmetric;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter pminDev = new DoubleParameter(DEV_MIN_ID, 0.);
      if(config.grab(pminDev)) {
        minDev = pminDev.getValue();
      }
      DoubleParameter pmaxDev = new DoubleParameter(DEV_MAX_ID);
      if(config.grab(pmaxDev)) {
        maxDev = pmaxDev.getValue();
      }
      Flag symmetricF = new Flag(SYMMETRIC_ID);
      if(config.grab(symmetricF)) {
        symmetric = symmetricF.isTrue();
      }
    }

    @Override
    protected SimpleGaussianUncertainifier makeInstance() {
      return new SimpleGaussianUncertainifier(minDev, maxDev, symmetric);
    }
  }
}