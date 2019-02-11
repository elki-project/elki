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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseDoubleVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.random.FastNonThreadsafeRandom;

/**
 * Validate spatial distance functions by ensuring that mindist <= distance for
 * random objects.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public abstract class AbstractDistanceFunctionTest {
  /**
   * Basic integrity checks.
   * 
   * @param dist Distance function
   */
  public static void basicChecks(DistanceFunction<?> dist) {
    // Additional API checks
    assertFalse("Equals not null safe", dist.equals(null));
    assertFalse("Equals Object.class?", dist.equals(new Object()));
    assertTrue("Inconsistent equals", dist.equals(dist));
    assertFalse("Squared distances shouldn't be metric?", dist.isMetric() && dist.isSquared());
    assertTrue("Squared distances shouldn't be metric?", dist.isSquared() || !dist.toString().contains("Square"));
    assertTrue("Sqrt distances should be metric?", !dist.isSquared() || !dist.toString().contains("Sqrt"));
    // assertTrue("Missing toString()", dis.toString().indexOf('@') < 0);
  }

  /**
   * Simple toy vectors
   */
  public static final DoubleVector[] BASIC = new DoubleVector[] { //
      new DoubleVector(new double[] { 0 }), //
      new DoubleVector(new double[] { 1 }), //
      new DoubleVector(new double[] { 0, 0 }), //
      new DoubleVector(new double[] { 0, 1 }), //
      new DoubleVector(new double[] { .1 }), //
      new DoubleVector(new double[] { .2 }), //
      new DoubleVector(new double[] { .3 }), //
  };

  /**
   * Basic regression test for variable length vectors.
   *
   * @param delta tolerance
   * @param dist Distance function
   * @param ds Correct vectors
   */
  public static void varyingLengthBasic(double tolerance, PrimitiveDistanceFunction<? super NumberVector> dist, double... ds) {
    // Should accept variable lengths in the API:
    assertTrue("Does not accept variable length.", dist.getInputTypeRestriction().isAssignableFromType(new VectorTypeInformation<>(DoubleVector.class, 1, 2)));

    assertEquals("Basic 0", ds[0], dist.distance(BASIC[0], BASIC[1]), tolerance);
    assertEquals("Basic 1", ds[1], dist.distance(BASIC[0], BASIC[2]), tolerance);
    assertEquals("Basic 2", ds[2], dist.distance(BASIC[0], BASIC[3]), tolerance);
    assertEquals("Basic 3", ds[3], dist.distance(BASIC[1], BASIC[2]), tolerance);
    assertEquals("Basic 4", ds[4], dist.distance(BASIC[1], BASIC[3]), tolerance);
    assertEquals("Basic 5", ds[5], dist.distance(BASIC[2], BASIC[3]), tolerance);

    double expect = dist.distance(BASIC[0], BASIC[3]);
    assertEquals("Distance not as expected", expect, dist.distance(BASIC[1], BASIC[2]), 1e-15);
    if(dist.isSymmetric()) {
      assertEquals("Distance not as expected", expect, dist.distance(BASIC[3], BASIC[0]), 1e-15);
      assertEquals("Distance not as expected", expect, dist.distance(BASIC[2], BASIC[1]), 1e-15);
    }
    if(dist instanceof Norm) {
      Norm<? super NumberVector> norm = (Norm<? super NumberVector>) dist;
      assertEquals("Distance not as expected", dist.distance(BASIC[0], BASIC[1]), norm.norm(BASIC[1]), 1e-15);
      assertEquals("Distance not as expected", dist.distance(BASIC[2], BASIC[3]), norm.norm(BASIC[3]), 1e-15);
    }
    if(dist instanceof SpatialPrimitiveDistanceFunction) {
      SpatialPrimitiveDistanceFunction<? super NumberVector> sdf = (SpatialPrimitiveDistanceFunction<? super NumberVector>) dist;
      compareDistances(BASIC[0], BASIC[3], new HyperBoundingBox(BASIC[3].toArray(), BASIC[3].toArray()), sdf);
      compareDistances(BASIC[2], BASIC[1], new HyperBoundingBox(BASIC[1].toArray(), BASIC[1].toArray()), sdf);
    }
    if(dist.isMetric()) {
      assertTrue("Trivial metric test failed.", dist.distance(BASIC[4], BASIC[6]) <= dist.distance(BASIC[4], BASIC[5]) + dist.distance(BASIC[5], BASIC[6]));
    }
  }

  /**
   * Simple toy vectors
   */
  public static final SparseDoubleVector[] SBASIC = new SparseDoubleVector[] { //
      new SparseDoubleVector(new double[] { 0 }), //
      new SparseDoubleVector(new double[] { 1 }), //
      new SparseDoubleVector(new double[] { 0, 0 }), //
      new SparseDoubleVector(new double[] { 0, 1 }), //
      new SparseDoubleVector(new double[] { .1 }), //
      new SparseDoubleVector(new double[] { .2 }), //
      new SparseDoubleVector(new double[] { .3 }), //
  };

  /**
   * Basic regression test for sparse vectors.
   *
   * @param delta tolerance
   * @param dist Distance function
   * @param ds Correct vectors
   */
  public static void sparseBasic(double tolerance, PrimitiveDistanceFunction<? super SparseNumberVector> dist, double... ds) {
    // Should accept variable lengths in the API:
    assertTrue("Does not accept variable length.", dist.getInputTypeRestriction().isAssignableFromType(new VectorTypeInformation<>(SparseDoubleVector.class, 1, 2)));

    assertEquals("Basic 0", ds[0], dist.distance(SBASIC[0], SBASIC[1]), tolerance);
    assertEquals("Basic 1", ds[1], dist.distance(SBASIC[0], SBASIC[2]), tolerance);
    assertEquals("Basic 2", ds[2], dist.distance(SBASIC[0], SBASIC[3]), tolerance);
    assertEquals("Basic 3", ds[3], dist.distance(SBASIC[1], SBASIC[2]), tolerance);
    assertEquals("Basic 4", ds[4], dist.distance(SBASIC[1], SBASIC[3]), tolerance);
    assertEquals("Basic 4b", ds[4], dist.distance(SBASIC[3], SBASIC[1]), tolerance);
    assertEquals("Basic 5", ds[5], dist.distance(SBASIC[2], SBASIC[3]), tolerance);
    assertEquals("Basic 5b", ds[5], dist.distance(SBASIC[3], SBASIC[2]), tolerance);

    double expect = dist.distance(SBASIC[0], SBASIC[3]);
    assertEquals("Distance not as expected", expect, dist.distance(SBASIC[1], SBASIC[2]), 1e-15);
    if(dist.isSymmetric()) {
      assertEquals("Distance not as expected", expect, dist.distance(SBASIC[3], SBASIC[0]), 1e-15);
      assertEquals("Distance not as expected", expect, dist.distance(SBASIC[2], SBASIC[1]), 1e-15);
    }
    if(dist instanceof Norm) {
      Norm<? super SparseNumberVector> norm = (Norm<? super SparseNumberVector>) dist;
      assertEquals("Distance not as expected", dist.distance(SBASIC[0], SBASIC[1]), norm.norm(SBASIC[1]), 1e-15);
      assertEquals("Distance not as expected", dist.distance(SBASIC[2], SBASIC[3]), norm.norm(SBASIC[3]), 1e-15);
    }
    if(dist.isMetric()) {
      assertTrue("Trivial metric test failed.", dist.distance(SBASIC[4], SBASIC[6]) <= dist.distance(SBASIC[4], SBASIC[5]) + dist.distance(SBASIC[5], SBASIC[6]));
    }
  }

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
   * @param dist Distance function to test
   */
  public static void nonnegativeSpatialConsistency(SpatialPrimitiveDistanceFunction<? super NumberVector> dist) {
    final Random rnd = new FastNonThreadsafeRandom(1);
    final int dim = TEST_DIM, iters = 10000;
    double[] d1 = new double[dim], d2 = new double[dim], d3 = new double[dim],
        d4 = new double[dim];
    DoubleVector v1 = DoubleVector.wrap(d1), v2 = DoubleVector.wrap(d2);
    HyperBoundingBox mbr = new HyperBoundingBox(d3, d4);
    d1[0] = d2[1] = d4[1] = d4[2] = 1.; // Trivial sitations, with many zeros.
    compareDistances(v1, v2, mbr, dist);
    for(int i = 0; i < iters; i++) {
      for(int d = 0; d < dim; d++) {
        d1[d] = rnd.nextDouble() * 2E4;
        double v = d2[d] = rnd.nextDouble() * 2E4;
        d3[d] = rnd.nextDouble() * v;
        d4[d] = rnd.nextDouble() * (2E4 - v) + v;
      }
      compareDistances(v1, v2, mbr, dist);
    }
  }

  public static void compareDistances(NumberVector v1, NumberVector v2, HyperBoundingBox mbr2, SpatialPrimitiveDistanceFunction<? super NumberVector> dist) {
    double exact = dist.distance(v1, v2), mind = dist.minDist(v1, v2),
        mbrd = dist.minDist(v1, mbr2), zero = dist.minDist(v2, mbr2),
        mbrd2 = dist.minDist(mbr2, v1), zero2 = dist.minDist(mbr2, v2);
    assertEquals("Not same: " + dist.toString(), exact, mind, 1e-10);
    assertTrue("Not smaller:" + dist.toString() + " " + mbrd + " > " + exact + " " + mbr2 + " " + v1, mbrd <= exact);
    assertTrue("Not smaller:" + dist.toString() + " " + mbrd2 + " > " + exact + " " + mbr2 + " " + v1, mbrd2 <= exact);
    assertEquals("Not zero: " + dist.toString(), 0., zero, 1e-10);
    assertEquals("Not zero: " + dist.toString(), 0., zero2, 1e-10);
  }
}
