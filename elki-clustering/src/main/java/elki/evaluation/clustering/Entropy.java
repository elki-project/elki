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
package elki.evaluation.clustering;

import static elki.math.statistics.distribution.GammaDistribution.logGamma;
import static net.jafama.FastMath.log;

import elki.logging.LoggingUtil;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Entropy based measures, implemented using natural logarithms.
 * <p>
 * Key References:
 * <p>
 * M. Meilă<br>
 * Comparing clusterings by the variation of information<br>
 * Learning theory and kernel machines
 * <p>
 * X. V. Nguyen, J. Epps, J. Bailey<br>
 * Information theoretic measures for clusterings comparison:
 * is a correction for chance necessary?<br>
 * Proc. 26th Ann. Int. Conf. on Machine Learning (ICML '09)
 *
 * @author Sascha Goldhofer
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "M. Meilă", //
    title = "Comparing clusterings by the variation of information", //
    booktitle = "Learning theory and kernel machines", //
    url = "https://doi.org/10.1007/978-3-540-45167-9_14", //
    bibkey = "DBLP:conf/colt/Meila03")
@Reference(authors = "X. V. Nguyen, J. Epps, J. Bailey", //
    title = "Information theoretic measures for clusterings comparison: is a correction for chance necessary?", //
    booktitle = "Proc. 26th Ann. Int. Conf. on Machine Learning (ICML '09)", //
    url = "https://doi.org/10.1145/1553374.1553511", //
    bibkey = "DBLP:conf/icml/NguyenEB09")
public class Entropy {
  /**
   * Entropy in first
   */
  protected double entropyFirst;

  /**
   * Entropy in second
   */
  protected double entropySecond;

  /**
   * Joint entropy
   */
  protected double entropyJoint;

  /**
   * Mutual information (computed directly)
   */
  protected double mutualInformation;

  /**
   * Variation of information (computed directly)
   */
  protected double variationOfInformation;

  /**
   * Expected mutual information
   */
  protected double expectedMutualInformation;

  /**
   * log(n), for bounding VI
   */
  protected double vibound;

  /**
   * Constructor.
   *
   * @param table Contingency table
   */
  protected Entropy(ClusterContingencyTable table) {
    super();
    final int r = table.size1, c = table.size2;
    final int[][] contingency = table.contingency;
    final int n = contingency[r][c];
    if(table.contingency[table.size1][table.size2 + 1] != n || table.contingency[table.size1 + 1][table.size2] != n) {
      LoggingUtil.warning("Entropy measure are not well defined for overlapping and incomplete clusterings. The number of elements are: " + table.contingency[table.size1][table.size2 + 1] + " != " + table.contingency[table.size1 + 1][table.size2] + " elements.");
    }
    final double byn = 1.0 / n;
    // -log(N) and log(fac(N))
    final double mlogn = -log(n), lfacN = logFac(n);
    // Maximum cluster size, and cluster sizes:
    final int m = maxClusterSize(contingency, r, c);
    // Precompute necessary logarithms and factorials:
    // -log(i), log(factorial(i)), log(factorial(N-i))
    double[] mlog = new double[m + 1];
    double[] lfac = new double[m + 1], lfacNm = new double[m + 1];
    for(int i = 0, Nmi = n; i <= m; i++, Nmi--) {
      mlog[i] = -log(i);
      lfac[i] = logFac(i);
      lfacNm[i] = Nmi > m ? logFac(Nmi) : lfac[Nmi];
    }

    final double entropyFirst = computeEntropyFirst(contingency, r, c, byn, mlogn, mlog);
    final double entropySecond = computeEntropySecond(contingency, r, c, byn, mlogn, mlog);
    final int[] lastrow = contingency[r];
    double entropyJoint = 0.0, mi = 0.0, vi = 0.0, emi = 0.0;
    for(int i = 0; i < r; i++) {
      final int[] rowi = contingency[i];
      final int ai = rowi[c];
      final double mlogain = mlog[ai] - mlogn; // -log(ai/N)=log(N/ai)
      final double lfacai = lfac[ai], lfacNmai = lfacNm[ai];
      for(int j = 0; j < c; j++) {
        final int bj = lastrow[j];
        final int vij = rowi[j];
        final double mlogbjn = mlog[bj] - mlogn; // -log(bj/N)=log(N/bj)
        // Joint Entropy, Mutual Information, Variation of Information:
        if(vij > 0) {
          final double p = vij * byn, mlogp = mlog[vij] - mlogn;
          entropyJoint += p * mlogp;
          mi += p * (mlogain + mlogbjn - mlogp);
          vi += p * (mlogp - mlogain + mlogp - mlogbjn);
        }
        // Expected Mutual Information:
        for(int nij = Math.max(ai + bj - n, 1), end = Math.min(ai, bj); nij <= end; nij++) {
          // Note that we have -log a/N = log N/a here from above.
          emi += nij * byn * (mlogain + mlogbjn + mlogn - mlog[nij]) * FastMath.exp( //
              lfacai + lfac[bj] + lfacNmai + lfacNm[bj] //
                  - lfacN - lfac[nij] - lfac[ai - nij] - lfac[bj - nij] - logFac(n - ai - bj + nij));
        }
      }
    }

    // Store output in fields
    this.entropyFirst = entropyFirst;
    this.entropySecond = entropySecond;
    this.entropyJoint = entropyJoint;
    this.mutualInformation = mi;
    this.variationOfInformation = vi;
    this.expectedMutualInformation = emi;
    this.vibound = Math.min(-mlogn, 2 * log(Math.max(r, c)));
  }

