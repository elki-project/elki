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
 * Leverage interestingness measure.
 * <p>
 * \[ \text{support}(X\Rightarrow Y)-\text{support}(X)\text{support}(Y)
 * =P(X\cap Y)-P(X)P(Y) \]
 * <p>
 * Reference:
 * <p>
 * G. Piatetsky-Shapiro<br>
 * Discovery, analysis, and presentation of strong rules<br>
 * In Knowledge Discovery in Databases 1991
 *
 * @author Frederic Sautter
 * @since 0.7.5
 */
@Reference(authors = "G. Piatetsky-Shapiro", //
    title = "Discovery, analysis, and presentation of strong rules", //
    booktitle = "Knowledge Discovery in Databases 1991", //
    bibkey = "DBLP:books/mit/PF91/Piatetsky91")
public class Leverage implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public Leverage() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return (sXY - sX * (long) sY / (double) t) / t;
  }
}
