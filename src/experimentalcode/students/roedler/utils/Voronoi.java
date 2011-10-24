package experimentalcode.students.roedler.utils;

import java.awt.geom.Line2D;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;

/**
 * Draw the Voronoi cells
 * 
 * @author Robert Rödler
 * @author Erich Schubert
 */
public class Voronoi {
  /**
   * Draw the Delaunay triangulation.
   * 
   * @param delaunay Triangulation
   * @param projmeans Projected means
   * @return Path
   */
  public static SVGPath drawDelaunay(List<SweepHullDelaunay2D.Triangle> delaunay, List<Vector> projmeans) {
    final SVGPath path = new SVGPath();
    for(SweepHullDelaunay2D.Triangle del : delaunay) {
      path.moveTo(projmeans.get(del.a));
      path.drawTo(projmeans.get(del.b));
      path.moveTo(projmeans.get(del.a));
      path.drawTo(projmeans.get(del.c));
      path.moveTo(projmeans.get(del.b));
      path.drawTo(projmeans.get(del.c));
    }
    return path;
  }

  public static SVGPath drawVoronoi(double[] graphSize, List<SweepHullDelaunay2D.Triangle> delaunay, List<Vector> means) {
    final SVGPath path = new SVGPath();
    for(int i = 0; i < delaunay.size(); i++) {
      SweepHullDelaunay2D.Triangle del = delaunay.get(i);
      if(del.ab > i) {
        Triangle oth = delaunay.get(del.ab);
        path.moveTo(del.cx, del.cy);
        path.drawTo(oth.cx, oth.cy);
      }
      else if(del.ab < 0) {
        double angle = lineDirection(means.get(del.a), means.get(del.b), means.get(del.c), new Vector(del.cx, del.cy));
        double len = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, new double[] { del.cx, del.cy });
        path.moveTo(del.cx, del.cy);
        path.drawTo(del.cx + len * Math.cos(angle), del.cy + len * Math.sin(angle));
      }

      if(del.bc > i) {
        Triangle oth = delaunay.get(del.bc);
        path.moveTo(del.cx, del.cy);
        path.drawTo(oth.cx, oth.cy);
      }
      else if(del.bc < 0) {
        double angle = lineDirection(means.get(del.b), means.get(del.c), means.get(del.a), new Vector(del.cx, del.cy));
        double len = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, new double[] { del.cx, del.cy });
        path.moveTo(del.cx, del.cy);
        path.drawTo(del.cx + len * Math.cos(angle), del.cy + len * Math.sin(angle));
      }

      if(del.ca > i) {
        Triangle oth = delaunay.get(del.ca);
        path.moveTo(del.cx, del.cy);
        path.drawTo(oth.cx, oth.cy);
      }
      else if(del.ca < 0) {
        double angle = lineDirection(means.get(del.c), means.get(del.a), means.get(del.b), new Vector(del.cx, del.cy));
        double len = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, new double[] { del.cx, del.cy });
        path.moveTo(del.cx, del.cy);
        path.drawTo(del.cx + len * Math.cos(angle), del.cy + len * Math.sin(angle));
      }
    }
    return path;
  }

  /**
   * Fake voronoi. For two means only
   * 
   * @param graphSize Graph size
   * @param meansproj Projected means.
   * @return SVG path
   */
  public static SVGPath drawFakeVoronoi(double[] graphSize, List<Vector> meansproj) {
    final SVGPath path = new SVGPath();
    // Difference
    final double dx = meansproj.get(1).get(0) - meansproj.get(0).get(0);
    final double dy = meansproj.get(1).get(1) - meansproj.get(0).get(1);
    // Mean
    final double mx = (meansproj.get(0).get(0) + meansproj.get(1).get(0)) / 2;
    final double my = (meansproj.get(0).get(1) + meansproj.get(1).get(1)) / 2;
    // As double[]
    final double[] p = { mx, my };

    final double angle = Math.atan2(dy, dx);
    double len = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle + Math.PI / 2, p);
    path.moveTo(mx, my);
    path.relativeLineTo(len * Math.cos(angle + Math.PI / 2), len * Math.sin(angle + Math.PI / 2));
    len = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle - Math.PI / 2, p);
    path.moveTo(mx, my);
    path.relativeLineTo(len * Math.cos(angle - Math.PI / 2), len * Math.sin(angle - Math.PI / 2));
    return path;
  }

  private static double lineDirection(Vector a, Vector b, Vector c, Vector p) {
    final double midx = (a.get(0) + b.get(0)) / 2;
    final double midy = (a.get(1) + b.get(1)) / 2;

    if(Line2D.linesIntersect(p.get(0), p.get(1), c.get(0), c.get(1), a.get(0), a.get(1), b.get(0), b.get(1))) {
      return Math.atan2(p.get(1) - midy, p.get(0) - midx);
    }
    else {
      return Math.atan2(midy - p.get(1), midx - p.get(0));
    }
  }
}