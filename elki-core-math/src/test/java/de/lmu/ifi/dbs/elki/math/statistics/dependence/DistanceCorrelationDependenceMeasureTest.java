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
 * Validate dCor by comparing to output of the official R version.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DistanceCorrelationDependenceMeasureTest {
  double[][] data = { //
      { 1, 2, 3, 4 }, //
      { 1, 3, 5, 7 }, //
      { 4, 3, 2, 1 }, //
      { 1, 4, 2, 3 }, //
      { 1, 0, 0, 1 }, //
      { 0, 0, 0, 0 } };

  double[][] R = { //
      { 1. }, //
      { 1., 1. }, //
      { 1., 1., 1. }, //
      { 0.7337994, 0.7337994, 0.7337994, 1. }, //
      { 0.5266404, 0.5266404, 0.5266404, 0.5266404, 1 }, //
      { 0., 0., 0., 0., 0., 0. }, //
  };

  @Test
  public void testBasic() {
    DistanceCorrelationDependenceMeasure dCor = DistanceCorrelationDependenceMeasure.STATIC;
    // This will become better with data size.
    AbstractDependenceMeasureTest.checkPerfectLinear(dCor, 1000, 1, 1, 1e-13);
    AbstractDependenceMeasureTest.checkUniform(dCor, 1000, 1.0, 1e-15, 0.05, 0.01);
  }

  @Test
  public void testR() {
    DistanceCorrelationDependenceMeasure dCor = DistanceCorrelationDependenceMeasure.STATIC;
    for(int i = 0; i < data.length; i++) {
      for(int j = 0; j <= i; j++) {
        double dcor = dCor.dependence(data[i], data[j]);
        assertEquals("dCor does not match for " + i + "," + j, R[i][j], dcor, 1e-7);
      }
    }
    // Bulk computation
    double[] mat = dCor.dependence(DoubleArrayAdapter.STATIC, Arrays.asList(data));
    for(int i = 0, c = 0; i < data.length; i++) {
      for(int j = 0; j < i; j++) {
        double co = mat[c++];
        assertEquals("dCor does not match for " + i + "," + j, R[i][j], co, 1e-7);
      }
    }
  }
}
