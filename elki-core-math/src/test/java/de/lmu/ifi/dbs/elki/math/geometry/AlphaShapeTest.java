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
package de.lmu.ifi.dbs.elki.math.geometry;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;

/**
 * Unit test for Alpha Shapes.#
 *
 * FIXME: test for actual alpha shapes, not convex hulls!
 *
 * FIXME: return lines and points for degenerate cases?
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class AlphaShapeTest {
  @Test
  public void simpleConvexHull() {
    ArrayList<double[]> t = new ArrayList<>();
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 1, 0 });
    t.add(new double[] { 0, 1 });
    t.add(new double[] { 1, 1 });
    List<Polygon> polys = new AlphaShape(t, 100.).compute();
    assertEquals("Too many polys.", 1, polys.size());
    assertEquals("Hull size not as expected.", 4, polys.get(0).size());
  }

  @Test
  public void simple2ConvexHull() {
    ArrayList<double[]> t = new ArrayList<>();
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 0, 1 });
    t.add(new double[] { 1, 0 });
    t.add(new double[] { 1, 1 });
    List<Polygon> polys = new AlphaShape(t, 100.).compute();
    assertEquals("Too many polys.", 1, polys.size());
    assertEquals("Hull size not as expected.", 4, polys.get(0).size());
  }

  @Test
  public void duplicatesConvexHull() {
    ArrayList<double[]> t = new ArrayList<>();
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 0, 0 });
    List<Polygon> polys = new AlphaShape(t, 100.).compute();
    // TODO: shouldn't we return a single point in this special case?
    assertEquals("Too many polys.", 0, polys.size());
  }

  @Test
  public void degeneratreConvexHull() {
    ArrayList<double[]> t = new ArrayList<>();
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 1, 1 });
    t.add(new double[] { 1, 1 });
    t.add(new double[] { 1, 1 });
    t.add(new double[] { 1, 1 });
    t.add(new double[] { 2, 2 });
    List<Polygon> polys = new AlphaShape(t, 100.).compute();
    // TODO: shouldn't we return a line in this special case?
    assertEquals("Too many polys.", 0, polys.size());
  }

  @Test
  public void randomConvexHull() {
    ArrayList<double[]> t = new ArrayList<>();
    Random r = new Random(0L);
    for(int i = 0; i < 1000; i++) {
      t.add(new double[] { r.nextDouble(), r.nextDouble() });
    }
    List<Polygon> polys = new AlphaShape(t, 100.).compute();
    assertEquals("Too many polys.", 1, polys.size());
    assertEquals("Hull size not as expected.", 21, polys.get(0).size());
  }

  @Test
  public void difficultConvexHull() {
    ArrayList<double[]> t = new ArrayList<>();
    t.add(new double[] { 47.82, 13.0 });
    t.add(new double[] { 47.825, 13.0 });
    t.add(new double[] { 47.82, 13.005 });
    t.add(new double[] { 47.825, 13.005 });
    t.add(new double[] { 47.825, 13.01 });
    t.add(new double[] { 47.825, 12.995 });
    t.add(new double[] { 47.83, 12.995 });
    t.add(new double[] { 47.83, 13. });
    t.add(new double[] { 47.83, 13.005 });
    t.add(new double[] { 47.83, 13.01 });
    t.add(new double[] { 47.83, 12.99 });
    t.add(new double[] { 47.835, 12.99 });
    t.add(new double[] { 47.835, 12.995 });
    t.add(new double[] { 47.835, 13. });
    t.add(new double[] { 47.835, 13.005 });
    t.add(new double[] { 47.835, 13.01 });
    t.add(new double[] { 47.84, 13. });
    t.add(new double[] { 47.84, 13.005 });
    t.add(new double[] { 47.84, 13.01 });
    t.add(new double[] { 47.845, 13.005 });
    t.add(new double[] { 47.845, 13.01 });
    t.add(new double[] { 47.85, 13.005 });
    t.add(new double[] { 47.85, 13.01 });
    t.add(new double[] { 47.855, 13.005 });
    List<Polygon> polys = new AlphaShape(t, 100.).compute();
    assertEquals("Too many polys.", 1, polys.size());
    // TODO: only 7 are really needed. The others are colinear.
    assertEquals("Hull size not as expected.", 12, polys.get(0).size());
  }


  @Test
  public void classic() {
    ArrayList<double[]> t = new ArrayList<>();
    t.add(new double[] { 0, 0 });
    t.add(new double[] { 0, 1 });
    t.add(new double[] { 0, 2 });
    t.add(new double[] { 1, 2 });
    t.add(new double[] { 1, 1 });
    t.add(new double[] { 2, 1 });
    t.add(new double[] { 3, 1 });
    t.add(new double[] { 3, 2 });
    t.add(new double[] { 4, 2 });
    t.add(new double[] { 4, 1 });
    t.add(new double[] { 4, 0 });
    t.add(new double[] { 3, 0 });
    t.add(new double[] { 2, 0 });
    t.add(new double[] { 1, 0 });
    List<Polygon> polys = new AlphaShape(t, .75).compute();
    assertEquals("Too many polys.", 1, polys.size());
    assertEquals("Hull size not as expected.", 11, polys.get(0).size());
  }
}
