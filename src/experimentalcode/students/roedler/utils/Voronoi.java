package experimentalcode.students.roedler.utils;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Class generates the Borderlines used by kMeansBorderVisualization
 * 
 * @author Robert Rödler
 */
public class Voronoi {
  SVGPlot svgp;

  Projection2D proj;

  public Voronoi(SVGPlot svgp, Projection2D proj) {
    this.svgp = svgp;
    this.proj = proj;
  }

  public Element createVoronoi(Vector[] means, Vector[] meansproj) {
    return createVoronoi(means, meansproj, false, true);
  }

  /**
   * 
   * @param means
   * @param meansproj Projections of the means
   * @param drawTriangles draw the Delaunay Triangulation
   * @param drawVoronoi draw Voronoi
   * @return path element
   */
  public Element createVoronoi(Vector[] means, Vector[] meansproj, boolean drawTriangles, boolean drawVoronoi) {
    double linesLonger = 1.2;

    final List<Vector> points = Arrays.asList(meansproj);
    final List<SweepHullDelaunay2D.Triangle> delaunay;
    if(meansproj.length > 2) {
      SweepHullDelaunay2D dt = new SweepHullDelaunay2D(points);
      delaunay = dt.getDelaunay();
    } else {
      delaunay = Collections.emptyList();
    }

    int max = 4 + (means.length - 3) * 2;
    List<Line2D.Double> lines = new ArrayList<Line2D.Double>(2 * max);

    // graphsize for checkGraphsize method
    double[] graphSize = new double[4];
    Pair<DoubleMinMax, DoubleMinMax> vp = proj.estimateViewport();
    graphSize[0] = vp.first.getMax() * linesLonger;
    graphSize[1] = vp.second.getMax() * linesLonger;
    graphSize[2] = vp.first.getMin() * linesLonger;
    graphSize[3] = vp.second.getMin() * linesLonger;

    double width, angle;

    for(int i = 0; i < delaunay.size(); i++) {
      SweepHullDelaunay2D.Triangle del = delaunay.get(i);
      if(del.ab > i) {
        Triangle oth = delaunay.get(del.ab);
        lines.add(new Line2D.Double(del.cx, del.cy, oth.cx, oth.cy));
      }
      else if(del.ab < 0) {
        angle = lineDirection(meansproj[del.a], meansproj[del.b], meansproj[del.c], new Vector(del.cx, del.cy));
        width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, new double[] { del.cx, del.cy });
        lines.add(new Line2D.Double(del.cx, del.cy, del.cx + width * Math.toDegrees(Math.cos(angle)), del.cy + width * Math.toDegrees(Math.sin(angle))));
      }

      if(del.bc > i) {
        Triangle oth = delaunay.get(del.bc);
        lines.add(new Line2D.Double(del.cx, del.cy, oth.cx, oth.cy));
      }
      else if(del.bc < 0) {
        angle = lineDirection(meansproj[del.b], meansproj[del.c], meansproj[del.a], new Vector(del.cx, del.cy));
        width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, new double[] { del.cx, del.cy });
        lines.add(new Line2D.Double(del.cx, del.cy, del.cx + width * Math.toDegrees(Math.cos(angle)), del.cy + width * Math.toDegrees(Math.sin(angle))));
      }

      if(del.ca > i) {
        Triangle oth = delaunay.get(del.ca);
        lines.add(new Line2D.Double(del.cx, del.cy, oth.cx, oth.cy));
      }
      else if(del.ca < 0) {
        angle = lineDirection(meansproj[del.c], meansproj[del.a], meansproj[del.b], new Vector(del.cx, del.cy));
        width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, new double[] { del.cx, del.cy });
        lines.add(new Line2D.Double(del.cx, del.cy, del.cx + width * Math.toDegrees(Math.cos(angle)), del.cy + width * Math.toDegrees(Math.sin(angle))));
      }
    }

    if(means.length == 2) {
      double mx, my;
      mx = (meansproj[0].get(0) + meansproj[1].get(0)) / 2;
      my = (meansproj[0].get(1) + meansproj[1].get(1)) / 2;

      double gradient;
      if(meansproj[0].get(0) - meansproj[1].get(0) != 0.0) {
        gradient = (meansproj[0].get(1) - meansproj[1].get(1)) / (meansproj[0].get(0) - meansproj[1].get(0));
        gradient = -1 / gradient;
      }
      else {
        gradient = 0.0;
      }

      double[] p = { mx, my };

      angle = Math.atan2(my - meansproj[0].get(1), mx - meansproj[0].get(0));
      angle = Math.toRadians(Math.toDegrees(angle) + 90);
      width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, p);
      lines.add(new Line2D.Double(mx, my, mx + width * Math.toDegrees(Math.cos(angle)), my + width * Math.toDegrees(Math.sin(angle))));

      angle += Math.PI;
      width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, p);
      lines.add(new Line2D.Double(mx, my, mx + width * Math.toDegrees(Math.cos(angle)), my + width * Math.toDegrees(Math.sin(angle))));
    }

    SVGPath path = new SVGPath();

    if(drawVoronoi) {
      for(Line2D.Double line : lines) {
        path.moveTo(line.getX1(), line.getY1());
        path.drawTo(line.getX2(), line.getY2());
      }
    }

    if(drawTriangles) {
      for(SweepHullDelaunay2D.Triangle del : delaunay) {
        path.moveTo(meansproj[del.a].get(0), meansproj[del.a].get(1));
        path.drawTo(meansproj[del.b].get(0), meansproj[del.b].get(1));
        path.moveTo(meansproj[del.a].get(0), meansproj[del.a].get(1));
        path.drawTo(meansproj[del.c].get(0), meansproj[del.c].get(1));
        path.moveTo(meansproj[del.b].get(0), meansproj[del.b].get(1));
        path.drawTo(meansproj[del.c].get(0), meansproj[del.c].get(1));
      }
    }

    return path.makeElement(svgp);
  }

  public double lineDirection(Vector a, Vector b, Vector c, Vector p) {
    double angle;
    double midx, midy;
    midx = (a.get(0) + b.get(0)) / 2;
    midy = (a.get(1) + b.get(1)) / 2;

    Line2D.Double testLine = new Line2D.Double(a.get(0), a.get(1), b.get(0), b.get(1));
    Line2D.Double testSide = new Line2D.Double(p.get(0), p.get(1), c.get(0), c.get(1));

    if(testSide.intersectsLine(testLine)) {
      angle = Math.atan2(p.get(1) - midy, p.get(0) - midx);
    }
    else {
      angle = Math.atan2(midy - p.get(1), midx - p.get(0));
    }
    return angle;
  }

  public Vector projVector(Vector v) {
    double[] pt = proj.fastProjectDataToRenderSpace(v);
    return new Vector(pt);
  }
}