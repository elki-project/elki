package de.lmu.ifi.dbs.elki.math;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

public class TestMathUtil implements JUnit4Test {

  @Test
  public void testPearsonCorrelation() {
    final int size = 1000;
    final long seed = 1;
    double[] data1 = new double[size];
    double[] data2 = new double[size];
    double[] weight1 = new double[size];
    double[] weight2 = new double[size];

    Random r = new Random(seed);
    for (int i = 0; i < size; i++) {
      data1[i] = r.nextDouble();
      data2[i] = r.nextDouble();
      weight1[i] = 1.0;
      weight2[i] = 0.1;
    }
    
    double pear = MathUtil.pearsonCorrelationCoefficient(data1, data2);
    double wpear1 = MathUtil.weightedPearsonCorrelationCoefficient(data1, data2, weight1);
    double wpear2 = MathUtil.weightedPearsonCorrelationCoefficient(data1, data2, weight2);
    assertEquals("Pearson and weighted pearson should be the same with constant weights.", pear, wpear1, 1E-10);
    assertEquals("Weighted pearsons should be the same with constant weights.", wpear1, wpear2, 1E-10);
  }
}
