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

import net.jafama.FastMath;

/**
 * Cosine interestingness measure,
 * \(\tfrac{\text{support}(A\cup B)}{\sqrt{\text{support}(A)\text{support}(B)}}
 * =\tfrac{P(A\cap B)}{\sqrt{P(A)P(B)}}\).
 * <p>
 * The interestingness measure called IS by Tan and Kumar.
 * <p>
 * Reference:
 * <p>
 * P. Tan, V. Kumar<br>
 * Interestingness measures for association patterns: A perspective<br>
 * In Proc. Workshop on Postprocessing in Machine Learning and Data Mining
 * 
 * @author Frederic Sautter
 * @since 0.7.5
 */
@Reference(authors = "P. Tan, V. Kumar", //
    title = "Interestingness measures for association patterns: A perspective", //
    booktitle = "Proc. Workshop on Postprocessing in Machine Learning and Data Mining", //
    url = "https://www.cs.umn.edu/sites/cs.umn.edu/files/tech_reports/00-036.pdf", //
    bibkey = "tr/umn/TanK00")
public class Cosine implements InterestingnessMeasure {
  /**
   * Constructor.
   */
  public Cosine() {
    super();
  }

  @Override
  public double measure(int t, int sX, int sY, int sXY) {
    return sXY / FastMath.sqrt(sX * sY);
  }
}