  /**
   * Get the maximum cluster size of a contingency table.
   *
   * @param contingency Contingency table
   * @param r Rows
   * @param c Columns
   * @return Maximum
   */
  private static int maxClusterSize(final int[][] contingency, final int r, final int c) {
    int maxc = 0;
    final int[] lastrow = contingency[r];
    for(int j = 0; j < c; j++) {
      final int v = lastrow[j];
      maxc = maxc > v ? maxc : v;
    }
    for(int i = 0; i < r; i++) {
      final int v = contingency[i][c];
      maxc = maxc > v ? maxc : v;
    }
    return maxc;
  }

  /**
   * Compute log(factorial(i)) = log(Gamma(i + 1))
   *
   * @param i Input value
   * @return log factorial
   */
  private static double logFac(double i) {
    return logGamma(i + 1);
  }

  /**
   * Compute entropy of first clustering.
   *
   * @param contingency Contingency table
   * @param r Rows
   * @param c Columns
   * @param byn 1 / N
   * @param mlogn -log(N)
   * @param mlog precomputed -log values
   * @return entropy of first clustering
   */
  private static double computeEntropyFirst(final int[][] contingency, final int r, final int c, final double byn, final double mlogn, double[] mlog) {
    double entropyFirst = 0.0;
    for(int i = 0; i < r; i++) {
      final int v = contingency[i][c];
      if(v > 0) {
        entropyFirst += v * byn * (mlog[v] - mlogn);
      }
    }
    return entropyFirst;
  }

  /**
   * Compute entropy of second clustering.
   *
   * @param contingency Contingency table
   * @param r Rows
   * @param c Columns
   * @param byn 1 / N
   * @param mlogn -log(N)
   * @param mlog precomputed -log values
   * @return entropy of second clustering
   */
  private static double computeEntropySecond(final int[][] contingency, final int r, final int c, final double byn, final double mlogn, double[] mlog) {
    double entropySecond = 0.0;
    int[] lastrow = contingency[r];
    for(int j = 0; j < c; j++) {
      final int v = lastrow[j];
      if(v > 0) {
        entropySecond += v * byn * (mlog[v] - mlogn);
      }
    }
    return entropySecond;
  }

  /**
   * Get the entropy of the first clustering
   * (not normalized, 0 = equal).
   *
   * @return Entropy of first clustering
   */
  public double entropyFirst() {
    return entropyFirst;
  }

  /**
   * Get the entropy of the second clustering
   * (not normalized, 0 = equal).
   *
   * @return Entropy of second clustering
   */
  public double entropySecond() {
    return entropySecond;
  }

  /**
   * Get the joint entropy of both clusterings
   * (not normalized, 0 = equal).
   *
   * @return Joint entropy of both clusterings
   */
  public double entropyJoint() {
    return entropyJoint;
  }

  /**
   * Get the conditional entropy of the first clustering
   * (not normalized, 0 = equal).
   *
   * @return Conditional entropy of first clustering
   */
  public double conditionalEntropyFirst() {
    return entropyJoint - entropySecond;
  }

