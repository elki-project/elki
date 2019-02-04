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
package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

import static net.jafama.FastMath.log;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunctionTest;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for Jeffrey distance.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class JeffreyDivergenceDistanceFunctionTest extends AbstractDistanceFunctionTest {
  @Test
  public void testBasic() {
    // Also test the builder - we could have just used .STATIC
    JeffreyDivergenceDistanceFunction df = new ELKIBuilder<>(JeffreyDivergenceDistanceFunction.class).build();
    basicChecks(df);
    nonnegativeSpatialConsistency(df);
  }

  // Manual computation of correct distances:
  static double[][] TOY_DISTANCES;
  static {
    double d01 = (.8 * log(16. / 9) + .1 * log(2. / 9)) * 2;
    double d03 = (.8 * log(24. / 17) + log(10. / 17) / 3) + (.1 * log(6. / 13) + log(20. / 13) / 3) * 2;
    double d04 = (.8 * log(8. / 7) + .6 * log(6. / 7)) + (.1 * log(2. / 3) + .2 * log(4. / 3)) * 2;
    double d14 = (.1 * log(2. / 7) + .6 * log(12. / 7)) + (.8 * log(1.6) + .2 * log(.4)) + (.1 * log(2. / 3) + .2 * log(4. / 3));
    double d34 = (.6 * log(9. / 7) + log(5. / 7) / 3) + (.2 * log(.75) + 1. / 3 * log(1.25)) * 2;
    TOY_DISTANCES = new double[][] { //
        { 0., d01, d01, d03, d04 }, //
        { d01, 0., d01, d03, d14 }, //
        { d01, d01, 0., d03, d14 }, //
        { d03, d03, d03, 0., d34 }, //
        { d04, d14, d14, d34, 0. }, //
    };
  }

  @Test
  public void testJeffreyDivergenceDistance() {
    double[][] vecs = TOY_VECTORS;

    JeffreyDivergenceDistanceFunction df = new ELKIBuilder<>(JeffreyDivergenceDistanceFunction.class).build();
    for(int i = 0; i < vecs.length; i++) {
      DoubleVector vi = DoubleVector.wrap(vecs[i]);
      HyperBoundingBox mbri = new HyperBoundingBox(vecs[i], vecs[i]);
      for(int j = 0; j < vecs.length; j++) {
        DoubleVector vj = DoubleVector.wrap(vecs[j]);
        assertEquals("Distance " + i + "," + j, TOY_DISTANCES[i][j], df.distance(vi, vj), 1e-15);
        compareDistances(vj, vi, mbri, df);
      }
    }
  }
}
