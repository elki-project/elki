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
 * Klösgen interestingness measure.
 * <p>
 * \[ \sqrt{\text{support}(X\cup Y)}
 * \left(\text{confidence}(X \rightarrow Y) - \text{support}(Y)\right)
 * = \sqrt{P(X\cap Y)}\left(P(Y|X)-P(Y)\right) \]
 * <p>
 * Reference:
 * <p>
 * W. Klösgen<br>
 * Explora: A multipattern and multistrategy discovery assistant<br>
 * Advances in Knowledge Discovery and Data Mining
 *
 * @author Frederic Sautter
 * @since 0.7.5
 */
@Reference(authors = "W. Klösgen", //
    title = "Explora: A multipattern and multistrategy discovery assistant", //
    booktitle = "Advances in Knowledge Discovery and Data Mining", //
    bibkey = "DBLP:books/mit/fayyadPSU96/Klosgen96")
public class Klosgen implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public Klosgen() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return Math.sqrt(sXY / (double) t) * ((sXY / (double) sX) - (sY / (double) t));
  }
}
