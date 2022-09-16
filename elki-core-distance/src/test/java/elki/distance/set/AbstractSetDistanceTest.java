/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import static org.junit.Assert.assertEquals;

import elki.data.BitVector;
import elki.data.FeatureVector;
import elki.distance.AbstractDistanceTest;
import elki.distance.NumberVectorDistance;
import elki.distance.PrimitiveDistance;

/**
 * Abstract base test for set distances.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public abstract class AbstractSetDistanceTest extends AbstractDistanceTest {
  /**
   * BitVectors for Set testing
   */
  static final BitVector[] VECTORS = { new BitVector(new long[] { 0b11100_00000L }, 10), // a
      new BitVector(new long[] { 0b00011_11111L }, 10), // b
      new BitVector(new long[] { 0b10101_01010L }, 10), // c
      new BitVector(new long[] { 0b11111_11111L }, 10), // d
      new BitVector(new long[] { 0b00000_00000L }, 10) }; // e

  /**
   * Test setup pairs
   */
  static final BitVector[][] TESTS = { //
      { VECTORS[0], VECTORS[1] }, //
      { VECTORS[2], VECTORS[3] }, //
      { VECTORS[2], VECTORS[4] }, //
      { VECTORS[0], VECTORS[2] }, //
      { VECTORS[1], VECTORS[3] }, //
      { VECTORS[4], VECTORS[4] } };

  /**
   * IntegerVector for Set testing
   */
  static final FeatureVectorStub[] FEATVECTORS = { //
      new FeatureVectorStub(new int[] { 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 }), // a
      new FeatureVectorStub(new int[] { 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }), // b
      new FeatureVectorStub(new int[] { 1, 0, 1, 0, 1, 0, 1, 0, 1, 0 }), // c
      new FeatureVectorStub(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }), // d
      new FeatureVectorStub(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }), // e
      new FeatureVectorStub(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }), // f
      new FeatureVectorStub(new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0 }) }; // g

  /**
   * Test setup pairs
   */
  static final FeatureVectorStub[][] FEATTESTS = { //
      { FEATVECTORS[0], FEATVECTORS[1] }, //
      { FEATVECTORS[2], FEATVECTORS[3] }, //
      { FEATVECTORS[2], FEATVECTORS[4] }, //
      { FEATVECTORS[0], FEATVECTORS[2] }, //
      { FEATVECTORS[1], FEATVECTORS[3] }, //
      { FEATVECTORS[4], FEATVECTORS[4] } };

  /**
   * Tests the given Distance function using Bitvectors
   * 
   * @param distfunc the distance function to test
   * @param expected An array of results for the given Testcases
   * @param tolerance tolerance used for floating point distances
   */
  public static void assertBitVectorDistances(PrimitiveDistance<? super BitVector> distfunc, double[] expected, double tolerance) {
    for(int i = 0; i < TESTS.length; i++) {
      assertEquals("BitVector distance #" + i, expected[i], distfunc.distance(TESTS[i][0], TESTS[i][1]), tolerance);
    }
  }

  /**
   * Tests the given number vector distance function using DoubleVectors
   *
   * @param distfunc the distance function to test
   * @param expected An array of results for the given Testcases
   * @param tolerance tolerance used for floating point distances
   */
  public static void assertNumberVectorDistances(NumberVectorDistance<?> distfunc, double[] expected, double tolerance) {
    for(int i = 0; i < TESTS.length; i++) {
      assertEquals("NumberVector distance #" + i, expected[i], distfunc.distance(TESTS[i][0], TESTS[i][1]), tolerance);
    }
  }

  /**
   * Tests the given distance function using feature vectors
   *
   * @param distfunc the distance function to test
   * @param results An array of results for the given Testcases
   * @param tolerance tolerance used for floating point distances
   */
  public static void assertFeatureVectorDistances(PrimitiveDistance<? super FeatureVector<?>> distfunc, double[] results, double tolerance) {
    for(int i = 0; i < TESTS.length; i++) {
      assertEquals("FeatureVector distance #" + i, results[i], distfunc.distance(FEATTESTS[i][0], FEATTESTS[i][1]), tolerance);
    }
  }

  /**
   * Tests the given distance function with varying length
   * 
   * @param distfunc the distance function to test
   * @param result expected value
   * @param tolerance tolerance used for floating point distances
   */
  public static void assertIntegerVectorVarLen(PrimitiveDistance<? super FeatureVector<?>> distfunc, double result, double tolerance) {
    assertEquals("Varying length distances", result, distfunc.distance(FEATVECTORS[6], FEATVECTORS[5]), tolerance);
    assertEquals("Varying length distances", result, distfunc.distance(FEATVECTORS[5], FEATVECTORS[6]), tolerance);
  }

  /**
   * Stub class for testing purposes only.
   *
   * @author Erich Schubert
   */
  public static class FeatureVectorStub implements FeatureVector<Integer> {
    /**
     * Actual values
     */
    int[] values;

    /**
     * Constructor.
     *
     * @param values
     */
    public FeatureVectorStub(int[] values) {
      this.values = values;
    }

    @Override
    public int getDimensionality() {
      return values.length;
    }

    @Override
    public Integer getValue(int dimension) {
      return values[dimension];
    }
  }
}
