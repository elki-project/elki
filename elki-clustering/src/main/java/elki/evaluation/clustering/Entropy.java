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
package elki.evaluation.clustering;

import static elki.math.statistics.distribution.GammaDistribution.logGamma;

import elki.logging.LoggingUtil;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Entropy based measures, implemented using natural logarithms.
 * <p>
 * Computing the expected mutual information is well optimized, but nevertheless
 * takes O(n) logarithm computations, for n up to 10000, we will perform this
 * more expensive evaluation, for smaller values it will (currently) not be
 * computed. In theory, as n grows, the expected value becomes 0 anyway.
 * <p>
 * FIXME: this measure does not yet support noise clusters.
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
 * <p>
 * S. Romano, J. Bailey, X. V. Nguyen, K. Verspoor<br>
 * Standardized Mutual Information for Clustering Comparisons:
 * One Step Further in Adjustment for Chance<br>
 * Proc. 31th Int. Conf. on Machine Learning (ICML 2014)
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
@Reference(authors = "S. Romano, J. Bailey, X. V. Nguyen, K. Verspoor", //
    title = "Standardized Mutual Information for Clustering Comparisons: One Step Further in Adjustment for Chance", //
    booktitle = "Proc. 31th Int. Conf. on Machine Learning (ICML 2014)", //
    url = "http://proceedings.mlr.press/v32/romano14.html", //
    bibkey = "DBLP:conf/icml/RomanoBNV14")
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
    // 1/N and -log(N)
    final double byn = 1.0 / n, mlogn = -FastMath.log(n);
    // We compute EMI values only for small clusterings.
    if(n <= 10000) {
      // Maximum cluster size, and cluster sizes:
      final int m = maxClusterSize(contingency, r, c);
      final double[] logs = new double[m]; // Cache
      this.entropyFirst = computeEntropyFirst(contingency, r, c, byn, mlogn, logs);
      this.entropySecond = computeEntropySecond(contingency, r, c, byn, mlogn, logs);
      computeMIFull(contingency, r, c, n, m, byn, mlogn, logs);
    }
    else {
      computeMILarge(contingency, r, c, byn, mlogn);
    }
  }

  /**
   * Compute mutual information measures, but skip expensive computation of
   * AMI/EMI for large data sets, where they do not differ much.
   * 
   * @param contingency Contingency table
   * @param r Rows
   * @param c Columns
   * @param byn 1/N factor
   * @param mlogn -log(N)
   */
  private void computeMILarge(int[][] contingency, int r, int c, double byn, double mlogn) {
    final int[] lastrow = contingency[r];
    final double[] logs = new double[14]; // Small cache only.
    final double[] mlogbn = new double[c]; // Log cluster sizes
    double ent1 = 0.0, ent2 = 0.0, joint = 0.0, mi = 0.0, vi = 0.0;
    for(int j = 0; j < c; j++) {
      final int v = lastrow[j];
      if(v > 0) {
        ent2 += v * byn * (mlogbn[j] = -log(v, logs) - mlogn);
      }
    }

    for(int i = 0; i < r; i++) {
      final int[] rowi = contingency[i];
      final int an = rowi[c];
      if(an <= 0) {
        continue;
      }
      final double mlogain = -log(an, logs) - mlogn; // -log(ai/N)=log(N/ai)
      ent1 += an * byn * mlogain;
      for(int j = 0; j < c; j++) {
        final int vij = rowi[j];
        final double mlogbjn = mlogbn[j]; // -log(bj/N)=log(N/bj)
        // Joint Entropy, Mutual Information, Variation of Information:
        if(vij > 0) {
          final double p = vij * byn, mlogp = -log(vij, logs) - mlogn;
          joint += p * mlogp;
          mi += p * (mlogain + mlogbjn - mlogp);
          vi += p * (mlogp - mlogain + mlogp - mlogbjn);
        }
      }
    }
    // Store output in fields
    this.entropyFirst = ent1;
    this.entropySecond = ent2;
    this.entropyJoint = joint;
    this.mutualInformation = mi;
    this.variationOfInformation = vi;
    this.expectedMutualInformation = 0.;
    this.vibound = Math.min(-mlogn, 2 * FastMath.log(Math.max(r, c)));
  }

  /**
   * Full computation of mutual information measures, including AMI/EMI.
   * 
   * @param contingency Contingency table
   * @param r Rows
   * @param c Columns
   * @param n Total size
   * @param m Maximum cluster size
   * @param byn 1/N factor
   * @param mlogn -log(N)
   * @param logs Logarithm cache
   */
  private void computeMIFull(int[][] contingency, int r, int c, int n, int m, double byn, double mlogn, double[] logs) {
    // Precompute log(factorial) table:
    double[] lfacs = new double[Math.max(n, 1)];
    double tmp = 0.;
    for(int i = 2, e = Math.max(n - m, 2); i <= e; i++) {
      lfacs[i - 2] = tmp += log(i, logs);
    }
    final int[] lastrow = contingency[r];
    double joint = 0.0, mi = 0.0, vi = 0.0, emi = 0.0;
    for(int i = 0; i < r; i++) {
      final int[] rowi = contingency[i];
      final int ai = rowi[c];
      final double mlogain = -log(ai, logs) - mlogn; // -log(ai/N)=log(N/ai)
      final double lfacai = lfac(ai, lfacs), lfacNmai = lfac(n - ai, lfacs);
      for(int j = 0; j < c; j++) {
        final int bj = lastrow[j];
        final int vij = rowi[j];
        final double mlogbjn = -log(bj, logs) - mlogn; // -log(bj/N)=log(N/bj)
        // Joint Entropy, Mutual Information, Variation of Information:
        if(vij > 0) {
          final double p = vij * byn, mlogp = -log(vij, logs) - mlogn;
          joint += p * mlogp;
          mi += p * (mlogain + mlogbjn - mlogp);
          vi += p * (mlogp - mlogain + mlogp - mlogbjn);
        }
        // Expected Mutual Information:
        int start = Math.max(ai + bj - n, 1), end = Math.min(ai, bj);
        if(start <= end) {
          // Note that we have -log a/N = log N/a here from above.
          final double t1 = mlogain + mlogbjn + mlogn;
          double t2 = FastMath.exp(lfacai + lfac(bj, lfacs) + lfacNmai + lfac(n - bj, lfacs) //
              - lfac(n, lfacs) - lfac(start, lfacs) //
              - lfac(ai - start, lfacs) - lfac(bj - start, lfacs) - lfac(n - ai - bj + start, lfacs));
          emi += start * byn * (t1 + log(start, logs)) * t2;
          // Faster computation based on Romano et al. 2014
          for(int nij = start + 1; nij <= end; nij++) {
            t2 *= (ai - nij + 1.) * (bj - nij + 1.) / (nij * (n - ai - bj + nij));
            if(t2 < 0.) { // Numeric bounds
              break;
            }
            emi += nij * byn * (t1 + log(nij, logs)) * t2;
          }
        }
      }
    }
    // Store output in fields
    this.entropyJoint = joint;
    this.mutualInformation = mi;
    this.variationOfInformation = vi;
    this.expectedMutualInformation = emi;
    this.vibound = Math.min(-mlogn, 2 * FastMath.log(Math.max(r, c)));
  }

  /**
   * Cached access to log values.
   *
   * @param i Parameter
   * @param logs Logarithm cache
   * @return Logarithm
   */
  private static double log(int i, double[] logs) {
    if(i <= 1) {
      return 0.;
    }
    if(i - 2 >= logs.length) {
      return FastMath.log(i);
    }
    final double v = logs[i - 2];
    return v > 0 ? v : (logs[i - 2] = FastMath.log(i));
  }

  /**
   * Get log(fac(i)) from the cache.
   *
   * @param i Factorial
   * @param lfac Cache
   * @return Cached or computed value.
   */
  private static double lfac(int i, double[] lfac) {
    if(i <= 1) {
      return 0.;
    }
    final double v = lfac[i - 2];
    if(v > 0) {
      return v;
    }
    // Note: we require the cache to be prefilled by at least 1 element,
    // and to be sized large enough.
    final double p = lfac[i - 3];
    return lfac[i - 2] = p > 0 ? p + FastMath.log(i) : logGamma(i + 1.);
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
   * Compute entropy of first clustering.
   *
   * @param contingency Contingency table
   * @param r Rows
   * @param c Columns
   * @param byn 1 / N
   * @param mlogn -log(N)
   * @param logs log value cache
   * @return entropy of first clustering
   */
  private static double computeEntropyFirst(final int[][] contingency, final int r, final int c, final double byn, final double mlogn, double[] logs) {
    double entropyFirst = 0.0;
    for(int i = 0; i < r; i++) {
      final int v = contingency[i][c];
      if(v > 0) {
        entropyFirst += v * byn * (-log(v, logs) - mlogn);
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
   * @param logs log value cache
   * @return entropy of second clustering
   */
  private static double computeEntropySecond(final int[][] contingency, final int r, final int c, final double byn, final double mlogn, double[] logs) {
    double entropySecond = 0.0;
    int[] lastrow = contingency[r];
    for(int j = 0; j < c; j++) {
      final int v = lastrow[j];
      if(v > 0) {
        entropySecond += v * byn * (-log(v, logs) - mlogn);
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
        mutualInformation / Math.sqrt(entropyFirst * entropySecond);
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
        (mutualInformation - expectedMutualInformation) / (Math.sqrt(entropyFirst * entropySecond) - expectedMutualInformation);
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
