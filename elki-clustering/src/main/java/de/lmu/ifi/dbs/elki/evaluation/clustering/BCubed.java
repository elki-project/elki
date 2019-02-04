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

import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable.Util;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * BCubed measures.
 * <p>
 * Reference:
 * <p>
 * A. Bagga, B. Baldwin<br>
 * Entity-based cross-document coreferencing using the Vector Space Model<br>
 * Proc. 17th Int. Conf. on Computational Linguistics (COLING '98)
 *
 * @author Sascha Goldhofer
 * @since 0.5.0
 */
@Reference(authors = "A. Bagga, B. Baldwin", //
    title = "Entity-based cross-document coreferencing using the Vector Space Model", //
    booktitle = "Proc. 17th Int. Conf. on Computational Linguistics (COLING '98)", //
    url = "https://doi.org/10.3115/980451.980859", //
    bibkey = "doi:10.3115/980451.980859")
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
    bCubedPrecision = 0.0;
    bCubedRecall = 0.0;

    for(int i1 = 0; i1 < table.size1; i1++) {
      final int[] row = table.contingency[i1];
      for(int i2 = 0; i2 < table.size2; i2++) {
        final int c = row[i2];
        if(c > 0) {
          // precision of one item
          double precision = 1.0 * c / row[table.size2];
          // precision for all items in cluster
          bCubedPrecision += (precision * c);

          // recall of one item
          double recall = 1.0 * c / table.contingency[table.size1][i2];
          // recall for all items in cluster
          bCubedRecall += (recall * c);
        }
      }
    }
    final int total = table.contingency[table.size1][table.size2];
    bCubedPrecision = bCubedPrecision / total;
    bCubedRecall = bCubedRecall / total;
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
    return Util.f1Measure(precision(), recall());
  }
}
