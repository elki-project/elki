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
package de.lmu.ifi.dbs.elki.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Unit test for angle computations in ELKI.
 *
 * TODO: add numerically difficult test cases.
 *
 * TODO: add tests for the other functions as well.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class VectorUtilTest {
  @Test
  public void denseAngle() {
    NumberVector v1 = new DoubleVector(new double[] { 1.0, 2.0, 3.0 });
    NumberVector v2 = new FloatVector(new float[] { 3.f, 2.f, 1.f });
    NumberVector v3 = new ByteVector(new byte[] { 1, 2, 3, 0 });
    NumberVector v4 = new IntegerVector(new int[] { 3, 2, 1, 0 });
    // Exact: (3+4+3)/(1+4+9) = 0.7142857142857143
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v1, v1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v2, v2), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v1, v3), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v2, v4), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v3, v1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v4, v2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(v1, v2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(v1, v2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(v2, v1), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(v2, v1), 0.);
  }

  @Test
  public void angleDegenerate() {
    NumberVector o1 = new DoubleVector(new double[] { 0. });
    NumberVector o2 = new DoubleVector(new double[] {});
    NumberVector v1 = new DoubleVector(new double[] { 1.0 });
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o1, o1), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o1, o2), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o2, o2), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o2, v1), 0.);
  }

  @Test
  public void denseAngleOffset() {
    NumberVector v1 = new DoubleVector(new double[] { 1.0, 2.0, 3.0 });
    NumberVector v2 = new FloatVector(new float[] { 3.f, 2.f, 1.f });

    NumberVector o = new DoubleVector(new double[] { 0. });
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angle(v2, v1, o), 0.);

    // Exact: (0+1+0)/(0+1+4) = 0.2
    NumberVector one = new DoubleVector(new double[] { 1., 1., 1. });
    assertEquals("Angle not exact.", 0.2, VectorUtil.angle(v2, v1, one), 0.);
  }

  @Test
  public void sparseAngle() {
    SparseNumberVector s1 = new SparseDoubleVector(new double[] { 1.0, 2.0, 3.0 });
    SparseNumberVector s2 = new SparseFloatVector(new float[] { 3.f, 2.f, 1.f });
    // Exact: (3+4+3)/(1+4+9) = 0.7142857142857143
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(s1, s1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleSparse(s1, s1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleDense(s1, s1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(s2, s2), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleSparse(s2, s2), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleDense(s2, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(s1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleSparse(s1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(s1, s2), 0.);

    SparseNumberVector s3 = new SparseDoubleVector(new int[] { 1, 2, 3, 5, 7, 9 }, //
        new double[] { 1.0, -1.0, 2.0, 3.0, -2.0, -3.0 }, 100);
    SparseNumberVector s4 = new SparseFloatVector(new int[] { 1, 3, 4, 5, 6, 8 }, //
        new float[] { 3.f, 2.f, -2.f, 1.f, -1.f, -3.f }, 100);
    // Exact: (3+4+3)/(1+4+9+1+4+9) = 0.35714285714285715
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(s3, s3), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleDense(s3, s3), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleSparse(s3, s3), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(s4, s4), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleDense(s4, s4), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleSparse(s4, s4), 0.);
    assertEquals("Angle not exact.", 0.35714285714285715, VectorUtil.angleDense(s3, s4), 0.);
    assertEquals("Angle not exact.", 0.35714285714285715, VectorUtil.cosAngle(s3, s4), 0.);
    assertEquals("Angle not exact.", 0.35714285714285715, VectorUtil.angleSparse(s3, s4), 0.);
    assertEquals("Angle not exact.", 0.35714285714285715, VectorUtil.angleDense(s4, s3), 0.);
    assertEquals("Angle not exact.", 0.35714285714285715, VectorUtil.cosAngle(s4, s3), 0.);
    assertEquals("Angle not exact.", 0.35714285714285715, VectorUtil.angleSparse(s4, s3), 0.);
  }

  @Test
  public void sparseDenseAngle() {
    NumberVector d1 = new DoubleVector(new double[] { 1.0, 2.0, 3.0, 0.0 });
    NumberVector d2 = new FloatVector(new float[] { 3.f, 2.f, 1.f, 0.f });
    SparseNumberVector s1 = new SparseDoubleVector(new double[] { 1.0, 2.0, 3.0 });
    SparseNumberVector s2 = new SparseFloatVector(new float[] { 3.f, 2.f, 1.f });
    SparseNumberVector s3 = new SparseDoubleVector(new double[] { 1.0, 2.0, 3.0, 4.0 });
    SparseNumberVector s4 = new SparseFloatVector(new float[] { 3.f, 2.f, 1.f, 4.f });
    SparseNumberVector s5 = new SparseFloatVector(new float[] { 0.f, 0.f, 0.f, 0.f, 5.f });
    SparseNumberVector s6 = new SparseFloatVector(new float[] { });
    assertEquals("Angle not exact.", 1., VectorUtil.angleDense(s2, d2), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleSparseDense(s2, d2), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleDense(s1, d1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleSparseDense(s1, d1), 0.);
    // Exact: (3+4+3)/(1+4+9) = 0.7142857142857143
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(s1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleSparse(s1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(s1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(d1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(s2, d1), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleSparseDense(s2, d1), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(s1, d2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(s1, d2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleSparseDense(s1, d2), 0.);
    // Exact: (3+4+3+16)/(1+4+9+16) = 0.8666666666666667
    assertEquals("Angle not exact.", 0.8666666666666667, VectorUtil.cosAngle(s3, s4), 0.);
    assertEquals("Angle not exact.", 0.8666666666666667, VectorUtil.angleSparse(s3, s4), 0.);
    assertEquals("Angle not exact.", 0.8666666666666667, VectorUtil.angleDense(s3, s4), 0.);
    // Exact: (3+4+3)/sqrt((1+4+9)*(1+4+9+16)) = 0.4879500364742666
    assertEquals("Angle not exact.", 0.4879500364742666, VectorUtil.cosAngle(d1, s4), 0.);
    assertEquals("Angle not exact.", 0.4879500364742666, VectorUtil.angleDense(s4, d1), 0.);
    assertEquals("Angle not exact.", 0.4879500364742666, VectorUtil.angleSparseDense(s4, d1), 0.);
    assertEquals("Angle not exact.", 0.4879500364742666, VectorUtil.cosAngle(s3, d2), 0.);
    assertEquals("Angle not exact.", 0.4879500364742666, VectorUtil.angleDense(s3, d2), 0.);
    assertEquals("Angle not exact.", 0.4879500364742666, VectorUtil.angleSparseDense(s3, d2), 0.);
    // Note: this used to trigger a bug, sparse vectors with leading zeros.
    assertEquals("Angle not exact.", 0., VectorUtil.angleSparseDense(s5, d1), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.angleSparseDense(s5, d2), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.angleSparseDense(s6, d1), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.angleSparseDense(s6, d2), 0.);
  }

  @Test
  public void sparseAngleDegenerate() {
    NumberVector o1 = new SparseDoubleVector(new double[] {});
    Int2DoubleOpenHashMap v2 = new Int2DoubleOpenHashMap();
    v2.put(3, 0.);
    v2.put(4, 0.);
    v2.put(42, 0.);
    NumberVector o2 = new SparseDoubleVector(v2, 100);
    Int2DoubleOpenHashMap v3 = new Int2DoubleOpenHashMap();
    v3.put(15, 0.);
    v3.put(5, 1.);
    NumberVector v1 = new SparseDoubleVector(v3, 100);
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o1, o1), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o1, o2), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o2, o2), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o1, v1), 0.);
    assertEquals("Angle not exact.", 0., VectorUtil.cosAngle(o2, v1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v1, v1), 0.);
  }
}
