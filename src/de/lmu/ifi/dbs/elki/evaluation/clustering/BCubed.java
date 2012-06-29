package de.lmu.ifi.dbs.elki.evaluation.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable.Util;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * BCubed measures.
 * 
 * Reference:
 * <p>
 * Bagga, A. and Baldwin, B.<br />
 * Entity-based cross-document coreferencing using the Vector Space Model<br />
 * Proc. COLING '98 Proceedings of the 17th international conference on
 * Computational linguistics
 * </p>
 * 
 * @author Sascha Goldhofer
 */
@Reference(authors = "Bagga, A. and Baldwin, B.", title = "Entity-based cross-document coreferencing using the Vector Space Model", booktitle = "Proc. COLING '98 Proceedings of the 17th international conference on Computational linguistics", url = "http://dx.doi.org/10.3115/980451.980859")
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
      for(int i2 = 0; i2 < table.size2; i2++) {
        // precision of one item
        double precision = 1.0 * table.contingency[i1][i2] / table.contingency[i1][table.size2];
        // precision for all items in cluster
        bCubedPrecision += (precision * table.contingency[i1][i2]);

        // recall of one item
        double recall = 1.0 * table.contingency[i1][i2] / table.contingency[table.size1][i2];
        // recall for all items in cluster
        bCubedRecall += (recall * table.contingency[i1][i2]);
      }
    }
    bCubedPrecision = bCubedPrecision / table.contingency[table.size1][table.size2];
    bCubedRecall = bCubedRecall / table.contingency[table.size1][table.size2];
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