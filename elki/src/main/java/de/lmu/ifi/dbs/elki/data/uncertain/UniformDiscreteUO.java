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
  public UniformDiscreteUO(DoubleVector[] samples, RandomFactory randomFactory) {
    this.samples = samples;
    this.dimensions = samples[0].getDimensionality();
    this.rand = randomFactory.getRandom();
    // Compute bounds:
    final double min[] = new double[this.dimensions];
    final double max[] = new double[this.dimensions];
    DoubleVector first = this.samples[0];
    for(int d = 0; d < this.dimensions; d++) {
      min[d] = max[d] = first.doubleValue(d);
    }
    for(int i = 1; i < this.samples.length; i++) {
      DoubleVector v = this.samples[i];
      for(int d = 0; d < this.dimensions; d++) {
        final double c = v.doubleValue(d);
        min[d] = c < min[d] ? c : min[d];
        max[d] = c > max[d] ? c : max[d];
      }
    }
    this.bounds = new HyperBoundingBox(min, max);
  }

  @Override
  public DoubleVector drawSample() {
    // Since the probability is the same for each samplePoint and
    // precisely 1:samplePoints.size(), it should be fair enough
    // to simply draw a sample by returning the point at
    // Index := random.mod(samplePoints.size())
    return samples[rand.nextInt(samples.length)];
  }

  @Override
  public DoubleVector getMean() {
    double[] meanVals = new double[this.dimensions];

    for(DoubleVector sp : samples) {
      double[] vals = sp.getValues();
      for(int i = 0; i < this.dimensions; i++) {
        meanVals[i] += vals[i];
      }
    }

    for(int i = 0; i < this.dimensions; i++) {
      meanVals[i] /= this.dimensions;
    }

    return new DoubleVector(meanVals);
  }

  @Override
  public Double getValue(int dimension) {
    // Not particularly meaningful, but currently unused anyway.
    return (bounds.getMax(dimension) + bounds.getMin(dimension)) * .5;
  }

  public static class Factory extends UncertainObject.Factory<UniformDiscreteUO> {
    private double minMin, maxMin, minMax, maxMax;

    private int multMin, multMax;

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
    public Factory(double minMin, double maxMin, double minMax, double maxMax, int multMin, int multMax, RandomFactory randFac) {
      this.minMin = minMin;
      this.maxMin = maxMin;
      this.minMax = minMax;
      this.maxMax = maxMax;
      this.multMin = multMin;
      this.multMax = multMax;
      this.drand = randFac.getRandom();
    }

    @Override
    public <A> UniformDiscreteUO newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
      final int dim = adapter.size(array);
      final int distributionSize = drand.nextInt((int) (multMax - multMin) + 1) + (int) multMin;
      DoubleVector[] samples = new DoubleVector[distributionSize];
      double difMin = drand.nextDouble() * (maxMin - minMin) + minMin;
      double difMax = drand.nextDouble() * (maxMax - minMax) + minMax;
      double randDev = blur ? (drand.nextInt(2) == 0 ? drand.nextDouble() * -difMin : drand.nextDouble() * difMax) : 0;
      for(int i = 0; i < distributionSize; i++) {
        double[] svec = new double[dim];
        for(int j = 0; j < dim; j++) {
          double gtv = adapter.getDouble(array, j);
          svec[j] = gtv + drand.nextDouble() * difMax - drand.nextDouble() * difMin + randDev;
        }
        samples[i] = new DoubleVector(svec);
      }
      return new UniformDiscreteUO(samples, new RandomFactory(drand.nextLong()));
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
      protected double minMin, maxMin, minMax, maxMax;

      protected int multMin, multMax;

      protected RandomFactory randFac;

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
        final DoubleParameter pminMin = new DoubleParameter(Parameterizer.MIN_MIN_ID, UncertainObject.DEFAULT_MIN_MAX_DEVIATION);
        if(config.grab(pminMin)) {
          this.minMin = pminMin.getValue();
        }
        final DoubleParameter pmaxMin = new DoubleParameter(Parameterizer.MAX_MIN_ID, UncertainObject.DEFAULT_MIN_MAX_DEVIATION);
        if(config.grab(pmaxMin)) {
          this.maxMin = pmaxMin.getValue();
        }
        final DoubleParameter pminMax = new DoubleParameter(Parameterizer.MIN_MAX_ID, UncertainObject.DEFAULT_MIN_MAX_DEVIATION);
        if(config.grab(pminMax)) {
          this.minMax = pminMax.getValue();
        }
        final DoubleParameter pmaxMax = new DoubleParameter(Parameterizer.MAX_MAX_ID, UncertainObject.DEFAULT_MIN_MAX_DEVIATION);
        if(config.grab(pmaxMax)) {
          this.maxMax = pmaxMax.getValue();
        }
        final IntParameter pmultMin = new IntParameter(Parameterizer.MULT_MIN_ID, UncertainObject.DEFAULT_SAMPLE_SIZE);
        if(config.grab(pmultMin)) {
          this.multMin = pmultMin.getValue();
        }
        final IntParameter pmultMax = new IntParameter(Parameterizer.MULT_MAX_ID, UncertainObject.DEFAULT_SAMPLE_SIZE);
        if(config.grab(pmultMax)) {
          this.multMax = pmultMax.getValue();
        }
        final RandomParameter pseed = new RandomParameter(Parameterizer.SEED_ID);
        if(config.grab(pseed)) {
          this.randFac = pseed.getValue();
        }
        final DoubleParameter maxProb = new DoubleParameter(Parameterizer.MAXIMUM_PROBABILITY_ID, UncertainObject.DEFAULT_MAX_TOTAL_PROBABILITY);
        if(config.grab(maxProb)) {
          this.maxTotalProb = maxProb.getValue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(minMin, maxMin, minMax, maxMax, multMin, multMax, randFac);
      }
    }
  }
}
