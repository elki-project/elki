package de.lmu.ifi.dbs.elki.visualization.svg;

import java.util.List;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;

/**
 * Utility class to draw hypercubes, wireframe and filled.
 * 
 * @author Erich Schubert
 */
public class SVGHyperSphere {
  /**
   * Factor used for approximating circles with cubic beziers.
   * 
   * kappa = 4 * (Math.sqrt(2)-1)/3
   */
  public final static double EUCLIDEAN_KAPPA = 0.5522847498;

  /**
   * Wireframe "manhattan" hypersphere
   * 
   * @param <V> vector type
   * @param <D> radius
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param rad radius
   * @return path element
   */
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<?, ?>> Element drawManhattan(SVGPlot svgp, VisualizationProjection proj, V mid, D rad) {
    Vector v_mid = mid.getColumnVector();
    // Vector r_mid = proj.projectDataToRenderSpace(mid);
    List<Integer> dims = proj.computeVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(Integer dim : dims) {
      Vector v1 = v_mid.copy();
      v1.set(dim, v1.get(dim) + rad.doubleValue());
      Vector v2 = v_mid.copy();
      v2.set(dim, v2.get(dim) - rad.doubleValue());
      v1 = proj.projectDataToRenderSpace(v1);
      v2 = proj.projectDataToRenderSpace(v2);
      for(Integer dim2 : dims) {
        if(dim < dim2) {
          Vector v3 = v_mid.copy();
          v3.set(dim2, v3.get(dim2) + rad.doubleValue());
          Vector v4 = v_mid.copy();
          v4.set(dim2, v4.get(dim2) - rad.doubleValue());
          v3 = proj.projectDataToRenderSpace(v3);
          v4 = proj.projectDataToRenderSpace(v4);

          path.moveTo(v1.get(0), v1.get(1));
          path.drawTo(v3.get(0), v3.get(1));
          path.moveTo(v1.get(0), v1.get(1));
          path.drawTo(v4.get(0), v4.get(1));
          path.moveTo(v2.get(0), v2.get(1));
          path.drawTo(v3.get(0), v3.get(1));
          path.moveTo(v2.get(0), v2.get(1));
          path.drawTo(v4.get(0), v4.get(1));
          path.close();
        }
      }
    }
    return path.makeElement(svgp);
  }

  /**
   * Wireframe "euclidean" hypersphere
   * 
   * @param <V> vector type
   * @param <D> radius
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param rad radius
   * @return path element
   */
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<?, ?>> Element drawEuclidean(SVGPlot svgp, VisualizationProjection proj, V mid, D rad) {
    Vector v_mid = mid.getColumnVector();
    // Vector r_mid = proj.projectDataToRenderSpace(mid);
    List<Integer> dims = proj.computeVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(Integer dim : dims) {
      Vector v1 = v_mid.copy();
      v1.set(dim, v1.get(dim) + rad.doubleValue());
      Vector v2 = v_mid.copy();
      v2.set(dim, v2.get(dim) - rad.doubleValue());
      v1 = proj.projectDataToRenderSpace(v1);
      v2 = proj.projectDataToRenderSpace(v2);
      // delta vector
      Vector d1 = new Vector(v1.getDimensionality());
      d1.set(dim, rad.doubleValue());
      d1 = proj.projectRelativeDataToRenderSpace(d1);
      for(Integer dim2 : dims) {
        if(dim < dim2) {
          Vector v3 = v_mid.copy();
          v3.set(dim2, v3.get(dim2) + rad.doubleValue());
          Vector v4 = v_mid.copy();
          v4.set(dim2, v4.get(dim2) - rad.doubleValue());
          v3 = proj.projectDataToRenderSpace(v3);
          v4 = proj.projectDataToRenderSpace(v4);
          // delta vector
          Vector d2 = new Vector(v2.getDimensionality());
          d2.set(dim2, rad.doubleValue());
          d2 = proj.projectRelativeDataToRenderSpace(d2);

          path.moveTo(v1.get(0), v1.get(1));
          path.cubicTo(v1.get(0) + d2.get(0) * EUCLIDEAN_KAPPA, v1.get(1) + d2.get(1) * EUCLIDEAN_KAPPA, v3.get(0) + d1.get(0) * EUCLIDEAN_KAPPA, v3.get(1) + d1.get(1) * EUCLIDEAN_KAPPA, v3.get(0), v3.get(1));
          path.cubicTo(v3.get(0) - d1.get(0) * EUCLIDEAN_KAPPA, v3.get(1) - d1.get(1) * EUCLIDEAN_KAPPA, v2.get(0) + d2.get(0) * EUCLIDEAN_KAPPA, v2.get(1) + d2.get(1) * EUCLIDEAN_KAPPA, v2.get(0), v2.get(1));
          path.cubicTo(v2.get(0) - d2.get(0) * EUCLIDEAN_KAPPA, v2.get(1) - d2.get(1) * EUCLIDEAN_KAPPA, v4.get(0) - d1.get(0) * EUCLIDEAN_KAPPA, v4.get(1) - d1.get(1) * EUCLIDEAN_KAPPA, v4.get(0), v4.get(1));
          path.cubicTo(v4.get(0) + d1.get(0) * EUCLIDEAN_KAPPA, v4.get(1) + d1.get(1) * EUCLIDEAN_KAPPA, v1.get(0) - d2.get(0) * EUCLIDEAN_KAPPA, v1.get(1) - d2.get(1) * EUCLIDEAN_KAPPA, v1.get(0), v1.get(1));
          path.close();
        }
      }
    }
    return path.makeElement(svgp);
  }

