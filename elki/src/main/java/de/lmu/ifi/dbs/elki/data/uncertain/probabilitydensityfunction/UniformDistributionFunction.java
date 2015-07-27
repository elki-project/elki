package de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction;

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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

/**
 * ProbabilityDensityFunction class to model uncertain objects
 * where values are randomly drawn, bounded by a {@link SpatialComparable}
 * without further constraints.
 * 
 * @author Alexander Koos
 */
public class UniformDistributionFunction extends ProbabilityDensityFunction {
  /**
   * Field to hold the value the randomly created maximum negative
   * deviation from the groundtruth shall have in minimum.
   */
  protected double minMin;
  
  /**
   * Field to hold the value the randomly created maximum negative
   * deviation from the groundtruth shall have in maximum.
   */
  protected double maxMin;
  
  /**
   * Field to hold the value the randomly created maximum positive
   * deviation from the groundtruth shall have in minimum.
   */
  protected double minMax;
  
  /**
   * Field to hold the value the randomly created maximum positive
   * deviation from the groundtruth shall have in maximum.
   */
  protected double maxMax;
  
  /**
   * Field to hold the random for uncertainification.
   */
  protected Random rand;

  /**
   * 
   * Constructor.
   *
   * @param minMin
   * @param maxMin
   * @param minMax
   * @param maxMax
   * @param rand
   */
  public UniformDistributionFunction(final double minMin, final double maxMin, final double minMax, final double maxMax, final Random rand) {
    this.minMin = minMin;
    this.maxMin = maxMin;
    this.minMax = minMax;
    this.maxMax = maxMax;
    this.rand = rand;
  }
  
  /**
   * 
   * Constructor - blank, perhaps obsolete.
   *
   */
  public UniformDistributionFunction() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public DoubleVector drawValue(SpatialComparable bounds, final Random rand) {
    double[] values = new double[bounds.getDimensionality()];
    
    for(int i = 0; i < bounds.getDimensionality(); i++) {
      if(!Double.valueOf(bounds.getMax(i) - bounds.getMin(i)).isInfinite()){
        values[i] = rand.nextDouble() * (bounds.getMax(i) - bounds.getMin(i)) + bounds.getMin(i);
      } else {
        values[i] = rand.nextInt(2) == 0 ? rand.nextDouble() * rand.nextInt(Integer.MAX_VALUE) : rand.nextDouble() * (-rand.nextInt(Integer.MAX_VALUE));
      }
    }
      
    return new DoubleVector(values);
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
  public UncertainObject<UOModel> uncertainify(NumberVector vec, boolean blur, boolean uncertainify, int dims) {
    final double[] min = new double[uncertainify ? vec.getDimensionality() : dims];
    final double[] max = new double[uncertainify ? vec.getDimensionality() : dims];
    for(int i = 0; i < ( uncertainify ? vec.getDimensionality() : dims ); i++) {
      if( uncertainify ) {
        final double preDev = rand.nextDouble();
        final double difMin = ( rand.nextDouble() * ( maxMin - minMin ) ) + minMin;
        final double difMax = ( rand.nextDouble() * ( maxMax - minMax ) ) + minMax;
        final double randDev = blur ? ( (rand.nextInt() % 2) == 0 ? preDev * -difMin : preDev * difMax ) : 0 ;
        min[i] = vec.doubleValue(i) - (rand.nextDouble() * difMin) + randDev;
        max[i] = vec.doubleValue(i) + (rand.nextDouble() * difMax) + randDev;
      } else {
        min[i] = vec.doubleValue(i);
        max[i] = vec.doubleValue(i + dims);
      }
    }
    
    return new UncertainObject<UOModel>(new ContinuousUncertainObject<UniformDistributionFunction>(min, max, new UniformDistributionFunction(), new RandomFactory(rand.nextLong())), new DoubleVector(vec.getColumnVector()));
  }
  
  /**
   * 
   * Parameterizer class.
   * 
   * @author Alexander Koos
   *
   */
  public final static class Parameterizer extends AbstractParameterizer {

    /**
     * Field to hold parameter value.
     */
    protected double minMin;
    
    /**
     * Field to hold parameter value.
     */
    protected double maxMin;
    
    /**
     * Field to hold parameter value.
     */
    protected double minMax;
    
    /**
     * Field to hold parameter value.
     */
    protected double maxMax;
    
    /**
     * Field to hold random for uncertainification.
     */
    protected Random rand;
    
    /**
     * Parameter to specify the minimum value for randomly drawn
     * deviation from the groundtruth in negative direction.
     */
    public static final OptionID MIN_MIN_ID = new OptionID("objects.lbound.min","Minimum lower boundary.");
    
    /**
     * Parameter to specify the maximum value for randomly drawn
     * deviation from the groundtruth in negative direction.
     */
    public static final OptionID MAX_MIN_ID = new OptionID("objects.lbound.max","Maximum lower boundary.");
    
    /**
     * Parameter to specify the minimum value for randomly drawn
     * deviation from the groundtruth in positive direction.
     */
    public static final OptionID MIN_MAX_ID = new OptionID("objects.ubound.min","Minimum upper boundary.");
    
    /**
     * Parameter to specify the maximum value for randomly drawn
     * deviation from the groundtruth in positive direction.
     */
    public static final OptionID MAX_MAX_ID = new OptionID("objects.ubound.max","Maximum upper boundary.");
    
    /**
     * Parameter to specify the seed for uncertainification.
     */
    public static final OptionID SEED_ID = new OptionID("uo.pdf.seed","Seed for uncertain objects private Random.");
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter pminMin = new DoubleParameter(MIN_MIN_ID, UOModel.DEFAULT_MIN_MAX_DEVIATION);
      if(config.grab(pminMin)) {
        minMin = pminMin.getValue();
      }
      final DoubleParameter pmaxMin = new DoubleParameter(MAX_MIN_ID, UOModel.DEFAULT_MIN_MAX_DEVIATION);
      if(config.grab(pmaxMin)) {
        minMin = pmaxMin.getValue();
      }
      final DoubleParameter pminMax = new DoubleParameter(MIN_MAX_ID, UOModel.DEFAULT_MIN_MAX_DEVIATION);
      if(config.grab(pminMax)) {
        minMin = pminMax.getValue();
      }
      final DoubleParameter pmaxMax = new DoubleParameter(MAX_MAX_ID, UOModel.DEFAULT_MIN_MAX_DEVIATION);
      if(config.grab(pmaxMax)) {
        maxMax = pmaxMax.getValue();
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
      return new UniformDistributionFunction(minMin, maxMin, minMax, maxMax, rand);
    }
  }
}
