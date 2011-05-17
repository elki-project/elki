package experimentalcode.students.roedler.utils;

import org.w3c.dom.Element;

import java.awt.geom.Line2D;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.students.roedler.utils.convexhull.DelaunayT;

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
   * @param dT draw the Delaunay Triangulation
   * @param dV draw Voronoi
   * @return path element
   */
  public Element createVoronoi(Vector[] means, Vector[] meansproj, boolean dT, boolean dV) {

    boolean drawVoronoi = dV;
    boolean drawTriangles = dT;
    double linesLonger = 1.2;

    Triangle delaunay = null;
    if(means.length > 3) {
      DelaunayT dt = new DelaunayT();

      delaunay = dt.start(means);
      dt.clearDelaunay();
    }
    else if(means.length == 3) {
      delaunay = new Triangle(0, 1, 2);
      setCircumcircleCenter(delaunay, means);
    }

    Triangle del = delaunay;
    while(del != null) {
      del.setCCCVector(projVector(del.getCCCVector()));
      del = del.next;
    }

    del = delaunay;
    Triangle opponent;

    int i;

    int max = 4 + (means.length - 3) * 2;
    Line2D.Double[] Lines = new Line2D.Double[2 * max];

    for(i = 0; i < Lines.length; i++) {
      Lines[i] = new Line2D.Double(0.0, 0.0, 0.0, 0.0);
    }
    i = 0;

    // graphsize for checkGraphsize method
    double[] graphSize = new double[4];
    graphSize[0] = proj.estimateViewport().first.getMax() * linesLonger;
    graphSize[1] = proj.estimateViewport().second.getMax() * linesLonger;
    graphSize[2] = proj.estimateViewport().first.getMin() * linesLonger;
    graphSize[3] = proj.estimateViewport().second.getMin() * linesLonger;

    double width, angle;

    while(del != null) {
      opponent = Triangle.getOpponentTriangle(delaunay, del.v1, del.v2, del.v3);
      if(opponent != null) {
        if(!lineInList(Lines, del.ccX, del.ccY, opponent.ccX, opponent.ccY, i)) {
          Lines[i].setLine(del.ccX, del.ccY, opponent.ccX, opponent.ccY);
          i++;
        }
      }
      else {
        angle = lineDirection(meansproj[del.v1], meansproj[del.v2], meansproj[del.v3], del.getCCCVector());
        width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, del.getCCCDouble());
        Lines[i].setLine(del.ccX, del.ccY, del.ccX + width * Math.toDegrees(Math.cos(angle)), del.ccY + width * Math.toDegrees(Math.sin(angle)));
        i++;
      }

      opponent = Triangle.getOpponentTriangle(delaunay, del.v1, del.v3, del.v2);
      if(opponent != null) {
        if(!lineInList(Lines, del.ccX, del.ccY, opponent.ccX, opponent.ccY, i)) {
          Lines[i].setLine(del.ccX, del.ccY, opponent.ccX, opponent.ccY);
          i++;
        }
      }
      else {
        angle = lineDirection(meansproj[del.v1], meansproj[del.v3], meansproj[del.v2], del.getCCCVector());
        width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, del.getCCCDouble());
        Lines[i].setLine(del.ccX, del.ccY, del.ccX + width * Math.toDegrees(Math.cos(angle)), del.ccY + width * Math.toDegrees(Math.sin(angle)));
        i++;
      }

      opponent = Triangle.getOpponentTriangle(delaunay, del.v2, del.v3, del.v1);
      if(opponent != null) {
        if(!lineInList(Lines, del.ccX, del.ccY, opponent.ccX, opponent.ccY, i)) {
          Lines[i].setLine(del.ccX, del.ccY, opponent.ccX, opponent.ccY);
          i++;
        }
      }
      else {
        angle = lineDirection(meansproj[del.v2], meansproj[del.v3], meansproj[del.v1], del.getCCCVector());
        width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, del.getCCCDouble());
        Lines[i].setLine(del.ccX, del.ccY, del.ccX + width * Math.toDegrees(Math.cos(angle)), del.ccY + width * Math.toDegrees(Math.sin(angle)));
        i++;
      }
      del = del.next;
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
      Lines[0].setLine(mx, my, mx + width * Math.toDegrees(Math.cos(angle)), my + width * Math.toDegrees(Math.sin(angle)));

      angle += Math.PI;
      width = DistanceFunctionDrawUtils.checkGraphSize(graphSize, angle, p);
      Lines[1].setLine(mx, my, mx + width * Math.toDegrees(Math.cos(angle)), my + width * Math.toDegrees(Math.sin(angle)));
      i = 2;
    }

    SVGPath path = new SVGPath();

    for(int j = 0; j < i && drawVoronoi; j++) {
      path.moveTo(Lines[j].getX1(), Lines[j].getY1());
      path.drawTo(Lines[j].getX2(), Lines[j].getY2());
    }

    del = delaunay;
    while(del != null && drawTriangles) {
      path.moveTo(meansproj[del.v1].get(0), meansproj[del.v1].get(1));
      path.drawTo(meansproj[del.v2].get(0), meansproj[del.v2].get(1));
      path.moveTo(meansproj[del.v1].get(0), meansproj[del.v1].get(1));
      path.drawTo(meansproj[del.v3].get(0), meansproj[del.v3].get(1));
      path.moveTo(meansproj[del.v2].get(0), meansproj[del.v2].get(1));
      path.drawTo(meansproj[del.v3].get(0), meansproj[del.v3].get(1));
      del = del.next;
    }

    return path.makeElement(svgp);
  }

  public Vector projVector(Vector v) {
    double[] pt = proj.fastProjectDataToRenderSpace(v);
    return new Vector(pt);
  }

  public boolean lineInList(Line2D[] list, double p1X, double p1Y, double p2X, double p2Y, int length) {
    for(int i = 0; i < length; i++) {
      if(!((list[i].getX1() == 0.0) && (list[i].getX2() == 0.0) && (list[i].getY1() == 0.0) && (list[i].getY2() == 0.0))) {
        if((list[i].getX1() == p1X || list[i].getX2() == p1X) && (list[i].getX1() == p2X || list[i].getX2() == p2X)) {
          if((list[i].getY1() == p1Y || list[i].getY2() == p1Y) && (list[i].getY1() == p2Y || list[i].getY2() == p2Y)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static void setCircumcircleCenter(Triangle t, Vector[] points) {
    Vector center;
    center = getCircumcircleCenter(points[t.v1], points[t.v2], points[t.v3]);
    t.ccX = center.get(0);
    t.ccY = center.get(1);
  }

  public static boolean pointInside(Triangle t, Vector[] points) {

    double x, y;
    x = t.ccX - points[t.v1].get(0);
    y = t.ccY - points[t.v1].get(1);

    double radius = Math.sqrt(x * x + y * y);

    for(int i = 0; i < points.length; i++) {
      if(!(i == t.v1 || i == t.v2 || i == t.v3)) {
        x = t.ccX - points[i].get(0);
        y = t.ccY - points[i].get(1);
        if(Math.sqrt(x * x + y * y) <= radius) {
          return true;
        }
      }
    }
    return false;
  }

  public static double[] getCircumcircleCenter(double[] a, double[] b, double[] c) {

    Vector va = new Vector(2);
    va.set(0, a[0]);
    va.set(1, a[1]);
    Vector vb = new Vector(2);
    vb.set(0, b[0]);
    vb.set(1, b[1]);
    Vector vc = new Vector(2);
    vc.set(0, c[0]);
    vc.set(1, c[1]);

    Vector vret = getCircumcircleCenter(va, vb, vc);
    double[] ret = new double[2];
    ret[0] = vret.get(0);
    ret[1] = vret.get(1);

    return ret;
  }

  public static Vector getCircumcircleCenter(Vector a, Vector b, Vector c) {

    double u, v, d;

    u = (b.get(1) - c.get(1)) * (a.get(0) * a.get(0) + a.get(1) * a.get(1)) + (c.get(1) - a.get(1)) * (b.get(0) * b.get(0) + b.get(1) * b.get(1)) + (a.get(1) - b.get(1)) * (c.get(0) * c.get(0) + c.get(1) * c.get(1));

    v = (c.get(0) - b.get(0)) * (a.get(0) * a.get(0) + a.get(1) * a.get(1)) + (a.get(0) - c.get(0)) * (b.get(0) * b.get(0) + b.get(1) * b.get(1)) + (b.get(0) - a.get(0)) * (c.get(0) * c.get(0) + c.get(1) * c.get(1));

    d = a.get(0) * b.get(1) + b.get(0) * c.get(1) + c.get(0) * a.get(1) - a.get(0) * c.get(1) - b.get(0) * a.get(1) - c.get(0) * b.get(1);

    double x = 0.5 * u / d;
    double y = 0.5 * v / d;

    Vector ret = new Vector(2);
    ret.set(0, x);
    ret.set(1, y);

    return ret;
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
}