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
package de.lmu.ifi.dbs.elki.math.statistics.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Validate the two-sample anderson darling test.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class StandardizedTwoSampleAndersonDarlingTestTest {
  // Table 4, Scholz and Stephens (1987, p.922)
  double[][] pairs = { //
  { 38.7, 41.5, 43.8, 44.5, 45.5, 46.0, 47.7, 58.0 },//
  { 39.2, 39.3, 39.7, 41.4, 41.8, 42.9, 43.3, 45.8 },//
  { 34.0, 35.0, 39.0, 40.0, 43.0, 43.0, 44.0, 45.0 },//
  { 34.0, 34.8, 34.8, 35.4, 37.2, 37.8, 41.2, 42.8 }, //
  };

  // Reference values, as computed by SciPy:
  double[][] pairs_py = { //
  { 1.7183096744360862, 1.9909419068355798, 6.1292029931792227 }, //
  { -0.36399357810365712, 3.9832882398990201 }, //
  { 1.1838143230001976 }, //
  };

  @Test
  public void testTwoSampleAndersonDarlingTest() {
    StandardizedTwoSampleAndersonDarlingTest t = StandardizedTwoSampleAndersonDarlingTest.STATIC;
    for(int i = 0; i < pairs.length; i++) {
      for(int j = i + 1; j < pairs.length; j++) {
        final double exp = pairs_py[i][j - i - 1];
        double A2 = t.deviation(new double[][] { pairs[i].clone(), pairs[j].clone() });
        assertEquals("k-sample A2 does not match for " + i + " " + j, exp, A2, 1e-14);
        double A2b = t.deviation(pairs[i].clone(), pairs[j].clone());
        assertEquals("2-sample A2 does not match for " + i + " " + j, exp, A2b, 1e-14);
      }
    }
    double A2 = t.unstandardized(pairs);
    assertEquals("A2 does not match Scholz&Stephens", 8.3926, A2, 1e-5);
    double K = t.deviation(pairs);
    assertEquals("K does not match Scholz&Stephens", 4.480, K, 1e-3);
    assertEquals("K does not match SciPy", 4.4797806271353506, K, 1e-13);
  }
}
