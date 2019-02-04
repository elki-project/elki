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
package de.lmu.ifi.dbs.elki.math;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Unit test for pearson correlation.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class PearsonCorrelationTest {

  @Test
  public void testPearsonCorrelation() {
    final int size = 1000;
    final double WEIGHT1 = 1.0, WEIGHT2 = 0.1;
    final long seed = 1L;
    double[] data1 = new double[size];
    double[] data2 = new double[size];
    double[] weight1 = new double[size];
    double[] weight2 = new double[size];

    Random r = new Random(seed);
    for(int i = 0; i < size; i++) {
      data1[i] = r.nextDouble();
      data2[i] = r.nextDouble();
      weight1[i] = WEIGHT1;
      weight2[i] = WEIGHT2;
    }

    double pear = PearsonCorrelation.coefficient(data1, data2);
    double wpear1 = PearsonCorrelation.weightedCoefficient(data1, data2, weight1);
    double wpear2 = PearsonCorrelation.weightedCoefficient(data1, data2, weight2);
    double pear2 = PearsonCorrelation.coefficient(new NV(data1), new NV(data2));
    double wpear3 = PearsonCorrelation.weightedCoefficient(new NV(data1), new NV(data2), weight1);
    double wpear4 = PearsonCorrelation.weightedCoefficient(new NV(data1), new NV(data2), new NV(weight2));
    assertEquals("Pearson and weighted pearson should be the same with constant weights.", pear, wpear1, 1E-15);
    assertEquals("Weighted pearsons should be the same with constant weights.", wpear1, wpear2, 1E-15);
    assertEquals("NumberVector version should yield same result.", pear, pear2, 1E-15);
    assertEquals("NumberVector version should yield same result.", wpear1, wpear3, 1E-15);
    assertEquals("NumberVector version should yield same result.", wpear2, wpear4, 1E-15);

    // Test incremental
    PearsonCorrelation pc0 = new PearsonCorrelation();
    PearsonCorrelation wpc1 = new PearsonCorrelation();
    PearsonCorrelation wpc2 = new PearsonCorrelation();
    for(int i = 0; i < size; i++) {
      pc0.put(data1[i], data2[i]);
      wpc1.put(data1[i], data2[i], weight1[i]);
      wpc2.put(data1[i], data2[i], weight2[i]);
    }
    assertEquals("Count", size, pc0.getCount(), 0.);
    assertEquals("Count", size * WEIGHT1, wpc1.getCount(), 0.);
    assertEquals("Count", size * WEIGHT2, wpc2.getCount(), 1e-13 * size * WEIGHT2);
    assertEquals("Batch and incremental should be the same", pear, pc0.getCorrelation(), 0.);
    assertEquals("Pearson and weighted pearson should be the same with constant weights.", pc0.getCorrelation(), wpc1.getCorrelation(), 1E-15);
    assertEquals("Weighted pearsons should be the same with constant weights.", wpc1.getCorrelation(), wpc2.getCorrelation(), 1E-15);
    assertEquals("Batch and incremental should be the same", wpear1, wpc1.getCorrelation(), 0.);
    assertEquals("Batch and incremental should be the same", wpear2, wpc2.getCorrelation(), 0.);

    // Reset, and rerun.
    pc0.reset();
    assertEquals("Count", 0., pc0.getCount(), 0.);
    for(int i = 0; i < size; i++) {
      pc0.put(data1[i], data2[i]);
    }
    assertEquals("Count", size, pc0.getCount(), 0.);
    assertEquals("Batch and incremental should be the same", pear, pc0.getCorrelation(), 0.);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLengthDifference() {
    PearsonCorrelation.coefficient(new double[1], new double[2]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLengthDifference2() {
    PearsonCorrelation.weightedCoefficient(new double[1], new double[1], new double[2]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLengthDifference3() {
    PearsonCorrelation.weightedCoefficient(new double[1], new double[2], new double[1]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLengthDifference4() {
    PearsonCorrelation.coefficient(new NV(new double[1]), new NV(new double[2]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLengthDifference5() {
    PearsonCorrelation.weightedCoefficient(new NV(new double[1]), new NV(new double[1]), new NV(new double[2]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLengthDifference6() {
    PearsonCorrelation.weightedCoefficient(new NV(new double[1]), new NV(new double[2]), new NV(new double[1]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLengthDifference7() {
    PearsonCorrelation.weightedCoefficient(new NV(new double[1]), new NV(new double[1]), new double[2]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLengthDifference8() {
    PearsonCorrelation.weightedCoefficient(new NV(new double[1]), new NV(new double[2]), new double[1]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroLength() {
    PearsonCorrelation.coefficient(new double[0], new double[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroLength2() {
    PearsonCorrelation.weightedCoefficient(new double[0], new double[0], new double[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroLength3() {
    PearsonCorrelation.coefficient(new NV(new double[0]), new NV(new double[0]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroLength4() {
    PearsonCorrelation.weightedCoefficient(new NV(new double[0]), new NV(new double[0]), new NV(new double[0]));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testZeroLength5() {
    PearsonCorrelation.weightedCoefficient(new NV(new double[0]), new NV(new double[0]), new double[0]);
  }

  @Test
  public void testPearsonCorrelationToy() {
    final int size = 1001; // Keep odd.
    double[] data1 = new double[size];
    double[] data2 = new double[size];

    for(int i = 0; i < size; i++) {
      data1[i] = i;
      data2[i] = size - i;
    }

    double pear = PearsonCorrelation.coefficient(data1, data2);
    assertEquals("Pearson must be -1.", -1, pear, 0.);

    // Test incremental
    PearsonCorrelation pc0 = new PearsonCorrelation();
    for(int i = 0; i < size; i++) {
      pc0.put(data1[i], data2[i]);
    }
    assertEquals("Pearson must be -1.", -1, pc0.getCorrelation(), 0.);
    assertEquals("Mean X", (size - 1) * 0.5, pc0.getMeanX(), 0.);
    assertEquals("Mean Y", (size + 1) * 0.5, pc0.getMeanY(), 0.);
    assertEquals("Count", size, pc0.getCount(), 0.);

    assertEquals("Size should be kept odd.", 1, size & 1);
    int half = size >> 1;
    double var = half * (half + 1) * (2 * half + 1) / 3. / size;
    double svar = var * size / (size - 1);
    double std = Math.sqrt(var);
    double sstd = Math.sqrt(svar);
    assertEquals("Stddev X", std, pc0.getNaiveStddevX(), 0.);
    assertEquals("Var X", var, pc0.getNaiveVarianceX(), 0.);
    assertEquals("Stddev Y", std, pc0.getNaiveStddevY(), 0.);
    assertEquals("Var Y", var, pc0.getNaiveVarianceY(), 0.);
    assertEquals("Stddev X", sstd, pc0.getSampleStddevX(), 0.);
    assertEquals("Var X", svar, pc0.getSampleVarianceX(), 0.);
    assertEquals("Stddev Y", sstd, pc0.getSampleStddevY(), 0.);
    assertEquals("Var Y", svar, pc0.getSampleVarianceY(), 0.);
  }

  private static class NV implements NumberVector {
    double[] data;

    public NV(double[] data) {
      this.data = data;
    }

    @Override
    public int getDimensionality() {
      return data.length;
    }

    @Override
    public double doubleValue(int dimension) {
      return data[dimension];
    }

    @Override
    public long longValue(int dimension) {
      return (long) data[dimension];
    }

    @Override
    public double[] toArray() {
      return data;
    }
  }
}
