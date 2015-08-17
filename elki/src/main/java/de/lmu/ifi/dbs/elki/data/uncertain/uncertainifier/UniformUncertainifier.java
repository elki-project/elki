package de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier;
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

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.FeatureVector.Factory;
import de.lmu.ifi.dbs.elki.data.uncertain.UniformContinuousUncertainObject;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Factory class.
 *
 * @author Erich Schubert
 */
public class UniformUncertainifier extends AbstractUncertainifier<UniformContinuousUncertainObject> {
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
  public UniformUncertainifier(double minDev, double maxDev, boolean symmetric, RandomFactory rand) {
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
  public Factory<UniformContinuousUncertainObject, ?> getFactory() {
    return UniformContinuousUncertainObject.FACTORY;
  }

  /**
   * Parameterizer class.
   *
   * @author Alexander Koos
   * @author Erich Schubert
   */
  public final static class Parameterizer extends AbstractUncertainifier.Parameterizer {
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
    protected UniformUncertainifier makeInstance() {
      return new UniformUncertainifier(minDev, maxDev, symmetric, rand);
    }
  }
}