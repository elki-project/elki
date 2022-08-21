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
 * Edit distance measures.
 * <p>
 * P. Pantel, D. Lin<br>
 * Document clustering with committees<br>
 * Proc. 25th ACM SIGIR Conf. on Research and Development in Information
 * Retrieval
 *
 * @author Sascha Goldhofer
 * @since 0.5.0
 */
@Reference(authors = "P. Pantel, D. Lin", //
    title = "Document clustering with committees", //
    booktitle = "Proc. 25th ACM SIGIR Conf. on Research and Development in Information Retrieval", //
    url = "https://doi.org/10.1145/564376.564412", //
    bibkey = "DBLP:conf/sigir/PantelL02")
public class EditDistance {
  /**
   * Edit operations for first clustering to second clustering.
   */
  int editFirst = -1;

  /**
   * Edit operations for second clustering to first clustering.
   */
  int editSecond = -1;

  /**
   * Baseline for edit operations
   */
  int editOperationsBaseline;

  protected EditDistance(ClusterContingencyTable table) {
    super();
    final int[][] contingency = table.contingency;
    final int r = table.size1, c = table.size2;
    int ed1 = 0, ed2 = 0;
    // We perform the editing the opposite way as in the original paper, hence
    // we switch the output variables. This is minimally simpler, as we do not
    // need to store the target label.
    for(int i = 0; i < c; i++) {
      final int csize = contingency[r][i];
      if(csize > 0) {
        // get largest cell in column
        int largestLabelSet = 0;
        for(int j = 0; j < r; j++) {
          largestLabelSet = Math.max(largestLabelSet, contingency[j][i]);
        }
        // Merge, move remaining objects
        ed1 += 1 + csize - largestLabelSet;
      }
    }
    for(int i = 0; i < r; i++) {
      final int csize = contingency[i][c];
      if(csize > 0) {
        // get largest cell in row
        int largestLabelSet = 0;
        for(int j = 0; j < c; j++) {
          largestLabelSet = Math.max(largestLabelSet, contingency[i][j]);
        }
        // Merge, move remaining objects
        ed2 += 1 + csize - largestLabelSet;
      }
    }
    this.editFirst = ed1;
    this.editSecond = ed2;
    this.editOperationsBaseline = contingency[r][c];
  }

  /**
   * Get the baseline editing Operations (= total objects)
   * 
   * @return worst case amount of operations
   */
  public int editOperationsBaseline() {
    return editOperationsBaseline;
  }

  /**
   * Get the editing operations required to transform first clustering to
   * second clustering
   * 
   * @return Editing operations used to transform first into second clustering
   */
  public int editOperationsFirst() {
    return editFirst;
  }

  /**
   * Get the editing operations required to transform second clustering to
   * first clustering
   * 
   * @return Editing operations used to transform second into first clustering
   */
  public int editOperationsSecond() {
    return editSecond;
  }

  /**
   * Get the editing distance to transform second clustering to first
   * clustering (normalized, 0 = unequal)
   * 
   * @return Editing distance first into second clustering
   */
  public double editDistanceFirst() {
    return 1.0 - editOperationsFirst() / (double) editOperationsBaseline();
  }

  /**
   * Get the editing distance to transform second clustering to first
   * clustering (normalized, 0 = unequal)
   * 
   * @return Editing distance second into first clustering
   */
  public double editDistanceSecond() {
    return 1.0 - editOperationsSecond() / (double) editOperationsBaseline();
  }

  /**
   * Get the edit distance F1-Measure
   * 
   * @return Edit Distance F1-Measure
   */
  public double f1Measure() {
    return Util.f1Measure(editDistanceFirst(), editDistanceSecond());
  }
}
