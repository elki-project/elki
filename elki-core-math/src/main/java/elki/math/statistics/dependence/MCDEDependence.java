/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import elki.math.statistics.dependence.mcde.MCDETest;
import elki.math.statistics.dependence.mcde.MWPTest;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Implementation of bivariate Monte Carlo Density Estimation as described in
 * <p>
 * This is an abstract class. In order to use MCDE extend it and implement an
 * appropriate statistical test that returns a p-value and index structure for
 * efficient computation of the statistical test.
 * <p>
 * The instantiation of MCDE based on the Mann-Whitney U test is called
 * {@link MWPTest} (as described in the paper).
 * <p>
 * Reference:
 * <p>
 * E. Fouché and K. Böhm<br>
 * Monte Carlo Density Estimation<br>
 * Proc. Scientific and Statistical Database Management (SSDBM 2019)
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 * @since 0.8.0
 */
@Reference(authors = "E. Fouché, K. Böhm", //
    title = "Monte Carlo Density Estimation", //
    booktitle = "Proc. Scientific and Statistical Database Management (SSDBM 2019)", //
    url = "https://doi.org/10.1145/3335783.3335795", //
    bibkey = "DBLP:conf/ssdbm/FoucheB19")
public class MCDEDependence implements Dependence {
  /**
   * Monte-Carlo iterations.
   */
  protected int m = 50;

  /**
   * Expected share of instances in slice (independent dimensions).
   */
  protected double alpha = 0.5;

  /**
   * Parameter that specifies the size of the marginal restriction.
   * Note that in the original paper alpha = beta and as such there is no
   * explicit distinction between the parameters.
   */
  protected double beta = 0.5;

  /**
   * Random generator.
   */
  protected RandomFactory rnd;

  /**
   * Statistical Test returning p-value tailored to MCDE Framework.
   */
  protected MCDETest<MCDETest.RankStruct> mcdeTest;

  /**
   * Constructor.
   */
  @SuppressWarnings("unchecked")
  public MCDEDependence(int m, double alpha, double beta, RandomFactory rnd, MCDETest<?> mcdeTest) {
    this.m = m;
    this.alpha = alpha;
    this.beta = beta;
    this.rnd = rnd;
    this.mcdeTest = (MCDETest<MCDETest.RankStruct>) mcdeTest;
  }

  /**
   * Bivariate data slicing
   *
   * @param random Random generator
   * @param nonRefIndex Index (see correctedRank()) computed for the dimension
   *        that is not the reference dimension
   * @return Array of booleans that states which instances are part of the slice
   */
  protected boolean[] randomSlice(Random random, MCDETest.RankStruct nonRefIndex) {
    int len = nonRefIndex.index.length;
    boolean slice[] = new boolean[len];
    // According to the actual formula it should be
    // Math.ceil(Math.pow(this.alpha, 1.0) * len).
    // The exponent in the multivariate case should be the no. of dimensions - 1
    // which in the bivariate case is always simply 1.
    final int sliceSize = (int) Math.ceil(this.alpha * len);
    final int start = random.nextInt(len - sliceSize);
    for(int j = start, end = start + sliceSize; j < end; j++) {
      slice[nonRefIndex.index[j]] = true;
    }
    return slice;
  }

  /**
   * Multivariate data slicing
   *
   * @param random Random generator
   * @param nonRefIndex Array of indices computed for each dimension
   * @param refDim Indexvalue of reference dimension
   * @param nDim No of dimensions
   * @return Array of booleans that states which instances are part of the slice
   */
  protected boolean[] randomSlice(Random random, MCDETest.RankStruct[] nonRefIndex, int refDim, int nDim) {
    int len = nonRefIndex[0].index.length;
    boolean slice[] = new boolean[len];
    Arrays.fill(slice, true);
    // alpha is between 0.1 and 0.9
    final int sliceSize = (int) Math.ceil(Math.pow(this.alpha, 1.0 / (double) (nDim - 1)) * len);

    for(int i = 0; i < nDim; i++) {
      if(i != refDim) {
        final int[] idx = nonRefIndex[i].index;
        final int start = random.nextInt(len - sliceSize);
        for(int j = 0; j < start; j++) {
          slice[idx[j]] = false;
        }
        for(int j = start + sliceSize; j < len; j++) {
          slice[idx[j]] = false;
        }
      }
    }
    return slice;
  }