  /**
   * Get the conditional entropy of the first clustering
   * (not normalized, 0 = equal).
   *
   * @return Conditional entropy of second clustering
   */
  public double conditionalEntropySecond() {
    return entropyJoint - entropyFirst;
  }

  /**
   * Get Powers entropy (normalized, 0 = equal) Powers = 1 - NMI_Sum
   * 
   * @return Powers
   */
  public double entropyPowers() {
    return 2 * entropyJoint / (entropyFirst + entropySecond) - 1;
  }

  /**
   * Get the mutual information (not normalized, small values are good).
   * Beware of the interpretability issues, so use an adjusted or at least
   * normalized version instead.
   *
   * @return Mutual information
   */
  public double mutualInformation() {
    // Note: ideally, entropyFirst + entropySecond - entropyJoint
    return mutualInformation;
  }

  /**
   * Get an upper bound for the mutual information (for scaling).
   *
   * @return Upper bound, using the individual entropies
   */
  public double upperBoundMI() {
    return Math.min(entropyFirst, entropySecond);
  }

  /**
   * Get the joint-normalized mutual information. Values close to 1 are good.
   * <p>
   * Reference:
   * <p>
   * Y. Y. Yao<br>
   * Information-Theoretic Measures for Knowledge Discovery and Data Mining<br>
   * Entropy Measures, Maximum Entropy Principle and Emerging Applications
   *
   * @return Joint Normalized Mutual information
   */
  @Reference(authors = "Y. Y. Yao", //
      title = "Information-Theoretic Measures for Knowledge Discovery and Data Mining", //
      booktitle = "Entropy Measures, Maximum Entropy Principle and Emerging Applications", //
      url = "https://doi.org/10.1007/978-3-540-36212-8_6", //
      bibkey = "doi:10.1007/978-3-540-36212-8_6")
  public double jointNMI() {
    return entropyJoint == 0 ? 0 : mutualInformation / entropyJoint;
  }

  /**
   * Get the min-normalized mutual information. Values close to 1 are good.
   * <p>
   * Reference:
   * <p>
   * T. O. Kvålseth<br>
   * Entropy and Correlation: Some Comments<br>
   * IEEE Trans. Systems, Man, and Cybernetics 17(3)
   *
   * @return Min Normalized Mutual information
   */
  @Reference(authors = "Tarald O. Kvålseth", //
      title = "Entropy and Correlation: Some Comments", //
      booktitle = "IEEE Trans. Systems, Man, and Cybernetics 17(3)", //
      url = "https://doi.org/10.1109/TSMC.1987.4309069", //
      bibkey = "DBLP:journals/tsmc/Kvalseth87")
  public double minNMI() {
    return mutualInformation / Math.min(entropyFirst, entropySecond);
  }

  /**
   * Get the max-normalized mutual information. Values close to 1 are good.
   * <p>
   * Reference:
   * <p>
   * T. O. Kvålseth<br>
   * Entropy and Correlation: Some Comments<br>
   * IEEE Trans. Systems, Man, and Cybernetics 17(3)
   *
   * @return Max Normalized Mutual information
   */
  @Reference(authors = "Tarald O. Kvålseth", //
      title = "Entropy and Correlation: Some Comments", //
      booktitle = "IEEE Trans. Systems, Man, and Cybernetics 17(3)", //
      url = "https://doi.org/10.1109/TSMC.1987.4309069", //
      bibkey = "DBLP:journals/tsmc/Kvalseth87")
  public double maxNMI() {
    return mutualInformation / Math.max(entropyFirst, entropySecond);
  }

  /**
   * Get the arithmetic averaged normalized mutual information. Values close to
   * 1 are good. This is equivalent to the V-Measure for beta=1.
   * <p>
   * Reference:
   * <p>
   * T. O. Kvålseth<br>
   * Entropy and Correlation: Some Comments<br>
   * IEEE Trans. Systems, Man, and Cybernetics 17(3)
   * <p>
   * A. Rosenberg, J. Hirschberg<br>
   * V-Measure: A Conditional Entropy-Based External Cluster Evaluation
   * Measure<br>
   * EMNLP-CoNLL 2007
   *
   * @return Sum Normalized Mutual information
   */
  @Reference(authors = "Tarald O. Kvålseth", //
      title = "Entropy and Correlation: Some Comments", //
      booktitle = "IEEE Trans. Systems, Man, and Cybernetics 17(3)", //
      url = "https://doi.org/10.1109/TSMC.1987.4309069", //
      bibkey = "DBLP:journals/tsmc/Kvalseth87")
  @Reference(authors = "A. Rosenberg, J. Hirschberg", //
      title = "V-Measure: A Conditional Entropy-Based External Cluster Evaluation Measure", //
      booktitle = "EMNLP-CoNLL 2007", //
      url = "https://www.aclweb.org/anthology/D07-1043/", //
      bibkey = "DBLP:conf/emnlp/RosenbergH07")
  public double arithmeticNMI() {
    return 2 * mutualInformation / (entropyFirst + entropySecond);
  }

