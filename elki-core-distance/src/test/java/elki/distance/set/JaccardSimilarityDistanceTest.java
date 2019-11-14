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
package elki.distance.set;

import org.junit.Test;

import elki.utilities.ELKIBuilder;

public class JaccardSimilarityDistanceTest extends AbstractSetDistanceTest {

  final double[] SCORES = { 1, // a and b / a or b = 0/10 = 0 d = 1
      .5, // c and d / c or d = 5/10 = 0.5 d = .5
      1, // c and e / c or e = 0/5 = 0 d = 1
      2. / 3, // a and c / a or c = 2/6 = 0.3333 d = .66666
      .3, // b and d / b or d = 7/10 = 0.7 d = .3
      0 };

  @Test
  public void testJaccardSimilarityDistance() {
    JaccardSimilarityDistance dist = new ELKIBuilder<>(JaccardSimilarityDistance.class).build();

    basicChecks(dist);
    sparseBasic(1e-10, dist, 1, 0, 1, 1, 1, 1);
    
    bitVectorSet(1e-10, dist, SCORES);
    numberVectorSet(1e-10, dist, SCORES);
    intFeatVectorSet(1e-10, dist, SCORES);
    intFeatVectorSetVarLen(1e-10, dist, .8);
  }

}
