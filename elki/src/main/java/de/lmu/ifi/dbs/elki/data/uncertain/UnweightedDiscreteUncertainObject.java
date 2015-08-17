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
 * Unweighted implementation of discrete uncertain objects.
 *
 * <ul>
 * <li>Every object is represented by a finite number of discrete samples.</li>
 * <li>Every sample has the same weight.</li>
 * <li>Every sample is equally likely to be returned by {@link #drawSample}.
 * </ul>
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
public class UnweightedDiscreteUncertainObject extends AbstractDiscreteUncertainObject {
  /**
   * Sample vectors.
   */
  private DoubleVector[] samples;

  /**
   * Constructor.
   *
   * @param samples Samples
   */
  public UnweightedDiscreteUncertainObject(DoubleVector[] samples) {
    super();
    if(samples.length == 0) {
      throw new AbortException("Discrete Uncertain Objects must have at least one point.");
    }
    this.samples = samples;
    this.bounds = computeBounds(samples);
  }

  @Override
  public DoubleVector drawSample(Random rand) {
    return samples[rand.nextInt(samples.length)];
  }

  @Override
  public DoubleVector getCenterOfMass() {
    final int dim = getDimensionality();
    // Unweighted average.
    double[] meanVals = new double[dim];
    for(int i = 0; i < samples.length; i++) {
      DoubleVector vals = samples[i];
      for(int d = 0; d < dim; d++) {
        meanVals[d] += vals.doubleValue(d);
      }
    }

    for(int d = 0; d < dim; d++) {
      meanVals[d] /= samples.length;
    }
    return new DoubleVector(meanVals);
  }

  /**
   * Factory class
   *
   * @author Alexander Koos
   * @author Erich Schubert
   */
  public static class Factory extends AbstractDiscreteUncertainObject.Factory<UnweightedDiscreteUncertainObject> {
    /**
     * Minimum and maximum deviations.
     */
    public double minDev, maxDev;

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
    public <A> UnweightedDiscreteUncertainObject newFeatureVector(A array, NumberArrayAdapter<?, A> adapter) {
      final int dim = adapter.size(array);
      final int distributionSize = rand.nextInt((maxQuant - minQuant) + 1) + (int) minQuant;
      DoubleVector[] samples = new DoubleVector[distributionSize];
      double[] offrange = generateRandomRange(dim, minDev, maxDev, symmetric, rand);
      // Produce samples:
      double[] buf = new double[dim];
      for(int i = 0; i < distributionSize; i++) {
        for(int j = 0, k = 0; j < dim; j++) {
          double gtv = adapter.getDouble(array, j);
          buf[j] = gtv + offrange[k++] + rand.nextDouble() * offrange[k++];
        }
        samples[i] = new DoubleVector(buf);
      }
      return new UnweightedDiscreteUncertainObject(samples);
    }

    @Override
    public Class<? super UnweightedDiscreteUncertainObject> getRestrictionClass() {
      return UnweightedDiscreteUncertainObject.class;
    }

    @Override
    public ByteBufferSerializer<UnweightedDiscreteUncertainObject> getDefaultSerializer() {
      return null; // TODO: Not yet available.
    }

    public static class Parameterizer extends AbstractDiscreteUncertainObject.Factory.Parameterizer {
      protected double minDev, maxDev;

      boolean symmetric;

      @Override
      protected void makeOptions(final Parameterization config) {
        super.makeOptions(config);
        DoubleParameter pmaxDev = new DoubleParameter(DEV_MAX_ID);
        if(config.grab(pmaxDev)) {
          maxDev = pmaxDev.doubleValue();
        }
        DoubleParameter pminDev = new DoubleParameter(DEV_MIN_ID, 0.);
        if(config.grab(pminDev)) {
          minDev = pminDev.doubleValue();
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
