package de.lmu.ifi.dbs.elki.math;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
    for(int i = 0; i < size; i++) {
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

  @Test
  public void testBitMath() {
    assertEquals("Bit math issues", 1024, MathUtil.nextPow2Int(912));
    assertEquals("Bit math issues", 8, MathUtil.nextPow2Int(5));
    assertEquals("Bit math issues", 4, MathUtil.nextPow2Int(4));
    assertEquals("Bit math issues", 4, MathUtil.nextPow2Int(3));
    assertEquals("Bit math issues", 2, MathUtil.nextPow2Int(2));
    assertEquals("Bit math issues", 1, MathUtil.nextPow2Int(1));
    assertEquals("Bit math issues", 0, MathUtil.nextPow2Int(0));
    assertEquals("Bit math issues", 1024L, MathUtil.nextPow2Long(912L));
    assertEquals("Bit math issues", 0, MathUtil.nextPow2Int(-1));
    assertEquals("Bit math issues", 0, MathUtil.nextPow2Int(-2));
    assertEquals("Bit math issues", 0, MathUtil.nextPow2Int(-99));
    assertEquals("Bit math issues", 15, MathUtil.nextAllOnesInt(8));
    assertEquals("Bit math issues", 7, MathUtil.nextAllOnesInt(4));
    assertEquals("Bit math issues", 3, MathUtil.nextAllOnesInt(3));
    assertEquals("Bit math issues", 3, MathUtil.nextAllOnesInt(2));
    assertEquals("Bit math issues", 1, MathUtil.nextAllOnesInt(1));
    assertEquals("Bit math issues", 0, MathUtil.nextAllOnesInt(0));
    assertEquals("Bit math issues", -1, MathUtil.nextAllOnesInt(-1));
    assertEquals("Bit math issues", 0, 0 >>> 1);
  }

  @Test
  public void testFloatToDouble() {
    Random r = new Random(1l);
    for(int i = 0; i < 10000; i++) {
      final double dbl = Double.longBitsToDouble(r.nextLong());
      final float flt = (float) dbl;
      final double uppd = MathUtil.floatToDoubleUpper(flt);
      final float uppf = (float) uppd;
      final double lowd = MathUtil.floatToDoubleLower(flt);
      final float lowf = (float) lowd;
      assertTrue("Expected value to become larger, but " + uppd + " < " + dbl, uppd >= dbl || Double.isNaN(dbl));
      assertTrue("Expected value to round to the same float.", flt == uppf || Double.isNaN(flt));
      assertTrue("Expected value to become smaller, but " + lowd + " > " + dbl, lowd <= dbl || Double.isNaN(dbl));
      assertTrue("Expected value to round to the same float.", flt == lowf || Double.isNaN(flt));
    }
  }
  
  @Test
  public void testAngle() {
    Random r = new Random(1l);
    int dim = 10;
    for (int i = 0; i < 10000; i++) {
      double[] r1 = new double[dim]; 
      double[] r2 = new double[dim];
      for (int d = 0; d < dim; d++) {
        r1[d] = r.nextDouble();
        r2[d] = r.nextDouble();
      }
      Vector v1 = new Vector(r1);
      Vector v2 = new Vector(r2);
      double a1 = v1.transposeTimes(v2) / (v1.euclideanLength() * v2.euclideanLength());
      double a2 = MathUtil.angle(v1, v2);
      assertEquals("Angle computation incorrect.", a1, a2, 1E-10);
    }
  }
}