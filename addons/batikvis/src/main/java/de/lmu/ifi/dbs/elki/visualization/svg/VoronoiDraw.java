package de.lmu.ifi.dbs.elki.visualization.svg;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.List;

import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.visualization.projections.CanvasSize;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;

/**
 * Draw the Voronoi cells
 * 
 * @author Robert Rödler
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle
 * @apiviz.uses Projection2D
 */
public class VoronoiDraw {
  /**
   * Draw the Delaunay triangulation.
   * 
   * @param proj Projection
   * @param delaunay Triangulation
   * @param means Means
   * @return Path
   */
  public static SVGPath drawDelaunay(Projection2D proj, List<SweepHullDelaunay2D.Triangle> delaunay, List<double[]> means) {
    final SVGPath path = new SVGPath();
    for(SweepHullDelaunay2D.Triangle del : delaunay) {
      path.moveTo(proj.fastProjectDataToRenderSpace(means.get(del.a)));
      path.drawTo(proj.fastProjectDataToRenderSpace(means.get(del.b)));
      path.drawTo(proj.fastProjectDataToRenderSpace(means.get(del.c)));
      path.close();
    }
    return path;
  }

  /**
   * Draw a Voronoi diagram
   * 
   * @param proj Projection
   * @param delaunay Delaunay triangulation
   * @param means Cluster means
   * @return SVG path
   */
  public static SVGPath drawVoronoi(Projection2D proj, List<SweepHullDelaunay2D.Triangle> delaunay, List<double[]> means) {
    final SVGPath path = new SVGPath();
    CanvasSize viewport = proj.estimateViewport();
    for(int i = 0; i < delaunay.size(); i++) {
      SweepHullDelaunay2D.Triangle del = delaunay.get(i);
      final double[] projcx = proj.fastProjectDataToRenderSpace(del.m.getArrayRef());
      if(del.ab > i) {
        Triangle oth = delaunay.get(del.ab);
        path.moveTo(projcx);
        path.drawTo(proj.fastProjectDataToRenderSpace(oth.m.getArrayRef()));
      }
      else if(del.ab < 0) {
        double[] dirv = VMath.minus(means.get(del.a), means.get(del.b));
        VMath.rotate90Equals(dirv);
        double[] dir = proj.fastProjectRelativeDataToRenderSpace(dirv);
        final double factor = viewport.continueToMargin(projcx, dir);
        if(factor > 0) {
          path.moveTo(projcx);
          path.relativeLineTo(factor * dir[0], factor * dir[1]);
        }
      }

      if(del.bc > i) {
        Triangle oth = delaunay.get(del.bc);
        path.moveTo(projcx);
        path.drawTo(proj.fastProjectDataToRenderSpace(oth.m.getArrayRef()));
      }
      else if(del.bc < 0) {
        double[] dirv = VMath.minus(means.get(del.b), means.get(del.c));
        VMath.rotate90Equals(dirv);
        double[] dir = proj.fastProjectRelativeDataToRenderSpace(dirv);
        final double factor = viewport.continueToMargin(projcx, dir);
        if(factor > 0) {
          path.moveTo(projcx);
          path.relativeLineTo(factor * dir[0], factor * dir[1]);
        }
      }

      if(del.ca > i) {
        Triangle oth = delaunay.get(del.ca);
        path.moveTo(projcx);
        path.drawTo(proj.fastProjectDataToRenderSpace(oth.m.getArrayRef()));
      }
      else if(del.ca < 0) {
        double[] dirv = VMath.minus(means.get(del.c), means.get(del.a));
        VMath.rotate90Equals(dirv);
        double[] dir = proj.fastProjectRelativeDataToRenderSpace(dirv);
        final double factor = viewport.continueToMargin(projcx, dir);
        if(factor > 0) {
          path.moveTo(projcx);
          path.relativeLineTo(factor * dir[0], factor * dir[1]);
        }
      }
    }
    return path;
  }

  /**
   * Fake Voronoi diagram. For two means only
   * 
   * @param proj Projection
   * @param means Mean vectors
   * @return SVG path
   */
  public static SVGPath drawFakeVoronoi(Projection2D proj, List<double[]> means) {
    CanvasSize viewport = proj.estimateViewport();
    final SVGPath path = new SVGPath();
    // Difference
    final double[] dirv = VMath.minus(means.get(1), means.get(0));
    VMath.rotate90Equals(dirv);
    double[] dir = proj.fastProjectRelativeDataToRenderSpace(dirv);
    // Mean
    final double[] mean = VMath.plus(means.get(0), means.get(1));
    VMath.timesEquals(mean, 0.5);
    double[] projmean = proj.fastProjectDataToRenderSpace(mean);

    double factor = viewport.continueToMargin(projmean, dir);
    path.moveTo(projmean[0] + factor * dir[0], projmean[1] + factor * dir[1]);
    // Inverse direction:
    dir[0] *= -1;
    dir[1] *= -1;
    factor = viewport.continueToMargin(projmean, dir);
    path.drawTo(projmean[0] + factor * dir[0], projmean[1] + factor * dir[1]);
    return path;
  }
}