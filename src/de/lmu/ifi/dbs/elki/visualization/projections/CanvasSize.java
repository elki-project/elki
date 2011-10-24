package de.lmu.ifi.dbs.elki.visualization.projections;

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
 * Size of a canvas. A 2D bounding rectangle.
 * 
 * @author Erich Schubert
 */
public class CanvasSize {
  /**
   * Minimum X
   */
  public final double minx;

  /**
   * Maximum X
   */
  public final double maxx;

  /**
   * Minimum Y
   */
  public final double miny;

  /**
   * Maximum Y
   */
  public final double maxy;

  /**
   * Constructor.
   * 
   * @param minx Minimum X
   * @param maxx Maximum X
   * @param miny Minimum Y
   * @param maxy Maximum Y
   */
  public CanvasSize(double minx, double maxx, double miny, double maxy) {
    super();
    this.minx = minx;
    this.maxx = maxx;
    this.miny = miny;
    this.maxy = maxy;
  }

  /**
   * @return the mininum X
   */
  public double getMinX() {
    return minx;
  }

  /**
   * @return the maximum X
   */
  public double getMaxX() {
    return maxx;
  }

  /**
   * @return the minimum Y
   */
  public double getMinY() {
    return miny;
  }

  /**
   * @return the maximum Y
   */
  public double getMaxY() {
    return maxy;
  }

  /**
   * @return the length on X
   */
  public double getDiffX() {
    return maxx - minx;
  }

  /**
   * @return the length on Y
   */
  public double getDiffY() {
    return maxy - miny;
  }
}