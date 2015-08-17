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
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Discrete uncertain objects are based a finite number of samples.
 *
 * @author Erich Schubert
 */
public abstract class AbstractDiscreteUncertainObject extends AbstractUncertainObject {
  /**
   * Compute the bounding box for some samples.
   *
   * @param samples Samples
   * @return Bounding box.
   */
  protected static HyperBoundingBox computeBounds(DoubleVector[] samples) {
    assert(samples.length > 0) : "Cannot compute bounding box of empty set.";
    // Compute bounds:
    final int dimensions = samples[0].getDimensionality();
    final double min[] = new double[dimensions];
    final double max[] = new double[dimensions];
    DoubleVector first = samples[0];
    for(int d = 0; d < dimensions; d++) {
      min[d] = max[d] = first.doubleValue(d);
    }
    for(int i = 1; i < samples.length; i++) {
      DoubleVector v = samples[i];
      for(int d = 0; d < dimensions; d++) {
        final double c = v.doubleValue(d);
        min[d] = c < min[d] ? c : min[d];
        max[d] = c > max[d] ? c : max[d];
      }
    }
    return new HyperBoundingBox(min, max);
  }

  /**
   * Factory class for discrete uncertain objects.
   *
   * @author Erich Schubert
   *
   * @param <UO> Uncertain object type.
   */
  public abstract static class Factory<UO extends UncertainObject> extends AbstractUncertainObject.Factory<UO> {
    /**
     * Minimum and maximum number of samples.
     */
    protected int minQuant, maxQuant;

    /**
     * Random generator.
     */
    protected Random rand;

    /**
     * Constructor.
     *
     * @param minQuant Minimum number of samples
     * @param maxQuant Maximum number of samples
     * @param rand Random generator
     */
    public Factory(int minQuant, int maxQuant, RandomFactory rand) {
      super();
      this.minQuant = minQuant;
      this.maxQuant = maxQuant;
      this.rand = rand.getRandom();
    }

    /**
     * Parameterizer.
     *
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    public static abstract class Parameterizer extends AbstractUncertainObject.Factory.Parameterizer {
      /**
       * Default sample size for generating finite representations.
       */
      public final static int DEFAULT_SAMPLE_SIZE = 10;

      /**
       * Maximum quantity of generated samples.
       */
      public static final OptionID MULT_MAX_ID = new OptionID("uo.quantity.max", "Maximum points per uncertain object.");

      /**
       * Minimum quantity of generated samples.
       */
      public static final OptionID MULT_MIN_ID = new OptionID("uo.quantity.min", "Minimum points per uncertain object (defaults to maximum.");

      /**
       * Minimum and maximum number of samples.
       */
      protected int minQuant, maxQuant;

      /**
       * Random generator.
       */
      protected RandomFactory randFac;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        RandomParameter pseed = new RandomParameter(SEED_ID);
        if(config.grab(pseed)) {
          randFac = pseed.getValue();
        }
        IntParameter pmultMax = new IntParameter(MULT_MAX_ID, DEFAULT_SAMPLE_SIZE);
        if(config.grab(pmultMax)) {
          maxQuant = pmultMax.intValue();
        }
        IntParameter pmultMin = new IntParameter(MULT_MIN_ID) //
        .setOptional(true);
        minQuant = config.grab(pmultMin) ? pmultMin.intValue() : maxQuant;
      }
    }
  }
}