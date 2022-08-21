/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.utilities.datastructures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * Test the Kuhn-Munkres implementation.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class KuhnMunkresTest {
  // Test case that requires multiple steps:
  protected static final double[][] TEST1 = { //
      { 18, 15, 15, 16 }, //
      { 7, 17, 11, 13 }, //
      { 25, 19, 18, 21 }, //
      { 9, 22, 19, 23 } };

  // Test case with 0 rows and columns from German Wikipedia
  protected static final double[][] TEST2 = { //
      { 0, 0, 0, 0 }, //
      { 0, 1, 3, 3 }, //
      { 0, 5, 5, 9 }, //
      { 0, 1, 3, 7 } };

  // Non-square test case.
  protected static final double[][] NONSQUARE = { //
      { 797690, 306449, 993296, 351623, 702639, 758851, }, //
      { 733184, 797690, 194140, 248635, 851663, 858445, }, //
      { 941561, 334640, 797690, 781242, 937382, 87235, }, //
      { 279247, 706878, 114174, 797690, 49694, 547076, } };

  protected static final double[][] DIFFICULT = { //
      { 0.72245755, 0.34725406, 0.81567248, 0.86704067, 0.76755449, 0.25860308, 0.34417351, 0.71210787 }, //
      { 0.34036014, 0.54538063, 0.88588854, 0.89788171, 0.15486295, 0.93155998, 0.62333656, 0.53914451 }, //
      { 0.86768289, 0.12719514, 0.31435056, 0.30410005, 0.89211683, 0.69675164, 0.33070014, 0.78154677 }, //
      { 0.85259999, 0.63646429, 0.25444321, 0.19521635, 0.23417946, 0.65892204, 0.23357477, 0.64560555 }, //
      { 0.80210380, 0.83639900, 0.39717465, 0.91607537, 0.79338972, 0.56402239, 0.63513825, 0.89268620 }, //
      { 0.71523583, 0.76968727, 0.95870725, 0.81618360, 0.82441030, 0.91969921, 0.47540027, 0.90974405 }, //
      { 0.00724754, 0.64744549, 0.73557621, 0.40753575, 0.83164675, 0.76696662, 0.83774546, 0.64684458 }, //
      { 0.00047284, 0.02411942, 0.21587758, 0.68046110, 0.30001845, 0.18509392, 0.47433648, 0.81621894 } };

  protected static final double[][] DIFFICULT2 = { //
      { 0.73240108, 0.52782608, 0.51265870, 0.03395640, 0.67417532 }, //
      { 0.56683783, 0.12893919, 0.70445984, 0.79098418, 0.00792179 }, //
      { 0.31942814, 0.63096292, 0.78731553, 0.29131024, 0.43910240 }, //
      { 0.06770240, 0.21805831, 0.11953815, 0.93230284, 0.96110592 }, //
      { 0.72311956, 0.39931389, 0.60570097, 0.09342243, 0.99029870 } };

  @Test
  public void test1() {
    int[] assignment = new KuhnMunkres().run(TEST1);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += TEST1[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 55, sum, 0);
  }

  @Test
  public void test2() {
    int[] assignment = new KuhnMunkres().run(TEST2);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += TEST2[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 4, sum, 0);
  }

  @Test
  public void testNonSq() {
    int[] assignment = new KuhnMunkres().run(NONSQUARE);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += NONSQUARE[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 637518, sum, 0);
  }

  @Test
  public void testDifficult() {
    int[] assignment = new KuhnMunkres().run(DIFFICULT);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += DIFFICULT[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 2.24, sum, 1e-4);
  }

  @Test
  public void testDifficult2() {
    int[] assignment = new KuhnMunkres().run(DIFFICULT2);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += DIFFICULT2[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 0.8802, sum, 1e-4);
  }

  @Test
  public void testLarge() {
    long seed = 0L;
    Random rnd = new Random(seed);
    double[][] mat = new double[100][100];
    for(int i = 0; i < mat.length; i++) {
      double[] row = mat[i];
      for(int j = 0; j < row.length; j++) {
        row[j] = Math.abs(rnd.nextDouble());
      }
    }
    int[] assignment = new KuhnMunkres().run(mat);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += mat[i][assignment[i]];
    }
    if(seed == 0) {
      if(mat.length == 10 && mat[0].length == 10) {
        assertEquals("sum", 1.467733381753002, sum, 1e-8);
        // Duration: 0.008426656000000001
      }
      if(mat.length == 100 && mat[0].length == 100) {
        assertEquals("sum", 1.5583906418867581, sum, 1e-8);
        // Duration: 0.037667963000000006
      }
      if(mat.length == 1000 && mat[0].length == 1000) {
        assertEquals("sum", 1.6527526146559663, sum, 1e-8);
        // Duration: 37.682129663000005
      }
      if(mat.length == 10000 && mat[0].length == 10000) {
        assertEquals("sum", 1.669458072091596, sum, 1e-8);
        // Duration: too long
      }
    }
  }
}