  /**
   * Get the geometric mean normalized mutual information (using the square
   * root). Values close to 1 are good. This was proposed, for example, by:
   * <p>
   * A. Strehl, J. Ghosh<br>
   * Cluster Ensembles -A Knowledge Reuse Framework for Combining Multiple
   * Partitions<br>
   * J. Mach. Learn. Res. 3
   *
   * @return Sqrt Normalized Mutual information
   */
  @Reference(authors = "A. Strehl, J. Ghosh", //
      title = "Cluster Ensembles -A Knowledge Reuse Framework for Combining Multiple Partitions", //
      booktitle = "J. Mach. Learn. Res. 3", //
      url = "http://jmlr.org/papers/v3/strehl02a.html", //
      bibkey = "DBLP:journals/jmlr/StrehlG02")
  public double geometricNMI() {
    return entropyFirst * entropySecond <= 0 ? mutualInformation : //
        mutualInformation / FastMath.sqrt(entropyFirst * entropySecond);
  }

  /**
   * Get the variation of information (not normalized, small values are good).
   *
   * @return Variation of information
   */
  public double variationOfInformation() {
    return variationOfInformation;
  }

  /**
   * Get an upper bound for the VI (for scaling).
   *
   * @return Upper bound, based on the number of points and clusters.
   */
  public double upperBoundVI() {
    return vibound;
  }

  /**
   * Get the normalized variation of information (normalized, small values are
   * good). This is {@code 1-jointNMI()}.
   *
   * @return Normalized Variation of information
   */
  public double normalizedVariationOfInformation() {
    return 1.0 - mutualInformation / entropyJoint;
  }

  /**
   * Get the normalized information distance (normalized, small values are
   * good). This is {@code 1-maxNMI()}.
   *
   * @return Normalized Variation of information
   */
  public double normalizedInformationDistance() {
    return 1.0 - mutualInformation / Math.max(entropyFirst, entropySecond);
  }

  /**
   * Get the expected mutual information.
   *
   * @return Expected mutual information
   */
  public double expectedMutualInformation() {
    return expectedMutualInformation;
  }

  /**
   * Get the adjusted mutual information using the joint version.
   *
   * @return Adjusted mutual information
   */
  public double adjustedJointMI() {
    return (mutualInformation - expectedMutualInformation) / (entropyJoint - expectedMutualInformation);
  }

  /**
   * Get the adjusted mutual information using the arithmetic version.
   *
   * @return Adjusted mutual information
   */
  public double adjustedArithmeticMI() {
    return (mutualInformation - expectedMutualInformation) / (0.5 * (entropyFirst + entropySecond) - expectedMutualInformation);
  }

  /**
   * Get the adjusted mutual information using the geometric version.
   *
   * @return Adjusted mutual information
   */
  public double adjustedGeometricMI() {
    return entropyFirst * entropySecond <= 0 ? mutualInformation - expectedMutualInformation : //
        (mutualInformation - expectedMutualInformation) / (FastMath.sqrt(entropyFirst * entropySecond) - expectedMutualInformation);
  }

  /**
   * Get the adjusted mutual information using the min version.
   *
   * @return Adjusted mutual information
   */
  public double adjustedMinMI() {
    return (mutualInformation - expectedMutualInformation) / (Math.min(entropyFirst, entropySecond) - expectedMutualInformation);
  }

  /**
   * Get the adjusted mutual information using the max version.
   *
   * @return Adjusted mutual information
   */
  public double adjustedMaxMI() {
    return (mutualInformation - expectedMutualInformation) / (Math.max(entropyFirst, entropySecond) - expectedMutualInformation);
  }
}
