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
import java.util.Collections;
import java.util.Random;

import org.junit.Test;

/**
 * Test filtered convex hulls.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class FilteredConvexHull2DTest {
  @Test
  public void simple() {
    FilteredConvexHull2D t = new FilteredConvexHull2D();
    t.add(0, 0);
    t.add(1, 0);
    t.add(0, 1);
    t.add(1, 1);
    assertEquals("Hull size not as expected.", 4, t.getHull().size());
  }

  @Test
  public void simple2() {
    FilteredConvexHull2D t = new FilteredConvexHull2D();
    t.add(0, 0);
    t.add(0, 1);
    t.add(1, 0);
    t.add(1, 1);
    t.add(.5, .5);
    assertEquals("Hull size not as expected.", 4, t.getHull().size());
  }

  @Test
  public void duplicates() {
    FilteredConvexHull2D t = new FilteredConvexHull2D();
    t.add(0, 0);
    t.add(0, 0);
    t.add(0, 0);
    t.add(0, 0);
    assertEquals("Hull size not as expected.", 1, t.getHull().size());
  }

  @Test
  public void degeneratre() {
    FilteredConvexHull2D t = new FilteredConvexHull2D();
    t.add(0, 0);
    t.add(0, 0);
    t.add(0, 0);
    t.add(0, 0);
    t.add(1, 1);
    t.add(1, 1);
    t.add(1, 1);
    t.add(1, 1);
    t.add(2, 2);
    assertEquals("Hull size not as expected.", 3, t.getHull().size());
  }

  @Test
  public void random() {
    FilteredConvexHull2D t = new FilteredConvexHull2D();
    Random r = new Random(0L);
    for(int i = 0; i < 1000; i++) {
      t.add(r.nextDouble(), r.nextDouble());
    }
    assertEquals("Hull size not as expected.", 21, t.getHull().size());
  }

  @Test
  public void difficult() {
    FilteredConvexHull2D t = new FilteredConvexHull2D();
    t.add(47.82, 13.0);
    t.add(47.825, 13.0);
    t.add(47.82, 13.005);
    t.add(47.825, 13.005);
    t.add(47.825, 13.01);
    t.add(47.825, 12.995);
    t.add(47.83, 12.995);
    t.add(47.83, 13.);
    t.add(47.83, 13.005);
    t.add(47.83, 13.01);
    t.add(47.83, 12.99);
    t.add(47.835, 12.99);
    t.add(47.835, 12.995);
    t.add(47.835, 13.);
    t.add(47.835, 13.005);
    t.add(47.835, 13.01);
    t.add(47.84, 13.);
    t.add(47.84, 13.005);
    t.add(47.84, 13.01);
    t.add(47.845, 13.005);
    t.add(47.845, 13.01);
    t.add(47.85, 13.005);
    t.add(47.85, 13.01);
    t.add(47.855, 13.005);
    assertEquals("Hull size not as expected.", 7, t.getHull().size());
  }

  @Test
  public void circle() {
    final int size = 1000;
    ArrayList<double[]> pts = new ArrayList<>();
    for(int i = 0; i < size; i++) {
      double a = i * 2 * Math.PI / size;
      pts.add(new double[] { Math.sin(a), Math.cos(a) });
    }
    Collections.shuffle(pts, new Random(1L));
    for(int i = 0; i < size; i++) {
      double a = i * 2 * Math.PI / size;
      pts.add(new double[] { Math.sin(a) * .01, Math.cos(a) * .01 });
    }

    FilteredConvexHull2D t = new FilteredConvexHull2D();
    for(double[] p : pts) {
      t.add(p);
    }
    assertEquals("Hull size not as expected.", size, t.getHull().size());
  }
}
