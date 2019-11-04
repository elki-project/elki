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

import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Entropy based measures, implemented using base 2 logarithms.
 * <p>
 * References:
 * <p>
 * M. Meilă<br>
 * Comparing clusterings by the variation of information<br>
 * Learning theory and kernel machines
 *
 * @author Sascha Goldhofer
 * @since 0.5.0
 */
@Reference(authors = "M. Meilă", //
    title = "Comparing clusterings by the variation of information", //
    booktitle = "Learning theory and kernel machines", //
    url = "https://doi.org/10.1007/978-3-540-45167-9_14", //
    bibkey = "DBLP:conf/colt/Meila03")
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
    final int total = table.contingency[table.size1][table.size2];
    final double[] logmarg1 = new double[table.size1];
    final double[] logmarg2 = new double[table.size2];
    final double norm = -1.0 / total;
    final double logtotal = FastMath.log(total);
    double entropyFirst = 0.0;
    // iterate over first clustering marginals
    for(int i1 = 0; i1 < table.size1; i1++) {
      int v = table.contingency[i1][table.size2];
      if(v > 0) {
        final double logp = logmarg1[i1] = FastMath.log(v) - logtotal;
        entropyFirst += v * /* negative */ norm * logp;
      }
    }
    double entropySecond = 0.0;
    // iterate over second clustering marginals
    int[] lastrow = table.contingency[table.size1];
    for(int i2 = 0; i2 < table.size2; i2++) {
      int v = lastrow[i2];
      if(v > 0) {
        final double logp = logmarg2[i2] = FastMath.log(v) - logtotal;
        entropySecond += v * /* negative */ norm * logp;
      }
    }
    double entropyJoint = 0.0, mi = 0.0, vi = 0.0;
    for(int i1 = 0; i1 < table.size1; i1++) {
      final int[] row1 = table.contingency[i1];
      final double logmarg1i1 = logmarg1[i1];
      for(int i2 = 0; i2 < table.size2; i2++) {
        int v = row1[i2];
        if(v > 0) {
          final double logp = FastMath.log(v) - logtotal;
          entropyJoint += v * /* negative */ norm * logp;
          mi += v * /* negative */ norm * (logmarg1i1 + logmarg2[i2] - logp);
          vi += v * /* negative */ norm * (logp - logmarg1i1 + logp - logmarg2[i2]);
        }
      }
    }
    // Store in fields
    this.entropyFirst = entropyFirst;
    this.entropySecond = entropySecond;
    this.entropyJoint = entropyJoint;
    this.mutualInformation = mi;
    this.variationOfInformation = vi;
    this.vibound = Math.min(logtotal, 2 * FastMath.log(Math.max(table.size1, table.size2)));
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
   * <p>
   * X. V. Nguyen, J. Epps, J. Bailey<br>
   * Information theoretic measures for clusterings comparison:
   * is a correction for chance necessary?<br>
   * Proc. 26th Ann. Int. Conf. on Machine Learning (ICML '09)
   *
   * @return Normalized Variation of information
   */
  @Reference(authors = "X. V. Nguyen, J. Epps, J. Bailey", //
      title = "Information theoretic measures for clusterings comparison: is a correction for chance necessary?", //
      booktitle = "Proc. 26th Ann. Int. Conf. on Machine Learning (ICML '09)", //
      url = "https://doi.org/10.1145/1553374.1553511", //
      bibkey = "DBLP:conf/icml/NguyenEB09")
  public double normalizedVariationOfInformation() {
    return 1.0 - mutualInformation / entropyJoint;
  }

  /**
   * Get the normalized information distance (normalized, small values are
   * good). This is {@code 1-maxNMI()}.
   * <p>
   * X. V. Nguyen, J. Epps, J. Bailey<br>
   * Information theoretic measures for clusterings comparison:
   * is a correction for chance necessary?<br>
   * Proc. 26th Ann. Int. Conf. on Machine Learning (ICML '09)
   *
   * @return Normalized Variation of information
   */
  @Reference(authors = "X. V. Nguyen, J. Epps, J. Bailey", //
      title = "Information theoretic measures for clusterings comparison: is a correction for chance necessary?", //
      booktitle = "Proc. 26th Ann. Int. Conf. on Machine Learning (ICML '09)", //
      url = "https://doi.org/10.1145/1553374.1553511", //
      bibkey = "DBLP:conf/icml/NguyenEB09")
  public double normalizedInformationDistance() {
    return 1.0 - mutualInformation / Math.max(entropyFirst, entropySecond);
  }
}
