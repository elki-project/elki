package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;

/**
 * Unit test for Absolute Pearson correlation distance.
 * 
 * @author Erich Schubert
 */
public class TestAbsoluteUncenteredCorrelationDistanceFunctionTest implements JUnit4Test {
  /** Inherited test data */
  final static DoubleVector[] TESTS = TestUncenteredCorrelationDistanceFunctionTest.TESTS;

  /** Note, these are not yet adjusted */
  final static double[][] SCORES = TestUncenteredCorrelationDistanceFunctionTest.SCORES;

  @Test
  public void testAbsoluteUncenteredCorrelation() {
    AbsoluteUncenteredCorrelationDistanceFunction f = AbsoluteUncenteredCorrelationDistanceFunction.STATIC;
    for(int i = 0; i < TESTS.length; i++) {
      for(int j = 0; j < TESTS.length; j++) {
        final double dist = f.distance(TESTS[i], TESTS[j]);
        final double r = 1. - SCORES[i][j];
        assertEquals("Distance does not agree: " + TESTS[i] + " <-> " + TESTS[j], 1. - Math.abs(r), dist, 1e-15);
      }
    }
  }
}
