package de.lmu.ifi.dbs.elki.math;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import java.util.Random;

import org.junit.Test;

/**
 * Unit test for pearson correlation.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class PearsonCorrelationTest {
  @Test
  public void testPearsonCorrelation() {
    final int size = 1000;
    final long seed = 1L;
    double[] data1 = new double[size];
    double[] data2 = new double[size];
    double[] weight1 = new double[size];
    double[] weight2 = new double[size];

    Random r = new Random(seed);
    for(int i = 0; i < size; i++) {
      data1[i] = r.nextDouble();
      data2[i] = r.nextDouble();
      weight1[i] = 1.0;
      weight2[i] = 0.1;
    }

    double pear = PearsonCorrelation.coefficient(data1, data2);
    double wpear1 = PearsonCorrelation.weightedCoefficient(data1, data2, weight1);
    double wpear2 = PearsonCorrelation.weightedCoefficient(data1, data2, weight2);
    assertEquals("Pearson and weighted pearson should be the same with constant weights.", pear, wpear1, 1E-10);
    assertEquals("Weighted pearsons should be the same with constant weights.", wpear1, wpear2, 1E-10);
  }
}
