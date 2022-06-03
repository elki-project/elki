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
package elki.math.geometry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import elki.data.spatial.Polygon;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;

/**
 * Compute the alpha-shape of a point set, using Delaunay triangulation.
 * <p>
 * Reference:
 * <p>
 * H. Edelsbrunner, D. G. Kirkpatrick, R. Seidel<br>
 * On the shape of a set of points in the plane<br>
 * IEEE Trans. Inf. Theory 29(4)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @assoc - - - SweepHullDelaunay2D
 * @has - - - Polygon
 */
@Reference(authors = "H. Edelsbrunner, D. G. Kirkpatrick, R. Seidel", //
    title = "On the shape of a set of points in the plane", //
    booktitle = "IEEE Trans. Inf. Theory 29(4)", //
    url = "https://doi.org/10.1109/TIT.1983.1056714", //
    bibkey = "DBLP:journals/tit/EdelsbrunnerKS83")
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
   * @param points point set
   * @param alpha alpha parameter
   */
  public AlphaShape(List<double[]> points, double alpha) {
    this.alpha2 = alpha * alpha;
    this.points = points;
  }

  /**
   * Compute the alpha shape.
   *
   * @return polygons
   */
  public List<Polygon> compute() {
    // Compute delaunay triangulation:
    delaunay = (new SweepHullDelaunay2D(points)).getDelaunay();

    List<Polygon> polys = new ArrayList<>();
    List<IntegerArray> open = new ArrayList<>();

    // Working data
    long[] visited = BitsUtil.zero(delaunay.size());
    // Find an unprocessed triangle to start with:
    for(int i = 0; i < delaunay.size() && i >= 0; i = BitsUtil.nextClearBit(visited, i + 1)) {
      assert !BitsUtil.get(visited, i);
      BitsUtil.setI(visited, i);
      SweepHullDelaunay2D.Triangle tri = delaunay.get(i);
      if(tri.r2 <= alpha2) {
        processNeighbor(open, visited, i, tri);
      }
      for(IntegerArray po : open) {
        List<double[]> cur = new ArrayList<>(po.size);
        for(int j = 0; j < po.size; j++) {
          cur.add(points.get(po.data[j]));
        }
        polys.add(new Polygon(cur));
      }
      open.clear();
    }
    return polys;
  }

  private void processNeighbor(List<IntegerArray> open, long[] visited, int i, SweepHullDelaunay2D.Triangle tri) {
    Deque<int[]> stack = new ArrayDeque<>();

    // Check neighbors
    stack.push(new int[]{i, tri.ca, tri.c, tri.a});
    stack.push(new int[]{i, tri.bc, tri.b, tri.c});
    stack.push(new int[]{i, tri.ab, tri.a, tri.b});

    while (!stack.isEmpty()) {
      int[] nextParams = stack.pop();

      int cur = nextParams[0];
      int ab = nextParams[1];
      int a = nextParams[2];
      int b = nextParams[3];

      if(ab < 0) { // Nonexistant neighbor
        addEdge(open, a, b);
        continue;
      }
      final SweepHullDelaunay2D.Triangle next = delaunay.get(ab);
      if(BitsUtil.get(visited, ab)) {
        // We already discarded the neighbor polygon, but we still get an edge.
        if(next.r2 > alpha2) {
          addEdge(open, a, b);
        }
        continue;
      }
      BitsUtil.setI(visited, ab);
      if(next.r2 <= alpha2) {
        // Walk 'around' the next triangle
        if(next.ab == cur) {
          assert next.b == a && next.a == b;
          stack.push(new int[]{ab, next.ca, next.c, b});
          stack.push(new int[]{ab, next.bc, a, next.c});
        }
        else if(next.bc == cur) {
          assert next.c == a && next.b == b;
          stack.push(new int[]{ab, next.ab, next.a, b});
          stack.push(new int[]{ab, next.ca, a, next.a});
        }
        else /* if(next.ca == i) */ {
          assert next.ca == cur;
          assert next.a == a && next.c == b;
          stack.push(new int[]{ab, next.bc, next.b, b});
          stack.push(new int[]{ab, next.ab, a, next.b});
        }
        continue;
      }
      addEdge(open, a, b);
    }
  }

  /**
   * Add an edge to the corresponding polygon. This handles holes.
   * 
   * @param open List of open polygons
   * @param a previous point
   * @param b next point
   */
  private void addEdge(List<IntegerArray> open, int a, int b) {
    for(IntegerArray cur : open) {
      if(cur.data[cur.size - 1] == a) {
        cur.add(b);
        return;
      }
    }
    IntegerArray cur = new IntegerArray();
    cur.add(b);
    open.add(cur);
  }
}
