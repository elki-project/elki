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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * JUnit Test for the class {@link AffineTransformation}
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 */
public class AffineTransformationTest {
  /**
   * Test identity transform
   */
  @Test
  public void testIdentityTransform() {
    int testdim = 5;
    AffineTransformation t = new AffineTransformation(testdim);
    assertEquals(testdim, t.getDimensionality());
    double[][] tm = t.getTransformation();
    assertTrue("initial transformation matrix should be unity", VMath.almostEquals(tm, unitMatrix(testdim + 1)));

    // test application to a vector
    double[] dv = new double[testdim];
    for(int i = 0; i < testdim; i++) {
      dv[i] = i * i + testdim;
    }
    double[] v3 = t.apply(dv);
    assertArrayEquals("identity transformation wasn't identical", dv, v3, 0.);

    double[] v4 = t.applyInverse(dv);
    assertArrayEquals("inverse of identity wasn't identity", dv, v4, 0.);
  }

  /**
   * Test adding translation vectors
   */
  @Test
  public void testTranslation() {
    int testdim = 5;
    AffineTransformation t = new AffineTransformation(testdim);
    assertEquals("Dimensionality does not match.", testdim, t.getDimensionality());
    double[][] tm = t.getTransformation();
    assertTrue("initial transformation matrix should be unity", VMath.almostEquals(tm, unitMatrix(testdim + 1)));

    // translation vector
    double[] tv = new double[testdim];
    for(int i = 0; i < testdim; i++) {
      tv[i] = i + testdim;
    }
    t.addTranslation(tv);

    double[][] tm2 = t.getTransformation();
    // Manually do the same changes to the matrix tm
    for(int i = 0; i < testdim; i++) {
      tm[i][testdim] = i + testdim;
    }
    // Compare the results
    assertTrue("Translation wasn't added correctly to matrix.", VMath.almostEquals(tm, tm2));

    // test application to a vector
    double[] v1 = new double[testdim];
    double[] v2t = new double[testdim];
    for(int i = 0; i < testdim; i++) {
      v1[i] = i * i + testdim;
      v2t[i] = i * i + i + 2 * testdim;
    }

    double[] v1t = t.apply(v1);
    assertArrayEquals("Vector wasn't translated properly forward.", v2t, v1t, 0.);
    double[] v2b = t.applyInverse(v2t);
    assertArrayEquals("Vector wasn't translated properly backwards.", v1, v2b, 0.);
    double[] v1b = t.applyInverse(v1t);
    assertArrayEquals("Vector wasn't translated properly back and forward.", v1, v1b, 0.);

    // Translation
    double[] vd = minus(v1, v2b);
    double[] vtd = minus(v1t, v2t);
    assertArrayEquals("Translation changed vector difference.", vd, vtd, 0.);

    // Translation shouldn't change relative vectors.
    assertArrayEquals("Relative vectors weren't left unchanged by translation!", v1, t.applyRelative(v1), 0.);
    assertArrayEquals("Relative vectors weren't left unchanged by translation!", v2t, t.applyRelative(v2t), 0.);
    assertArrayEquals("Relative vectors weren't left unchanged by translation!", v1t, t.applyRelative(v1t), 0.);
    assertArrayEquals("Relative vectors weren't left unchanged by translation!", v2b, t.applyRelative(v2b), 0.);
  }

