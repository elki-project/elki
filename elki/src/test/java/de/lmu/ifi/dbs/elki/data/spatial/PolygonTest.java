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
package de.lmu.ifi.dbs.elki.data.spatial;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test the polygon class.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class PolygonTest {
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
