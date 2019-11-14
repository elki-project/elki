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

import static org.junit.Assert.assertEquals;

import elki.data.BitVector;
import elki.data.FeatureVector;
import elki.data.IntegerVector;
import elki.distance.AbstractDistanceTest;
import elki.distance.IntegerFeatureVector;

public abstract class AbstractSetDistanceTest extends AbstractDistanceTest {
  /**
   * BitVectors for Set testing
   */
  final BitVector[] VECTORS = { new BitVector(new long[] { 0b11100_00000L }, 10), // a
      new BitVector(new long[] { 0b00011_11111L }, 10), // b
      new BitVector(new long[] { 0b10101_01010L }, 10), // c
      new BitVector(new long[] { 0b11111_11111L }, 10), // d
      new BitVector(new long[] { 0b00000_00000L }, 10) // e
  };

  /**
   * Test setup pairs
   */
  final BitVector[][] TESTS = { { VECTORS[0], VECTORS[1] }, { VECTORS[2], VECTORS[3] }, { VECTORS[2], VECTORS[4] }, { VECTORS[0], VECTORS[2] }, { VECTORS[1], VECTORS[3] }, { VECTORS[4], VECTORS[4] } };

  /**
   * IntegerFeatureVector for Set testing
   * 
   */
  final IntegerFeatureVector[] INTFEATVECTORS = { new IntegerFeatureVector(new int[] { 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 }), // a
      new IntegerFeatureVector(new int[] { 0, 0, 0, 1, 1, 1, 1, 1, 1, 1 }), // b
      new IntegerFeatureVector(new int[] { 1, 0, 1, 0, 1, 0, 1, 0, 1, 0 }), // c
      new IntegerFeatureVector(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }), // d
      new IntegerFeatureVector(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }), // e
      new IntegerFeatureVector(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }), // f
      new IntegerFeatureVector(new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0 }) // g
  };

  /**
   * Test setup pairs
   */
  final IntegerFeatureVector[][] INTFEATTESTS = { { INTFEATVECTORS[0], INTFEATVECTORS[1] }, { INTFEATVECTORS[2], INTFEATVECTORS[3] }, { INTFEATVECTORS[2], INTFEATVECTORS[4] }, { INTFEATVECTORS[0], INTFEATVECTORS[2] }, { INTFEATVECTORS[1], INTFEATVECTORS[3] }, { INTFEATVECTORS[4], INTFEATVECTORS[4] } };

  /**
   * Tests the given Distance function using Bitvectors
   * 
   * @param tolerance tolerance used for floating point distances
   * @param distfunc the distance function to test
   * @param results An array of results for the given Testcases
   */
  public void bitVectorSet(double tolerance, AbstractSetDistance<FeatureVector<?>> distfunc, double... results) {
    assertEquals(results[0], distfunc.distance(TESTS[0][0], TESTS[0][1]), tolerance);
    assertEquals(results[1], distfunc.distance(TESTS[1][0], TESTS[1][1]), tolerance);
    assertEquals(results[2], distfunc.distance(TESTS[2][0], TESTS[2][1]), tolerance);
    assertEquals(results[3], distfunc.distance(TESTS[3][0], TESTS[3][1]), tolerance);
    assertEquals(results[4], distfunc.distance(TESTS[4][0], TESTS[4][1]), tolerance);
    // assertEquals(results[5], distfunc.distance(TESTS[5][0],
    // TESTS[5][1]),tolerance);
  }

  /**
   * Tests the given Distance function using (Int)-Numbervectors
   * 
   * @param tolerance tolerance used for floating point distances
   * @param distfunc the distance function to test
   * @param results An array of results for the given Testcases
   */
  public void numberVectorSet(double tolerance, AbstractSetDistance<FeatureVector<?>> distfunc, double... results) {
    assertEquals(results[0], distfunc.distance(IntegerVector.STATIC.newNumberVector(TESTS[0][0]), IntegerVector.STATIC.newNumberVector(TESTS[0][1])), tolerance);
    assertEquals(results[1], distfunc.distance(IntegerVector.STATIC.newNumberVector(TESTS[1][0]), IntegerVector.STATIC.newNumberVector(TESTS[1][1])), tolerance);
    assertEquals(results[2], distfunc.distance(IntegerVector.STATIC.newNumberVector(TESTS[2][0]), IntegerVector.STATIC.newNumberVector(TESTS[2][1])), tolerance);
    assertEquals(results[3], distfunc.distance(IntegerVector.STATIC.newNumberVector(TESTS[3][0]), IntegerVector.STATIC.newNumberVector(TESTS[3][1])), tolerance);
    assertEquals(results[4], distfunc.distance(IntegerVector.STATIC.newNumberVector(TESTS[4][0]), IntegerVector.STATIC.newNumberVector(TESTS[4][1])), tolerance);
    // assertEquals(results[5],
    // distfunc.distance(IntegerVector.STATIC.newNumberVector(TESTS[5][0]),IntegerVector.STATIC.newNumberVector(TESTS[5][1])),tolerance);

  }

  /**
   * Tests the given Distance function using (Int)-Featurevectors
   * 
   * @param tolerance tolerance used for floating point distances
   * @param distfunc the distance function to test
   * @param results An array of results for the given Testcases
   */
  public void intFeatVectorSet(double tolerance, AbstractSetDistance<FeatureVector<?>> distfunc, double... results) {
    assertEquals(results[0], distfunc.distance(INTFEATTESTS[0][0], INTFEATTESTS[0][1]), tolerance);
    assertEquals(results[1], distfunc.distance(INTFEATTESTS[1][0], INTFEATTESTS[1][1]), tolerance);
    assertEquals(results[2], distfunc.distance(INTFEATTESTS[2][0], INTFEATTESTS[2][1]), tolerance);
    assertEquals(results[3], distfunc.distance(INTFEATTESTS[3][0], INTFEATTESTS[3][1]), tolerance);
    assertEquals(results[4], distfunc.distance(INTFEATTESTS[4][0], INTFEATTESTS[4][1]), tolerance);
    // assertEquals(results[5],
    // distfunc.distance(INTFEATTESTS[5][0],INTFEATTESTS[5][1]),tolerance);

  }

  /**
   * Tests the given Distance function using (Int)-Featurevectors
   * 
   * @param tolerance tolerance used for floating point distances
   * @param distfunc the distance function to test
   * @param results An array of results for the given Testcases
   */
  public void intFeatVectorSetVarLen(double tolerance, AbstractSetDistance<FeatureVector<?>> distfunc, double result) {
    assertEquals(result, distfunc.distance(INTFEATVECTORS[6], INTFEATVECTORS[5]), tolerance);
    assertEquals(result, distfunc.distance(INTFEATVECTORS[5], INTFEATVECTORS[6]), tolerance);

  }

}
