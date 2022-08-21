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
 * BCubed measures for cluster evaluation.
 * <p>
 * Reference:
 * <p>
 * A. Bagga, B. Baldwin<br>
 * Entity-based cross-document coreferencing using the Vector Space Model<br>
 * Proc. 17th Int. Conf. on Computational Linguistics (COLING '98)
 * <p>
 * E. Amigó, J. Gonzalo, J. Artiles, F. Verdejo<br>
 * A comparison of extrinsic clustering evaluation metrics based on formal
 * constraints<br>
 * Information Retrieval 12(4)
 *
 * @author Sascha Goldhofer
 * @since 0.5.0
 */
@Reference(authors = "A. Bagga, B. Baldwin", //
    title = "Entity-based cross-document coreferencing using the Vector Space Model", //
    booktitle = "Proc. 17th Int. Conf. on Computational Linguistics (COLING '98)", //
    url = "https://doi.org/10.3115/980451.980859", //
    bibkey = "doi:10.3115/980451.980859")
@Reference(authors = "E. Amigó, J. Gonzalo, J. Artiles, F. Verdejo", //
    title = "A comparison of extrinsic clustering evaluation metrics based on formal constraints", //
    booktitle = "Information Retrieval 12(4)", //
    url = "https://doi.org/10.1007/s10791-008-9066-8", //
    bibkey = "DBLP:journals/ir/AmigoGAV09")
public class BCubed {
  /**
   * Result cache
   */
  protected double bCubedPrecision = -1.0, bCubedRecall = -1.0;

  /**
   * Constructor.
   *
   * @param table Contingency table
   */
  protected BCubed(ClusterContingencyTable table) {
    super();
    double aggPrec = 0.0, aggRec = 0.0;
    final int selfpair = table.selfPairing ? 0 : 1;
    for(int i1 = 0; i1 < table.size1; i1++) {
      final int[] sumrow = table.contingency[i1];
      for(int i2 = 0; i2 < table.size2; i2++) {
        final int c = sumrow[i2];
        if(c > selfpair) {
          aggPrec += c * (c - selfpair) / (double) (table.contingency[table.size1][i2] - selfpair);
          aggRec += c * (c - selfpair) / (double) (sumrow[table.size2] - selfpair);
        }
      }
    }
    final int total = table.contingency[table.size1][table.size2];
    this.bCubedPrecision = aggPrec / total;
    this.bCubedRecall = aggRec / total;
  }

  /**
   * Get the BCubed Precision (first clustering) (normalized, 0 = unequal)
   *
   * @return BCubed Precision
   */
  public double precision() {
    return bCubedPrecision;
  }

  /**
   * Get the BCubed Recall (first clustering) (normalized, 0 = unequal)
   *
   * @return BCubed Recall
   */
  public double recall() {
    return bCubedRecall;
  }

  /**
   * Get the BCubed F1-Measure
   *
   * @return BCubed F1-Measure
   */
  public double f1Measure() {
    return Util.f1Measure(bCubedPrecision, bCubedRecall);
  }
}
