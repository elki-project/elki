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
package elki.math.statistics.dependence;

import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;
import elki.math.statistics.tests.mcde.*;

import java.util.Arrays;
import java.util.Random;

/**
 * Implementation of bivariate Monte Carlo Density Estimation as described in
 * Edouard Fouché & Klemens Böhm<br>
 * Monte Carlo Density Estimation<br>
 * Proc. 2019 ACM Int. Conf. on Scientific and Statistical Database Management (SSDBM 2019)
 * <p>
 * This is an abstract class. In order to use MCDE extend it and implement an appropriate statistical test that
 * returns a p-value and index structure for efficient computation of the statistical test.
 * <p>
 * The instantiation of MCDE based on the Mann-Whitney U test is called MWP (as described in the paper).
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */

@Reference(authors = "Edouard Fouché, Klemens Böhm", //
    title = "Monte Carlo Density Estimation", //
    booktitle = "Proc. 2019 ACM Int. Conf. on Scientific and Statistical Database Management (SSDBM 2019)", url = "https://doi.org/10.1145/3335783.3335795", //
    bibkey = "DBLP:conf/ssdbm/FoucheB19")

public class MCDEDependenceMeasure
    extends AbstractDependenceMeasure {
  /**
   * Monte-Carlo iterations.
   */
  protected int m = 50;

  /**
   * Expected share of instances in slice (independent dimensions).
   */
  protected double alpha = 0.5;

  /**
   * Parameter that specifies the size of the marginal restriction. Note that in the original paper
   * alpha = beta and as such there is no explicit distinction between the parameters.
   */
  protected double beta = 0.5;

  /**
   * Random generator.
   */
  protected RandomFactory rnd;

  /**
   * Statistical Test returning p-value tailored to MCDE Framework.
   */
  protected MCDETest mcdeTest;

  /**
   * Constructor.
   */
  public MCDEDependenceMeasure(int m, double alpha, double beta, RandomFactory rnd, MCDETest mcdeTest) {
    this.m = m;
    this.alpha = alpha;
    this.beta = beta;
    this.rnd = rnd;
    this.mcdeTest = mcdeTest;
  }

  /**
   * Data Slicing
   *
   * @param len         No of data instances
   * @param nonRefIndex Index (see correctedRank()) computed for the dimension that is not the reference dimension
   * @return Array of booleans that states which instances are part of the slice
   */
  protected boolean[] randomSlice(int len, MCDETest.RankStruct[] nonRefIndex) {
    final Random random = rnd.getSingleThreadedRandom();
    boolean slice[] = new boolean[len];
    Arrays.fill(slice, true);

    // According to the actual formula it should be Math.ceil(Math.pow(this.alpha, 1.0) * len).
    // The exponent in the multivariate case should be the no. of dimensions - 1 which in the
    // bivariate case is always simply 1.
    final int slizeSize = (int) Math.ceil(this.alpha * len);
    final int start = random.nextInt(len - slizeSize);
    final int end = start + slizeSize;

    for(int j = 0; j < start; j++) {
      slice[nonRefIndex[j].index] = false;
    }

    for(int j = end; j < len; j++) {
      slice[ nonRefIndex[j].index] = false;
    }

    return slice;
  }

  /**
   * Implements dependence from DependenceMeasure superclass. Corresponds to Algorithm 4 in source paper.
   *
   * @param adapter1 First data adapter
   * @param data1    First data set
   * @param adapter2 Second data adapter
   * @param data2    Second data set
   * @param <A>      Numeric data type, such as double
   * @param <B>      Numeric data type, such as double
   * @return MCDE result
   */
  @Override public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2) {
    final Random random = rnd.getSingleThreadedRandom();
    final int len = adapter1.size(data1);

    if(len != adapter2.size(data2)) {
      throw new AbortException("Size of both arrays must match!");
    }

    final MCDETest.RankStruct[] index_0 = mcdeTest.corrected_ranks(adapter1, data1, len);
    final MCDETest.RankStruct[] index_1 = mcdeTest.corrected_ranks(adapter2, data2, len);

    double mwp = 0;

    for(int i = 0; i < this.m; i++) {
      int r = random.nextInt(2);
      MCDETest.RankStruct[] ref_index;
      MCDETest.RankStruct[] other_index;

      if(r == 1) {
        ref_index = index_1;
        other_index = index_0;
      }
      else {
        ref_index = index_0;
        other_index = index_1;
      }

      final int start = random.nextInt((int) (len * (1 - this.beta)));
      final int end = start + (int) Math.ceil(len * this.beta);
      mwp += mcdeTest.statistical_test(start,end, randomSlice(len, other_index), ref_index);
    }
    return mwp / m;
  }

  public static class Par implements Parameterizer {
    /**
     * Parameter that specifies the number of iterations in the Monte-Carlo
     * process of identifying high contrast subspaces.
     */
    public static final OptionID M_ID = new OptionID("MCDE.test", "No. of Monte-Carlo iterations.");

    /**
     * Parameter that specifies the size of the slice
     */
    public static final OptionID ALPHA_ID = new OptionID("MCDE.test", "Expected share of instances in slice (independent dimensions).");

    /**
     * Parameter that specifies the size of the marginal restriction. Note that in the original paper
     * alpha = beta and as such there is no explicit distinction between the parameters.
     */
    public static final OptionID BETA_ID = new OptionID("MCDE.test", "Expected share of instances in marginal restriction (dependent dimensions).");

    /**
     * Parameter that specifies the random seed.
     */
    public static final OptionID SEED_ID = new OptionID("MCDE.test", "The random seed.");

    /**
     * Parameter that specifies which mcde statistical test to use in order to
     * calculate the contrast between the instances in the slice vs out of slice.
     */
    public static final OptionID TEST_ID = new OptionID("MCDE.test", "The mcde statistical test that is used to calculate the deviation of two data samples");


    /**
     * Holds the value of {@link #M_ID}.
     */
    protected int m = 50;

    /**
     * Holds the value of {@link #ALPHA_ID}.
     */
    protected double alpha = 0.5;

    /**
     * Holds the value of {@link #BETA_ID} .
     */
    protected double beta = 0.5;

    /**
     * Holds the value of {@link #TEST_ID}.
     */
    private MCDETest mcdeTest;

    /**
     * Random generator.
     */
    protected RandomFactory rnd;

    @Override public void configure(Parameterization config) {
      new IntParameter(M_ID, 50) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT).grab(config, x -> m = x);

      new DoubleParameter(ALPHA_ID, 0.5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE).addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE).grab(config, x -> alpha = x);

      new DoubleParameter(BETA_ID, 0.5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE).addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE).grab(config, x -> beta = x);

      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);

      new ObjectParameter<MCDETest>(TEST_ID, MCDETest.class, MWPTest.class) //
          .grab(config, x -> mcdeTest = x);
    }

    @Override public MCDEDependenceMeasure make() {
      return new MCDEDependenceMeasure(m, alpha, beta, rnd, mcdeTest);
    }
  }
}
