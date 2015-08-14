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
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

public class DistributedDiscreteUO extends UncertainObject {
  private DoubleVector[] samples;

  private double[] weights;

  private double weightSum;

  // Constructor
  public DistributedDiscreteUO(DoubleVector[] samples, double[] weights) {
    super();
    if(samples.length == 0) {
      throw new AbortException("Discrete Uncertain Objects must have at least one point.");
    }
    double check = 0;
    for(double weight : weights) {
      if(!(weight > 0 && weight < Double.POSITIVE_INFINITY)) {
        throw new IllegalArgumentException("Probabilities must be in positive and finite.");
      }
      check += weight;
    }
    this.samples = samples;
    this.bounds = computeBounds(samples);
    this.weights = weights;
    this.weightSum = check;
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    // Weighted sampling:
    double r = rand.nextDouble() * weightSum;
    int index = weights.length;
    while(--index >= 0 && r < weights[index]) {
      r -= weights[index];
    }
    if(index < 0) { // Within rounding errors
      index = rand.nextInt(samples.length);
    }
    return samples[index];
  }

  @Override
  public DoubleVector getMean() {
    final int dim = getDimensionality();
    // Weighted average.
    double[] meanVals = new double[dim];
    for(int i = 0; i < samples.length; i++) {
      DoubleVector v = samples[i];
      for(int d = 0; d < dim; d++) {
        meanVals[d] += v.doubleValue(d) * weights[i];
      }
    }

    for(int d = 0; d < dim; d++) {
      meanVals[d] /= weightSum;
    }
    return new DoubleVector(meanVals);
  }

  public static class Factory extends UncertainObject.Factory<DistributedDiscreteUO> {
    private double minDev, maxDev;

    private int minQuant, maxQuant;

    private Random rand;

    // Constructor for uncertainifyFilter-use
    //
    // This one is basically constructing a Factory,
    // one could argue that it would be better practice
    // to actually implement such a factory, but for
    // the time being I'll stick with this kind of
    // under engineered approach, until everything is
    // fine and I can give more priority to beauty
    // than to functionality.
    public Factory(double minDev, double maxDev, int minQuant, int maxQuant, RandomFactory rand) {
      this.minDev = minDev;
      this.maxDev = maxDev;
      this.minQuant = minQuant;
      this.maxQuant = maxQuant;
      this.rand = rand.getRandom();
    }

    @Override
    public <A> DistributedDiscreteUO newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
      final int dim = adapter.size(array);
      final int distributionSize = rand.nextInt((maxQuant - minQuant) + 1) + (int) minQuant;
      DoubleVector[] samples = new DoubleVector[distributionSize];
      double[] offrange = generateRandomRange(dim, minDev, maxDev, !blur, rand);
      // Produce samples:
      double[] weights = new double[distributionSize];
      double[] buf = new double[dim];
      for(int i = 0; i < distributionSize; i++) {
        for(int j = 0, k = 0; j < dim; j++) {
          double gtv = adapter.getDouble(array, j);
          buf[j] = gtv + offrange[k++] + rand.nextDouble() * offrange[k++];
        }
        samples[i] = new DoubleVector(buf);
      }
      return new DistributedDiscreteUO(samples, weights);
    }

    @Override
    public Class<? super DistributedDiscreteUO> getRestrictionClass() {
      return DistributedDiscreteUO.class;
    }

    @Override
    public ByteBufferSerializer<DistributedDiscreteUO> getDefaultSerializer() {
      return null; // TODO: not yet available
    }

    public static class Parameterizer extends UncertainObject.Factory.Parameterizer {
      protected double minDev, maxDev;

      protected int minQuant, maxQuant;

      protected RandomFactory randFac;

      protected double maxTotalProb;

      @Override
      protected void makeOptions(final Parameterization config) {
        super.makeOptions(config);
        DoubleParameter pmaxMin = new DoubleParameter(MAX_MIN_ID);
        if(config.grab(pmaxMin)) {
          maxDev = pmaxMin.doubleValue();
        }
        DoubleParameter pminMin = new DoubleParameter(MIN_MIN_ID, 0.);
        if(config.grab(pminMin)) {
          minDev = pminMin.doubleValue();
        }
        IntParameter pmultMax = new IntParameter(MULT_MAX_ID, DEFAULT_SAMPLE_SIZE);
        if(config.grab(pmultMax)) {
          maxQuant = pmultMax.intValue();
        }
        IntParameter pmultMin = new IntParameter(MULT_MIN_ID) //
        .setOptional(true);
        if(config.grab(pmultMin)) {
          minQuant = pmultMin.intValue();
        }
        else {
          minQuant = maxQuant;
        }
        RandomParameter pseed = new RandomParameter(SEED_ID);
        if(config.grab(pseed)) {
          randFac = pseed.getValue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(minDev, maxDev, minQuant, maxQuant, randFac);
      }
    }
  }
}
