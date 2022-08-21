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
 * Yule's Y interestingness measure.
 * <p>
 * \[ \frac{\sqrt{P(X \cap Y) P(\neg X \cap \neg Y)} - \sqrt{P(X \cap \neg Y)
 * P(\neg X \cap Y)}}{\sqrt{P(X \cap Y) P(\neg X \cap \neg Y)} + \sqrt{P(X \cap
 * \neg Y) P(\neg X \cap Y)}} = \frac{\sqrt{\alpha} - 1}{\sqrt{\alpha} + 1} \]
 * <p>
 * Reference:
 * <p>
 * G. U. Yule<br>
 * On the methods of measuring association between two attributes<br>
 * Journal of the Royal Statistical Society 75 (6)
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
    title = "On the methods of measuring association between two attributes", //
    booktitle = "Journal of the Royal Statistical Society 75 (6)", //
    url = "https://doi.org/10.2307/2340126", //
    bibkey = "doi:10.2307/2340126")
@Reference(authors = "P.-N. Tan, V. Kumar, J. Srivastava", // )
    title = "Selecting the right objective measure for association analysis", //
    booktitle = "Information Systems 29.4", //
    url = "https://doi.org/10.1016/S0306-4379(03)00072-3", //
    bibkey = "DBLP:journals/is/TanKS04")
public class YulesY extends OddsRatio {
  /**
   * Constructor.
   */
  public YulesY() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    double sqrtalpha = Math.sqrt(super.measure(t, sX, sY, sXY));
    return (sqrtalpha - 1) / (sqrtalpha + 1);
  }
}
