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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Validate jensen shannon dependence.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class JensenShannonEquiwidthDependenceMeasureTest {
  double[][] data = { //
      { 1, 2, 3, 4 }, //
      { 1, 3, 5, 7 }, //
      { 4, 3, 2, 1 }, //
      { 1, 4, 2, 3 }, //
      { 1, 0, 0, 1 }, //
      { 0, 1, 1, 1 }, //
  };

  // Regression testing
  final static double HH = 0.299925222;

  // Regression testing
  final static double H4 = 0.773079609;

  double[][] manual = { //
      { 1. }, //
      { 1., 1. }, //
      { 1., 1., 1. }, //
      { 0., 0., 0., 1. }, //
      { 0., 0., 0., 0., 1 }, //
      { HH, HH, HH, HH, HH, H4 }, //
  };

  @Test
  public void testJS() {
    DependenceMeasure mi = JensenShannonEquiwidthDependenceMeasure.STATIC;
    // Single computations
    for(int i = 0; i < data.length; i++) {
      for(int j = 0; j <= i; j++) {
        double co = mi.dependence(data[i], data[j]);
        assertEquals("JD does not match for " + i + "," + j, manual[i][j], co, 1e-7);
      }
    }
    // Bulk computation
    double[] mat = mi.dependence(DoubleArrayAdapter.STATIC, Arrays.asList(data));
    for(int i = 0, c = 0; i < data.length; i++) {
      for(int j = 0; j < i; j++) {
        double co = mat[c++];
        assertEquals("JD does not match for " + i + "," + j, manual[i][j], co, 1e-7);
      }
    }
  }
  
  @Test
  public void testBasic() {
    DependenceMeasure mi = JensenShannonEquiwidthDependenceMeasure.STATIC;
    // This will become better with data size.
    AbstractDependenceMeasureTest.checkPerfectLinear(mi, 1000, 0.938, 0.938, 0.001);
    AbstractDependenceMeasureTest.checkUniform(mi, 1000, 0.998, 0.001, 0.267, 0.001);
  }
}
