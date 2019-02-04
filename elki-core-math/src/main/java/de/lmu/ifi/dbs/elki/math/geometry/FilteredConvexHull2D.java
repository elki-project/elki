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

/**
 * Accelerate computing the convex hull with a simple filter.
 *
 * For this filter, we maintain a simple box that is guaranteed to be fully
 * inside the convex hull. Points in this rectangle will be skipped.
 * 
 * TODO: allow using this with other bounding box agorithms, too.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - Polygon
 * @has - - - GrahamScanConvexHull2D
 */
public class FilteredConvexHull2D {
  /**
   * The current set of candidate points.
   */
  private List<double[]> points;

  /** Left bound **/
  private double leftx = Double.POSITIVE_INFINITY, //
      /** Right bound **/
      rightx = Double.NEGATIVE_INFINITY, //
      /** Top bound **/
      topy = Double.NEGATIVE_INFINITY, //
      /** Bottom bound **/
      bottomy = Double.POSITIVE_INFINITY;

  /** Top-left point */
  private double[] topleft, //
      /** Top-right point */
      topright, //
      /** bottom-right point */
      bottomright, //
      /** bottom-left point */
      bottomleft;

  /**
   * Computed polygon.
   */
  private Polygon poly = null;

  /**
   * Constructor.
   */
  public FilteredConvexHull2D() {
    this.points = new ArrayList<>();
  }

  /**
   * Add a single point to the list (this does not compute the hull!)
   *
   * @param point Point to add
   */
  public void add(double... point) {
    this.poly = null;
    if(checkCandidateUpdate(point)) {
      this.points.add(point);
    }
  }

  /**
   * Compute the convex hull.
   *
   * @return resulting polygon
   */
  private Polygon computeConvexHull() {
    GrahamScanConvexHull2D inner = new GrahamScanConvexHull2D();
    for(double[] point : points) {
      if(checkCandidate(point)) {
        inner.add(point);
      }
    }
    return inner.getHull();
  }

  private boolean checkCandidate(double[] point) {
    double v = point[0];
    return v <= leftx || v >= rightx || (v = point[1]) <= bottomy || v >= topy;
  }

  /**
   * Check whether a point is inside the current bounds, and update the bounds
   *
   * @param point New point
   * @return {@code true} if the point is potentially on the hull
   */
  private boolean checkCandidateUpdate(double[] point) {
    final double x = point[0], y = point[1];
    if(points.isEmpty()) {
      leftx = rightx = x;
      topy = bottomy = y;
      topleft = topright = bottomleft = bottomright = point;
      return true;
    }
    // A non-regular diamond spanned by left, top, right, and bottom.
    if(x <= leftx || x >= rightx || y <= bottomy || y >= topy) {
      double xpy = x + y, xmy = x - y;
      // Update bounds:
      boolean changed = false;
      if(xpy < bottomleft[0] + bottomleft[1]) {
        bottomleft = point;
        changed = true;
      }
      else if(xpy > topright[0] + topright[1]) {
        topright = point;
        changed = true;
      }
      if(xmy < topleft[0] - topleft[1]) {
        topleft = point;
        changed = true;
      }
      else if(xmy > bottomright[0] - bottomright[1]) {
        bottomright = point;
        changed = true;
      }
      if(changed) {
        leftx = Math.max(bottomleft[0], topleft[0]);
        rightx = Math.min(bottomright[0], topright[0]);
        topy = Math.min(topleft[1], topright[1]);
        bottomy = Math.max(bottomleft[1], bottomright[1]);
      }
      return true;
    }
    return false;
  }

  /**
   * Compute the convex hull, and return the resulting polygon.
   *
   * @return Polygon of the hull
   */
  public Polygon getHull() {
    return poly == null ? poly = computeConvexHull() : poly;
  }
}