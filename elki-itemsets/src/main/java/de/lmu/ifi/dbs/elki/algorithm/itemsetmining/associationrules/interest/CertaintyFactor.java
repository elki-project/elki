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

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Certainty factor (CF; Loevinger) interestingness measure.
 * \( \tfrac{\text{confidence}(X \rightarrow Y) -
 * \text{support}(Y)}{\text{support}(\neg Y)} \).
 * <p>
 * Reference:
 * <p>
 * F. Berzal, I. Blanco, D. Sánchez, M. Vila<br>
 * Measuring the accuracy and interest of association rules: A new framework<br>
 * Intelligent Data Analysis, 6(3), 2002
 * 
 * @author Frederic Sautter
 * @since 0.7.5
 */
@Reference(authors = "F. Berzal, I. Blanco, D. Sánchez, M. Vila", //
    title = "Measuring the accuracy and interest of association rules: A new framework", //
    booktitle = "Intelligent Data Analysis, 6(3), 2002", //
    url = "http://content.iospress.com/articles/intelligent-data-analysis/ida00089", //
    bibkey = "DBLP:journals/ida/GalianoBSM02")
@Alias({ "Loevinger" })
public class CertaintyFactor implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public CertaintyFactor() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return sY < t ? ((sXY / (double) sX) * t - sY) / (t - sY) : 0.;
  }
}
