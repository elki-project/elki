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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

public class DistributedDiscreteUO extends AbstractDiscreteUncertainObject<List<Pair<DoubleVector, Integer>>> {
  private int totalProbability;

  private final static DoubleVector noObjectChosen = new DoubleVector(new double[] { Double.NEGATIVE_INFINITY });

  private double minMin, maxMin, minMax, maxMax;

  private long multMin, multMax;

  private Random drand;

  // Constructor for uncertainifyFilter-use
  //
  // This one is basically constructing a Factory,
  // one could argue that it would be better practice
  // to actually implement such a factory, but for
  // the time being I'll stick with this kind of
  // under engineered approach, until everything is
  // fine and I can give more priority to beauty
  // than to functionality.
  public DistributedDiscreteUO(final double minMin, final double maxMin, final double minMax, final double maxMax, final long multMin, final long multMax, final Long distributionSeed, final double maxTotalProb, final RandomFactory randFac) {
    this.minMin = minMin;
    this.maxMin = maxMin;
    this.minMax = minMax;
    this.maxMax = maxMax;
    this.multMin = multMin;
    this.multMax = multMax;
    this.rand = randFac.getRandom();
    this.drand = (new RandomFactory(distributionSeed)).getRandom();
    this.totalProbability = (int) (UOModel.PROBABILITY_SCALE * maxTotalProb);
  }

  // Constructor
  public DistributedDiscreteUO(final List<Pair<DoubleVector, Integer>> samplePoints, final RandomFactory randomFactory) {
    int check = 0;
    for(final Pair<DoubleVector, Integer> pair : samplePoints) {
      if(pair.getSecond() < 0) {
        throw new IllegalArgumentException("[W: ]\tA probability less than 0 is not possible.");
      }
      check += pair.getSecond();
    }

    // User of this class should think of a way to handle possible exception at
    // this point
    // to not find their program crashing without need.
    // To avoid misunderstanding one could compile a ".*total of 1\.$"-like
    // pattern against
    // raised IllegalArgumentExceptions and thereby customize his handle for
    // this case.
    if(check > UOModel.PROBABILITY_SCALE) {
      throw new IllegalArgumentException("[W: ]\tThe sum of probabilities exceeded a total of 1.");
    }
    this.samplePoints = samplePoints;
    this.totalProbability = check;
    this.dimensions = samplePoints.get(0).getFirst().getDimensionality();
    this.rand = randomFactory.getRandom();
    this.setBounds();
  }

  @Override
  public DoubleVector drawSample() {
    final int[] weights = new int[this.samplePoints.size()];
    for(int i = 0; i < this.samplePoints.size(); i++) {
      weights[i] = this.samplePoints.get(i).getSecond();
    }

    final int ind = UncertainUtil.drawIndexFromIntegerWeights(this.rand, weights, this.totalProbability);

    return ind < this.samplePoints.size() ? this.samplePoints.get(ind).getFirst() : DistributedDiscreteUO.noObjectChosen;
  }
  
  public DoubleVector getMean() {
    double[] meanVals = new double[this.dimensions];
    int sumWeights = 0;
    
    for (Pair<DoubleVector,Integer> derp : this.samplePoints) {
      double[] vals = derp.first.getValues();
      sumWeights += derp.second;
      for (int i=0; i<this.dimensions; i++) {
        meanVals[i] += vals[i] * derp.second;
      }
    }
    
    for (int i=0; i<this.dimensions; i++) {
      meanVals[i] /= sumWeights;
    }
    
    return new DoubleVector(meanVals);
  }

  protected void setBounds() {
    final double min[] = new double[this.dimensions];
    Arrays.fill(min, Double.MAX_VALUE);
    final double max[] = new double[this.dimensions];
    Arrays.fill(max, -Double.MAX_VALUE);
    for(final Pair<DoubleVector, Integer> samplePoint : this.samplePoints) {
      for(int d = 0; d < this.dimensions; d++) {
        min[d] = Math.min(min[d], samplePoint.getFirst().doubleValue(d));
        max[d] = Math.max(max[d], samplePoint.getFirst().doubleValue(d));
      }
    }
    this.bounds = new HyperBoundingBox(min, max);
  }

