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
 * Odds ratio interestingness measure.
 * <p>
 * \[\frac{P(X\cap Y) P(\neg X\cap\neg Y)}{P(X\cap\neg Y) P(\neg X\cap Y)}\]
 * <p>
 * Popularized for association by:
 * <p>
 * F. Mosteller<br>
 * Association and Estimation in Contingency Tables<br>
 * Journal of the American Statistical Association 63:321
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
@Reference(authors = "F. Mosteller", //
    title = "Association and Estimation in Contingency Tables", //
    booktitle = "Journal of the American Statistical Association 63:321", //
    url = "https://doi.org/10.1080/01621459.1968.11009219", //
    bibkey = "doi:10.1080/01621459.1968.11009219")
@Reference(authors = "P.-N. Tan, V. Kumar, J. Srivastava", // )
    title = "Selecting the right objective measure for association analysis", //
    booktitle = "Information Systems 29.4", //
    url = "https://doi.org/10.1016/S0306-4379(03)00072-3", //
    bibkey = "DBLP:journals/is/TanKS04")
public class OddsRatio implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public OddsRatio() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    double pXY = sXY / (double) t;
    double pXnotY = (sX - sXY) / (double) t;
    double pnotXY = (sY - sXY) / (double) t;
    double pnotXnotY = 1 - (sX + sY - sXY) / (double) t;
    return (pXY * pnotXnotY) / (pXnotY * pnotXY);
  }
}
