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
package elki.itemsetmining.associationrules.interest;

import elki.utilities.documentation.Reference;

/**
 * Laplace Corrected Confidence interestingness measure.
 * <p>
 * \[ \frac{nP(X \cap Y) + 1}{nP(X) + 2} \]
 * <p>
 * Reference:
 * <p>
 * P. Clark, R. Boswell<br>
 * Rule induction with CN2: Some recent improvements<br>
 * European Working Session on Learning, EWSL-91
 * <p>
 * The use for association rule mining was studied in:
 * <p>
 * P.-N. Tan, V. Kumar, J. Srivastava<br>
 * Selecting the right objective measure for association analysis<br>
 * Information Systems 29.4
 * 
 * @author Abhishek Sharma
 * @since 0.8.0
 */
@Reference(authors = "P. Clark, R. Boswell", //
    title = "Rule induction with CN2: Some recent improvements", //
    booktitle = "European Working Session on Learning, EWSL-91", //
    url = "https://doi.org/10.1007/BFb0016999", //
    bibkey = "DBLP:conf/ecml/ClarkB91")
@Reference(authors = "P.-N. Tan, V. Kumar, J. Srivastava", // )
    title = "Selecting the right objective measure for association analysis", //
    booktitle = "Information Systems 29.4", //
    url = "https://doi.org/10.1016/S0306-4379(03)00072-3", //
    bibkey = "DBLP:journals/is/TanKS04")
public class LaplaceCorrectedConfidence implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public LaplaceCorrectedConfidence() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return (sXY + 1) / (double) (sX + 2);
  }
}
