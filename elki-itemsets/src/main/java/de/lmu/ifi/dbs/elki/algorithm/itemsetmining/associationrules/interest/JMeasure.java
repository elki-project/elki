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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * J-Measure interestingness measure.
 * \(P(X\cap Y)\log\tfrac{P(Y|X)}{P(Y)}
 * + P(X\cap \neg Y)\log\tfrac{P(\neg Y|X)}{P(\neg Y)}\).
 * <p>
 * Reference:
 * <p>
 * R. M. Goodman, P. Smyth<br>
 * Rule induction using information theory<br>
 * Knowledge Discovery in Databases 1991
 * 
 * @author Frederic Sautter
 * @since 0.7.5
 */
@Reference(authors = "R. M. Goodman, P. Smyth", //
    title = "Rule induction using information theory", //
    booktitle = "Knowledge Discovery in Databases 1991", //
    bibkey = "DBLP:books/mit/PF91/SmythG91")
public class JMeasure implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public JMeasure() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    double pXY = sXY / (double) t;
    double pY_X = sXY / (double) sX;
    double pY = sY / (double) t;
    int sXnotY = sX - sXY;
    double pXnotY = sXnotY / (double) t;
    double pnotY_X = sXnotY / (double) sX;
    double pnotY = 1. - pY;
    return (pXY * FastMath.log(pY_X / pY) + pXnotY * FastMath.log(pnotY_X / pnotY)) * MathUtil.ONE_BY_LOG2;
  }
}
