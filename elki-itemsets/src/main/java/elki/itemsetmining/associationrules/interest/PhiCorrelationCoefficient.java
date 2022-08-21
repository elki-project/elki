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
 * Phi Correlation Coefficient interestingness measure.
 * <p>
 * \[ \frac{n P(X \cap Y) - P(X) - P(Y)}{\sqrt{P(X)P(Y)P(\neg X)P(\neg Y)}} \]
 * <p>
 * This is closely related to the χ² statistic.
 * <p>
 * The use for association rule mining was studied in:
 * <p>
 * P.-N. Tan, V. Kumar, J. Srivastava<br>
 * Selecting the right objective measure for association analysis<br>
 * Information Systems 29.4
 * <p>
 * Tan et al. attribute this measure to:
 * <p>
 * A. Agresti<br>
 * Categorical Data Analysis<br>
 * Wiley Series in Probability and Statistics
 * 
 * @author Abhishek Sharma
 * @since 0.8.0
 */
@Reference(authors = "A. Agresti", //
    title = "Categorical Data Analysis", //
    booktitle = "Categorical Data Analysis", //
    bibkey = "books/wiley/Agresti90")
@Reference(authors = "P.-N. Tan, V. Kumar, J. Srivastava", // )
    title = "Selecting the right objective measure for association analysis", //
    booktitle = "Information Systems 29.4", //
    url = "https://doi.org/10.1016/S0306-4379(03)00072-3", //
    bibkey = "DBLP:journals/is/TanKS04")
public class PhiCorrelationCoefficient implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public PhiCorrelationCoefficient() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    double pXY = sXY / (double) t;
    double pX = sX / (double) t;
    double pY = sY / (double) t;
    return (pXY - pX * pY) / Math.sqrt(pX * pY * (1 - pX) * (1 - pY));
  }
}
