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

import elki.evaluation.clustering.ClusterContingencyTable.Util;
import elki.utilities.documentation.Reference;

/**
 * Set matching purity measures.
 * <p>
 * References:
 * <p>
 * M. Meilă<br>
 * Comparing clusterings<br>
 * University of Washington, Seattle, Technical Report 418
 * <p>
 * Y. Zhao, G. Karypis<br>
 * Criterion functions for document clustering: Experiments and analysis<br>
 * University of Minnesota, Dep. Computer Science, Technical Report 01-40
 * <p>
 * M. Steinbach, G. Karypis, V. Kumar<br>
 * A Comparison of Document Clustering Techniques<br>
 * KDD workshop on text mining. Vol. 400. No. 1
 * <p>
 * E. Amigó, J. Gonzalo, J. Artiles, and F. Verdejo<br>
 * A comparison of extrinsic clustering evaluation metrics based on formal
 * constraints<br>
 * Inf. Retrieval 12(5)
 *
 * @author Sascha Goldhofer
 * @since 0.5.0
 */
@Reference(authors = "M. Meilă", //
    title = "Comparing clusterings", //
    booktitle = "University of Washington, Seattle, Technical Report 418", //
    url = "http://www.stat.washington.edu/mmp/Papers/compare-colt.pdf", //
    bibkey = "tr/washington/Meila02")
public class SetMatchingPurity {
  /**
   * Result cache
   */
  protected double smPurity = -1.0, smInversePurity = -1.0, smFFirst = -1.0,
      smFSecond = -1.0;

  /**
   * Constructor.
   * 
   * @param table Contingency table
   */
  protected SetMatchingPurity(ClusterContingencyTable table) {
    super();
    final int[][] contingency = table.contingency;
    final int r = table.size1, c = table.size2;
    double aggPurity = 0.0, aggFirst = 0.0;
    // iterate first clustering
    for(int i = 0; i < r; i++) {
      double precisionMax = 0.0, fMax = 0.0;
      for(int j = 0; j < c; j++) {
        precisionMax = Math.max(precisionMax, contingency[i][j]);
        fMax = Math.max(fMax, (2.0 * contingency[i][j]) / (contingency[i][c] + contingency[r][j]));
      }
      aggPurity += precisionMax;
      aggFirst += contingency[i][c] * fMax;
    }
    double aggInvP = 0.0, aggSecond = 0.0;
    // iterate second clustering
    for(int i = 0; i < c; i++) {
      double recallMax = 0.0, fMax = 0.0;
      for(int j = 0; j < r; j++) {
        recallMax = Math.max(recallMax, contingency[j][i]);
        fMax = Math.max(fMax, (2.0 * contingency[j][i]) / (contingency[j][c] + contingency[r][i]));
      }
      aggInvP += recallMax;
      aggSecond += contingency[r][i] * fMax;
    }
    final int numobj = contingency[r][c];
    // Store and scale
    this.smPurity = aggPurity / numobj;
    this.smFFirst = aggFirst / numobj;
    this.smInversePurity = aggInvP / numobj;
    this.smFSecond = aggSecond / numobj;
  }

  /**
   * Get the set matchings purity (first:second clustering)
   * (normalized, 1 = equal)
   * <p>
   * Y. Zhao, G. Karypis<br>
   * Criterion functions for document clustering: Experiments and analysis<br>
   * University of Minnesota, Dep. Computer Science, Technical Report 01-40
   *
   * @return purity
   */
  @Reference(authors = "Y. Zhao, G. Karypis", //
      title = "Criterion functions for document clustering: Experiments and analysis", //
      booktitle = "University of Minnesota, Dep. Computer Science, Technical Report 01-40", //
      url = "http://www-users.cs.umn.edu/~karypis/publications/Papers/PDF/vscluster.pdf", //
      bibkey = "tr/umn/ZhaoK01")
  public double purity() {
    return smPurity;
  }

  /**
   * Get the set matchings inverse purity (second:first clustering) (normalized,
   * 1 = equal)
   * 
   * @return Inverse purity
   */
  public double inversePurity() {
    return smInversePurity;
  }

  /**
   * Get the set matching F1-Measure
   * <p>
   * M. Steinbach, G. Karypis, V. Kumar<br>
   * A Comparison of Document Clustering Techniques<br>
   * KDD workshop on text mining. Vol. 400. No. 1
   *
   * @return Set Matching F1-Measure
   */
  @Reference(authors = "M. Steinbach, G. Karypis, V. Kumar", //
      title = "A Comparison of Document Clustering Techniques", //
      booktitle = "KDD workshop on text mining. Vol. 400. No. 1", //
      url = "http://glaros.dtc.umn.edu/gkhome/fetch/papers/docclusterKDDTMW00.pdf", //
      bibkey = "conf/kdd/SteinbachKK00")
  public double f1Measure() {
    return Util.f1Measure(purity(), inversePurity());
  }

  /**
   * Get the Van Rijsbergen’s F measure (asymmetric) for first clustering
   * <p>
   * E. Amigó, J. Gonzalo, J. Artiles, and F. Verdejo<br>
   * A comparison of extrinsic clustering evaluation metrics based on formal
   * constraints<br>
   * Information Retrieval 12(5)
   *
   * @return Set Matching F-Measure of first clustering
   */
  @Reference(authors = "E. Amigó, J. Gonzalo, J. Artiles, F. Verdejo", //
      title = "A comparison of extrinsic clustering evaluation metrics based on formal constraints", //
      booktitle = "Information Retrieval 12(5)", //
      url = "https://doi.org/10.1007/s10791-009-9106-z", //
      bibkey = "DBLP:journals/ir/AmigoGAV09a")
  public double fMeasureFirst() {
    return smFFirst;
  }

  /**
   * Get the Van Rijsbergen’s F measure (asymmetric) for second clustering
   * <p>
   * E. Amigó, J. Gonzalo, J. Artiles, and F. Verdejo<br>
   * A comparison of extrinsic clustering evaluation metrics based on formal
   * constraints<br>
   * Information Retrieval 12(5)
   *
   * @return Set Matching F-Measure of second clustering
   */
  @Reference(authors = "E. Amigó, J. Gonzalo, J. Artiles, F. Verdejo", //
      title = "A comparison of extrinsic clustering evaluation metrics based on formal constraints", //
      booktitle = "Information Retrieval 12(5)", //
      url = "https://doi.org/10.1007/s10791-009-9106-z", //
      bibkey = "DBLP:journals/ir/AmigoGAV09a")
  public double fMeasureSecond() {
    return smFSecond;
  }
}
