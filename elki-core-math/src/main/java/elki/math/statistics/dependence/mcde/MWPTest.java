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
package elki.math.statistics.dependence.mcde;

import elki.math.MathUtil;
import elki.math.statistics.dependence.Dependence;
import elki.math.statistics.dependence.MCDEDependence;
import elki.math.statistics.distribution.NormalDistribution;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.random.RandomFactory;

/**
 * Implementation of Mann-Whitney U test returning the p-value (not the test
 * statistic, thus MWP) for {@link MCDEDependence}. Implements algorithm 1 and 3
 * of reference paper.
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
   * Structure to hold values needed for computing MWP in MCDEDependene.
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
    // Note: As described in Algorithm 1 of reference paper.
    int[] idx = Dependence.Utils.sortedIndex(adapter, data, len);
    MWPRanking ranking = new MWPRanking(idx);
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
        correction += t * (t * (long) t - 1);
        if(correction < 0) {
          throw new ArithmeticException("Long overflow: too many ties (>2^10)");
        }
        for(int m = j; m <= k; m++) {
          ranking.adjusted[m] = adjusted;
          ranking.correction[m] = correction;
        }
      }
      else {
        ranking.adjusted[j] = j;
        ranking.correction[j] = correction;
      }
      j += t;
    }
    return ranking;
  }

  /**
   * Efficient implementation of MWP statistical test using appropriate index
   * structure as described in Algorithm 3 of reference paper. Tailored to MCDE
   * Framework.
   *
   * @param start Starting index value for statistical test
   * @param width Width of the slice (endindex = start + width)
   * @param slice Return value of randomSlice() created with the index that is
   *        not for the reference dimension
   * @param corrected_ranks Index of the reference dimension, return value of
   *        correctedRanks() computed for reference dimension
   * @return p-value from two sided Mann-Whitney-U test
   */
  @Override
  public double statisticalTest(int start, int width, boolean[] slice, MWPRanking corrected_ranks) {
    final int safeStart = getSafeCut(start, corrected_ranks.adjusted);
    final int len = corrected_ranks.index.length;
    final int sliceEndSearchStart = safeStart + width > len - 1 ? len - 1 : safeStart + width;
    final int safeEnd = getSafeCut(sliceEndSearchStart, corrected_ranks.adjusted);

    double R = 0.0;
    int n1 = 0;
    for(int j = safeStart; j < safeEnd; j++) {
      if(slice[corrected_ranks.index[j]]) {
        R += corrected_ranks.adjusted[j];
        n1++;
      }
    }

    // This is to cancel the offset in case the marginal restriction does not
    // start from 0 see "acc-(cutStart*count)" is reference implementation of
    // MWP
    R -= safeStart * n1;

    final int cutLength = safeEnd - safeStart;
    if(n1 == 0 || n1 == cutLength) {
      return 1;
    }

    final double U = R - 0.5 * n1 * (n1 - 1);
    final int n2 = cutLength - n1;
    final long b_end = corrected_ranks.correction[safeEnd - 1];
    final long b_start = safeStart == 0 ? 0 : corrected_ranks.correction[safeStart - 1];
    final double correction = (double) (b_end - b_start) / (cutLength * (cutLength - 1));
    final double std = Math.sqrt((((double) (n1 * n2)) / 12) * (cutLength + 1 - correction));
    final double Z = std > 0 ? Math.abs((U - (0.5 * n1 * n2)) / std) : 0.;
    // Note that this is equivalent to do 1-2*(1-cdf(Z,0,1));
    return NormalDistribution.erf(Z * MathUtil.SQRTHALF);
    // erf(Z / Math.sqrt(2)) is the cdf of the half-normal distribution
  }

  /**
   * Adjusts idx so that it lies not in the middle of a tie by searching the
   * ranks index
   *
   * @param idx starting index. In MCDE MWP the start and end of a slice
   * @param ref adjusted ranks
   * @return adjusted idx
   */
  protected int getSafeCut(int idx, double[] ref) {
    for(int i = idx, j = idx, e = ref.length - 1;; i--, j++) {
      if(i == 0 || ref[i] != ref[i - 1]) {
        return i;
      }
      if(j == e || ref[j] != ref[j + 1]) {
        return j;
      }
    }
  }

  /**
   * Parameterizer, returning the static instance.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public MWPTest make() {
      return STATIC;
    }
  }
}
