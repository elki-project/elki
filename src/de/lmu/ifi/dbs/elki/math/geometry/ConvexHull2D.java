package de.lmu.ifi.dbs.elki.math.geometry;

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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Restricted;

/**
 * Classes to compute the convex hull of a set of points in 2D, using the
 * classic Grahams scan. Also computes a bounding box.
 * 
 * @author Erich Schubert
 */
@Restricted("Defect?")
@Reference(authors = "Paul Graham", title = "An Efficient Algorithm for Determining the Convex Hull of a Finite Planar Set", booktitle = "Information Processing Letters 1")
public class ConvexHull2D {
  /**
   * The current set of points
   */
  private List<Vector> points;

  /**
   * Min/Max in X
   */
  private DoubleMinMax minmaxX = new DoubleMinMax();

  /**
   * Min/Max in Y
   */
  private DoubleMinMax minmaxY = new DoubleMinMax();

  /**
   * Flag to indicate that the hull has been computed.
   */
  private boolean ok = false;

  /**
   * Scaling factor if we have very small polygons.
   * 
   * TODO: needed? Does this actually improve things?
   */
  private double factor = 1.0;

  /**
   * Constructor.
   */
  public ConvexHull2D() {
    this.points = new LinkedList<Vector>();
  }

  /**
   * Add a single point to the list (this does not compute the hull!)
   * 
   * @param point Point to add
   */
  public void add(Vector point) {
    if(this.ok) {
      this.points = new LinkedList<Vector>(this.points);
      this.ok = false;
    }
    this.points.add(point);
    // Update data set extends
    minmaxX.put(point.get(0));
    minmaxY.put(point.get(1));
  }

  /**
   * Compute the convex hull.
   */
  private void computeConvexHull() {
    // Trivial cases
    if(points.size() < 3) {
      this.ok = true;
      return;
    }
    // Avoid numerical instabilities by rescaling
    double maxX = Math.max(Math.abs(minmaxX.getMin()), Math.abs(minmaxX.getMax()));
    double maxY = Math.max(Math.abs(minmaxY.getMin()), Math.abs(minmaxY.getMax()));
    if(maxX < 10.0 || maxY < 10.0) {
      factor = 10 / maxX;
      if(10 / maxY > factor) {
        factor = 10 / maxY;
      }
    }
    // Find the new origin point
    findStartingPoint();
    // Sort points for the scan
    final Vector origin = points.get(0);
    Collections.sort(this.points, new Comparator<Vector>() {
      @Override
      public int compare(Vector o1, Vector o2) {
        return isLeft(o1, o2, origin) ? +1 : 0;
      }
    });

    grahamScan();
    this.ok = true;
  }

  /**
   * Find the starting point, and sort it to the beginning of the list. The
   * starting point must be on the outer hull. Any "skyline" point will do, e.g.
   * the one with the lowest Y coordinate and for ties with the lowest X.
   */
  private void findStartingPoint() {
    // Well, we already know the best Y value...
    final double bestY = minmaxY.getMin();
    double bestX = Double.POSITIVE_INFINITY;
    int bestI = -1;
    Iterator<Vector> iter = this.points.iterator();
    for(int i = 0; iter.hasNext(); i++) {
      Vector vec = iter.next();
      if(vec.get(1) == bestY && vec.get(0) < bestX) {
        bestX = vec.get(0);
        bestI = i;
      }
    }
    assert (bestI >= 0);
    // Bring the reference object to the head.
    if(bestI > 0) {
      points.add(0, points.remove(bestI));
    }
  }

  /**
   * Get the relative X coordinate to the origin.
   * 
   * @param a
   * @param origin origin vector
   * @return relative X coordinate
   */
  private final double getRX(Vector a, Vector origin) {
    return (a.get(0) - origin.get(0)) * factor;
  }

  /**
   * Get the relative Y coordinate to the origin.
   * 
   * @param a
   * @param origin origin vector
   * @return relative Y coordinate
   */
  private final double getRY(Vector a, Vector origin) {
    return (a.get(1) - origin.get(1)) * factor;
  }

  /**
   * Test whether a point is left of the other wrt. the origin.
   * 
   * @param a Vector A
   * @param b Vector B
   * @param o Origin vector
   * @return true when left
   */
  protected final boolean isLeft(Vector a, Vector b, Vector o) {
    final double cross = getRX(a, o) * getRY(b, o) - getRY(a, o) * getRX(b, o);
    if(cross == 0) {
      // Compare manhattan distances - same angle!
      final double dista = Math.abs(getRX(a, o)) + Math.abs(getRY(a, o));
      final double distb = Math.abs(getRX(b, o)) + Math.abs(getRY(b, o));
      return dista > distb;
    }
    return cross > 0;
  }

  /**
   * Manhattan distance.
   * 
   * @param a Vector A
   * @param b Vector B
   * @return Manhattan distance
   */
  private double mdist(Vector a, Vector b) {
    return Math.abs(a.get(0) * factor - b.get(0) * factor) + Math.abs(a.get(1) * factor - b.get(1) * factor);
  }

  /**
   * Simple convexity test.
   * 
   * @param a Vector A
   * @param b Vector B
   * @param c Vector C
   * @return convexity
   */
  private final boolean isConvex(Vector a, Vector b, Vector c) {
    // We're using factor to improve numerical contrast for small polygons.
    double area = (b.get(0) * factor - a.get(0) * factor) * (c.get(1) * factor - a.get(1) * factor) - (c.get(0) * factor - a.get(0) * factor) * (b.get(1) * factor - a.get(1) * factor);
    if(area == 0) {
      return (mdist(b, c) >= mdist(a, b) + mdist(a, c));
    }
    return (area < 0);
  }

  /**
   * The actual graham scan main loop.
   */
  private void grahamScan() {
    if(points.size() < 3) {
      return;
    }
    Iterator<Vector> iter = points.iterator();
    Stack<Vector> stack = new Stack<Vector>();
    // Start with the first two points on the stack
    stack.add(iter.next());
    stack.add(iter.next());
    while(iter.hasNext()) {
      Vector next = iter.next();
      Vector curr = stack.pop();
      Vector prev = stack.peek();
      while((stack.size() > 1) && !isConvex(prev, curr, next)) {
        curr = stack.pop();
        prev = stack.peek();
      }
      stack.add(curr);
      stack.add(next);
    }
    points = stack;
  }

  /**
   * Compute the convex hull, and return the resulting polygon.
   * 
   * @return Polygon of the hull
   */
  public Polygon getHull() {
    if(!ok) {
      computeConvexHull();
    }
    return new Polygon(points, minmaxX.getMin(), minmaxX.getMax(), minmaxY.getMin(), minmaxY.getMax());
  }
}