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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.outlier.meta.HiCS;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

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
public class HiCSDependenceMeasure extends AbstractDependenceMeasure {
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
  public HiCSDependenceMeasure(GoodnessOfFitTest statTest, int m, double alpha, RandomFactory rnd) {
    super();
    this.statTest = statTest;
    this.m = m;
    this.alphasqrt = Math.sqrt(alpha);
    this.rnd = rnd;
  }

  @Override
  public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2) {
    final int len = size(adapter1, data1, adapter2, data2);
    final int windowsize = (int) (len * alphasqrt);
    final Random random = rnd.getSingleThreadedRandom();

    // Sorted copies for slicing.
    int[] s1 = MathUtil.sequence(0, len), s2 = MathUtil.sequence(0, len);
    IntegerArrayQuickSort.sort(s1, (x, y) -> Double.compare(adapter1.getDouble(data1, x), adapter1.getDouble(data1, y)));
    IntegerArrayQuickSort.sort(s2, (x, y) -> Double.compare(adapter2.getDouble(data2, x), adapter2.getDouble(data2, y)));

    // Distributions for testing
    double[] fullValues = new double[len];
    double[] sampleValues = new double[windowsize];
    double deviationSum = 0.;

    // For the first half, we use the first dimension as reference
    for(int i = 0; i < len; i++) {
      fullValues[i] = adapter1.getDouble(data1, i);
      if(fullValues[i] != fullValues[i]) {
        throw new AbortException("NaN values are not allowed by this implementation!");
      }
    }

    int half = m >> 1; // TODO: remove bias?
    for(int i = 0; i < half; ++i) {
      // Build the sample
      for(int j = random.nextInt(len - windowsize), k = 0; k < windowsize; ++k, ++j) {
        sampleValues[k] = adapter2.getDouble(data2, j);
      }
      double contrast = statTest.deviation(fullValues, sampleValues);
      if(Double.isNaN(contrast)) {
        --i; // Retry.
        continue;
      }
      deviationSum += contrast;
    }

    // For the second half, we use the second dimension as reference
    for(int i = 0; i < len; i++) {
      fullValues[i] = adapter2.getDouble(data2, i);
      if(fullValues[i] != fullValues[i]) {
        throw new AbortException("NaN values are not allowed by this implementation!");
      }
    }

    for(int i = half; i < m; ++i) {
      // Build the sample
      for(int j = random.nextInt(len - windowsize), k = 0; k < windowsize; ++k, ++j) {
        sampleValues[k] = adapter1.getDouble(data1, j);
      }
      double contrast = statTest.deviation(fullValues, sampleValues);
      if(Double.isNaN(contrast)) {
        --i; // Retry.
        continue;
      }
      deviationSum += contrast;
    }

    return 1 - deviationSum / m;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter mP = new IntParameter(HiCS.Parameterizer.M_ID, 50) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(mP)) {
        m = mP.intValue();
      }

      final DoubleParameter alphaP = new DoubleParameter(HiCS.Parameterizer.ALPHA_ID, 0.1) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      final ObjectParameter<GoodnessOfFitTest> testP = new ObjectParameter<>(HiCS.Parameterizer.TEST_ID, GoodnessOfFitTest.class, KolmogorovSmirnovTest.class);
      if(config.grab(testP)) {
        statTest = testP.instantiateClass(config);
      }

      final RandomParameter rndP = new RandomParameter(HiCS.Parameterizer.SEED_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected HiCSDependenceMeasure makeInstance() {
      return new HiCSDependenceMeasure(statTest, m, alpha, rnd);
    }
  }
}
