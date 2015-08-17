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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Weighted version of discrete uncertain objects.
 *
 * <ul>
 * <li>Every object is represented by a finite number of discrete samples.</li>
 * <li>Every sample has a weight associated with it.</li>
 * <li>Samples with higher weight are more likely to be returned by
 * {@link #drawSample}.
 * </ul>
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
public class WeightedDiscreteUncertainObject extends AbstractDiscreteUncertainObject {
  /**
   * Samples
   */
  private DoubleVector[] samples;

  /**
   * Sample weights
   */
  private double[] weights;

  /**
   * Total sum of weights.
   */
  private double weightSum;

  // Constructor
  public WeightedDiscreteUncertainObject(DoubleVector[] samples, double[] weights) {
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

  /**
   * Factory to produce uncertain vectors.
   *
   * @author Alexander Koos
   * @author Erich Schubert
   */
  public static class Factory extends AbstractDiscreteUncertainObject.Factory<WeightedDiscreteUncertainObject> {
    /**
     * Minimum and maximum deviation.
     */
    private double minDev, maxDev;

    /**
     * Only generate symmetric distributions.
     */
    boolean symmetric;

    /**
     * Constructor.
     *
     * @param minDev Minimum deviation
     * @param maxDev Maximum deviation
     * @param minQuant Minimum number of samples
     * @param maxQuant Maximum number of samples
     * @param symmetric Generate symmetric distributions only
     * @param rand Random generator
     */
    public Factory(double minDev, double maxDev, int minQuant, int maxQuant, boolean symmetric, RandomFactory rand) {
      super(minQuant, maxQuant, rand);
      this.minDev = minDev;
      this.maxDev = maxDev;
      this.symmetric = symmetric;
    }

    @Override
    public <A> WeightedDiscreteUncertainObject newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
      final int dim = adapter.size(array);
      final int distributionSize = rand.nextInt((maxQuant - minQuant) + 1) + (int) minQuant;
      DoubleVector[] samples = new DoubleVector[distributionSize];
      double[] offrange = generateRandomRange(dim, minDev, maxDev, symmetric, rand);
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
      return new WeightedDiscreteUncertainObject(samples, weights);
    }

    @Override
    public Class<? super WeightedDiscreteUncertainObject> getRestrictionClass() {
      return WeightedDiscreteUncertainObject.class;
    }

    @Override
    public ByteBufferSerializer<WeightedDiscreteUncertainObject> getDefaultSerializer() {
      return null; // TODO: not yet available
    }

    /**
     * Parameterization class.
     *
     * @author Alexander Koos
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractDiscreteUncertainObject.Factory.Parameterizer {
      protected double minDev, maxDev;

      protected boolean symmetric;

      @Override
      protected void makeOptions(final Parameterization config) {
        super.makeOptions(config);
        DoubleParameter pmaxMin = new DoubleParameter(DEV_MAX_ID);
        if(config.grab(pmaxMin)) {
          maxDev = pmaxMin.doubleValue();
        }
        DoubleParameter pminMin = new DoubleParameter(DEV_MIN_ID, 0.);
        if(config.grab(pminMin)) {
          minDev = pminMin.doubleValue();
        }
        Flag symmetricF = new Flag(SYMMETRIC_ID);
        if(config.grab(symmetricF)) {
          symmetric = symmetricF.isTrue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(minDev, maxDev, minQuant, maxQuant, symmetric, randFac);
      }
    }
  }
}
