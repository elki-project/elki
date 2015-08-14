package de.lmu.ifi.dbs.elki.data.uncertain;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * This class is derived from {@link AbstractDiscreteUncertainObject} and models
 * Discrete-Uncertain-Data-Objects with a uniform distribution of their values
 * probabilities, i.e. every possible value has the same probability to be drawn
 * and they sum up to 1.
 *
 * @author Alexander Koos
 */
public class UniformDiscreteUO extends UncertainObject {
  private DoubleVector[] samples;

  // Constructor
  public UniformDiscreteUO(DoubleVector[] samples) {
    this.samples = samples;
    int dimensions = samples[0].getDimensionality();
    // Compute bounds:
    final double min[] = new double[dimensions];
    final double max[] = new double[dimensions];
    DoubleVector first = this.samples[0];
    for(int d = 0; d < dimensions; d++) {
      min[d] = max[d] = first.doubleValue(d);
    }
    for(int i = 1; i < this.samples.length; i++) {
      DoubleVector v = this.samples[i];
      for(int d = 0; d < dimensions; d++) {
        final double c = v.doubleValue(d);
        min[d] = c < min[d] ? c : min[d];
        max[d] = c > max[d] ? c : max[d];
      }
    }
    this.bounds = new HyperBoundingBox(min, max);
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    // Since the probability is the same for each samplePoint and
    // precisely 1:samplePoints.size(), it should be fair enough
    // to simply draw a sample by returning the point at
    // Index := random.mod(samplePoints.size())
    return samples[rand.nextInt(samples.length)];
  }

  @Override
  public DoubleVector getMean() {
    final int dimensions = getDimensionality();
    double[] meanVals = new double[dimensions];

    for(DoubleVector sp : samples) {
      double[] vals = sp.getValues();
      for(int i = 0; i < dimensions; i++) {
        meanVals[i] += vals[i];
      }
    }

    for(int i = 0; i < dimensions; i++) {
      meanVals[i] /= samples.length;
    }
    return new DoubleVector(meanVals);
  }

  @Override
  public Double getValue(int dimension) {
    // Not particularly meaningful, but currently unused anyway.
    return (bounds.getMax(dimension) + bounds.getMin(dimension)) * .5;
  }

  public static class Factory extends UncertainObject.Factory<UniformDiscreteUO> {
    private double minDev, maxDev;

    private int minQuant, maxQuant;

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
    public Factory(double minDev, double maxDev, int minQuant, int maxQuant, RandomFactory randFac) {
      this.minDev = minDev;
      this.maxDev = maxDev;
      this.minQuant = minQuant;
      this.maxQuant = maxQuant;
      this.drand = randFac.getRandom();
    }

    @Override
    public <A> UniformDiscreteUO newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
      final int dim = adapter.size(array);
      final int distributionSize = drand.nextInt((maxQuant - minQuant) + 1) + (int) minQuant;
      DoubleVector[] samples = new DoubleVector[distributionSize];
      double[] off = new double[dim], range = new double[dim];
      for(int j = 0; j < dim; j++) {
        off[j] = -1 * (drand.nextDouble() * (maxDev - minDev) + minDev);
        range[j] = -off[j] + (drand.nextDouble() * (maxDev - minDev) + minDev);
        if(blur) {
          off[j] += range[j] * (drand.nextDouble() - .5);
        }
      }
      // Produce samples:
      for(int i = 0; i < distributionSize; i++) {
        double[] svec = new double[dim];
        for(int j = 0; j < dim; j++) {
          double gtv = adapter.getDouble(array, j);
          svec[j] = gtv + off[j] + drand.nextDouble() * range[j];
        }
        samples[i] = new DoubleVector(svec);
      }
      return new UniformDiscreteUO(samples);
    }

    @Override
    public Class<? super UniformDiscreteUO> getRestrictionClass() {
      return UniformDiscreteUO.class;
    }

    @Override
    public ByteBufferSerializer<UniformDiscreteUO> getDefaultSerializer() {
      return null; // Not yet available.
    }

    // TODO: remove redundancy with DistributedDiscreteUO.Parameterizer
    public static class Parameterizer extends AbstractParameterizer {
      protected double minDev, maxDev;

      protected int minQuant, maxQuant;

      protected RandomFactory randFac;

      public static final OptionID MIN_MIN_ID = new OptionID("uo.uncertainty.min", "Minimum width of uncertain region.");

      public static final OptionID MAX_MIN_ID = new OptionID("uo.uncertainty.max", "Maximum width of uncertain region.");

      public static final OptionID MULT_MIN_ID = new OptionID("uo.quantity.min", "Minimum Points per uncertain object.");

      public static final OptionID MULT_MAX_ID = new OptionID("uo.quantity.max", "Maximum Points per uncertain object.");

      public static final OptionID SEED_ID = new OptionID("uo.seed", "Seed for uncertainification.");

      public static final OptionID DISTRIBUTION_SEED_ID = new OptionID("ret.uo.seed", "Seed for uncertain objects private Random.");

      public static final OptionID MAXIMUM_PROBABILITY_ID = new OptionID("uo.maxprob", "Maximum total probability to draw a valid sample at all.");

      @Override
      protected void makeOptions(final Parameterization config) {
        super.makeOptions(config);
        DoubleParameter pmaxMin = new DoubleParameter(Parameterizer.MAX_MIN_ID);
        if(config.grab(pmaxMin)) {
          maxDev = pmaxMin.doubleValue();
        }
        DoubleParameter pminMin = new DoubleParameter(Parameterizer.MIN_MIN_ID, 0.);
        if(config.grab(pminMin)) {
          minDev = pminMin.doubleValue();
        }
        IntParameter pmultMax = new IntParameter(Parameterizer.MULT_MAX_ID, UncertainObject.DEFAULT_SAMPLE_SIZE);
        if(config.grab(pmultMax)) {
          maxQuant = pmultMax.intValue();
        }
        IntParameter pmultMin = new IntParameter(Parameterizer.MULT_MIN_ID) //
        .setOptional(true);
        if(config.grab(pmultMin)) {
          minQuant = pmultMin.intValue();
        }
        else {
          minQuant = maxQuant;
        }
        RandomParameter pseed = new RandomParameter(Parameterizer.SEED_ID);
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
