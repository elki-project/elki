package de.lmu.ifi.dbs.elki.math.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
/**
 * Compute the Convex Hull and/or Delaunay Triangulation, using the sweep-hull
 * approach of David Sinclair.
 * 
 * Note: This implementation does not check or handle duplicate points!
 * 
 * @author Erich Schubert
 */
@Reference(authors = "David Sinclair", title = "S-hull: a fast sweep-hull routine for Delaunay triangulation", booktitle = "Online: http://s-hull.org/")
public class SweepHullDelaunay2D {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(SweepHullDelaunay2D.class);

  /**
   * The current set of points.
   * 
   * Note: this list should not be changed after running the algorithm, since we
   * use it for object indexing, and the ids should not change
   */
  private List<Vector> points;

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
    this(new ArrayList<Vector>());
  }

  /**
   * Constructor.
   * 
   * @param points Existing points
   */
  public SweepHullDelaunay2D(List<Vector> points) {
    this.points = points;
  }

  /**
   * Add a single point to the list (this does not compute or update the
   * triangulation!)
   * 
   * @param point Point to add
   */
  public void add(Vector point) {
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
    hull = new LinkedList<IntIntPair>();
    tris = hullonly ? null : new ArrayList<Triangle>(len);

    final Vector seed;
    final int seedid = 0;
    final DoubleIntPair[] sort = new DoubleIntPair[len];
    // TODO: remove duplicates.

    // Select seed, sort by squared euclidean distance
    {
      Iterator<Vector> iter = points.iterator();
      seed = iter.next();
      for(int i = 0; iter.hasNext(); i++) {
        assert (i < len);
        Vector p = iter.next();
        // Pair with distance, list-position
        sort[i] = new DoubleIntPair(quadraticEuclidean(seed, p), i + 1);
      }
      assert (sort[len - 1] != null);
      Arrays.sort(sort);
    }
    assert (sort[0].first > 0);
    // final Vector seed2 = points.get(sort[0].second);
    final int seed2id = sort[0].second;
    int start = 1;

    // Find minimal triangle for these two points:
    Triangle besttri = new Triangle(seedid, seed2id, -1);
    {
      besttri.r2 = Double.MAX_VALUE;
      Triangle testtri = new Triangle(seedid, seed2id, -1);
      int besti = -1;
      for(int i = start; i < len; i++) {
        // Update test triad
        testtri.c = sort[i].second;
        if(testtri.updateCircumcircle(points) && testtri.r2 < besttri.r2) {
          besttri.copyFrom(testtri);
          besti = i;
        }
        else if(besttri.r2 * 4 < sort[i].first) {
          // Stop early, points are too far away from seed.
          break;
        }
      }
      assert (besti != -1);
      // Rearrange - remove third seed point.
      if(besti > 1) {
        DoubleIntPair tmp = sort[besti];
        System.arraycopy(sort, 1, sort, 2, besti - 1);
        sort[1] = tmp;
      }
    }
    start = 2; // First two points have already been processed.

    // Make right-handed:
    besttri.makeClockwise(points);
    // Seed triangulation
    if(!hullonly) {
      tris.add(besttri);
    }
    // Seed convex hull
    hull.add(new IntIntPair(besttri.a, 0));
    hull.add(new IntIntPair(besttri.b, 0));
    hull.add(new IntIntPair(besttri.c, 0));

    if(logger.isDebuggingFinest()) {
      debugHull();
    }

    // Resort from triangle center
    Vector center = besttri.m;
    for(int i = start; i < len; i++) {
      sort[i].first = quadraticEuclidean(center, points.get(sort[i].second));
    }
    Arrays.sort(sort, start, len);

    // Grow hull and triangles
    for(int i = start; i < len; i++) {
      final int pointId = sort[i].second;
      final Vector newpoint = points.get(pointId);

      LinkedList<Triangle> newtris = hullonly ? null : new LinkedList<Triangle>();
      // We identify edges by their starting point. -1 is invalid.
      int hstart = -1, hend = -1;
      // Find first and last consecutive visible edge, backwards:
      {
        Iterator<IntIntPair> iter = hull.descendingIterator();
        IntIntPair next = hull.getFirst();
        Vector nextV = points.get(next.first);
        for(int pos = hull.size() - 1; iter.hasNext(); pos--) {
          IntIntPair prev = iter.next();
          Vector prevV = points.get(prev.first);
          // Not yet visible:
          if(hend < 0) {
            if(leftOf(prevV, nextV, newpoint)) {
              hstart = pos;
              hend = pos;
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
        Vector prevV = points.get(prev.first);
        while(iter.hasNext()) {
          IntIntPair next = iter.next();
          Vector nextV = points.get(next.first);
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
        firsttri = -1;
        lasttri = -1;
      }
      else {
        final int tristart = tris.size();
        firsttri = tristart;
        lasttri = tristart + newtris.size() - 1;
      }
      final int hullsize = hull.size();
      if(logger.isDebuggingFinest()) {
        logger.debugFinest("Size: " + hullsize + " start: " + hstart + " end: " + hend);
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
          if(iter.hasPrevious()) {
            iter.previous().second = firsttri;
          }
          else {
            hull.getLast().second = firsttri;
          }
        }
      }
      else {
        // System.err.println("Case #2 "+pointId+" "+hstart+" "+hend+" "+hullsize);
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
      if(logger.isDebuggingFinest()) {
        debugHull();
      }
      if(!hullonly) {
        final int tristart = tris.size();
        // Connect triads (they are ordered)
        Iterator<Triangle> iter = newtris.iterator();
        for(int o = 0; iter.hasNext(); o++) {
          // This triangle has num tristart + o.
          Triangle cur = iter.next();
          if(o > 0) {
            cur.ca = tristart + o - 1; // previously added triangle
          }
          else {
            cur.ca = -1; // outside
          }
          if(iter.hasNext()) {
            cur.ab = tristart + o + 1; // next triangle
          }
          else {
            cur.ab = -1; // outside
          }
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
      BitSet flippedA = new BitSet(size);
      BitSet flippedB = new BitSet(size);
      // Initial flip
      int flipped = flipTriangles(null, flippedA);
      for(int iterations = 1; iterations < 2000 && flipped > 0; iterations++) {
        if(iterations % 2 == 1) {
          flipped = flipTriangles(flippedA, flippedB);
        }
        else {
          flipped = flipTriangles(flippedB, flippedA);
        }
      }
    }
  }

  /**
   * Debug helper
   */
  void debugHull() {
    StringBuffer buf = new StringBuffer();
    for(IntIntPair p : hull) {
      buf.append(p).append(" ");
    }
    logger.debugFinest(buf);
  }

  /**
   * Flip triangles as necessary
   * 
   * @param flippedA Bit set for triangles to test
   * @param flippedB Bit set to mark triangles as done
   */
  int flipTriangles(BitSet flippedA, BitSet flippedB) {
    final int size = tris.size();
    int numflips = 0;
    flippedB.clear();
    if(flippedA == null) {
      for(int i = 0; i < size; i++) {
        if(flipTriangle(i, flippedB) > 0) {
          numflips += 2;
        }
      }
    }
    else {
      for(int i = flippedA.nextSetBit(0); i > -1; i = flippedA.nextSetBit(i + 1)) {
        if(flipTriangle(i, flippedB) > 0) {
          numflips += 2;
        }
      }
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
  int flipTriangle(int i, BitSet flipped) {
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
        flipped.set(i);
        flipped.set(ot);
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
        throw new RuntimeException("Neighbor triangles not aligned?");
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
        flipped.set(i);
        flipped.set(ot);
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
        flipped.set(i);
        flipped.set(ot);
        return ot;
      }
    }
    return -1;
  }

  /**
   * Get the convex hull only.
   * 
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
    List<Vector> hullp = new ArrayList<Vector>(hull.size());
    for(IntIntPair pair : hull) {
      Vector v = points.get(pair.first);
      hullp.add(v);
      minmaxX.put(v.get(0));
      minmaxY.put(v.get(1));
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
   * @param v1 First vector
   * @param v2 Second vector
   * @return Quadratic distance
   */
  public static double quadraticEuclidean(Vector v1, Vector v2) {
    final double d1 = v1.get(0) - v2.get(0);
    final double d2 = v1.get(1) - v2.get(1);
    return (d1 * d1) + (d2 * d2);
  }

  /**
   * Test if the vector AD is right of AB.
   * 
   * @param a Starting point
   * @param b Reference point
   * @param d Test point
   * @return true when on the left side 
   */
  boolean leftOf(Vector a, Vector b, Vector d) {
    final double bax = b.get(0) - a.get(0);
    final double bay = b.get(1) - a.get(1);
    final double dax = d.get(0) - a.get(0);
    final double day = d.get(1) - a.get(1);
    final double cross = bax * day - bay * dax;
    return cross > 0;
  }

  /**
   * The possible orientations two triangles can have to each other. (Shared
   * edges must have different directions!)
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  static enum Orientation {
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
     * Center vector
     */
    public Vector m = new Vector(2);

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
     * @param opp Test vector
     * @return true when contained
     */
    public boolean inCircle(Vector opp) {
      double dx = opp.get(0) - m.get(0);
      double dy = opp.get(1) - m.get(1);
      return (dx * dx + dy * dy) <= r2;
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
      if(this.b == oth.b) {
        if(this.c == oth.a) {
          return Orientation.ORIENT_BC_BA;
        }
      }
      if(this.b == oth.c) {
        if(this.c == oth.b) {
          return Orientation.ORIENT_BC_CB;
        }
      }
      if(this.b == oth.a) {
        if(this.c == oth.c) {
          return Orientation.ORIENT_BC_AC;
        }
      }
      return null;
    }

    /**
     * Make the triangle clockwise
     */
    void makeClockwise(List<Vector> points) {
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
    boolean isClockwise(List<Vector> points) {
      // Mean
      double centX = (points.get(a).get(0) + points.get(b).get(0) + points.get(c).get(0)) / 3.0f;
      double centY = (points.get(a).get(1) + points.get(b).get(1) + points.get(c).get(1)) / 3.0f;

      double dr0 = points.get(a).get(0) - centX, dc0 = points.get(a).get(1) - centY;
      double dx01 = points.get(b).get(0) - points.get(a).get(0), dy01 = points.get(b).get(1) - points.get(a).get(1);

      double df = -dx01 * dc0 + dy01 * dr0;
      return (df <= 0);
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
      this.m.set(0, o.m.get(0));
      this.m.set(1, o.m.get(1));
    }

    /**
     * Recompute the location and squared radius of circumcircle.
     * 
     * Note: numerical stability is important!
     * 
     * @return success
     */
    boolean updateCircumcircle(List<Vector> points) {
      Vector pa = points.get(a), pb = points.get(b), pc = points.get(c);

      // Compute vectors from A: AB, AC:
      final double abx = pb.get(0) - pa.get(0), aby = pb.get(1) - pa.get(1);
      final double acx = pc.get(0) - pa.get(0), acy = pc.get(1) - pa.get(1);

      // Squared euclidean lengths
      final double ablen = abx * abx + aby * aby;
      final double aclen = acx * acx + acy * acy;

      // Compute D
      final double D = 2 * (abx * acy - aby * acx);
      
      // No circumcircle:
      if(D == 0) {
        return false;
      }

      // Compute offset:
      final double offx = (acy * ablen - aby * aclen) / D;
      final double offy = (abx * aclen - acx * ablen) / D;

      
      // Avoid degeneration:
      r2 = offx * offx + offy * offy;
      if((r2 > 1e10 * ablen || r2 > 1e10 * aclen)) {
        return false;
      }

      m.set(0, pa.get(0) + offx);
      m.set(1, pa.get(1) + offy);
      return true;
    }

    @Override
    public String toString() {
      return "Triangle [a=" + a + ", b=" + b + ", c=" + c + ", ab=" + ab + ", ac=" + ca + ", bc=" + bc + "]";
    }
  }

  public static void main(String[] args) {
    SweepHullDelaunay2D d = new SweepHullDelaunay2D();

    Random r = new Random(1);
    final int num = 100000;
    for(int i = 0; i < num; i++) {
      final Vector v = new Vector(r.nextDouble(), r.nextDouble());
      // System.err.println(i + ": " + FormatUtil.format(v.getArrayRef(), " "));
      d.add(v);
    }
    d.run(false);
  }
}