package de.lmu.ifi.dbs.elki.data;
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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/**
 * Unit test for angle computations in ELKI.
 *
 * TODO: add numerically difficult test cases.
 *
 * TODO: add tests for the other functions as well.
 *
 * @author Erich Schubert
 */
public class VectorUtilTest implements JUnit4Test {
  @Test
  public void denseAngle() {
    NumberVector v1 = new DoubleVector(new double[] { 1.0, 2.0, 3.0 });
    NumberVector v2 = new FloatVector(new float[] { 3.f, 2.f, 1.f });
    // Exact: (3+4+3)/(1+4+9) = 0.7142857142857143
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v1, v1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.cosAngle(v2, v2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(v1, v2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(v1, v2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(v2, v1), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(v2, v1), 0.);
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
  }

  @Test
  public void sparseDenseAngle() {
    NumberVector d1 = new DoubleVector(new double[] { 1.0, 2.0, 3.0 });
    NumberVector d2 = new FloatVector(new float[] { 3.f, 2.f, 1.f });
    SparseNumberVector s1 = new SparseDoubleVector(new double[] { 1.0, 2.0, 3.0 });
    SparseNumberVector s2 = new SparseFloatVector(new float[] { 3.f, 2.f, 1.f });
    // Exact: (3+4+3)/(1+4+9) = 0.7142857142857143
    assertEquals("Angle not exact.", 1., VectorUtil.angleDense(s2, d2), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleSparseDense(s2, d2), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleDense(s1, d1), 0.);
    assertEquals("Angle not exact.", 1., VectorUtil.angleSparseDense(s1, d1), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(s1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleSparse(s1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(s1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(d1, s2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(s2, d1), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleSparseDense(s2, d1), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.cosAngle(s1, d2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleDense(s1, d2), 0.);
    assertEquals("Angle not exact.", 0.7142857142857143, VectorUtil.angleSparseDense(s1, d2), 0.);
  }
}
