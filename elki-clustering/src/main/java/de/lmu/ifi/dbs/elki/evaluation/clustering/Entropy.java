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
package de.lmu.ifi.dbs.elki.evaluation.clustering;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Entropy based measures.
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
  protected double entropyFirst = -1.0;

  /**
   * Entropy in second
   */
  protected double entropySecond = -1.0;

  /**
   * Joint entropy
   */
  protected double entropyJoint = -1.0;

  /**
   * Constructor.
   * 
   * @param table Contingency table
   */
  protected Entropy(ClusterContingencyTable table) {
    super();
    double norm = 1.0 / table.contingency[table.size1][table.size2];
    {
      entropyFirst = 0.0;
      // iterate over first clustering
      for(int i1 = 0; i1 < table.size1; i1++) {
        if(table.contingency[i1][table.size2] > 0) {
          double probability = norm * table.contingency[i1][table.size2];
          entropyFirst -= probability * FastMath.log(probability);
        }
      }
    }
    {
      entropySecond = 0.0;
      // iterate over first clustering
      for(int i2 = 0; i2 < table.size2; i2++) {
        if(table.contingency[table.size1][i2] > 0) {
          double probability = norm * table.contingency[table.size1][i2];
          entropySecond -= probability * FastMath.log(probability);
        }
      }
    }
    {
      entropyJoint = 0.0;
      for(int i1 = 0; i1 < table.size1; i1++) {
        for(int i2 = 0; i2 < table.size2; i2++) {
          if(table.contingency[i1][i2] > 0) {
            double probability = norm * table.contingency[i1][i2];
            entropyJoint -= probability * FastMath.log(probability);
          }
        }
      }
    }
  }

  /**
   * Get the entropy of the first clustering using Log_2. (not normalized, 0 =
   * equal)
   * 
   * @return Entropy of first clustering
   */
  public double entropyFirst() {
    return entropyFirst;
  }

  /**
   * Get the entropy of the second clustering using Log_2. (not normalized, 0 =
   * equal)
   * 
   * @return Entropy of second clustering
   */
  public double entropySecond() {
    return entropySecond;
  }

  /**
   * Get the joint entropy of both clusterings (not normalized, 0 = equal)
   * 
   * @return Joint entropy of both clusterings
   */
  public double entropyJoint() {
    return entropyJoint;
  }

  /**
   * Get the conditional entropy of the first clustering. (not normalized, 0 =
   * equal)
   * 
   * @return Conditional entropy of first clustering
   */
  public double entropyConditionalFirst() {
    return (entropyJoint() - entropySecond());
  }

  /**
   * Get the conditional entropy of the first clustering. (not normalized, 0 =
   * equal)
   * 
   * @return Conditional entropy of second clustering
   */
  public double entropyConditionalSecond() {
    return (entropyJoint() - entropyFirst());
  }

  /**
   * Get Powers entropy (normalized, 0 = equal) Powers = 1 - NMI_Sum
   * 
   * @return Powers
   */
  public double entropyPowers() {
    return (2 * entropyJoint() / (entropyFirst() + entropySecond()) - 1);
  }

  /**
   * Get the mutual information (not normalized, 0 = equal)
   * 
   * @return Mutual information
   */
  public double entropyMutualInformation() {
    return (entropyFirst() + entropySecond() - entropyJoint());
  }

  /**
   * Get the joint-normalized mutual information (normalized, 0 = unequal)
   * 
   * @return Joint Normalized Mutual information
   */
  public double entropyNMIJoint() {
    if(entropyJoint() == 0) {
      return 0;
    }
    return (entropyMutualInformation() / entropyJoint());
  }

  /**
   * Get the min-normalized mutual information (normalized, 0 = unequal)
   * 
   * @return Min Normalized Mutual information
   */
  public double entropyNMIMin() {
    return (entropyMutualInformation() / Math.min(entropyFirst(), entropySecond()));
  }

  /**
   * Get the max-normalized mutual information (normalized, 0 = unequal)
   * 
   * @return Max Normalized Mutual information
   */
  public double entropyNMIMax() {
    return (entropyMutualInformation() / Math.max(entropyFirst(), entropySecond()));
  }

  /**
   * Get the sum-normalized mutual information (normalized, 0 = unequal)
   * 
   * @return Sum Normalized Mutual information
   */
  public double entropyNMISum() {
    return (2 * entropyMutualInformation() / (entropyFirst() + entropySecond()));
  }

  /**
   * Get the sqrt-normalized mutual information (normalized, 0 = unequal)
   * 
   * @return Sqrt Normalized Mutual information
   */
  public double entropyNMISqrt() {
    if(entropyFirst() * entropySecond() <= 0) {
      return entropyMutualInformation();
    }
    return (entropyMutualInformation() / FastMath.sqrt(entropyFirst() * entropySecond()));
  }

  /**
   * Get the variation of information (not normalized, 0 = equal)
   * 
   * @return Variation of information
   */
  public double variationOfInformation() {
    return (2 * entropyJoint() - (entropyFirst() + entropySecond()));
  }

  /**
   * Get the normalized variation of information (normalized, 0 = equal) NVI = 1
   * - NMI_Joint
   * <p>
   * X. V. Nguyen, J. Epps, J. Bailey<br>
   * Information theoretic measures for clusterings comparison: is a correction
   * for chance necessary?<br>
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
    return (1.0 - (entropyMutualInformation() / entropyJoint()));
  }
}