  /**
   * Test direct inclusion of matrices
   */
  @Test
  public void testMatrix() {
    int testdim = 5;
    int axis1 = 1;
    int axis2 = 3;

    assert (axis1 < testdim);
    assert (axis2 < testdim);
    // don't change the angle; we'll be using that executing the rotation
    // three times will be identity (approximately)
    double angle = Math.toRadians(360 / 3);
    AffineTransformation t = new AffineTransformation(testdim);
    assertTrue(t.getDimensionality() == testdim);
    double[][] tm = t.getTransformation();
    assertTrue("initial transformation matrix should be unity", VMath.almostEquals(tm, unitMatrix(testdim + 1)));

    // rotation matrix
    double[][] rm = new double[testdim][testdim];
    for(int i = 0; i < testdim; i++) {
      rm[i][i] = 1;
    }
    // add the rotation
    rm[axis1][axis1] = +Math.cos(angle);
    rm[axis1][axis2] = -Math.sin(angle);
    rm[axis2][axis1] = +Math.sin(angle);
    rm[axis2][axis2] = +Math.cos(angle);
    t.addMatrix(rm);
    double[][] tm2 = t.getTransformation();

    // We know that we didn't do any translations and tm is the unity matrix
    // so we can manually do the rotation on it, too.
    tm[axis1][axis1] = +Math.cos(angle);
    tm[axis1][axis2] = -Math.sin(angle);
    tm[axis2][axis1] = +Math.sin(angle);
    tm[axis2][axis2] = +Math.cos(angle);

    // Compare the results
    assertTrue("Rotation wasn't added correctly to matrix.", VMath.almostEquals(tm, tm2));

    // test application to a vector
    double[] v1 = new double[testdim];
    for(int i = 0; i < testdim; i++) {
      v1[i] = i * i + testdim;
    }
    double[] v2 = t.apply(v1);
    double[] v3 = t.applyInverse(v2);
    assertTrue("Forward-Backward didn't work correctly.", euclideanLength(minus(v1, v3)) < 0.0001);
    double[] v4 = t.apply(t.apply(t.apply(v1)));
    assertTrue("Triple-Rotation by 120 degree didn't work", euclideanLength(minus(v1, v4)) < 0.0001);

    // Rotation shouldn't disagree for relative vectors.
    // (they just are not affected by translation!)
    assertArrayEquals("Relative vectors were affected differently by pure rotation!", v2, t.applyRelative(v1), 0.);

    // should do the same as built-in rotation!
    AffineTransformation t2 = new AffineTransformation(testdim);
    t2.addRotation(axis1, axis2, angle);
    double[] t2v2 = t2.apply(v1);
    assertTrue("Manual rotation and AffineTransformation.addRotation disagree.", euclideanLength(minus(v2, t2v2)) < 0.0001);
  }

  /**
   * Test {@link AffineTransformation#reorderAxesTransformation}
   */
  @Test
  public void testReorder() {
    double[] v = new double[] { 3, 5, 7 };
    // all permutations
    double[] p1 = new double[] { 3, 5, 7 };
    double[] p2 = new double[] { 3, 7, 5 };
    double[] p3 = new double[] { 5, 3, 7 };
    double[] p4 = new double[] { 5, 7, 3 };
    double[] p5 = new double[] { 7, 3, 5 };
    double[] p6 = new double[] { 7, 5, 3 };
    double[][] ps = new double[][] {
        // with no arguments.
        p1,
        // with just one argument.
        p1, p3, p5,
        // with two arguments.
        p1, p2, p3, p4, p5, p6, };

    // index in reference array
    int idx = 0;
    // with 0 arguments
    {
      AffineTransformation aff = AffineTransformation.reorderAxesTransformation(v.length, new int[] {});
      double[] n = minusEquals(aff.apply(v), ps[idx]);
      assertEquals("Permutation " + idx + " doesn't match.", euclideanLength(n), 0.0, 0.001);
      idx++;
    }
    // with one argument
    for(int d1 = 1; d1 <= 3; d1++) {
      AffineTransformation aff = AffineTransformation.reorderAxesTransformation(v.length, new int[] { d1 });
      double[] n = minusEquals(aff.apply(v), ps[idx]);
      assertEquals("Permutation " + idx + " doesn't match.", euclideanLength(n), 0.0, 0.001);
      idx++;
    }
    // with two arguments
    for(int d1 = 1; d1 <= 3; d1++) {
      for(int d2 = 1; d2 <= 3; d2++) {
        if(d1 == d2) {
          continue;
        }
        AffineTransformation aff = AffineTransformation.reorderAxesTransformation(v.length, new int[] { d1, d2 });
        double[] n = minusEquals(aff.apply(v), ps[idx]);
        assertEquals("Permutation " + idx + " doesn't match.", euclideanLength(n), 0.0, 0.001);
        idx++;
      }
    }
  }
}
