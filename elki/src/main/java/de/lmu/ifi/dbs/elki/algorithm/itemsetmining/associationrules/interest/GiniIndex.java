/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
 * Gini-index based interestingness measure.
 *
 * Reference:
 * <p>
 * P. Tan and V. Kumar<br />
 * Interestingness measures for association patterns: A perspective<br />
 * In Proc. Workshop on Postprocessing in Machine Learning and Data Mining
 * </p>
 * 
 * The Gini was originally used in decision trees:
 * <p>
 * L. Breiman, J. Friedman, C. J. Stone, and R. A Olshen<br />
 * Classification and regression trees<br />
 * CRC press, 1984
 * </p>
 * 
 * @author Frederic Sautter
 */
@Reference(authors = "P. Tan and V. Kumar", //
    title = "Interestingness measures for association patterns: A perspective", //
    booktitle = "Proc. of Workshop on Postprocessing in Machine Learning and Data Mining, 2000", //
    url = "https://www.cs.umn.edu/sites/cs.umn.edu/files/tech_reports/00-036.pdf")
public class GiniIndex implements InterestingnessMeasure {
  /**
   * Additional reference, for Gini.
   */
  @Reference(authors = "L. Breiman, J. Friedman, C. J. Stone, and R. A Olshen", //
      title = "Classification and regression trees", //
      booktitle = "CRC press, 1984")
  public static Void ADDITIONAL_REFERENCE = null;

  /**
   * Constructor.
   */
  public GiniIndex() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    double pX = sX / (double) t;
    double pY_X = sXY / (double) sX;
    double pnotY_X = (sX - sXY) / (double) sX;
    double pnotX = (t - sX) / (double) t;
    double pY_notX = (sY - sXY) / (double) (t - sX);
    double pnotY_notX = ((t - sX) - (sY - sXY)) / (double) (t - sX);
    double pY = sY / (double) t;
    double pnotY = (t - sY) / (double) t;
    return pX * (pY_X * pY_X + pnotY_X * pnotY_X) + pnotX * (pY_notX * pY_notX + pnotY_notX * pnotY_notX) - pY * pY - pnotY * pnotY;
  }
}