  @Override
  public UncertainObject<UOModel> uncertainify(final NumberVector vec, final boolean blur) {
    final List<Pair<DoubleVector, Integer>> sampleList = new ArrayList<Pair<DoubleVector, Integer>>();
    final int genuine = this.rand.nextInt(vec.getDimensionality());
    final double difMin = this.rand.nextDouble() * (this.maxMin - this.minMin) + this.minMin;
    final double difMax = this.rand.nextDouble() * (this.maxMax - this.minMax) + this.minMax;
    final double randDev = blur ? (this.rand.nextInt(2) == 0 ? this.rand.nextDouble() * -difMin : this.rand.nextDouble() * difMax) : 0;
    final int distributionSize = this.rand.nextInt((int) (this.multMax - this.multMin) + 1) + (int) this.multMin;
    final int[] iweights = UncertainUtil.calculateRandomIntegerWeights(distributionSize, this.totalProbability, this.rand);
    for(int i = 0; i < distributionSize; i++) {
      if(i == genuine) {
        sampleList.add(new Pair<DoubleVector, Integer>(new DoubleVector(vec.getColumnVector()), iweights[i]));
        continue;
      }
      final double[] spair = new double[vec.getDimensionality()];
      for(int j = 0; j < vec.getDimensionality(); j++) {
        final double gtv = vec.doubleValue(j);
        spair[j] = gtv + this.rand.nextDouble() * difMax - this.rand.nextDouble() * difMin + randDev;
      }
      sampleList.add(new Pair<DoubleVector, Integer>(new DoubleVector(spair), iweights[i]));
    }
    return new UncertainObject<UOModel>(new DistributedDiscreteUO(sampleList, new RandomFactory(this.drand.nextLong())), vec.getColumnVector());
  }

  public static class Parameterizer extends AbstractParameterizer {

    protected double minMin, maxMin, minMax, maxMax;

    protected long multMin, multMax;

    protected RandomFactory randFac;

    protected Long distributionSeed;

    protected double maxTotalProb;

    public static final OptionID MIN_MIN_ID = new OptionID("objects.lbound.min", "Minimum lower boundary.");

    public static final OptionID MAX_MIN_ID = new OptionID("objects.lbound.max", "Maximum lower boundary.");

    public static final OptionID MIN_MAX_ID = new OptionID("objects.ubound.min", "Minimum upper boundary.");

    public static final OptionID MAX_MAX_ID = new OptionID("objects.ubound.max", "Maximum upper boundary.");

    public static final OptionID MULT_MIN_ID = new OptionID("uo.mult.min", "Minimum Points per uncertain object.");

    public static final OptionID MULT_MAX_ID = new OptionID("uo.mult.max", "Maximum Points per uncertain object.");

    public static final OptionID SEED_ID = new OptionID("uo.seed", "Seed for uncertainification.");

    public static final OptionID DISTRIBUTION_SEED_ID = new OptionID("ret.uo.seed", "Seed for uncertain objects private Random.");

    public static final OptionID MAXIMUM_PROBABILITY_ID = new OptionID("uo.maxprob", "Maximum total probability to draw a valid sample at all.");

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter pminMin = new DoubleParameter(Parameterizer.MIN_MIN_ID, UOModel.DEFAULT_MIN_MAX_DEVIATION);
      if(config.grab(pminMin)) {
        this.minMin = pminMin.getValue();
      }
      final DoubleParameter pmaxMin = new DoubleParameter(Parameterizer.MAX_MIN_ID, UOModel.DEFAULT_MIN_MAX_DEVIATION);
      if(config.grab(pmaxMin)) {
        this.maxMin = pmaxMin.getValue();
      }
      final DoubleParameter pminMax = new DoubleParameter(Parameterizer.MIN_MAX_ID, UOModel.DEFAULT_MIN_MAX_DEVIATION);
      if(config.grab(pminMax)) {
        this.minMax = pminMax.getValue();
      }
      final DoubleParameter pmaxMax = new DoubleParameter(Parameterizer.MAX_MAX_ID, UOModel.DEFAULT_MIN_MAX_DEVIATION);
      if(config.grab(pmaxMax)) {
        this.maxMax = pmaxMax.getValue();
      }
      final LongParameter pmultMin = new LongParameter(Parameterizer.MULT_MIN_ID, UOModel.DEFAULT_SAMPLE_SIZE);
      if(config.grab(pmultMin)) {
        this.multMin = pmultMin.getValue();
      }
      final LongParameter pmultMax = new LongParameter(Parameterizer.MULT_MAX_ID, UOModel.DEFAULT_SAMPLE_SIZE);
      if(config.grab(pmultMax)) {
        this.multMax = pmultMax.getValue();
      }
      final LongParameter pseed = new LongParameter(Parameterizer.SEED_ID);
      pseed.setOptional(true);
      if(config.grab(pseed)) {
        this.randFac = new RandomFactory(pseed.getValue());
      }
      else {
        this.randFac = new RandomFactory(null);
      }
      final LongParameter dseed = new LongParameter(Parameterizer.DISTRIBUTION_SEED_ID);
      dseed.setOptional(true);
      if(config.grab(dseed)) {
        this.distributionSeed = dseed.getValue();
      }
      final DoubleParameter maxProb = new DoubleParameter(Parameterizer.MAXIMUM_PROBABILITY_ID, UOModel.DEFAULT_MAX_TOTAL_PROBABILITY);
      if(config.grab(maxProb)) {
        this.maxTotalProb = maxProb.getValue();
      }
    }

    @Override
    protected AbstractDiscreteUncertainObject<?> makeInstance() {
      return new DistributedDiscreteUO(this.minMin, this.maxMin, this.minMax, this.maxMax, this.multMin, this.multMax, this.distributionSeed, this.maxTotalProb, this.randFac);
    }
  }
}
