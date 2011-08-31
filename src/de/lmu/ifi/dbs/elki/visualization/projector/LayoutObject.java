package de.lmu.ifi.dbs.elki.visualization.projector;
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

import de.lmu.ifi.dbs.elki.visualization.projections.Projection;

/**
 * Layout object for generating a UI.
 * 
 * @author Erich Schubert
 */
public class LayoutObject {
  /**
   * Requested x (relative)
   */
  public final double reqx;

  /**
   * Requested y (relative)
   */
  public final double reqy;

  /**
   * Requested width
   */
  public final double reqw;

  /**
   * Requested height
   */
  public final double reqh;

  /**
   * Projection to use
   */
  public final Projection proj;

  /**
   * Constructor.
   * 
   * @param reqx Requested x (relative)
   * @param reqy Requested y (relative)
   * @param reqw Requested width
   * @param reqh Requested height
   * @param proj Projection to use
   */
  public LayoutObject(double reqx, double reqy, double reqw, double reqh, Projection proj) {
    super();
    this.reqx = reqx;
    this.reqy = reqy;
    this.reqw = reqw;
    this.reqh = reqh;
    this.proj = proj;
  }
}