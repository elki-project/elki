package de.lmu.ifi.dbs.elki.test.math;

import static org.junit.Assert.*;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * JUnit Test for the class {@link AffineTransformation}
 * 
 * @author Erich Schubert
 * 
 */
public class TestAffineTransformation {
  /**
   * Test identity transform
   */
  @Test
  public void testIdentityTransform() {
    int testdim = 5;
    AffineTransformation t = new AffineTransformation(testdim);
    assertTrue(t.getDimensionality() == testdim);
    Matrix tm = t.getTransformation();
    assertEquals("initial transformation matrix should be unity", tm, Matrix.unitMatrix(testdim + 1));

    // test application to a vector
    double[] dv = new double[testdim];
    for(int i = 0; i < testdim; i++) {
      dv[i] = i * i + testdim;
    }
    Vector v1 = new Vector(dv);
    Vector v2 = new Vector(dv);

    Vector v3 = t.apply(v1);
    assertEquals("identity transformation wasn't identical", v2, v3);

    Vector v4 = t.applyInverse(v2);
    assertEquals("inverse of identity wasn't identity", v1, v4);
  }

  /**
   * Test adding translation vectors
   */
  @Test
  public void testTranslation() {
    int testdim = 5;
    AffineTransformation t = new AffineTransformation(testdim);
    assertTrue(t.getDimensionality() == testdim);
    Matrix tm = t.getTransformation();
    assertNotSame("getTransformation is expected to return a new copy", tm, t.getTransformation());
    assertEquals("initial transformation matrix should be unity", tm, Matrix.unitMatrix(testdim + 1));

    // translation vector
    double[] tv = new double[testdim];
    for(int i = 0; i < testdim; i++) {
      tv[i] = i + testdim;
    }
    t.addTranslation(new Vector(tv));

    Matrix tm2 = t.getTransformation();
    // Manually do the same changes to the matrix tm
    for(int i = 0; i < testdim; i++) {
      tm.set(i, testdim, i + testdim);
    }
    // Compare the results
    assertEquals("Translation wasn't added correctly to matrix.", tm, tm2);

    // test application to a vector
    double[] dv1 = new double[testdim];
    double[] dv2 = new double[testdim];
    for(int i = 0; i < testdim; i++) {
      dv1[i] = i * i + testdim;
      dv2[i] = i * i + i + 2 * testdim;
    }
    Vector v1 = new Vector(dv1);
    Vector v2t = new Vector(dv2);

    Vector v1t = t.apply(v1);
    assertEquals("Vector wasn't translated properly forward.", v2t, v1t);
    Vector v2b = t.applyInverse(v2t);
    assertEquals("Vector wasn't translated properly backwards.", v1, v2b);
    Vector v1b = t.applyInverse(v1t);
    assertEquals("Vector wasn't translated properly back and forward.", v1, v1b);
    
    // Translation
    Vector vd = v1.minus(v2b);
    Vector vtd = v1t.minus(v2t);
    assertEquals("Translation changed vector difference.", vd, vtd);
    
    // Translation shouldn't change relative vectors.
    assertEquals("Relative vectors weren't left unchanged by translation!", v1, t.applyRelative(v1));
    assertEquals("Relative vectors weren't left unchanged by translation!", v2t, t.applyRelative(v2t));
    assertEquals("Relative vectors weren't left unchanged by translation!", v1t, t.applyRelative(v1t));
    assertEquals("Relative vectors weren't left unchanged by translation!", v2b, t.applyRelative(v2b));
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
    Matrix tm = t.getTransformation();
    assertNotSame("getTransformation is expected to return a new copy", tm, t.getTransformation());
    assertEquals("initial transformation matrix should be unity", tm, Matrix.unitMatrix(testdim + 1));

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
    t.addMatrix(new Matrix(rm));
    Matrix tm2 = t.getTransformation();

    // We know that we didn't do any translations and tm is the unity matrix
    // so we can manually do the rotation on it, too.
    tm.set(axis1, axis1, +Math.cos(angle));
    tm.set(axis1, axis2, -Math.sin(angle));
    tm.set(axis2, axis1, +Math.sin(angle));
    tm.set(axis2, axis2, +Math.cos(angle));

    // Compare the results
    assertEquals("Rotation wasn't added correctly to matrix.", tm, tm2);

    // test application to a vector
    double[] dv = new double[testdim];
    for(int i = 0; i < testdim; i++) {
      dv[i] = i * i + testdim;
    }
    Vector v1 = new Vector(dv);
    Vector v2 = t.apply(v1);
    Vector v3 = t.applyInverse(v2);
    assertTrue("Forward-Backward didn't work correctly.", v1.almostEquals(v3));
    Vector v4 = t.apply(t.apply(t.apply(v1)));
    assertTrue("Triple-Rotation by 120 degree didn't work", v1.almostEquals(v4));
    
    // Rotation shouldn't disagree for relative vectors.
    // (they just are not affected by translation!)
    assertEquals("Relative vectors were affected differently by pure rotation!", v2, t.applyRelative(v1));

    // should do the same as built-in rotation!
    AffineTransformation t2 = new AffineTransformation(testdim);
    t2.addRotation(axis1, axis2, angle);
    Vector t2v2 = t2.apply(v1);
    assertTrue("Manual rotation and AffineTransformation.addRotation disagree.", v2.almostEquals(t2v2));
  }

  /**
   * Test {@link AffineTransformation#reorderAxesTransformation}
   */
  @Test
  public void testReorder() {
    Vector v = new Vector(new double[] { 3, 5, 7 });
    // all permutations
    Vector p1 = new Vector(new double[] { 3, 5, 7 });
    Vector p2 = new Vector(new double[] { 3, 7, 5 });
    Vector p3 = new Vector(new double[] { 5, 3, 7 });
    Vector p4 = new Vector(new double[] { 5, 7, 3 });
    Vector p5 = new Vector(new double[] { 7, 3, 5 });
    Vector p6 = new Vector(new double[] { 7, 5, 3 });
    Vector[] ps = new Vector[] {
    // with no arguments.
    p1,
    // with just one argument.
    p1, p3, p5,
    // with two arguments.
    p1, p2, p3, p4, p5, p6,
    };

    // index in reference array
    int idx = 0;
    // with 0 arguments
    {
      AffineTransformation aff = AffineTransformation.reorderAxesTransformation(v.getDimensionality(), new int[] {});
      Vector n = aff.apply(v).minus(ps[idx]);
      assertEquals("Permutation " + idx + " doesn't match.", n.length(), 0.0, 0.001);
      idx++;
    }
    // with one argument
    for(int d1 = 1; d1 <= 3; d1++) {
      AffineTransformation aff = AffineTransformation.reorderAxesTransformation(v.getDimensionality(), new int[] { d1 });
      Vector n = aff.apply(v).minus(ps[idx]);
      assertEquals("Permutation " + idx + " doesn't match.", n.length(), 0.0, 0.001);
      idx++;
    }
    // with two arguments
    for(int d1 = 1; d1 <= 3; d1++) {
      for(int d2 = 1; d2 <= 3; d2++) {
        if(d1 == d2) {
          continue;
        }
        AffineTransformation aff = AffineTransformation.reorderAxesTransformation(v.getDimensionality(), new int[] { d1, d2 });
        Vector n = aff.apply(v).minus(ps[idx]);
        assertEquals("Permutation " + idx + " doesn't match.", n.length(), 0.0, 0.001);
        idx++;
      }
    }
  }
}
