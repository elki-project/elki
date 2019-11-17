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
package elki.utilities.datastructures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * Test the Kuhn-Munkres implementation.
 *
 * @author Erich Schubert
 */
public class KuhnMunkresWongTest {
  @Test
  public void test1() {
    int[] assignment = new KuhnMunkresWong().run(KuhnMunkresTest.TEST1);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += KuhnMunkresTest.TEST1[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 55, sum, 0);
  }

  @Test
  public void test2() {
    int[] assignment = new KuhnMunkresWong().run(KuhnMunkresTest.TEST2);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += KuhnMunkresTest.TEST2[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 4, sum, 0);
  }

  @Test
  public void testNonSq() {
    int[] assignment = new KuhnMunkresWong().run(KuhnMunkresTest.NONSQUARE);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += KuhnMunkresTest.NONSQUARE[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 637518, sum, 0);
  }

  @Test
  public void testDifficult() {
    int[] assignment = new KuhnMunkresWong().run(KuhnMunkresTest.DIFFICULT);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += KuhnMunkresTest.DIFFICULT[i][assignment[i]];
    }
    assertEquals("Assignment not optimal", 2.24, sum, 1e-4);
  }

  @Test
  public void testDifficult2() {
    int[] assignment = new KuhnMunkresWong().run(KuhnMunkresTest.DIFFICULT2);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += KuhnMunkresTest.DIFFICULT2[i][assignment[i]];
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
    int[] assignment = new KuhnMunkresWong().run(mat);
    double sum = 0.;
    for(int i = 0; i < assignment.length; i++) {
      assertTrue("Unassigned row " + i, assignment[i] >= 0);
      sum += mat[i][assignment[i]];
    }
    if(seed == 0) {
      if(mat.length == 10 && mat[0].length == 10) {
        assertEquals("sum", 1.467733381753002, sum, 1e-8);
        // Duration: 0.007970609
      }
      if(mat.length == 100 && mat[0].length == 100) {
        assertEquals("sum", 1.5583906418867581, sum, 1e-8);
        // Duration: 0.015696813
      }
      if(mat.length == 1000 && mat[0].length == 1000) {
        assertEquals("sum", 1.6527526146559663, sum, 1e-8);
        // Duration: 0.8892345580000001
      }
      if(mat.length == 10000 && mat[0].length == 10000) {
        assertEquals("sum", 1.669458072091596, sum, 1e-8);
        // Duration: 3035.95495334
      }
    }
  }
}
