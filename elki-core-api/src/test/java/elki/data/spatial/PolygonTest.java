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
package elki.data.spatial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

/**
 * Unit tests for the polygon class.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class PolygonTest {
  @Test
  public void testContains() {
    final int size = 1000;
    double offset = 0.1234; // Random rotation
    ArrayList<double[]> pts = new ArrayList<>();
    for(int i = 0; i < size; i++) {
      double a = ((i / (double) size) + offset) * 2 * Math.PI;
      pts.add(new double[] { Math.sin(a), Math.cos(a) });
    }
    Polygon t = new Polygon(pts);

    int size2 = 100;
    double r2 = 1.00001, offset2 = 0.4321;
    for(int i = 0; i < size2; i++) {
      double a = ((i / (double) size2) + offset2) * 2 * Math.PI;
      double[] p = new double[] { Math.sin(a) * r2, Math.cos(a) * r2 };
      assertFalse("Not outside.", t.containsPoint2D(p));
    }
    double r3 = .99999, offset3 = 0.;
    for(int i = 0; i < size2; i++) {
      double a = ((i / (double) size2) + offset3) * 2 * Math.PI;
      double[] p = new double[] { Math.sin(a) * r3, Math.cos(a) * r3 };
      assertTrue("Not inside: " + Arrays.toString(p), t.containsPoint2D(p));
    }
  }

  @Test
  public void testClockwise() {
    Polygon t1 = new Polygon(Arrays.asList(new double[][] { { 1, 0 }, { 0, 1 }, { 0, 0 } }));
    Polygon t2 = new Polygon(Arrays.asList(new double[][] { { 0, 1 }, { 1, 0 }, { 0, 0 } }));
    assertEquals("Not counterclockwise", -1, t1.testClockwise());
    assertEquals("Not clockwise", 1, t2.testClockwise());
  }

  @Test
  public void areaOfCircle() {
    final int size = 1000;
    double offset = 0.1234; // Random rotation
    ArrayList<double[]> pts = new ArrayList<>();
    for(int i = 0; i < size; i++) {
      double a = ((i / (double) size) + offset) * 2 * Math.PI;
      pts.add(new double[] { Math.sin(a), Math.cos(a) });
    }
    Polygon t = new Polygon(pts);
    assertEquals("Hull size not as expected.", Math.PI, t.areaShoelace(), 1e-4);
  }

  @Test
  public void areaTriangle() {
    Polygon t = new Polygon(Arrays.asList(new double[][] { { 1, 0 }, { 0, 1 }, { 0, 0 } }));
    assertEquals("Hull size not as expected.", .5, t.areaShoelace(), 0.);
  }

  @Test
  public void testPolygonContainment() {
    Polygon p1 = new Polygon(Arrays.asList(new double[][] { { 0, 0 }, { .9, 0 }, { 0, .9 } }));
    Polygon p2 = new Polygon(Arrays.asList(new double[][] { { 1, 1 }, { 1, .1 }, { .1, 1 } }));
    Polygon p3 = new Polygon(Arrays.asList(new double[][] { { .1, .1 }, { .1, .9 }, { .9, .9 }, { .9, .1 } }));
    double[] pou = new double[] { -1, -1 };
    double[] p22 = new double[] { .2, .2 };
    assertFalse("P2 not in p1", p1.containsPoint2D(pou));
    assertFalse("P2 not in p2", p2.containsPoint2D(pou));
    assertFalse("P2 not in p3", p3.containsPoint2D(pou));
    assertTrue("P2 not in p1", p1.containsPoint2D(p22));
    assertFalse("P2 in p2", p2.containsPoint2D(p22));
    assertTrue("P2 not in p3", p3.containsPoint2D(p22));
    assertFalse("Polygons p1 and p2 must not intersect.", p1.intersects2DIncomplete(p2));
    assertTrue("Polygons p1 and p3 must intersect.", p1.intersects2DIncomplete(p3));
    assertTrue("Polygons p2 and p3 must intersect.", p2.intersects2DIncomplete(p3));
  }
}
