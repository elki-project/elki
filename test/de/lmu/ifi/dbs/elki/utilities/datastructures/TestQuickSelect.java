package de.lmu.ifi.dbs.elki.utilities.datastructures;

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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;

/**
 * Test the QuickSelect math class.
 * 
 * @author Erich Schubert
 */
public class TestQuickSelect implements JUnit4Test {
  /**
   * Array size to use.
   */
  final int SIZE = 10000;

  @Test
  public void testRandomDoubles() {
    for(int i = 1; i < 10; i++) {
      testQuickSelect(i);
    }
    testQuickSelect(SIZE);
    testQuickSelect(SIZE + 1);
  }

  private void testQuickSelect(int size) {
    double[] data = new double[size];
    double[] test;

    // Make a random generator, but remember the seed for debugging.
    Random r = new Random();
    long seed = r.nextLong();
    r = new Random(seed);

    // Produce data
    for(int i = 0; i < size; i++) {
      data[i] = r.nextDouble();
    }
    // Duplicate for reference, sort.
    test = Arrays.copyOf(data, size);
    Arrays.sort(test);

    // Run QuickSelect and compare with full sort
    // After a few iterations, the array will be largely sorted.
    // Better just run the whole test a few times, than doing too many
    // iterations here.
    for(int j = 0; j < 20; j++) {
      int q = r.nextInt(size);
      double r1 = QuickSelect.quickSelect(data, q);
      double r2 = test[q];
      assertEquals("QuickSelect returned incorrect element. Seed=" + seed, r2, r1, Double.MIN_VALUE);
    }
    double med = QuickSelect.median(data);
    if(size % 2 == 1) {
      double met = test[(size - 1) / 2];
      assertEquals("QuickSelect returned incorrect median. Seed=" + seed, met, med, Double.MIN_VALUE);
    }
    else {
      double met = (test[(size - 1) / 2] + test[(size + 1) / 2]) / 2;
      assertEquals("QuickSelect returned incorrect median. Seed=" + seed, med, met, Double.MIN_VALUE);
    }
    double qua = QuickSelect.quantile(data, 0.5);
    assertEquals("Median and 0.5 quantile do not agree. Seed=" + seed, med, qua, 1E-15);
  }

  @Test
  public void testPartialArray() {
    double data[] = new double[] { 0.1, 0.2, 1, 2, 3, 0.9, 0.95 };
    assertEquals("Partial median incorrect.", 1, QuickSelect.median(data, 2, 3), Double.MIN_VALUE);
    assertEquals("Partial median incorrect.", 3, QuickSelect.median(data, 4, 5), Double.MIN_VALUE);
    // Note: do not change the order, since this modifies the array.
    assertEquals("Partial median incorrect.", 2, QuickSelect.median(data, 2, 5), Double.MIN_VALUE);
    // Note: do not change the order, since this modifies the array.
    assertEquals("Full median incorrect.", 0.95, QuickSelect.median(data), Double.MIN_VALUE);
  }

  @Test
  public void testTies() {
    double data[] = new double[] { 0.1, 0.1, 0.9, 0.9, 0.5, 0.9, 0.1, 0.1, 0.1, 0.9, 0.9, 0.9, 0.9, 0.1, 0.1 };
    assertEquals("Full median incorrect.", 0.5, QuickSelect.median(data), Double.MIN_VALUE);
  }
}