  @Override
  public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2) {
    final int len = Dependence.Utils.size(adapter1, data1, adapter2, data2);
    // Note: Corresponds to Algorithm 4 in source paper.
    MCDETest.RankStruct i1 = mcdeTest.correctedRanks(adapter1, data1, len);
    MCDETest.RankStruct i2 = mcdeTest.correctedRanks(adapter2, data2, len);
    final Random random = rnd.getSingleThreadedRandom();
    double mwp = 0;
    for(int i = 0; i < m; i++) {
      final boolean flip = random.nextInt(2) == 1;
      final int width = (int) Math.ceil(len * this.beta);
      final int start = random.nextInt(len - width);

      boolean[] slice = randomSlice(random, flip ? i1 : i2);
      mwp += mcdeTest.statisticalTest(start, width, slice, flip ? i2 : i1);
    }
    return mwp / m;
  }

  @Override
  public <A> double[] dependence(NumberArrayAdapter<?, A> adapter, List<? extends A> data) {
    final int dims = data.size();
    final int len = Dependence.Utils.size(adapter, data);
    // Build indexes:
    MCDETest.RankStruct[] idx = new MCDETest.RankStruct[dims];
    for(int i = 0; i < dims; i++) {
      A d = data.get(i);
      if(adapter.size(d) != len) {
        throw new ArrayIndexOutOfBoundsException("Arrays must have the same size");
      }
      idx[i] = mcdeTest.correctedRanks(adapter, d, len);
    }

    final Random random = rnd.getSingleThreadedRandom();
    double[] out = new double[(dims * (dims - 1)) >> 1];
    int o = 0;
    for(int y = 1; y < dims; y++) {
      MCDETest.RankStruct iy = idx[y];
      for(int x = 0; x < y; x++) {
        double mwp = 0;
        for(int i = 0; i < m; i++) {
          final boolean flip = random.nextInt(2) == 1;
          final int width = (int) Math.ceil(len * this.beta);
          final int start = random.nextInt(len - width);

          boolean[] slice = randomSlice(random, flip ? iy : idx[x]);
          mwp += mcdeTest.statisticalTest(start, width, slice, flip ? idx[x] : iy);
        }
        out[o++] = mwp / m;
      }
    }
    return out;
  }

  /**
   * Runs MCDE Algorithm with possibly more than two dimensions
   *
   * @param adapter Array type adapter
   * @param data Data sets. Must have fast random access!
   * @param <A> Array type
   * @return Dependence Measure
   */
  public <A> double higherOrderDependence(NumberArrayAdapter<?, A> adapter, List<? extends A> data) {
    final int dims = data.size();
    final int len = Dependence.Utils.size(adapter, data);
    // Build indexes:
    MCDETest.RankStruct[] idx = new MCDETest.RankStruct[dims];
    for(int i = 0; i < dims; i++) {
      idx[i] = mcdeTest.correctedRanks(adapter, data.get(i), len);
    }

    final Random random = rnd.getSingleThreadedRandom();
    double mwp = 0;
    for(int i = 0; i < m; i++) {
      final int refDim = random.nextInt(dims);
      final int width = (int) Math.ceil(len * this.beta);
      final int start = random.nextInt(len - width);

      boolean[] slice = randomSlice(random, idx, refDim, dims);
      mwp += mcdeTest.statisticalTest(start, width, slice, idx[refDim]);
    }
    return mwp / m;
  }

  /**
   * Parameterizer
   *
   * @author Alan Mazankiewicz
   * @author Edouard Fouché
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter that specifies the number of iterations in the Monte-Carlo
     * process of identifying high contrast subspaces.
     */
    public static final OptionID M_ID = new OptionID("mcde.m", "Number of Monte-Carlo iterations.");

    /**
     * Parameter that specifies the size of the slice
     */
    public static final OptionID ALPHA_ID = new OptionID("mcde.alpha", "Expected share of instances in slice (independent dimensions).");

    /**
     * Parameter that specifies the size of the marginal restriction.
     * Note that in the original paper alpha = beta and as such there is no
     * explicit distinction between the parameters.
     */
    public static final OptionID BETA_ID = new OptionID("mcde.beta", "Expected share of instances in marginal restriction (dependent dimensions).");

    /**
     * Parameter that specifies the random seed.
     */
    public static final OptionID SEED_ID = new OptionID("mcde.seed", "The random seed.");

    /**
     * Parameter that specifies which MCDE statistical test to use in order to
     * calculate the contrast between the instances in the slice vs out of
     * slice.
     */
    public static final OptionID TEST_ID = new OptionID("mcde.test", "The mcde statistical test that is used to calculate the deviation of two data samples");

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
    private MCDETest<?> mcdeTest;

    /**
     * Random generator.
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(M_ID, 50) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> m = x);
      new DoubleParameter(ALPHA_ID, 0.5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> alpha = x);
      new DoubleParameter(BETA_ID, alpha) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> beta = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
      new ObjectParameter<MCDETest<?>>(TEST_ID, MCDETest.class, MWPTest.class) //
          .grab(config, x -> mcdeTest = x);
    }

    @Override
    public MCDEDependence make() {
      return new MCDEDependence(m, alpha, beta, rnd, mcdeTest);
    }
  }
}
