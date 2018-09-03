/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.random.FastNonThreadsafeRandom;

/**
 * Validate spatial distance functions by ensuring that mindist <= distance for
 * random objects.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public abstract class AbstractSpatialPrimitiveDistanceFunctionTest {
  /**
   * Toy vectors, used for unit testing distribution distances.
   */
  public static final double[][] TOY_VECTORS = new double[][] { //
      { 0.8, 0.1, 0.1 }, //
      { 0.1, 0.8, 0.1 }, //
      { 0.1, 0.1, 0.8 }, //
      { 1. / 3, 1. / 3, 1. / 3 }, //
      { 0.6, 0.2, 0.2 } };

  /**
   * Toy vectors, used for unit testing distribution distances.
   */
  public static final double[][] TOY_VECTORS_VAR = new double[][] { //
      { 0.8, 0.1, 0.1 }, //
      { 0.1, 0.8, 0.1, 0, 0 }, //
      { 0.1, 0.1, 0.8 }, //
      { 1. / 3, 1. / 3, 1. / 3 }, //
      { 0.6, 0.2, 0.2, 0 } };

  /**
   * Number of dimensions to use in basic test.
   */
  public static final int TEST_DIM = 5;

  /**
   * Compare two distances.
   *
   * @param ref Reference measure
   * @param test Test measure
   * @param v1 First vector
   * @param v2 Second vector
   * @param tol Tolerance
   * @param <V> vector type
   */
  public static <V> void assertSameDistance(PrimitiveDistanceFunction<? super V> ref, PrimitiveDistanceFunction<? super V> test, V v1, V v2, double tol) {
    assertEquals("Distances not same", ref.distance(v1, v2), test.distance(v1, v2), tol);
  }

  /**
   * Compare two distances.
   *
   * @param ref Reference measure
   * @param test Test measure
   * @param v1 First vector
   * @param v2 Second vector
   * @param tol Tolerance
   */
  public static void assertSameMinDist(SpatialPrimitiveDistanceFunction<?> ref, SpatialPrimitiveDistanceFunction<?> test, SpatialComparable v1, SpatialComparable v2, double tol) {
    assertEquals("Distances not same", ref.minDist(v1, v2), test.minDist(v1, v2), tol);
  }

  /**
   * Test that we can compare vectors with extra 0s.
   *
   * @param dist Distance function
   */
  public static void varyingLength(PrimitiveDistanceFunction<? super DoubleVector> dist) {
    assertFalse("Test setup inconsistent.", VMath.equals(TOY_VECTORS[1], TOY_VECTORS_VAR[1]));
    DoubleVector v1 = DoubleVector.wrap(TOY_VECTORS[0]);
    DoubleVector v2 = DoubleVector.wrap(TOY_VECTORS[1]);
    DoubleVector v3 = DoubleVector.wrap(TOY_VECTORS_VAR[1]);
    double expect = dist.distance(v1, v2);
    double have1 = dist.distance(v1, v3), have2 = dist.distance(v3, v1);
    assertEquals("Distance not as expected", expect, have1, 1e-15);
    assertEquals("Distance not as expected", expect, have2, 1e-15);
  }

  /**
   * MBR consistency check, around 0.
   *
   * @param dist Distance function to check
   */
  public static void spatialConsistency(SpatialPrimitiveDistanceFunction<? super NumberVector> dist) {
    final Random rnd = new FastNonThreadsafeRandom(0);
    final int dim = TEST_DIM, iters = 1000;

    double[] d1 = new double[dim], d2 = new double[dim], d3 = new double[dim],
        d4 = new double[dim];
    DoubleVector v1 = DoubleVector.wrap(d1), v2 = DoubleVector.wrap(d2);
    HyperBoundingBox mbr = new HyperBoundingBox(d3, d4);
    compareDistances(v1, v2, mbr, dist);
    for(int i = 0; i < iters; i++) {
      for(int d = 0; d < dim; d++) {
        d1[d] = (rnd.nextDouble() - .5) * 2E4;
        double m = d2[d] = (rnd.nextDouble() - .5) * 2E4;
        d3[d] = m - rnd.nextDouble() * 1E4;
        d4[d] = m + rnd.nextDouble() * 1E4;
      }
      compareDistances(v1, v2, mbr, dist);
    }
  }

  /**
   * Test not involving negative values.
   * 
   * @param dis Distance function to test
   */
  public static void nonnegativeSpatialConsistency(SpatialPrimitiveDistanceFunction<? super NumberVector> dis) {
    // These should probably go into a more generic function.
    assertFalse("Equals not null safe", dis.equals(null));
    assertFalse("Equals Object.class?", dis.equals(new Object()));
    assertTrue("Inconsistent equals", dis.equals(dis));
    // assertTrue("Missing toString()", dis.toString().indexOf('@') < 0);

    final Random rnd = new FastNonThreadsafeRandom(1);
    final int dim = TEST_DIM, iters = 10000;
    double[] d1 = new double[dim], d2 = new double[dim], d3 = new double[dim],
        d4 = new double[dim];
    DoubleVector v1 = DoubleVector.wrap(d1), v2 = DoubleVector.wrap(d2);
    HyperBoundingBox mbr = new HyperBoundingBox(d3, d4);
    d1[0] = d2[1] = d4[1] = d4[2] = 1.; // Trivial sitations, with many zeros.
    compareDistances(v1, v2, mbr, dis);
    for(int i = 0; i < iters; i++) {
      for(int d = 0; d < dim; d++) {
        d1[d] = rnd.nextDouble() * 2E4;
        double v = d2[d] = rnd.nextDouble() * 2E4;
        d3[d] = rnd.nextDouble() * v;
        d4[d] = rnd.nextDouble() * (2E4 - v) + v;
      }
      compareDistances(v1, v2, mbr, dis);
    }
  }

  public static void compareDistances(NumberVector v1, NumberVector v2, HyperBoundingBox mbr2, SpatialPrimitiveDistanceFunction<? super NumberVector> dist) {
    double exact = dist.distance(v1, v2), mind = dist.minDist(v1, v2),
        mbrd = dist.minDist(v1, mbr2), zero = dist.minDist(v2, mbr2);
    assertEquals("Not same: " + dist.toString(), exact, mind, 1e-10);
    assertTrue("Not smaller:" + dist.toString() + " " + mbrd + " > " + exact + " " + mbr2 + " " + v1, mbrd <= exact);
    assertEquals("Not zero: " + dist.toString(), 0., zero, 1e-10);
  }
}