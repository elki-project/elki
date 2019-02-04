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
 * Confidence interestingness measure,
 * \( \tfrac{\text{support}(X \cup Y)}{\text{support}(X)}
 * = \tfrac{P(X \cap Y)}{P(X)}=P(Y|X) \).
 * <p>
 * Reference:
 * <p>
 * R. Agrawal, T. Imielinski, A. Swami<br>
 * Mining association rules between sets of items in large databases<br>
 * Proc. ACM SIGMOD International Conference on Management of Data
 * 
 * @author Frederic Sautter
 * @since 0.7.5
 */
@Reference(authors = "R. Agrawal, T. Imielinski, A. Swami", //
    title = "Mining association rules between sets of items in large databases", //
    booktitle = "Proc. ACM SIGMOD International Conference on Management of Data", //
    url = "https://doi.org/10.1145/170036.170072", //
    bibkey = "DBLP:conf/sigmod/AgrawalIS93")
public class Confidence implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public Confidence() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return sXY / (double) sX;
  }
}
