/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ExpGammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Simple parameter estimation for the ExpGamma distribution.
 * 
 * This is a very naive estimation, based on the mean and variance only,
 * sometimes referred to as the "Method of Moments" (MOM).
 * 
 * This estimator based on the {@link GammaMOMEstimator} and a simple exp data
 * transformation.
 *
 * @author Erich Schubert
 * @since 0.7.5
 * 
 * @navassoc - estimates - ExpGammaDistribution
 */
public class ExpGammaExpMOMEstimator implements DistributionEstimator<ExpGammaDistribution> {
  /**
   * Static estimation using just the mean and variance.
   */
  public static final ExpGammaExpMOMEstimator STATIC = new ExpGammaExpMOMEstimator();

  /**
   * Private constructor.
   */
  private ExpGammaExpMOMEstimator() {
    // Do not instantiate - use static class
  }

  @Override
  public <A> ExpGammaDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    MeanVariance mv = new MeanVariance();
    for(int i = 0; i < len; i++) {
      mv.put(FastMath.exp(adapter.getDouble(data, i)));
    }
    return estimateFromExpMeanVariance(mv);
  }

  public ExpGammaDistribution estimateFromExpMeanVariance(MeanVariance mv) {
    final double mu = mv.getMean();
    final double var = mv.getSampleVariance();
    if(mu < Double.MIN_NORMAL || var < Double.MIN_NORMAL) {
      throw new ArithmeticException("Cannot estimate Gamma parameters on a distribution with zero mean or variance: " + mv.toString());
    }
    final double theta = mu / var;
    final double k = mu * theta;
    if(!(k > 0.) || !(theta > 0.)) {
      throw new ArithmeticException("Gamma estimation produced non-positive parameter values: k=" + k + " theta=" + theta);
    }
    return new ExpGammaDistribution(k, theta, 0);
  }

  @Override
  public Class<? super ExpGammaDistribution> getDistributionClass() {
    return ExpGammaDistribution.class;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ExpGammaExpMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
