package de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier;
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

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector.Factory;
import de.lmu.ifi.dbs.elki.data.uncertain.WeightedDiscreteUncertainObject;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Factory to produce uncertain vectors.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
public class WeightedDiscreteUncertainifier extends AbstractDiscreteUncertainifier<WeightedDiscreteUncertainObject> {
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
  public WeightedDiscreteUncertainifier(double minDev, double maxDev, int minQuant, int maxQuant, boolean symmetric, RandomFactory rand) {
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
  public Factory<WeightedDiscreteUncertainObject, ?> getFactory() {
    return WeightedDiscreteUncertainObject.FACTORY;
  }

  /**
   * Parameterization class.
   *
   * @author Alexander Koos
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDiscreteUncertainifier.Parameterizer {
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
    protected WeightedDiscreteUncertainifier makeInstance() {
      return new WeightedDiscreteUncertainifier(minDev, maxDev, minQuant, maxQuant, symmetric, randFac);
    }
  }
}