package de.lmu.ifi.dbs.elki.visualization.svg;

import java.util.BitSet;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;

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
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<?, ?>> Element drawManhattan(SVGPlot svgp, Projection2D proj, V mid, D rad) {
    Vector v_mid = mid.getColumnVector();
    BitSet dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = dims.nextSetBit(0); dim >= 0; dim = dims.nextSetBit(dim + 1)) {
      Vector v1 = v_mid.copy();
      v1.set(dim, v1.get(dim) + rad.doubleValue());
      Vector v2 = v_mid.copy();
      v2.set(dim, v2.get(dim) - rad.doubleValue());
      double[] p1 = proj.fastProjectDataToRenderSpace(v1);
      double[] p2 = proj.fastProjectDataToRenderSpace(v2);
      for(int dim2 = dims.nextSetBit(0); dim2 >= 0; dim2 = dims.nextSetBit(dim2 + 1)) {
        if(dim < dim2) {
          Vector v3 = v_mid.copy();
          v3.set(dim2, v3.get(dim2) + rad.doubleValue());
          Vector v4 = v_mid.copy();
          v4.set(dim2, v4.get(dim2) - rad.doubleValue());
          double[] p3 = proj.fastProjectDataToRenderSpace(v3);
          double[] p4 = proj.fastProjectDataToRenderSpace(v4);

          path.moveTo(p1[0], p1[1]);
          path.drawTo(p3[0], p3[1]);
          path.moveTo(p1[0], p1[1]);
          path.drawTo(p4[0], p4[1]);
          path.moveTo(p2[0], p2[1]);
          path.drawTo(p3[0], p3[1]);
          path.moveTo(p2[0], p2[1]);
          path.drawTo(p4[0], p4[1]);
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
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<?, ?>> Element drawEuclidean(SVGPlot svgp, Projection2D proj, V mid, D rad) {
    Vector v_mid = mid.getColumnVector();
    BitSet dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = dims.nextSetBit(0); dim >= 0; dim = dims.nextSetBit(dim + 1)) {
      Vector v1 = v_mid.copy();
      v1.set(dim, v1.get(dim) + rad.doubleValue());
      Vector v2 = v_mid.copy();
      v2.set(dim, v2.get(dim) - rad.doubleValue());
      double[] p1 = proj.fastProjectDataToRenderSpace(v1);
      double[] p2 = proj.fastProjectDataToRenderSpace(v2);
      // delta vector
      Vector dt1 = new Vector(v1.getDimensionality());
      dt1.set(dim, rad.doubleValue());
      double[] d1 = proj.fastProjectRelativeDataToRenderSpace(dt1);
      for(int dim2 = dims.nextSetBit(0); dim2 >= 0; dim2 = dims.nextSetBit(dim2 + 1)) {
        if(dim < dim2) {
          Vector v3 = v_mid.copy();
          v3.set(dim2, v3.get(dim2) + rad.doubleValue());
          Vector v4 = v_mid.copy();
          v4.set(dim2, v4.get(dim2) - rad.doubleValue());
          double[] p3 = proj.fastProjectDataToRenderSpace(v3);
          double[] p4 = proj.fastProjectDataToRenderSpace(v4);
          // delta vector
          Vector dt2 = new Vector(v2.getDimensionality());
          dt2.set(dim2, rad.doubleValue());
          double[] d2 = proj.fastProjectRelativeDataToRenderSpace(dt2);

          path.moveTo(p1[0], p1[1]);
          path.cubicTo(p1[0] + d2[0] * EUCLIDEAN_KAPPA, p1[1] + d2[1] * EUCLIDEAN_KAPPA, p3[0] + d1[0] * EUCLIDEAN_KAPPA, p3[1] + d1[1] * EUCLIDEAN_KAPPA, p3[0], p3[1]);
          path.cubicTo(p3[0] - d1[0] * EUCLIDEAN_KAPPA, p3[1] - d1[1] * EUCLIDEAN_KAPPA, p2[0] + d2[0] * EUCLIDEAN_KAPPA, p2[1] + d2[1] * EUCLIDEAN_KAPPA, p2[0], p2[1]);
          path.cubicTo(p2[0] - d2[0] * EUCLIDEAN_KAPPA, p2[1] - d2[1] * EUCLIDEAN_KAPPA, p4[0] - d1[0] * EUCLIDEAN_KAPPA, p4[1] - d1[1] * EUCLIDEAN_KAPPA, p4[0], p4[1]);
          path.cubicTo(p4[0] + d1[0] * EUCLIDEAN_KAPPA, p4[1] + d1[1] * EUCLIDEAN_KAPPA, p1[0] - d2[0] * EUCLIDEAN_KAPPA, p1[1] - d2[1] * EUCLIDEAN_KAPPA, p1[0], p1[1]);
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
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<?, ?>> Element drawLp(SVGPlot svgp, Projection2D proj, V mid, D rad, double p) {
    Vector v_mid = mid.getColumnVector();
    BitSet dims = proj.getVisibleDimensions2D();

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
    // LoggingUtil.warning("kappax: " + kappax + " kappay: " + kappay);

    SVGPath path = new SVGPath();
    for(int dim = dims.nextSetBit(0); dim >= 0; dim = dims.nextSetBit(dim + 1)) {
      Vector vp0 = v_mid.copy();
      vp0.set(dim, vp0.get(dim) + rad.doubleValue());
      Vector vm0 = v_mid.copy();
      vm0.set(dim, vm0.get(dim) - rad.doubleValue());
      double[] pvp0 = proj.fastProjectDataToRenderSpace(vp0);
      double[] pvm0 = proj.fastProjectDataToRenderSpace(vm0);
      // delta vector
      Vector tvd0 = new Vector(vp0.getDimensionality());
      tvd0.set(dim, rad.doubleValue());
      double[] vd0 = proj.fastProjectRelativeDataToRenderSpace(tvd0);
      for(int dim2 = dims.nextSetBit(0); dim2 >= 0; dim2 = dims.nextSetBit(dim2 + 1)) {
        if(dim < dim2) {
          Vector v0p = v_mid.copy();
          v0p.set(dim2, v0p.get(dim2) + rad.doubleValue());
          Vector v0m = v_mid.copy();
          v0m.set(dim2, v0m.get(dim2) - rad.doubleValue());
          double[] pv0p = proj.fastProjectDataToRenderSpace(v0p);
          double[] pv0m = proj.fastProjectDataToRenderSpace(v0m);
          // delta vector
          Vector tv0d = new Vector(vm0.getDimensionality());
          tv0d.set(dim2, rad.doubleValue());
          double[] v0d = proj.fastProjectRelativeDataToRenderSpace(tv0d);

          if(p > 1) {
            // p > 1
            path.moveTo(pvp0[0], pvp0[1]);
            // support points, p0 to 0p
            final double s_pp1_x = pvp0[0] + v0d[0] * kappax;
            final double s_pp1_y = pvp0[1] + v0d[1] * kappax;
            final double s_pp2_x = pv0p[0] + vd0[0] * kappax;
            final double s_pp2_y = pv0p[1] + vd0[1] * kappax;
            path.cubicTo(s_pp1_x, s_pp1_y, s_pp2_x, s_pp2_y, pv0p[0], pv0p[1]);
            // support points, 0p to m0
            final double s_mp1_x = pv0p[0] - vd0[0] * kappax;
            final double s_mp1_y = pv0p[1] - vd0[1] * kappax;
            final double s_mp2_x = pvm0[0] + v0d[0] * kappax;
            final double s_mp2_y = pvm0[1] + v0d[1] * kappax;
            path.cubicTo(s_mp1_x, s_mp1_y, s_mp2_x, s_mp2_y, pvm0[0], pvm0[1]);
            // support points, m0 to 0m
            final double s_mm1_x = pvm0[0] - v0d[0] * kappax;
            final double s_mm1_y = pvm0[1] - v0d[1] * kappax;
            final double s_mm2_x = pv0m[0] - vd0[0] * kappax;
            final double s_mm2_y = pv0m[1] - vd0[1] * kappax;
            path.cubicTo(s_mm1_x, s_mm1_y, s_mm2_x, s_mm2_y, pv0m[0], pv0m[1]);
            // support points, 0m to p0
            final double s_pm1_x = pv0m[0] + vd0[0] * kappax;
            final double s_pm1_y = pv0m[1] + vd0[1] * kappax;
            final double s_pm2_x = pvp0[0] - v0d[0] * kappax;
            final double s_pm2_y = pvp0[1] - v0d[1] * kappax;
            path.cubicTo(s_pm1_x, s_pm1_y, s_pm2_x, s_pm2_y, pvp0[0], pvp0[1]);
            path.close();
          }
          else if(p < 1) {
            // p < 1
            path.moveTo(vp0.get(0), vp0.get(1));
            // support points, p0 to 0p
            final double s_pp1_x = pvp0[0] - vd0[0] * kappay;
            final double s_pp1_y = pvp0[1] - vd0[1] * kappay;
            final double s_pp2_x = pv0p[0] - v0d[0] * kappay;
            final double s_pp2_y = pv0p[1] - v0d[1] * kappay;
            path.cubicTo(s_pp1_x, s_pp1_y, s_pp2_x, s_pp2_y, pv0p[0], pv0p[1]);
            // support points, 0p to m0
            final double s_mp1_x = pv0p[0] - v0d[0] * kappay;
            final double s_mp1_y = pv0p[1] - v0d[1] * kappay;
            final double s_mp2_x = pvm0[0] + vd0[0] * kappay;
            final double s_mp2_y = pvm0[1] + vd0[1] * kappay;
            path.cubicTo(s_mp1_x, s_mp1_y, s_mp2_x, s_mp2_y, pvm0[0], pvm0[1]);
            // support points, m0 to 0m
            final double s_mm1_x = pvm0[0] + vd0[0] * kappay;
            final double s_mm1_y = pvm0[1] + vd0[1] * kappay;
            final double s_mm2_x = pv0m[0] + v0d[0] * kappay;
            final double s_mm2_y = pv0m[1] + v0d[1] * kappay;
            path.cubicTo(s_mm1_x, s_mm1_y, s_mm2_x, s_mm2_y, pv0m[0], pv0m[1]);
            // support points, 0m to p0
            final double s_pm1_x = pv0m[0] + v0d[0] * kappay;
            final double s_pm1_y = pv0m[1] + v0d[1] * kappay;
            final double s_pm2_x = pvp0[0] - vd0[0] * kappay;
            final double s_pm2_y = pvp0[1] - vd0[1] * kappay;
            path.cubicTo(s_pm1_x, s_pm1_y, s_pm2_x, s_pm2_y, pvp0[0], pvp0[1]);
            path.close();
          }
          else {
            // p == 1 - Manhattan
            path.moveTo(pvp0[0], pvp0[1]);
            path.lineTo(pv0p[0], pv0p[1]);
            path.lineTo(pvm0[0], pvm0[1]);
            path.lineTo(pv0m[0], pv0m[1]);
            path.lineTo(pvp0[0], pvp0[1]);
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
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<?, ?>> Element drawCross(SVGPlot svgp, Projection2D proj, V mid, D rad) {
    Vector v_mid = mid.getColumnVector();
    BitSet dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = dims.nextSetBit(0); dim >= 0; dim = dims.nextSetBit(dim + 1)) {
      Vector v1 = v_mid.copy();
      v1.set(dim, v1.get(dim) + rad.doubleValue());
      Vector v2 = v_mid.copy();
      v2.set(dim, v2.get(dim) - rad.doubleValue());
      double[] p1 = proj.fastProjectDataToRenderSpace(v1);
      double[] p2 = proj.fastProjectDataToRenderSpace(v2);
      path.moveTo(p1[0], p1[1]);
      path.drawTo(p2[0], p2[1]);
      path.close();
    }
    return path.makeElement(svgp);
  }
}