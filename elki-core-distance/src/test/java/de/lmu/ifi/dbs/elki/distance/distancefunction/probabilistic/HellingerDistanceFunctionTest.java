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

import static net.jafama.FastMath.pow;
import static net.jafama.FastMath.sqrt;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunctionTest;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for Hellinger distance.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HellingerDistanceFunctionTest extends AbstractDistanceFunctionTest {
  @Test
  public void testBasic() {
    // Also test the builder - we could have just used .STATIC
    HellingerDistanceFunction df = new ELKIBuilder<>(HellingerDistanceFunction.class).build();
    basicChecks(df);
    varyingLengthBasic(1e-15, df, MathUtil.SQRTHALF, 0, MathUtil.SQRTHALF, MathUtil.SQRTHALF, 1, MathUtil.SQRTHALF);
    nonnegativeSpatialConsistency(df);
  }

  @Test
  public void testHellingerDistance() {
    double[][] vecs = TOY_VECTORS_VAR;

    // Manual computation of correct distances:
    double d0102sq = pow(sqrt(0.1) - sqrt(0.2), 2);
    double d0106sq = pow(sqrt(0.1) - sqrt(0.6), 2);
    double d0108sq = pow(sqrt(0.1) - sqrt(0.8), 2);
    double d0133sq = pow(sqrt(0.1) - sqrt(1. / 3), 2);
    double d0208sq = pow(sqrt(0.2) - sqrt(0.8), 2);
    double d0233sq = pow(sqrt(0.2) - sqrt(1. / 3), 2);
    double d0608sq = pow(sqrt(0.6) - sqrt(0.8), 2);
    double d0633sq = pow(sqrt(0.6) - sqrt(1. / 3), 2);
    double d0833sq = pow(sqrt(0.8) - sqrt(1. / 3), 2);
    double d01 = sqrt(0.5 * (d0108sq + d0108sq + 0));
    double d03 = sqrt(0.5 * (d0833sq + d0133sq + d0133sq));
    double d04 = sqrt(0.5 * (d0608sq + d0102sq + d0102sq));
    double d14 = sqrt(0.5 * (d0106sq + d0208sq + d0102sq));
    double d34 = sqrt(0.5 * (d0633sq + d0233sq + d0233sq));
    double[][] distances = { //
        { 0., d01, d01, d03, d04 }, //
        { d01, 0., d01, d03, d14 }, //
        { d01, d01, 0., d03, d14 }, //
        { d03, d03, d03, 0., d34 }, //
        { d04, d14, d14, d34, 0. }, //
    };
    // Manual computation of correct similarities:
    double s01 = sqrt(0.08) + sqrt(0.08) + sqrt(0.01);
    double s03 = sqrt(0.8 / 3) + sqrt(0.1 / 3) + sqrt(0.1 / 3);
    double s04 = sqrt(0.48) + sqrt(0.02) + sqrt(0.02);
    double s14 = sqrt(0.06) + sqrt(0.16) + sqrt(0.02);
    double s34 = sqrt(0.2) + sqrt(0.2 / 3) + sqrt(0.2 / 3);
    double[][] similarities = { //
        { 1., s01, s01, s03, s04 }, //
        { s01, 1., s01, s03, s14 }, //
        { s01, s01, 1., s03, s14 }, //
        { s03, s03, s03, 1., s34 }, //
        { s04, s14, s14, s34, 1. }, //
    };

    HellingerDistanceFunction df = new ELKIBuilder<>(HellingerDistanceFunction.class).build();
    for(int i = 0; i < vecs.length; i++) {
      DoubleVector vi = DoubleVector.wrap(vecs[i]);
      HyperBoundingBox mbri = new HyperBoundingBox(vecs[i], vecs[i]);
      for(int j = 0; j < vecs.length; j++) {
        DoubleVector vj = DoubleVector.wrap(vecs[j]);
        assertEquals("Distance " + i + "," + j, distances[i][j], df.distance(vi, vj), 1e-15);
        assertEquals("Similarity " + i + "," + j, 1 - pow(distances[i][j], 2), df.similarity(vi, vj), 1e-15);
        assertEquals("Distance " + i + "," + j, sqrt(1 - similarities[i][j]), df.distance(vi, vj), 1e-15);
        assertEquals("Similarity " + i + "," + j, similarities[i][j], df.similarity(vi, vj), 1e-15);
        compareDistances(vj, vi, mbri, df);
      }
    }
  }
}
