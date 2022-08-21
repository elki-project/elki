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
 * Yule's Q interestingness measure.
 * <p>
 * \[\frac{P(X \cap Y) P(\neg X \cap \neg Y) - P(X \cap \neg Y) P(\neg X \cap
 * Y)}{P(X \cap Y) P(\neg X \cap \neg Y) + P(X \cap \neg Y) P(\neg X \cap Y)}
 * = \frac{\alpha - 1}{\alpha + 1} \]
 * <p>
 * Reference:
 * <p>
 * G. U. Yule<br>
 * On the association of attributes in statistics<br>
 * Philosophical Transactions of the Royal Society A 194
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
@Reference(authors = "G. U. Yule", //
    title = "On the association of attributes in statistics", //
    booktitle = "Philosophical Transactions of the Royal Society A 194", //
    url = "https://doi.org/10.1098/rsta.1900.0019", //
    bibkey = "doi:10.1098/rsta.1900.0019")
@Reference(authors = "P.-N. Tan, V. Kumar, J. Srivastava", // )
    title = "Selecting the right objective measure for association analysis", //
    booktitle = "Information Systems 29.4", //
    url = "https://doi.org/10.1016/S0306-4379(03)00072-3", //
    bibkey = "DBLP:journals/is/TanKS04")
public class YulesQ extends OddsRatio {
  /**
   * Constructor.
   */
  public YulesQ() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    double alpha = super.measure(t, sX, sY, sXY);
    return (alpha - 1) / (alpha + 1);
  }
}
