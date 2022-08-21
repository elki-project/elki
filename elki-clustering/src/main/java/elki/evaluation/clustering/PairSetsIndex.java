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

import java.util.Arrays;

import elki.utilities.datastructures.KuhnMunkresStern;
import elki.utilities.documentation.Reference;

/**
 * The Pair Sets Index calculates an index based on the maximum matching of
 * relative cluster sizes by the Hungarian algorithm.
 * <p>
 * Reference:
 * <p>
 * M. Rezaei and F. Pasi<br>
 * Set Matching Measures for External Cluster Validity<br>
 * IEEE Transactions on Knowledge and Data Engineering 28(8)
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
@Reference(authors = "M. Rezaei and F. Pasi", //
    title = "Set Matching Measures for External Cluster Validity", //
    booktitle = "IEEE Transactions on Knowledge and Data Engineering 28(8)", //
    url = "https://doi.org/10.1109/TKDE.2016.2551240", //
    bibkey = "DBLP:journals/tkde/RezaeiF16")
public class PairSetsIndex {
  /**
   * Simplified PSI (with e = 1)
   */
  protected double simplifiedPSI = 0;

  /**
   * (s - e) / (max(size1,size2) - e)
   */
  protected double psi = 0;

  /**
   * Constructor.
   *
   * @param table contingency table for PSI calculation
   */
  public PairSetsIndex(ClusterContingencyTable table) {
    final int rowlen = table.size1, collen = table.size2;
    if(rowlen == collen && rowlen == 1) {
      psi = 1;
      simplifiedPSI = 1;
      return;
    }
    int[][] cont = table.contingency;
    final int maxlen = Math.max(rowlen, collen);
    // convert to normalized costs for minimum matching:
    double[][] costs = new double[maxlen][maxlen];
    for(int i = 0; i < rowlen; i++) {
      final int rowsum = cont[i][collen];
      if(rowsum > 0) {
        for(int j = 0; j < collen; j++) {
          costs[i][j] = cont[i][j] > 0 ? -cont[i][j] / (double) Math.max(cont[rowlen][j], rowsum) : 0;
        }
      }
    }
    int[] chosen = new KuhnMunkresStern().run(costs);
    // sum up matching cost s
    double s = 0;
    for(int i = 0; i < maxlen; i++) {
      s += -costs[i][chosen[i]];
    }

    // calculating e: searching for better sort options
    // copy cluster sizes from last column of cont matrix
    int[] firstlevelOrder = new int[rowlen];
    for(int i = 0; i < rowlen; i++) {
      firstlevelOrder[i] = cont[i][collen];
    }
    int[] secondlevelOrder = new int[collen];
    for(int i = 0; i < collen; i++) {
      secondlevelOrder[i] = cont[rowlen][i];
    }
    Arrays.sort(firstlevelOrder);
    Arrays.sort(secondlevelOrder);

    double e = 0;
    int minlength = Math.min(rowlen, collen);
    for(int i = 0; i < minlength; i++) {
      e += (firstlevelOrder[i] * secondlevelOrder[i] / (double) cont[rowlen][collen]) / Math.max(firstlevelOrder[i], secondlevelOrder[i]);
    }

    simplifiedPSI = s < 1 ? 0 : (s - 1) / (Math.max(rowlen, collen) - 1);
    psi = s < e ? 0 : (s - e) / (Math.max(rowlen, collen) - e);
  }

  /**
   * Get the PSI measure.
   * 
   * @return the calculated PSI value
   */
  public double psi() {
    return psi;
  }

  /**
   * Get the simplified PSI value using e = 1
   * 
   * @return the calculated simplified PSI value
   */
  public double simplifiedPSI() {
    return simplifiedPSI;
  }
}
