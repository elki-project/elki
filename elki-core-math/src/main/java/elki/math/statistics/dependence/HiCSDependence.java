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

import java.util.Random;

import elki.math.statistics.tests.GoodnessOfFitTest;
import elki.math.statistics.tests.KolmogorovSmirnovTest;
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
 * Use the statistical tests as used by HiCS to measure dependence of variables.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek<br>
 * Interactive Data Mining with 3D-Parallel-Coordinate-Trees<br>
 * Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)
 * <p>
 * Based on:
 * <p>
 * F. Keller, E. Müller, K. Böhm<br>
 * HiCS: High Contrast Subspaces for Density-Based Outlier Ranking<br>
 * In ICDE, pages 1037–1048, 2012.
 *
 * @author Erich Schubert
 * @author Robert Rödler
 * @since 0.5.5
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
    title = "Interactive Data Mining with 3D-Parallel-Coordinate-Trees", //
    booktitle = "Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)", //
    url = "https://doi.org/10.1145/2463676.2463696", //
    bibkey = "DBLP:conf/sigmod/AchtertKSZ13")
@Reference(authors = "F. Keller, E. Müller, K. Böhm", //
    title = "HiCS: High Contrast Subspaces for Density-Based Outlier Ranking", //
    booktitle = "Proc. IEEE 28th Int. Conf. on Data Engineering (ICDE 2012)", //
    url = "https://doi.org/10.1109/ICDE.2012.88", //
    bibkey = "DBLP:conf/icde/KellerMB12")
public class HiCSDependence implements Dependence {
  /**
   * Monte-Carlo iterations
   */
  private int m = 50;

  /**
   * Alpha threshold
   */
  private double alphasqrt = Math.sqrt(0.1);

  /**
   * Statistical test to use
   */
  private GoodnessOfFitTest statTest;

  /**
   * Random generator
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param statTest Test function
   * @param m Number of monte-carlo iterations
   * @param alpha Alpha threshold
   * @param rnd Random source
   */
  public HiCSDependence(GoodnessOfFitTest statTest, int m, double alpha, RandomFactory rnd) {
    super();
    this.statTest = statTest;
    this.m = m;
    this.alphasqrt = Math.sqrt(alpha);
    this.rnd = rnd;
  }

  @Override
  public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2) {
    final int len = Utils.size(adapter1, data1, adapter2, data2);
    final int windowsize = (int) (len * alphasqrt);
    final Random random = rnd.getSingleThreadedRandom();

    // Sorted copies for slicing.
    int[] s1 = Utils.sortedIndex(adapter1, data1, len);
    int[] s2 = Utils.sortedIndex(adapter2, data2, len);

    // Distributions for testing
    double[] full = new double[len], sample = new double[windowsize];
    double deviationSum = 0.;

    // For the first half, we use the first dimension as reference
    final int half = m >> 1; // TODO: remove bias?
    for(int i = 0; i < len; i++) {
      full[i] = adapter1.getDouble(data1, i);
      if(full[i] != full[i]) {
        throw new ArithmeticException("NaN values are not allowed by this implementation!");
      }
    }

    int retries = 1000;
    for(int i = 0; i < half && retries > 0; ++i) {
      // Build the sample
      for(int j = random.nextInt(len - windowsize), k = 0; k < windowsize; ++k, ++j) {
        sample[k] = adapter2.getDouble(data2, s1[j]);
      }
      double contrast = statTest.deviation(full, sample);
      if(Double.isNaN(contrast)) {
        --i; // Retry.
        --retries;
        continue;
      }
      deviationSum += contrast;
    }

    // For the second half, we use the second dimension as reference
    for(int i = 0; i < len; i++) {
      full[i] = adapter2.getDouble(data2, i);
      if(full[i] != full[i]) {
        throw new ArithmeticException("NaN values are not allowed by this implementation!");
      }
    }

    for(int i = half; i < m && retries > 0; ++i) {
      // Build the sample
      for(int j = random.nextInt(len - windowsize), k = 0; k < windowsize; ++k, ++j) {
        sample[k] = adapter1.getDouble(data1, s2[j]);
      }
      double contrast = statTest.deviation(full, sample);
      if(Double.isNaN(contrast)) {
        --i; // Retry.
        --retries;
        continue;
      }
      deviationSum += contrast;
    }

    if(retries == 0) {
      throw new ArithmeticException("Too many NaNs during HiCSDependence");
    }
    return 1 - deviationSum / m;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter that specifies the number of iterations in the Monte-Carlo
     * process of identifying high contrast subspaces.
     */
    public static final OptionID M_ID = new OptionID("hics.m", "The number of iterations in the Monte-Carlo processing.");

    /**
     * Parameter that determines the size of the test statistic during the
     * Monte-Carlo iteration.
     */
    public static final OptionID ALPHA_ID = new OptionID("hics.alpha", "The discriminance value that determines the size of the test statistic .");

    /**
     * Parameter that specifies which statistical test to use in order to
     * calculate the deviation of two given data samples.
     */
    public static final OptionID TEST_ID = new OptionID("hics.test", "The statistical test that is used to calculate the deviation of two data samples");

    /**
     * Parameter that specifies the random seed.
     */
    public static final OptionID SEED_ID = new OptionID("hics.seed", "The random seed.");

    /**
     * Statistical test to use
     */
    private GoodnessOfFitTest statTest;

    /**
     * Monte-Carlo iterations
     */
    private int m = 50;

    /**
     * Alpha threshold
     */
    private double alpha = 0.1;

    /**
     * Random generator.
     */
    private RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(M_ID, 50) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> m = x);
      new DoubleParameter(ALPHA_ID, 0.1) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> alpha = x);
      new ObjectParameter<GoodnessOfFitTest>(TEST_ID, GoodnessOfFitTest.class, KolmogorovSmirnovTest.class) //
          .grab(config, x -> statTest = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public HiCSDependence make() {
      return new HiCSDependence(statTest, m, alpha, rnd);
    }
  }
}
