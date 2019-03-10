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
package elki.distance.probabilistic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.data.HyperBoundingBox;
import elki.distance.AbstractDistanceTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for Chi<sup>2</sup> distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ChiSquaredDistanceTest extends AbstractDistanceTest {
  @Test
  public void testBasic() {
    // Also test the builder - we could have just used .STATIC
    ChiSquaredDistance df = new ELKIBuilder<>(ChiSquaredDistance.class).build();
    basicChecks(df);
    varyingLengthBasic(0, df, 2, 0, 2, 2, 4, 2);
    nonnegativeSpatialConsistency(df);
  }

  static double[][] TOY_DISTANCES;
  static {
    // Manual computation of correct distances:
    double d01 = 2 * (49. / 90 * 2);
    double d03 = 2 * (49. / 255 + 49. / 390 * 2);
    double d04 = 2 * (4. / 140 + 1. / 30 * 2);
    double d14 = 2 * (25. / 70 + .36 + 1. / 30);
    double d34 = 2 * (8. / 105 + 1. / 30 * 2);
    TOY_DISTANCES = new double[][] { //
        { 0., d01, d01, d03, d04 }, //
        { d01, 0., d01, d03, d14 }, //
        { d01, d01, 0., d03, d14 }, //
        { d03, d03, d03, 0., d34 }, //
        { d04, d14, d14, d34, 0. }, //
    };
  }

  @Test
  public void testChiSquaredDistance() {
    double[][] vecs = TOY_VECTORS_VAR;

    ChiSquaredDistance df = new ELKIBuilder<>(ChiSquaredDistance.class).build();
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
