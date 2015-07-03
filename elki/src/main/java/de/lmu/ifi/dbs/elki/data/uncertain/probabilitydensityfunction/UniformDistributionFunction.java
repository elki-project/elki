package de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction;

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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.uncertain.ContinuousUncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UOModel;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * ProbabilityDensityFunction class to model uncertain objects where values are
 * randomly drawn, bounded by a {@link SpatialComparable} without further
 * constraints.
 *
 * @author Alexander Koos
 */
public class UniformDistributionFunction extends ProbabilityDensityFunction {
  /**
   * Field to hold the value the randomly created minimum and maximum deviation.
   */
  protected double min, max;

  /**
   * Field to hold the random for uncertainification.
   */
  protected Random rand;

  /**
   * Constructor.
   *
   * @param min
   * @param max
   * @param rand
   */
  public UniformDistributionFunction(double min, double max, RandomFactory rand) {
    this.min = min;
    this.max = max;
    this.rand = rand;
  }

  @Override
  public DoubleVector drawValue(SpatialComparable bounds, final Random rand) {
    double[] values = new double[bounds.getDimensionality()];
    
    for(int i = 0; i < bounds.getDimensionality(); i++) {
      if((bounds.getMax(i) - bounds.getMin(i)) < Double.POSITIVE_INFINITY) {
        values[i] = rand.nextDouble() * (bounds.getMax(i) - bounds.getMin(i)) + bounds.getMin(i);
      }
      else {
        values[i] = rand.nextInt(2) == 0 ? rand.nextDouble() * rand.nextInt(Integer.MAX_VALUE) : rand.nextDouble() * (-rand.nextInt(Integer.MAX_VALUE));
      }
    }
      
    return new DoubleVector(values);
  }

  @Override
  public DoubleVector getMean(SpatialComparable bounds) {
    double[] meanVals = new double[bounds.getDimensionality()];

    for(int i = 0; i < bounds.getDimensionality(); i++) {
      if((bounds.getMax(i) - bounds.getMin(i)) < Double.POSITIVE_INFINITY) {
        meanVals[i] = (bounds.getMax(i) - bounds.getMin(i)) * .5;
      }
      else {
        meanVals[i] = 0;
      }
    }

    return new DoubleVector(meanVals);
  }

  @Override
  public SpatialComparable getDefaultBounds(final int dimensions) {
    double[] min = new double[dimensions];
    double[] max = new double[dimensions];
    for(int i = 0; i < dimensions; i++) {
      min[i] = ProbabilityDensityFunction.DEFAULT_MIN;
      max[i] = ProbabilityDensityFunction.DEFAULT_MAX;
    }
    return new HyperBoundingBox(min, max);
  }

  @Override
  public UncertainObject<UOModel> uncertainify(NumberVector vec, boolean blur) {
    final double[] min = new double[vec.getDimensionality()];
    final double[] max = new double[vec.getDimensionality()];
    Random r = rand.getSingleThreadedRandom();
    for(int i = 0; i < vec.getDimensionality(); i++) {
      final double preDev = r.nextDouble();
      final double difMin = (r.nextDouble() * (this.max - this.min)) + this.min;
      final double difMax = (r.nextDouble() * (this.max - this.min)) + this.min;
      final double randDev = blur ? ((r.nextInt() % 2) == 0 ? preDev * -difMin : preDev * difMax) : 0;
      min[i] = vec.doubleValue(i) - (r.nextDouble() * difMin) + randDev;
      max[i] = vec.doubleValue(i) + (r.nextDouble() * difMax) + randDev;
    }

    return new UncertainObject<UOModel>(new ContinuousUncertainObject<>(min, max, this, new RandomFactory(r.nextLong())), vec);
  }
  
  /**
   * Parameterizer class.
   * 
   * @author Alexander Koos
   */
  public final static class Parameterizer extends AbstractParameterizer {
    /**
     * Minimum and maximum allowed deviation.
     */
    protected double min, max;

    /**
     * Field to hold random for uncertainification.
     */
    protected Random rand;
    
    /**
     * Parameter to specify the minimum value for randomly drawn deviation.
     */
    public static final OptionID MIN_ID = new OptionID("objects.mindev", "Minimum deviation.");

    /**
     * Parameter to specify the maximum value for randomly drawn deviation.
     */
    public static final OptionID MAX_ID = new OptionID("objects.maxdev", "Maximum deviation.");

    /**
     * Parameter to specify the seed for uncertainification.
     */
    public static final OptionID SEED_ID = new OptionID("uo.pdf.seed", "Seed for uncertain objects private Random.");

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter pminMin = new DoubleParameter(MIN_ID, 0.);
      if(config.grab(pminMin)) {
        min = pminMin.getValue();
      }
      final DoubleParameter pmaxMin = new DoubleParameter(MAX_ID);
      if(config.grab(pmaxMin)) {
        max = pmaxMin.getValue();
      }
      final LongParameter pseed = new LongParameter(SEED_ID);
      pseed.setOptional(true);
      if(config.grab(pseed)) {
        rand = (new RandomFactory(pseed.getValue())).getRandom();
      } else {
        rand = (new RandomFactory(null)).getRandom();
      }
    }
    
    @Override
    protected Object makeInstance() {
      return new UniformDistributionFunction(min, max, rand);
    }
  }
}
