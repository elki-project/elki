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
 * Added value (AV) interestingness measure:
 * \( \text{confidence}(X \rightarrow Y) - \text{support}(Y) = P(Y|X)-P(Y) \).
 * <p>
 * Reference:
 * <p>
 * S. Sahar, Sigal, Y. Mansour<br>
 * Empirical evaluation of interest-level criteria<br>
 * Proc. SPIE 3695, Data Mining and Knowledge Discovery: Theory, Tools, and
 * Technology
 * 
 * @author Frederic Sautter
 * @since 0.7.5
 */
@Reference(authors = "S. Sahar, Sigal, Y. Mansour", //
    title = "Empirical evaluation of interest-level criteria", //
    booktitle = "Proc. SPIE 3695, Data Mining and Knowledge Discovery: Theory, Tools, and Technology", //
    url = "https://doi.org/10.1117/12.339991", //
    bibkey = "DBLP:conf/dmkdttt/SaharM99")
public class AddedValue implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public AddedValue() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return (sXY / (double) sX) - (sY / (double) t);
  }
}
