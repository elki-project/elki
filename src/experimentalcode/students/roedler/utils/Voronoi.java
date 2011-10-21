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
  public static SVGPath drawDelaunay(double[] graphSize, List<SweepHullDelaunay2D.Triangle> delaunay, Vector[] means) {
    final SVGPath path = new SVGPath();
    for(SweepHullDelaunay2D.Triangle del : delaunay) {
      path.moveTo(means[del.a].get(0), means[del.a].get(1));
      path.drawTo(means[del.b].get(0), means[del.b].get(1));
      path.moveTo(means[del.a].get(0), means[del.a].get(1));
      path.drawTo(means[del.c].get(0), means[del.c].get(1));
      path.moveTo(means[del.b].get(0), means[del.b].get(1));
      path.drawTo(means[del.c].get(0), means[del.c].get(1));
    }
    return path;
  }

  public static SVGPath drawVoronoi(double[] graphSize, List<SweepHullDelaunay2D.Triangle> delaunay, Vector[] means) {
    final SVGPath path = new SVGPath();
    for(int i = 0; i < delaunay.size(); i++) {
      SweepHullDelaunay2D.Triangle del = delaunay.get(i);
      if(del.ab > i) {
        Triangle oth = delaunay.get(del.ab);
        path.moveTo(del.cx, del.cy);
        path.drawTo(oth.cx, oth.cy);
      }
      else if(del.ab < 0) {
        double angle = lineDirection(means[del.a], means[del.b], means[del.c], new Vector(del.cx, del.cy));
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
        double angle = lineDirection(means[del.b], means[del.c], means[del.a], new Vector(del.cx, del.cy));
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
        double angle = lineDirection(means[del.c], means[del.a], means[del.b], new Vector(del.cx, del.cy));
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
  public static SVGPath drawFakeVoronoi(double[] graphSize, Vector[] meansproj) {
    final SVGPath path = new SVGPath();
    // Difference
    final double dx = meansproj[1].get(0) - meansproj[0].get(0);
    final double dy = meansproj[1].get(1) - meansproj[0].get(1);
    // Mean
    final double mx = (meansproj[0].get(0) + meansproj[1].get(0)) / 2;
    final double my = (meansproj[0].get(1) + meansproj[1].get(1)) / 2;
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

    Line2D.Double testLine = new Line2D.Double(a.get(0), a.get(1), b.get(0), b.get(1));
    Line2D.Double testSide = new Line2D.Double(p.get(0), p.get(1), c.get(0), c.get(1));

    if(testSide.intersectsLine(testLine)) {
      return Math.atan2(p.get(1) - midy, p.get(0) - midx);
    }
    else {
      return Math.atan2(midy - p.get(1), midx - p.get(0));
    }
  }
}