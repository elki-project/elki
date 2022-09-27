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

import elki.utilities.datastructures.KuhnMunkresStern;
import elki.utilities.documentation.Reference;

/**
 * Calculates the accuracy of a clustering based on the maximum set matching
 * found by the Hungarian algorithm. The resulting runtime is O(k³) for k
 * clusters due to the matching algorithm.
 * <p>
 * Reference:
 * <p>
 * M. J. Zaki and W. Meira Jr.<br>
 * Data Mining and Analysis: Fundamental Concepts and Algorithms<br>
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
@Reference(authors = "M. J. Zaki and W. Meira Jr.", //
    title = "Clustering Validation", //
    booktitle = "Data Mining and Analysis: Fundamental Concepts and Algorithms", //
    url = "https://dataminingbook.info/book_html/chap17/book.html", //
    bibkey = "DBLP:books/cu/ZM2014")
public class MaximumMatchingAccuracy {
  /**
   * Accuracy calculated with maximum matching
   */
  protected double accuracy;

  /**
   * Calculate the maximum matching accuracy.
   *
   * @param table cluster contingency table
   */
  public MaximumMatchingAccuracy(ClusterContingencyTable table) {
    int[][] cont = table.contingency;
    final int rowlen = table.size1, collen = table.size2;
    final int maxlen = Math.max(rowlen, collen);
    // convert to costs for minimum matching:
    double[][] costs = new double[maxlen][maxlen];
    for(int i = 0; i < rowlen; i++) {
      for(int j = 0; j < collen; j++) {
        costs[i][j] = -cont[i][j];
      }
    }
    int[] chosen = new KuhnMunkresStern().run(costs);
    // read off maximum matching
    double correctAssociations = 0;
    for(int i = 0; i < rowlen; i++) {
      correctAssociations += chosen[i] < collen ? cont[i][chosen[i]] : 0;
    }
    accuracy = correctAssociations / cont[rowlen][collen];
  }

  /**
   * Get the maximum matching cluster accuracy.
   * 
   * @return accuracy
   */
  public double getAccuracy() {
    return accuracy;
  }
}
