/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.data.uncertain.uncertainifier;

import java.util.Random;

import elki.data.FeatureVector.Factory;
import elki.data.HyperBoundingBox;
import elki.data.uncertain.UniformContinuousUncertainObject;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;

/**
 * Factory class.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - UniformContinuousUncertainObject
 */
public class UniformUncertainifier implements Uncertainifier<UniformContinuousUncertainObject> {
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
  public UniformUncertainifier(double minDev, double maxDev, boolean symmetric) {
    super();
    this.minDev = minDev;
    this.maxDev = maxDev;
    this.symmetric = symmetric;
  }

  @Override
  public <A> UniformContinuousUncertainObject newFeatureVector(Random rand, A array, NumberArrayAdapter<?, A> adapter) {
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
  public Factory<UniformContinuousUncertainObject, ?> getFactory() {
    return UniformContinuousUncertainObject.FACTORY;
  }

  /**
   * Parameterizer class.
   *
   * @author Alexander Koos
   * @author Erich Schubert
   */
  public final static class Par implements Parameterizer {
    /**
     * Minimum deviation of the generated bounding box.
     */
    public static final OptionID DEV_MIN_ID = new OptionID("uo.uncertainty.min", "Minimum deviation of uncertain bounding box.");

    /**
     * Maximum deviation of the generated bounding box.
     */
    public static final OptionID DEV_MAX_ID = new OptionID("uo.uncertainty.max", "Maximum deviation of uncertain bounding box.");

    /**
     * Minimum and maximum allowed deviation.
     */
    protected double minDev, maxDev;

    /**
     * Generate symmetric distributions only.
     */
    protected boolean symmetric;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(DEV_MIN_ID, 0.).grab(config, x -> minDev = x);
      new DoubleParameter(DEV_MAX_ID).grab(config, x -> maxDev = x);
      new Flag(SYMMETRIC_ID).grab(config, x -> symmetric = x);
    }

    @Override
    public UniformUncertainifier make() {
      return new UniformUncertainifier(minDev, maxDev, symmetric);
    }
  }
}
