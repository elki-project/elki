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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.interest;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Jaccard interestingness measure:
 * <p>
 * \[\tfrac{\text{support}(A \cup B)}{\text{support}(A \cap B)}
 * =\tfrac{P(A \cap B)}{P(A)+P(B)-P(A \cap B)}
 * =\tfrac{P(A \cap B)}{P(A \cup B)}\]
 * <p>
 * Reference:
 * <p>
 * P.-N. Tan, V. Kumar, J. Srivastava<br>
 * Selecting the right objective measure for association analysis<br>
 * Information Systems 29.4
 * <p>
 * Tan et al. credit Rijsbergen for the use of Jaccard in Information Retrieval
 * (it was not used for association rule mining here):
 * <p>
 * C. J. van Rijsbergen<br>
 * Information Retrieval, 2nd Edition<br>
 * Butterworths, London, 1979
 *
 * @author Frederic Sautter
 * @since 0.7.5
 */
@Reference(authors = "P.-N. Tan, V. Kumar, J. Srivastava", // )
    title = "Selecting the right objective measure for association analysis", //
    booktitle = "Information Systems 29.4", //
    url = "https://doi.org/10.1016/S0306-4379(03)00072-3", //
    bibkey = "DBLP:journals/is/TanKS04")
@Reference(authors = "C. J. van Rijsbergen", //
    title = "Information Retrieval, 2nd Edition", //
    booktitle = "Butterworths, London, 1979", //
    bibkey = "DBLP:books/bu/Rijsbergen79")
public class Jaccard implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public Jaccard() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return sXY / (double) (sX + sY - sXY);
  }
}
