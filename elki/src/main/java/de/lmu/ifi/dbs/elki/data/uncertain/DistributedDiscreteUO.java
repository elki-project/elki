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

public class DistributedDiscreteUO extends UncertainObject {
  private DoubleVector[] samples;

  private double[] weights;

  private double weightSum;

  // Constructor
  public DistributedDiscreteUO(DoubleVector[] samplePoints, double[] weights) {
    double check = 0;
    for(double weight : weights) {
      if(!(weight > 0 && weight < Double.POSITIVE_INFINITY)) {
        throw new IllegalArgumentException("Probabilities must be in positive and finite.");
      }
      check += weight;
    }

    // User of this class should think of a way to handle possible exception at
    // this point
    // to not find their program crashing without need.
    // To avoid misunderstanding one could compile a ".*total of 1\.$"-like
    // pattern against
    // raised IllegalArgumentExceptions and thereby customize his handle for
    // this case.
    if(check > UncertainObject.PROBABILITY_SCALE) {
      throw new IllegalArgumentException("The sum of probabilities exceeded a total of 1.");
    }
    this.samples = samplePoints;
    this.weights = weights;
    this.weightSum = check;

    int dimensions = samplePoints[0].getDimensionality();
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
  public Double getValue(int dimension) {
    // Not particularly meaningful, but currently unused anyway.
    return (bounds.getMax(dimension) + bounds.getMin(dimension)) * .5;
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    double r = rand.nextDouble() * weightSum;
    int index = weights.length;
    while(--index > 0 && r < weights[index]) {
      r -= weights[index];
    }
    return samples[index];
  }

  @Override
  public DoubleVector getMean() {
    int dimensions = getDimensionality();
    double[] meanVals = new double[dimensions];
    for(int i = 0; i < samples.length; i++) {
      DoubleVector v = samples[i];
      for(int d = 0; d < dimensions; d++) {
        meanVals[d] += v.doubleValue(d) * weights[i];
      }
    }

    for(int i = 0; i < dimensions; i++) {
      meanVals[i] /= weightSum;
    }

    return new DoubleVector(meanVals);
  }

  protected void setBounds() {
    int dimensions = getDimensionality();
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
    this.bounds = new HyperBoundingBox(min, max);
  }

  public static class Factory extends UncertainObject.Factory<DistributedDiscreteUO> {
    private double minMin, maxMin, minMax, maxMax;

    private int multMin, multMax;

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
    public Factory(double minMin, double maxMin, double minMax, double maxMax, int multMin, int multMax, RandomFactory randFac) {
      this.minMin = minMin;
      this.maxMin = maxMin;
      this.minMax = minMax;
      this.maxMax = maxMax;
      this.multMin = multMin;
      this.multMax = multMax;
      this.rand = randFac.getRandom();
    }

    @Override
    public ByteBufferSerializer<DistributedDiscreteUO> getDefaultSerializer() {
      return null; // TODO: not yet available
    }

    @Override
    public Class<? super DistributedDiscreteUO> getRestrictionClass() {
      return DistributedDiscreteUO.class;
    }

    @Override
    public <A> DistributedDiscreteUO newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
      final int dim = adapter.size(array);
      final int distributionSize = rand.nextInt((int) (multMax - multMin) + 1) + (int) multMin;
      DoubleVector[] samples = new DoubleVector[distributionSize];
      final double difMin = rand.nextDouble() * (maxMin - minMin) + minMin;
      final double difMax = rand.nextDouble() * (maxMax - minMax) + minMax;
      final double randDev = blur ? (rand.nextInt(2) == 0 ? rand.nextDouble() * -difMin : rand.nextDouble() * difMax) : 0;
      double[] weights = new double[distributionSize];
      double[] buf = new double[dim];
      for(int i = 0; i < distributionSize; i++) {
        weights[distributionSize] = rand.nextDouble(); // FIXME: could be 0.
        for(int j = 0; j < dim; j++) {
          final double gtv = adapter.getDouble(array, j);
          buf[j] = gtv + rand.nextDouble() * difMax - rand.nextDouble() * difMin + randDev;
        }
        samples[i] = new DoubleVector(buf);
      }
      return new DistributedDiscreteUO(samples, weights);
    }

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
        DoubleParameter pminMin = new DoubleParameter(Parameterizer.MIN_MIN_ID);
        if(config.grab(pminMin)) {
          minMin = pminMin.doubleValue();
        }
        DoubleParameter pmaxMin = new DoubleParameter(Parameterizer.MAX_MIN_ID);
        if(config.grab(pmaxMin)) {
          maxMin = pmaxMin.doubleValue();
        }
        DoubleParameter pminMax = new DoubleParameter(Parameterizer.MIN_MAX_ID);
        if(config.grab(pminMax)) {
          minMax = pminMax.doubleValue();
        }
        DoubleParameter pmaxMax = new DoubleParameter(Parameterizer.MAX_MAX_ID);
        if(config.grab(pmaxMax)) {
          maxMax = pmaxMax.doubleValue();
        }
        IntParameter pmultMin = new IntParameter(Parameterizer.MULT_MIN_ID, UncertainObject.DEFAULT_SAMPLE_SIZE);
        if(config.grab(pmultMin)) {
          multMin = pmultMin.intValue();
        }
        IntParameter pmultMax = new IntParameter(Parameterizer.MULT_MAX_ID, UncertainObject.DEFAULT_SAMPLE_SIZE);
        if(config.grab(pmultMax)) {
          multMax = pmultMax.intValue();
        }
        RandomParameter pseed = new RandomParameter(Parameterizer.SEED_ID);
        if(config.grab(pseed)) {
          randFac = pseed.getValue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(minMin, maxMin, minMax, maxMax, multMin, multMax, randFac);
      }
    }
  }
}
