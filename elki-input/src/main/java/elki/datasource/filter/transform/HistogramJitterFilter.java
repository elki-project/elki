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
package elki.datasource.filter.transform;

import java.util.Random;

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.datasource.filter.AbstractVectorStreamConversionFilter;
import elki.math.statistics.distribution.Distribution;
import elki.math.statistics.distribution.ExponentialDistribution;
import elki.utilities.documentation.Description;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Add Jitter, preserving the histogram properties (same sum, nonnegative).
 * <p>
 * For each vector, the total sum of all dimensions is computed.<br>
 * Then a random vector of the average length <code>jitter * scale</code> is
 * added and the result normalized to the original vectors sum. The individual
 * dimensions are drawn from an exponential distribution with scale
 * <code>jitter / dimensionality</code>, so it is expected that the error in
 * most dimensions will be low, and higher in few.
 * <p>
 * This is designed to degrade the quality of a histogram, while preserving the
 * total sum (e.g. to keep the normalization). The factor "jitter" can be used
 * to control the degradation amount.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @param <V> Vector type
 */
@Description("Add uniform Jitter to a dataset, while preserving the total vector sum.")
public class HistogramJitterFilter<V extends NumberVector> extends AbstractVectorStreamConversionFilter<V, V> {
  /**
   * Jitter amount.
   */
  double jitter;

  /**
   * Random generator.
   */
  Distribution dist;

  /**
   * Random generator.
   */
  Random rnd;

  /**
   * Constructor.
   * 
   * @param jitter Relative amount of jitter to add
   * @param rnd Random generator
   */
  public HistogramJitterFilter(double jitter, RandomFactory rnd) {
    super();
    this.jitter = jitter;
    this.dist = new ExponentialDistribution(1);
    this.rnd = rnd.getSingleThreadedRandom();
  }

  @Override
  protected V filterSingleObject(V obj) {
    final int dim = obj.getDimensionality();
    // Compute the total sum.
    double osum = 0;
    for(int i = 0; i < dim; i++) {
      osum += obj.doubleValue(i);
    }
    // Actual maximum jitter amount:
    final double maxjitter = 2 * jitter / dim * osum;
    // Generate jitter vector
    double[] raw = new double[dim];
    double jsum = 0; // Sum of jitter
    for(int i = 0; i < raw.length; i++) {
      raw[i] = dist.nextRandom(rnd) * maxjitter;
      jsum += raw[i];
    }
    final double mix = jsum / osum;
    // Combine the two vector
    for(int i = 0; i < raw.length; i++) {
      raw[i] = raw[i] + (1 - mix) * obj.doubleValue(i);
    }
    return factory.newNumberVector(raw);
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
  }

  @Override
  protected SimpleTypeInformation<V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return in;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Option ID for the jitter strength.
     */
    public static final OptionID JITTER_ID = new OptionID("jitter.amount", "Jitter amount relative to data.");

    /**
     * Option ID for the jitter random seed.
     */
    public static final OptionID SEED_ID = new OptionID("jitter.seed", "Jitter random seed.");

    /**
     * Jitter amount.
     */
    double jitter = 0.1;

    /**
     * Random generator seed.
     */
    RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(JITTER_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> jitter = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public HistogramJitterFilter<DoubleVector> make() {
      return new HistogramJitterFilter<>(jitter, rnd);
    }
  }
}
