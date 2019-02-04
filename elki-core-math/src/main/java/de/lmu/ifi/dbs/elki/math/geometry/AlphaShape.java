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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

/**
 * Compute the alpha-Shape of a point set, using Delaunay triangulation.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @assoc - - - SweepHullDelaunay2D
 * @has - - - Polygon
 */
public class AlphaShape {
  /**
   * Alpha shape
   */
  private double alpha2;
  
  /**
   * Points
   */
  private List<double[]> points;

  /**
   * Delaunay triangulation
   */
  private ArrayList<SweepHullDelaunay2D.Triangle> delaunay = null;

  /**
   * Constructor.
   *
   * @param points Point set
   * @param alpha
   */
  public AlphaShape(List<double[]> points, double alpha) {
    this.alpha2 = alpha * alpha;
    this.points = points;
  }

  public List<Polygon> compute() {
    // Compute delaunay triangulation:
    delaunay = (new SweepHullDelaunay2D(points)).getDelaunay();

    List<Polygon> polys = new ArrayList<>();

    // Working data
    long[] used = BitsUtil.zero(delaunay.size());
    List<double[]> cur = new ArrayList<>();

    for(int i = 0 /* = used.nextClearBit(0) */; i < delaunay.size() && i >= 0; i = BitsUtil.nextClearBit(used, i + 1)) {
      if(BitsUtil.get(used, i)) {
        continue;
      }
      BitsUtil.setI(used, i);
      SweepHullDelaunay2D.Triangle tri = delaunay.get(i);
      if(tri.r2 <= alpha2) {
        // Check neighbors
        processNeighbor(cur, used, i, tri.ab, tri.b);
        processNeighbor(cur, used, i, tri.bc, tri.c);
        processNeighbor(cur, used, i, tri.ca, tri.a);
      }
      if(!cur.isEmpty()) {
        polys.add(new Polygon(cur));
        cur = new ArrayList<>();
      }
    }

    return polys;
  }

  private void processNeighbor(List<double[]> cur, long[] used, int i, int ab, int b) {
    if(ab >= 0) {
      if(BitsUtil.get(used, ab)) {
        return;
      }
      BitsUtil.setI(used, ab);
      final SweepHullDelaunay2D.Triangle next = delaunay.get(ab);
      if(next.r2 <= alpha2) {
        // Continue where we left off...
        if(next.ab == i) {
          processNeighbor(cur, used, ab, next.bc, next.c);
          processNeighbor(cur, used, ab, next.ca, next.a);
        }
        else if(next.bc == i) {
          processNeighbor(cur, used, ab, next.ca, next.a);
          processNeighbor(cur, used, ab, next.ab, next.b);
        }
        else if(next.ca == i) {
          processNeighbor(cur, used, ab, next.ab, next.b);
          processNeighbor(cur, used, ab, next.bc, next.c);
        }
        return;
      }
    }
    cur.add(points.get(b));
  }
}
