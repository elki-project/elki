package de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.interest;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Klosgen interestingness measure.
 *
 * Reference:
 * <p>
 * W. Klosgen<br />
 * Explora: A multipattern and multistrategy discovery assistant<br />
 * In Advances in knowledge discovery and data mining, American Association for
 * Artificial Intelligence, 1996
 * </p>
 * 
 * @author Frederic Sautter
 */
@Reference(authors = "W. Klosgen", //
    title = "Explora: A multipattern and multistrategy discovery assistant", //
    booktitle = "Advances in knowledge discovery and data mining, American Association for Artificial Intelligence, 1996")
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
