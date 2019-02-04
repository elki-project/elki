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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Object representation consisting of (multiple) polygons.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @has - - - Polygon
 */
public class PolygonsObject implements SpatialComparable {
  /**
   * Static (empty) prototype
   */
  public static final PolygonsObject PROTOTYPE = new PolygonsObject(null);

  /**
   * The polygons
   */
  private Collection<Polygon> polygons;

  /**
   * Constructor.
   * 
   * @param polygons Polygons
   */
  public PolygonsObject(Collection<Polygon> polygons) {
    super();
    this.polygons = polygons;
    if(this.polygons == null) {
      this.polygons = Collections.emptyList();
    }
  }

  /**
   * Access the polygon data.
   * 
   * @return Polygon collection
   */
  public Collection<Polygon> getPolygons() {
    return Collections.unmodifiableCollection(polygons);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    appendToBuffer(buf);
    return buf.toString();
  }

  /**
   * Append polygons to the buffer.
   * 
   * @param buf Buffer to append to
   */
  public void appendToBuffer(StringBuilder buf) {
    Iterator<Polygon> iter = polygons.iterator();
    while(iter.hasNext()) {
      Polygon poly = iter.next();
      poly.appendToBuffer(buf);
      if(iter.hasNext()) {
        buf.append(" -- ");
      }
    }
  }

  @Override
  public int getDimensionality() {

    assert (!polygons.isEmpty());
    return polygons.iterator().next().getDimensionality();
  }

  @Override
  public double getMin(int dimension) {
    double min = Double.MAX_VALUE;
    for(Polygon p : polygons) {
      min = Math.min(min, p.getMin(dimension));
    }
    return min;
  }

  @Override
  public double getMax(int dimension) {
    double max = Double.MIN_VALUE;
    for(Polygon p : polygons) {
      max = Math.max(max, p.getMin(dimension));
    }
    return max;
  }
}