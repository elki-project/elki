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
package elki.math.statistics.dependence.mcde;

import elki.math.MathUtil;
import elki.math.statistics.dependence.Dependence;
import elki.math.statistics.dependence.MCDEDependence;
import elki.math.statistics.distribution.NormalDistribution;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Implementation of Mann-Whitney U test returning the p-value (not the test
 * statistic, thus MWP) for {@link MCDEDependence}. Implements algorithm 1 and 3
 * of the reference paper.
 * <p>
 * Reference:
 * <p>
 * E. Fouché and K. Böhm<br>
 * Monte Carlo Density Estimation<br>
 * Proc. Scientific and Statistical Database Management (SSDBM 2019)
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */
@Reference(authors = "E. Fouché, K. Böhm", //
    title = "Monte Carlo Density Estimation", //
    booktitle = "Proc. Scientific and Statistical Database Management (SSDBM 2019)", //
    url = "https://doi.org/10.1145/3335783.3335795", //
    bibkey = "DBLP:conf/ssdbm/FoucheB19")
public class MWPTest implements MCDETest<MWPTest.MWPRanking> {
  /**
   * Static Constructor - use this one
   */
  public static final MWPTest STATIC = new MWPTest();

  /**
   * Random generator.
   */
  protected RandomFactory rnd;

  /**
   * Structure to hold values needed for computing MWP in MCDEDependeneMeasure.
   * Values get computed during index creation.
   *
   * @author Alan Mazankiewicz
   * @author Edouard Fouché
   */
  public static class MWPRanking extends MCDETest.RankStruct {
    /**
     * Correction values
     */
    protected long[] correction;

    /**
     * Adjusted ranks
     */
    protected double[] adjusted;

    /**
     * Constructor.
     *
     * @param idx Index
     */
    public MWPRanking(int[] idx) {
      super(idx);
      this.correction = new long[idx.length];
      this.adjusted = new double[idx.length];
    }
  }

  /**
   * Private Constructor - use {@link #STATIC}
   */
  private MWPTest() {
    super();
  }

  @Override
  public <A> MWPRanking correctedRanks(NumberArrayAdapter<?, A> adapter, A data, int len) {
    int[] idx = Dependence.Util.sortedIndex(adapter, data, len);
    // Note: As described in Algorithm 1 of reference paper.
    MWPRanking I = new MWPRanking(idx);
    long correction = 0;
    for(int j = 0; j < len;) {
      final double v = adapter.getDouble(data, idx[j]);
      int k = j, t = 1, adjust = 0;
      while(k < len - 1 && adapter.getDouble(data, idx[k + 1]) == v) {
        adjust += k;
        k++;
        t++;
      }

      if(k > j) {
        double adjusted = (adjust + k) / (double) t;
        correction += t * (t * t - 1);
        if(correction < 0) {
          throw new ArithmeticException("Long overflow: too many ties (>2^10)");
        }
        for(int m = j; m <= k; m++) {
          I.adjusted[m] = adjusted;
          I.correction[m] = correction;
        }
      }
      else {
        I.adjusted[j] = j;
        I.correction[j] = correction;
      }
      j += t;
    }
    return I;
  }

  /**
   * Efficient implementation of MWP statistical test using appropriate index
   * structure as described in Algorithm 3 of reference paper. Tailored to MCDE
   * Framework.
   *
   * @param start Starting index value for statistical test
   * @param end End index value for statistical test
   * @param slice Return value of randomSlice() created with the index that is
   *        not for the reference dimension
   * @param corrected_ranks Index of the reference dimension, return value of
   *        corrected_ranks() computed for reference dimension
   * @return p-value from two sided Mann-Whitney-U test
   */
  public double statisticalTest(int start, int end, boolean[] slice, MWPRanking corrected_ranks) {
    double R = 0.0;
    int n1 = 0;
    for(int j = start; j < end; j++) {
      if(slice[corrected_ranks.index[j]]) {
        R += corrected_ranks.adjusted[j];
        n1++;
      }
    }

    // Cancel the offset in case the marginal restriction does not start from 0
    // see "acc-(cutStart*count)" in reference implementation of MWP
    R -= start * n1;

    final int cutLength = end - start;
    if(n1 == 0 || n1 == cutLength) {
      return 1;
    }

    final double U = R - 0.5 * n1 * (n1 - 1);
    final int n2 = cutLength - n1;
    final long b_end = corrected_ranks.correction[end - 1];
    final long b_start = start == 0 ? 0 : corrected_ranks.correction[start - 1];
    final double correction = (double) (b_end - b_start) / (cutLength * (cutLength - 1));
    final double std = FastMath.sqrt((((double) (n1 * n2)) / 12) * (cutLength + 1 - correction));
    final double Z = std > 0 ? Math.abs((U - (0.5 * n1 * n2)) / std) : 0.;
    // Note that this is equivalent to do 1-2*(1-cdf(Z,0,1));
    return NormalDistribution.erf(Z * MathUtil.SQRTHALF);
    // erf(Z / Math.sqrt(2)) is the cdf of the half-normal distribution
  }

  /**
   * Parameterizer, returning the static instance.
   * 
   * @author Erich Schubert
   */
  public static final class Par implements Parameterizer {
    @Override
    public MWPTest make() {
      return STATIC;
    }
  }
}
