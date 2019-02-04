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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Compute the Convex Hull and/or Delaunay Triangulation, using the sweep-hull
 * approach of David Sinclair.
 * <p>
 * Note: This implementation does not check or handle duplicate points!
 * <p>
 * TODO: Handle duplicates.
 * <p>
 * TODO: optimize data structures for memory usage
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @has - - - Polygon
 */
@Reference(authors = "D. Sinclair", //
    title = "S-hull: a fast sweep-hull routine for Delaunay triangulation", //
    booktitle = "Online", //
    url = "http://s-hull.org/", //
    bibkey = "web/Sinclair16")
public class SweepHullDelaunay2D {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SweepHullDelaunay2D.class);

  /**
   * The current set of points.
   * <p>
   * Note: this list should not be changed after running the algorithm, since we
   * use it for object indexing, and the ids should not change
   */
  private List<double[]> points;

  /**
   * Triangles
   */
  private ArrayList<Triangle> tris = null;

  /**
   * Internal representation of the hull
   */
  private LinkedList<IntIntPair> hull = null;

  /**
   * Constructor.
   */
  public SweepHullDelaunay2D() {
    this(new ArrayList<double[]>());
  }

  /**
   * Constructor.
   *
   * @param points Existing points
   */
  public SweepHullDelaunay2D(List<double[]> points) {
    this.points = points;
  }

  /**
   * Add a single point to the list (this does not compute or update the
   * triangulation!)
   *
   * @param point Point to add
   */
  public void add(double... point) {
    this.points.add(point);
    // Invalidate
    hull = null;
    tris = null;
  }

  /**
   * Run the actual algorithm
   *
   * @param hullonly
   */
  void run(boolean hullonly) {
    if(points.size() < 3) {
      throw new UnsupportedOperationException("There is no delaunay triangulation for less than three objects!");
    }
    int len = points.size() - 1;
    hull = new LinkedList<>();
    tris = hullonly ? null : new ArrayList<Triangle>(len);

    // 1. Seed point x_0
    final double[] seed;
    final int seedid = 0;

    final double[] sortd = new double[len];
    final int[] sorti = new int[len];
    Arrays.fill(sorti, -42); // To cause errors
    // TODO: remove duplicates.

    // 2. sort by squared Euclidean distance
    {
      Iterator<double[]> iter = points.iterator();
      seed = iter.next();
      for(int i = 0, j = 1; iter.hasNext(); j++, i++) {
        double dist = quadraticEuclidean(seed, iter.next());
        if(dist <= 0.) { // Duplicate.
          --len; // Decrease candidate set size
          --i; // Increase j, but not i.
          continue;
        }
        sortd[i] = dist;
        sorti[i] = j;
      }
      DoubleIntegerArrayQuickSort.sort(sortd, sorti, len);
    }
    // Detect some degenerate situations:
    if(len < 2) {
      hull.add(new IntIntPair(seedid, -1));
      if(len == 1) {
        hull.add(new IntIntPair(sorti[0], -1));
      }
      return;
    }
    assert (sortd[0] > 0);
    // final double[] seed2 = points.get(sort[0].second);
    final int seed2id = sorti[0];

    // 3. Find minimal triangle for these two points:
    Triangle besttri = findSmallest(seedid, seed2id, sortd, sorti, len);
    if(besttri == null) { // Degenerate
      hull.add(new IntIntPair(seedid, -1));
      hull.add(new IntIntPair(seed2id, -1));
      return;
    }
    // Note: sortd no longer accurate, recompute below!
    int start = 2; // First two points have already been processed.

    // 5. Make right-handed:
    besttri.makeClockwise(points);
    // Seed triangulation
    if(!hullonly) {
      tris.add(besttri);
    }
    // Seed convex hull (point, triangle)
    hull.add(new IntIntPair(besttri.a, 0));
    hull.add(new IntIntPair(besttri.b, 0));
    hull.add(new IntIntPair(besttri.c, 0));

    if(LOG.isDebuggingFinest()) {
      debugHull();
    }

    // 6. Resort from triangle circumcircle center
    double[] center = besttri.m;
    for(int i = start; i < len; i++) {
      sortd[i] = quadraticEuclidean(center, points.get(sorti[i]));
    }
    DoubleIntegerArrayQuickSort.sort(sortd, sorti, start, len);

    // Grow hull and triangles
    for(int i = start; i < len; i++) {
      final int pointId = sorti[i];
      final double[] newpoint = points.get(pointId);

      LinkedList<Triangle> newtris = hullonly ? null : new LinkedList<Triangle>();
      // We identify edges by their starting point. -1 is invalid.
      int hstart = -1, hend = -1;
      // Find first and last consecutive visible edge, backwards:
      {
        Iterator<IntIntPair> iter = hull.descendingIterator();
        IntIntPair next = hull.getFirst();
        double[] nextV = points.get(next.first);
        for(int pos = hull.size() - 1; iter.hasNext(); pos--) {
          IntIntPair prev = iter.next();
          double[] prevV = points.get(prev.first);
          // Not yet visible:
          if(hend < 0) {
            if(leftOf(prevV, nextV, newpoint)) {
              hstart = hend = pos;
              if(!hullonly) {
                // Clockwise, A is new point!
                Triangle tri = new Triangle(pointId, next.first, prev.first);
                assert (tri.isClockwise(points));
                assert (prev.second >= 0);
                tri.updateCircumcircle(points);
                tri.bc = prev.second;
                newtris.addFirst(tri);
              }
            }
          }
          else {
            if(leftOf(prevV, nextV, newpoint)) {
              hstart = pos;
              // Add triad:
              if(!hullonly) {
                // Clockwise, A is new point!
                Triangle tri = new Triangle(pointId, next.first, prev.first);
                assert (tri.isClockwise(points));
                assert (prev.second >= 0);
                tri.updateCircumcircle(points);
                tri.bc = prev.second;
                newtris.addFirst(tri);
              }
            }
            else {
              break;
            }
          }
          next = prev;
          nextV = prevV;
        }
      }
      // If the last edge was visible, we also need to scan forwards:
      if(hend == hull.size() - 1) {
        Iterator<IntIntPair> iter = hull.iterator();
        IntIntPair prev = iter.next();
        double[] prevV = points.get(prev.first);
        while(iter.hasNext()) {
          IntIntPair next = iter.next();
          double[] nextV = points.get(next.first);
          if(leftOf(prevV, nextV, newpoint)) {
            hend++;
            // Add triad:
            if(!hullonly) {
              // Clockwise, A is new point!
              Triangle tri = new Triangle(pointId, next.first, prev.first);
              assert (tri.isClockwise(points));
              assert (prev.second >= 0);
              tri.updateCircumcircle(points);
              tri.bc = prev.second;
              newtris.addLast(tri);
            }
          }
          else {
            break;
          }
          prev = next;
          prevV = nextV;
        }
      }
      assert (hstart >= 0 && hend >= hstart);
      // Note that hend can be larger than hull.size() now, interpret as
      // "hend % hull.size()"
      // Update hull, remove points
      final int firsttri, lasttri;
      if(hullonly) {
        firsttri = lasttri = -1;
      }
      else {
        final int tristart = tris.size();
        firsttri = tristart;
        lasttri = tristart + newtris.size() - 1;
      }
      final int hullsize = hull.size();
      if(LOG.isDebuggingFinest()) {
        LOG.debugFinest("Size: " + hullsize + " start: " + hstart + " end: " + hend);
      }
      if(hend < hullsize) {
        ListIterator<IntIntPair> iter = hull.listIterator();
        int p = 0;
        // Skip
        for(; p <= hstart; p++) {
          iter.next();
        }
        // Remove
        for(; p <= hend; p++) {
          iter.next();
          iter.remove();
        }
        // Insert, and update edge->triangle mapping
        iter.add(new IntIntPair(pointId, lasttri));
        iter.previous();
        if(!hullonly) {
          (iter.hasPrevious() ? iter.previous() : hull.getLast()).second = firsttri;
        }
      }
      else {
        ListIterator<IntIntPair> iter = hull.listIterator();
        // Remove end
        int p = hullsize;
        for(; p <= hend; p++) {
          iter.next();
          iter.remove();
        }
        // Insert
        iter.add(new IntIntPair(pointId, lasttri));
        // Wrap around
        p -= hullsize;
        IntIntPair pre = null;
        for(; p <= hstart; p++) {
          pre = iter.next();
        }
        assert (pre != null);
        pre.second = firsttri;
        // Remove remainder
        while(iter.hasNext()) {
          iter.next();
          iter.remove();
        }
      }
      if(LOG.isDebuggingFinest()) {
        debugHull();
      }
      if(!hullonly) {
        final int tristart = tris.size();
        // Connect triads (they are ordered)
        Iterator<Triangle> iter = newtris.iterator();
        for(int o = 0; iter.hasNext(); o++) {
          // This triangle has num tristart + o.
          Triangle cur = iter.next();
          cur.ca = o > 0 ? tristart + o - 1 : -1; // previously added triangle
          cur.ab = iter.hasNext() ? tristart + o + 1 : -1; // next triangle
          // cur.bc was set upon creation
          assert (cur.bc >= 0);
          Triangle other = tris.get(cur.bc);
          Orientation orient = cur.findOrientation(other);
          assert (orient != null) : "Inconsistent triangles: " + cur + " " + other;
          switch(orient){
          case ORIENT_BC_BA:
            assert (other.ab == -1) : "Inconsistent triangles: " + cur + " " + other;
            other.ab = tristart + o;
            break;
          case ORIENT_BC_CB:
            assert (other.bc == -1) : "Inconsistent triangles: " + cur + " " + other;
            other.bc = tristart + o;
            break;
          case ORIENT_BC_AC:
            assert (other.ca == -1) : "Inconsistent triangles: " + cur + " " + other;
            other.ca = tristart + o;
            break;
          default:
            assert (cur.isClockwise(points));
            assert (other.isClockwise(points));
            throw new RuntimeException("Inconsistent triangles: " + cur + " " + other + " size:" + tris.size());
          }
          tris.add(cur);
        }
        assert (tris.size() == lasttri + 1);
      }
    }
    // Now check for triangles that need flipping.
    if(!hullonly) {
      final int size = tris.size();
      long[] flippedA = BitsUtil.zero(size), flippedB = BitsUtil.zero(size);
      // Initial flip
      if(flipTriangles(flippedA) > 0) {
        for(int iterations = 1; iterations < 1000; iterations += 2) {
          if(LOG.isDebuggingFinest()) {
            debugHull();
          }
          if(flipTriangles(flippedA, flippedB) == 0) {
            break;
          }
          if(LOG.isDebuggingFinest()) {
            debugHull();
          }
          if(flipTriangles(flippedB, flippedA) == 0) {
            break;
          }
        }
      }
    }
  }

  /**
   * @param seedid First seed point
   * @param seed2id Second seed point
   * @param sorti Points
   * @param len Number of points
   * @return Best triangle
   */
  public Triangle findSmallest(int seedid, int seed2id, double[] sortd, int[] sorti, int len) {
    Triangle besttri = new Triangle(seedid, seed2id, -1);
    besttri.r2 = Double.MAX_VALUE;
    Triangle testtri = new Triangle(seedid, seed2id, -1);
    int besti = -1;
    for(int i = 1; i < len; i++) {
      // Update test triad
      testtri.c = sorti[i];
      if(!testtri.updateCircumcircle(points)) {
        continue; // Degenerated.
      }
      assert (testtri.r2 > 0.);
      if(testtri.r2 < besttri.r2) {
        besttri.copyFrom(testtri);
        besti = i;
      }
      else if(besttri.r2 * 4. < sortd[i]) {
        // Stop early, points are too far away from seed.
        break;
      }
    }
    if(besti == -1) {
      // Degenerated result, everything is colinear.
      hull.add(new IntIntPair(0, sorti[len - 1]));
      return null;
    }
    assert (besti >= 1);
    // Rearrange - remove third seed point.
    if(besti > 1) {
      // Note: we do NOT update the distances, they will be overwritten next.
      int i = sorti[besti];
      System.arraycopy(sorti, 1, sorti, 2, besti - 1);
      sorti[1] = i;
    }
    return besttri;
  }

  /**
   * Debug helper
   */
  void debugHull() {
    StringBuilder buf = new StringBuilder(hull.size() * 20);
    for(IntIntPair p : hull) {
      buf.append(p.first).append(" (").append(p.second).append(") ");
    }
    LOG.debugFinest(buf);
  }

  /**
   * Flip triangles as necessary
   *
   * @param flippedB Bit set to mark triangles as done
   */
  int flipTriangles(long[] flippedB) {
    final int size = tris.size();
    int numflips = 0;
    BitsUtil.zeroI(flippedB);
    for(int i = 0; i < size; i++) {
      if(!BitsUtil.get(flippedB, i) && flipTriangle(i, flippedB) >= 0) {
        numflips += 2;
      }
    }
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest("Flips: " + numflips);
    }
    return numflips;
  }

  /**
   * Flip triangles as necessary
   *
   * @param flippedA Bit set for triangles to test
   * @param flippedB Bit set to mark triangles as done
   */
  int flipTriangles(long[] flippedA, long[] flippedB) {
    int numflips = 0;
    BitsUtil.zeroI(flippedB);
    for(int i = BitsUtil.nextSetBit(flippedA, 0); i > -1; i = BitsUtil.nextSetBit(flippedA, i + 1)) {
      if(!BitsUtil.get(flippedB, i) && flipTriangle(i, flippedB) >= 0) {
        numflips += 2;
      }
    }
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest("Flips: " + numflips);
    }
    return numflips;
  }

  /**
   * Flip a single triangle, if necessary.
   *
   * @param i Triangle number
   * @param flipped Bitset to modify
   * @return number of other triangle, or -1
   */
  int flipTriangle(int i, long[] flipped) {
    final Triangle cur = tris.get(i);
    // Test edge AB:
    if(cur.ab >= 0) {
      final int ot = cur.ab;
      Triangle oth = tris.get(ot);
      Orientation orient = cur.findOrientation(oth);
      final int opp, lef, rig;
      switch(orient){
      case ORIENT_AB_BA:
        opp = oth.c;
        lef = oth.bc;
        rig = oth.ca;
        break;
      case ORIENT_AB_CB:
        opp = oth.a;
        lef = oth.ca;
        rig = oth.ab;
        break;
      case ORIENT_AB_AC:
        opp = oth.b;
        lef = oth.ab;
        rig = oth.bc;
        break;
      default:
        throw new RuntimeException("Neighbor triangles not aligned?");
      }
      if(cur.inCircle(points.get(opp))) {
        // Replace edge AB, connect c with "opp" instead.
        final int a = cur.c, b = cur.a, c = opp, d = cur.b;
        final int ab = cur.ca, bc = lef, cd = rig, da = cur.bc;
        final int ca = ot, ac = i;
        // Update current:
        cur.set(a, ab, b, bc, c, ca);
        cur.updateCircumcircle(points);
        // Update other:
        oth.set(c, cd, d, da, a, ac);
        oth.updateCircumcircle(points);
        // Update tri touching on BC and DA:
        if(bc >= 0) {
          tris.get(bc).replaceEdge(c, b, ot, i);
        }
        if(da >= 0) {
          tris.get(da).replaceEdge(a, d, i, ot);
        }
        BitsUtil.setI(flipped, i);
        BitsUtil.setI(flipped, ot);
        return ot;
      }
    }
    // Test edge BC:
    if(cur.bc >= 0) {
      final int ot = cur.bc;
      Triangle oth = tris.get(ot);
      Orientation orient = cur.findOrientation(oth);
      final int opp, lef, rig;
      switch(orient){
      case ORIENT_BC_BA:
        opp = oth.c;
        lef = oth.bc;
        rig = oth.ca;
        break;
      case ORIENT_BC_CB:
        opp = oth.a;
        lef = oth.ca;
        rig = oth.ab;
        break;
      case ORIENT_BC_AC:
        opp = oth.b;
        lef = oth.ab;
        rig = oth.bc;
        break;
      default:
        throw new RuntimeException("Neighbor triangles not aligned? " + orient);
      }
      if(cur.inCircle(points.get(opp))) {
        // Replace edge BC, connect A with "opp" instead.
        final int a = cur.a, b = cur.b, c = opp, d = cur.c;
        final int ab = cur.ab, bc = lef, cd = rig, da = cur.ca;
        final int ca = ot, ac = i;
        // Update current:
        cur.set(a, ab, b, bc, c, ca);
        cur.updateCircumcircle(points);
        // Update other:
        oth.set(c, cd, d, da, a, ac);
        oth.updateCircumcircle(points);
        // Update tri touching on BC and DA:
        if(bc >= 0) {
          tris.get(bc).replaceEdge(c, b, ot, i);
        }
        if(da >= 0) {
          tris.get(da).replaceEdge(a, d, i, ot);
        }
        BitsUtil.setI(flipped, i);
        BitsUtil.setI(flipped, ot);
        return ot;
      }
    }
    // Test edge CA:
    if(cur.ca >= 0) {
      final int ot = cur.ca;
      Triangle oth = tris.get(ot);
      Orientation orient = cur.findOrientation(oth);
      final int opp, lef, rig;
      switch(orient){
      case ORIENT_CA_BA:
        opp = oth.c;
        lef = oth.bc;
        rig = oth.ca;
        break;
      case ORIENT_CA_CB:
        opp = oth.a;
        lef = oth.ca;
        rig = oth.ab;
        break;
      case ORIENT_CA_AC:
        opp = oth.b;
        lef = oth.ab;
        rig = oth.bc;
        break;
      default:
        throw new RuntimeException("Neighbor triangles not aligned?");
      }
      if(cur.inCircle(points.get(opp))) {
        // Replace edge CA, connect B with "opp" instead.
        final int a = cur.b, b = cur.c, c = opp, d = cur.a;
        final int ab = cur.bc, bc = lef, cd = rig, da = cur.ab;
        final int ca = ot, ac = i;
        // Update current:
        cur.set(a, ab, b, bc, c, ca);
        cur.updateCircumcircle(points);
        // Update other:
        oth.set(c, cd, d, da, a, ac);
        oth.updateCircumcircle(points);
        // Update tri touching on BC and DA:
        if(bc >= 0) {
          tris.get(bc).replaceEdge(c, b, ot, i);
        }
        if(da >= 0) {
          tris.get(da).replaceEdge(a, d, i, ot);
        }
        BitsUtil.setI(flipped, i);
        BitsUtil.setI(flipped, ot);
        return ot;
      }
    }
    return -1;
  }

  /**
   * Get the convex hull only.
   * <p>
   * Note: if you also want the Delaunay Triangulation, you should get that
   * first!
   *
   * @return Convex hull
   */
  public Polygon getHull() {
    if(hull == null) {
      run(true);
    }
    DoubleMinMax minmaxX = new DoubleMinMax();
    DoubleMinMax minmaxY = new DoubleMinMax();
    List<double[]> hullp = new ArrayList<>(hull.size());
    for(IntIntPair pair : hull) {
      double[] v = points.get(pair.first);
      hullp.add(v);
      minmaxX.put(v[0]);
      minmaxY.put(v[1]);
    }
    return new Polygon(hullp, minmaxX.getMin(), minmaxX.getMax(), minmaxY.getMin(), minmaxY.getMax());
  }

  /**
   * Get the Delaunay triangulation.
   *
   * @return Triangle list
   */
  public ArrayList<Triangle> getDelaunay() {
    if(tris == null) {
      run(false);
    }
    return tris;
  }

  /**
   * Squared euclidean distance. 2d.
   *
   * @param v1 First double[]
   * @param v2 Second double[]
   * @return Quadratic distance
   */
  public static double quadraticEuclidean(double[] v1, double[] v2) {
    final double d1 = v1[0] - v2[0], d2 = v1[1] - v2[1];
    return (d1 * d1) + (d2 * d2);
  }

  /**
   * Test if the double[] AD is right of AB.
   *
   * @param a Starting point
   * @param b Reference point
   * @param d Test point
   * @return true when on the left side
   */
  boolean leftOf(double[] a, double[] b, double[] d) {
    final double bax = b[0] - a[0], bay = b[1] - a[1];
    final double dax = d[0] - a[0], day = d[1] - a[1];
    final double cross = bax * day - bay * dax;
    return cross > 1e-10 * Math.max(Math.max(bax, bay), Math.max(dax, day));
  }

  /**
   * The possible orientations two triangles can have to each other. (Shared
   * edges must have different directions!)
   *
   * @author Erich Schubert
   */
  private enum Orientation {
    ORIENT_AB_BA, ORIENT_AB_CB, ORIENT_AB_AC, ORIENT_BC_BA, ORIENT_BC_CB, ORIENT_BC_AC, ORIENT_CA_BA, ORIENT_CA_CB, ORIENT_CA_AC
  }

  /**
   * Class representing a triangle, by referencing points in a list.
   *
   * @author Erich Schubert
   */
  public static class Triangle {
    /**
     * References to points in Delaunay2D.points
     */
    public int a, b, c;

    /**
     * References to neighbor triangles
     */
    public int ab = -1, ca = -1, bc = -1;

    /**
     * Circumcircle parameters
     */
    public double r2 = -1;

    /**
     * Center double[]
     */
    public double[] m = new double[2];

    /**
     * Constructor.
     *
     * @param x
     * @param y
     * @param z
     */
    public Triangle(int x, int y, int z) {
      a = x;
      b = y;
      c = z;
    }

    /**
     * Replace an edge
     *
     * @param a First point
     * @param b Second point
     * @param ol Previous value
     * @param ne New value
     */
    void replaceEdge(int a, int b, int ol, int ne) {
      if(this.a == a && this.b == b) {
        assert (this.ab == ol) : "Edge doesn't match: " + this + " " + a + " " + b + " " + ol + " " + ne;
        this.ab = ne;
        return;
      }
      if(this.b == a && this.c == b) {
        assert (this.bc == ol) : "Edge doesn't match: " + this + " " + a + " " + b + " " + ol + " " + ne;
        this.bc = ne;
        return;
      }
      if(this.c == a && this.a == b) {
        assert (this.ca == ol) : "Edge doesn't match: " + this + " " + a + " " + b + " " + ol + " " + ne;
        this.ca = ne;
        return;
      }
    }

    /**
     * Update the triangle.
     *
     * @param a First point
     * @param ab Edge
     * @param b Second point
     * @param bc Edge
     * @param c Third point
     * @param ca Edge
     */
    void set(int a, int ab, int b, int bc, int c, int ca) {
      assert (ab != bc || ab == -1);
      assert (ab != ca || ab == -1);
      assert (bc != ca || bc == -1);
      this.a = a;
      this.ab = ab;
      this.b = b;
      this.bc = bc;
      this.c = c;
      this.ca = ca;
    }

    /**
     * Test whether a point is within the circumference circle.
     *
     * @param opp Test double[]
     * @return true when contained
     */
    public boolean inCircle(double[] opp) {
      final double dx = opp[0] - m[0], dy = opp[1] - m[1];
      return (dx * dx + dy * dy) < r2;
    }

    /**
     * Find the orientation of the triangles to each other.
     *
     * @param oth Other triangle
     * @return shared edge
     */
    Orientation findOrientation(Triangle oth) {
      if(this.a == oth.a) {
        if(this.b == oth.c) {
          return Orientation.ORIENT_AB_AC;
        }
        if(this.c == oth.b) {
          return Orientation.ORIENT_CA_BA;
        }
      }
      if(this.a == oth.b) {
        if(this.b == oth.a) {
          return Orientation.ORIENT_AB_BA;
        }
        if(this.c == oth.c) {
          return Orientation.ORIENT_CA_CB;
        }
      }
      if(this.a == oth.c) {
        if(this.b == oth.b) {
          return Orientation.ORIENT_AB_CB;
        }
        if(this.c == oth.a) {
          return Orientation.ORIENT_CA_AC;
        }
      }
      if(this.b == oth.b && this.c == oth.a) {
        return Orientation.ORIENT_BC_BA;
      }
      if(this.b == oth.c && this.c == oth.b) {
        return Orientation.ORIENT_BC_CB;
      }
      if(this.b == oth.a && this.c == oth.c) {
        return Orientation.ORIENT_BC_AC;
      }
      return null;
    }

    /**
     * Make the triangle clockwise
     */
    void makeClockwise(List<double[]> points) {
      if(!isClockwise(points)) {
        // Swap points B, C
        int t = b;
        b = c;
        c = t;
        // And the associated edges
        t = ab;
        ab = ca;
        ca = t;
      }
    }

    /**
     * Verify that the triangle is clockwise
     */
    boolean isClockwise(List<double[]> points) {
      final double[] pa = points.get(a), pb = points.get(b), pc = points.get(c);
      // Mean. NOT the circumcircle center here.
      final double mX = (pa[0] + pb[0] + pc[0]) * MathUtil.ONE_THIRD;
      final double mY = (pa[1] + pb[1] + pc[1]) * MathUtil.ONE_THIRD;

      final double max = pa[0] - mX, may = pa[1] - mY;
      final double abx = pb[0] - pa[0], aby = pb[1] - pa[1];
      return (-abx * may + aby * max <= 0);
    }

    /**
     * Copy the values from another triangle.
     *
     * @param o object to copy from
     */
    void copyFrom(Triangle o) {
      this.a = o.a;
      this.b = o.b;
      this.c = o.c;
      this.r2 = o.r2;
      this.m[0] = o.m[0];
      this.m[1] = o.m[1];
    }

    /**
     * Recompute the location and squared radius of circumcircle.
     * <p>
     * Note: numerical stability is important; and this is not entirely robust
     * to degenerate cases.
     * <p>
     * Careful: the midpoint of the circumcircle is <i>not</i> the average of
     * the corners!
     *
     * @return success
     */
    private boolean updateCircumcircle(List<double[]> points) {
      double[] pa = points.get(a), pb = points.get(b), pc = points.get(c);

      // Compute vectors from A: AB, AC:
      final double abx = pb[0] - pa[0], aby = pb[1] - pa[1];
      final double bcx = pc[0] - pb[0], bcy = pc[1] - pb[1];

      // Degenerated:
      final double D = abx * bcy - aby * bcx;
      if(D == 0) {
        m[0] = Double.NaN;
        m[1] = Double.NaN;
        r2 = Double.NaN;
        return false;
      }

      // Midpoints of AB and BC:
      final double mabx = (pa[0] + pb[0]) * .5, maby = (pa[1] + pb[1]) * .5;
      final double mbcx = (pb[0] + pc[0]) * .5, mbcy = (pb[1] + pc[1]) * .5;

      final double beta = (abx * (mbcx - mabx) + aby * (mbcy - maby)) / D;

      m[0] = mbcx - bcy * beta;
      m[1] = mbcy + bcx * beta;
      final double rx = pa[0] - m[0], ry = pa[1] - m[1];
      r2 = rx * rx + ry * ry;
      return true;
    }

    @Override
    public String toString() {
      return "Triangle [a=" + a + ", b=" + b + ", c=" + c + ", ab=" + ab + ", bc=" + bc + ", ca=" + ca + "]";
    }
  }
}
