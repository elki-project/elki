package de.lmu.ifi.dbs.elki.test.math;

import static org.junit.Assert.*;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

public class TestAffineTransformations {

  @Test
  public void testIdentityTransform() {
    int testdim = 5;
    AffineTransformation t = new AffineTransformation(testdim);
    assertTrue(t.getDimensionality() == testdim);
    Matrix tm = t.getTransformation();
    assertEquals("initial transformation matrix should be unity", tm, Matrix.unitMatrix(testdim + 1));

    // test application to a vector
    double[] dv = new double[testdim];
    for (int i=0; i<testdim; i++)
      dv[i] = i*i + testdim;
    Vector v1 = new Vector(dv);
    Vector v2 = new Vector(dv);

    Vector v3 = t.apply(v1);
    assertEquals("identity transformation wasn't identical",v2,v3);
    
    Vector v4 = t.applyInverse(v2);
    assertEquals("inverse of identity wasn't identity",v1,v4);
  }

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
    for (int i=0; i<testdim; i++)
      tv[i] = i + testdim;
    t.addTranslation(new Vector(tv));
    
    Matrix tm2 = t.getTransformation();
    // Manually do the same changes to the matrix tm
    for (int i=0; i<testdim; i++)
      tm.set(i,testdim, i + testdim);
    // Compare the results
    assertEquals("Translation wasn't added correctly to matrix.", tm, tm2);
    
    // test application to a vector
    double[] dv1 = new double[testdim];
    double[] dv2 = new double[testdim];
    for (int i=0; i<testdim; i++) {
      dv1[i] = i*i + testdim;
      dv2[i] = i*i + i + 2*testdim;
    }
    Vector v1 = new Vector(dv1);
    Vector v2 = new Vector(dv2);

    Vector v3 = t.apply(v1);
    assertEquals("Vector wasn't translated properly forward.",v2,v3);
    Vector v4 = t.applyInverse(v2);
    assertEquals("Vector wasn't translated properly backwards.",v1,v4);
  }


  @Test
  public void testMatrix() {
    int testdim = 5;
    int axis1 = 1;
    int axis2 = 3;
    
    assert(axis1 < testdim);
    assert(axis2 < testdim);
    // don't change the angle; we'll be using that executing the rotation
    // three times will be identity (approximately)
    double angle = Math.toRadians(360/3);
    AffineTransformation t = new AffineTransformation(testdim);
    assertTrue(t.getDimensionality() == testdim);
    Matrix tm = t.getTransformation();
    assertNotSame("getTransformation is expected to return a new copy", tm, t.getTransformation());
    assertEquals("initial transformation matrix should be unity", tm, Matrix.unitMatrix(testdim + 1));

    // rotation matrix
    double[][] rm = new double[testdim][testdim];
    for (int i=0; i<testdim; i++)
      rm[i][i] = 1;
    // add the rotation
    rm[axis1][axis1] = + Math.cos(angle);
    rm[axis1][axis2] = - Math.sin(angle);
    rm[axis2][axis1] = + Math.sin(angle);
    rm[axis2][axis2] = + Math.cos(angle);
    t.addMatrix(new Matrix(rm));
    Matrix tm2 = t.getTransformation();
    
    // We know that we didn't do any translations and tm is the unity matrix
    // so we can manually do the rotation on it, too.
    tm.set(axis1,axis1,+ Math.cos(angle));
    tm.set(axis1,axis2,- Math.sin(angle));
    tm.set(axis2,axis1,+ Math.sin(angle));
    tm.set(axis2,axis2,+ Math.cos(angle));
    
    // Compare the results
    assertEquals("Rotation wasn't added correctly to matrix.", tm, tm2);
    
    // test application to a vector
    double[] dv = new double[testdim];
    for (int i=0; i<testdim; i++) {
      dv[i] = i*i + testdim;
    }
    Vector v1 = new Vector(dv);
    Vector v3 = t.applyInverse(t.apply(v1));
    assertTrue("Forward-Backward didn't work correctly.", v1.almostEquals(v3));
    Vector v4 = t.apply(t.apply(t.apply(v1)));
    assertTrue("Triple-Rotation by 120 degree didn't work",v1.almostEquals(v4));
  }
}