  /**
   * Wireframe "Lp" hypersphere
   * 
   * @param <V> vector type
   * @param <D> radius
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param rad radius
   * @param p L_p value
   * @return path element
   */
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<?, ?>> Element drawLp(SVGPlot svgp, VisualizationProjection proj, V mid, D rad, double p) {
    Vector v_mid = mid.getColumnVector();
    // Vector r_mid = proj.projectDataToRenderSpace(mid);
    List<Integer> dims = proj.computeVisibleDimensions2D();

    final double kappax, kappay;
    if(p > 1.) {
      double kappal = Math.pow(0.5, 1. / p);
      kappax = Math.min(1.3, 4. * (2 * kappal - 1) / 3.);
      kappay = 0;
    }
    else if(p < 1.) {
      double kappal = 1 - Math.pow(0.5, 1. / p);
      kappax = 0;
      kappay = Math.min(1.3, 4. * (2 * kappal - 1) / 3.);
    }
    else {
      kappax = 0;
      kappay = 0;
    }
    //LoggingUtil.warning("kappax: " + kappax + " kappay: " + kappay);

    SVGPath path = new SVGPath();
    for(Integer dim : dims) {
      Vector vp0 = v_mid.copy();
      vp0.set(dim, vp0.get(dim) + rad.doubleValue());
      Vector vm0 = v_mid.copy();
      vm0.set(dim, vm0.get(dim) - rad.doubleValue());
      vp0 = proj.projectDataToRenderSpace(vp0);
      vm0 = proj.projectDataToRenderSpace(vm0);
      // delta vector
      Vector vd0 = new Vector(vp0.getDimensionality());
      vd0.set(dim, rad.doubleValue());
      vd0 = proj.projectRelativeDataToRenderSpace(vd0);
      for(Integer dim2 : dims) {
        if(dim < dim2) {
          Vector v0p = v_mid.copy();
          v0p.set(dim2, v0p.get(dim2) + rad.doubleValue());
          Vector v0m = v_mid.copy();
          v0m.set(dim2, v0m.get(dim2) - rad.doubleValue());
          v0p = proj.projectDataToRenderSpace(v0p);
          v0m = proj.projectDataToRenderSpace(v0m);
          // delta vector
          Vector v0d = new Vector(vm0.getDimensionality());
          v0d.set(dim2, rad.doubleValue());
          v0d = proj.projectRelativeDataToRenderSpace(v0d);

          if(p > 1) {
            // p > 1
            path.moveTo(vp0.get(0), vp0.get(1));
            // support points, p0 to 0p
            final double s_pp1_x = vp0.get(0) + v0d.get(0) * kappax;
            final double s_pp1_y = vp0.get(1) + v0d.get(1) * kappax;
            final double s_pp2_x = v0p.get(0) + vd0.get(0) * kappax;
            final double s_pp2_y = v0p.get(1) + vd0.get(1) * kappax;
            path.cubicTo(s_pp1_x, s_pp1_y, s_pp2_x, s_pp2_y, v0p.get(0), v0p.get(1));
            // support points, 0p to m0
            final double s_mp1_x = v0p.get(0) - vd0.get(0) * kappax;
            final double s_mp1_y = v0p.get(1) - vd0.get(1) * kappax;
            final double s_mp2_x = vm0.get(0) + v0d.get(0) * kappax;
            final double s_mp2_y = vm0.get(1) + v0d.get(1) * kappax;
            path.cubicTo(s_mp1_x, s_mp1_y, s_mp2_x, s_mp2_y, vm0.get(0), vm0.get(1));
            // support points, m0 to 0m
            final double s_mm1_x = vm0.get(0) - v0d.get(0) * kappax;
            final double s_mm1_y = vm0.get(1) - v0d.get(1) * kappax;
            final double s_mm2_x = v0m.get(0) - vd0.get(0) * kappax;
            final double s_mm2_y = v0m.get(1) - vd0.get(1) * kappax;
            path.cubicTo(s_mm1_x, s_mm1_y, s_mm2_x, s_mm2_y, v0m.get(0), v0m.get(1));
            // support points, 0m to p0
            final double s_pm1_x = v0m.get(0) + vd0.get(0) * kappax;
            final double s_pm1_y = v0m.get(1) + vd0.get(1) * kappax;
            final double s_pm2_x = vp0.get(0) - v0d.get(0) * kappax;
            final double s_pm2_y = vp0.get(1) - v0d.get(1) * kappax;
            path.cubicTo(s_pm1_x, s_pm1_y, s_pm2_x, s_pm2_y, vp0.get(0), vp0.get(1));
            path.close();
          }
          else if(p < 1) {
            // p < 1
            path.moveTo(vp0.get(0), vp0.get(1));
            // support points, p0 to 0p
            final double s_pp1_x = vp0.get(0) - vd0.get(0) * kappay;
            final double s_pp1_y = vp0.get(1) - vd0.get(1) * kappay;
            final double s_pp2_x = v0p.get(0) - v0d.get(0) * kappay;
            final double s_pp2_y = v0p.get(1) - v0d.get(1) * kappay;
            path.cubicTo(s_pp1_x, s_pp1_y, s_pp2_x, s_pp2_y, v0p.get(0), v0p.get(1));
            // support points, 0p to m0
            final double s_mp1_x = v0p.get(0) - v0d.get(0) * kappay;
            final double s_mp1_y = v0p.get(1) - v0d.get(1) * kappay;
            final double s_mp2_x = vm0.get(0) + vd0.get(0) * kappay;
            final double s_mp2_y = vm0.get(1) + vd0.get(1) * kappay;
            path.cubicTo(s_mp1_x, s_mp1_y, s_mp2_x, s_mp2_y, vm0.get(0), vm0.get(1));
            // support points, m0 to 0m
            final double s_mm1_x = vm0.get(0) + vd0.get(0) * kappay;
            final double s_mm1_y = vm0.get(1) + vd0.get(1) * kappay;
            final double s_mm2_x = v0m.get(0) + v0d.get(0) * kappay;
            final double s_mm2_y = v0m.get(1) + v0d.get(1) * kappay;
            path.cubicTo(s_mm1_x, s_mm1_y, s_mm2_x, s_mm2_y, v0m.get(0), v0m.get(1));
            // support points, 0m to p0
            final double s_pm1_x = v0m.get(0) + v0d.get(0) * kappay;
            final double s_pm1_y = v0m.get(1) + v0d.get(1) * kappay;
            final double s_pm2_x = vp0.get(0) - vd0.get(0) * kappay;
            final double s_pm2_y = vp0.get(1) - vd0.get(1) * kappay;
            path.cubicTo(s_pm1_x, s_pm1_y, s_pm2_x, s_pm2_y, vp0.get(0), vp0.get(1));
            path.close();
          }
          else {
            // p == 1 - Manhattan
            path.moveTo(vp0.get(0), vp0.get(1));
            path.lineTo(v0p.get(0), v0p.get(1));
            path.lineTo(vm0.get(0), vm0.get(1));
            path.lineTo(v0m.get(0), v0m.get(1));
            path.lineTo(vp0.get(0), vp0.get(1));
            path.close();
          }
        }
      }
    }
    return path.makeElement(svgp);
  }

  /**
   * Wireframe "cross" hypersphere
   * 
   * @param <V> vector type
   * @param <D> radius
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param rad radius
   * @return path element
   */
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<?, ?>> Element drawCross(SVGPlot svgp, VisualizationProjection proj, V mid, D rad) {
    Vector v_mid = mid.getColumnVector();
    // Vector r_mid = proj.projectDataToRenderSpace(mid);
    List<Integer> dims = proj.computeVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(Integer dim : dims) {
      Vector v1 = v_mid.copy();
      v1.set(dim, v1.get(dim) + rad.doubleValue());
      Vector v2 = v_mid.copy();
      v2.set(dim, v2.get(dim) - rad.doubleValue());
      v1 = proj.projectDataToRenderSpace(v1);
      v2 = proj.projectDataToRenderSpace(v2);
      path.moveTo(v1.get(0), v1.get(1));
      path.drawTo(v2.get(0), v2.get(1));
      path.close();
    }
    return path.makeElement(svgp);
  }
}