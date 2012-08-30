package de.lmu.ifi.dbs.elki.visualization.svg;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.BitSet;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;

/**
 * Utility class to draw hypercubes, wireframe and filled.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses SVGPath
 * @apiviz.uses Projection2D
 */
public class SVGHyperSphere {
  /**
   * Factor used for approximating circles with cubic beziers.
   * 
   * kappa = 4 * (Math.sqrt(2)-1)/3
   */
  public static final double EUCLIDEAN_KAPPA = 0.5522847498;

  /**
   * Wireframe "manhattan" hypersphere
   * 
   * @param <D> radius
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param rad radius
   * @return path element
   */
  public static <D extends NumberDistance<?, ?>> Element drawManhattan(SVGPlot svgp, Projection2D proj, NumberVector<?, ?> mid, D rad) {
    final double radius = rad.doubleValue();
    final double[] v_mid = mid.getColumnVector().getArrayRef(); // a copy
    final BitSet dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = dims.nextSetBit(0); dim >= 0; dim = dims.nextSetBit(dim + 1)) {
      v_mid[dim] += radius;
      double[] p1 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] -= radius;
      v_mid[dim] -= radius;
      double[] p2 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] += radius;
      for(int dim2 = dims.nextSetBit(0); dim2 >= 0; dim2 = dims.nextSetBit(dim2 + 1)) {
        if(dim < dim2) {
          v_mid[dim2] += radius;
          double[] p3 = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] -= radius;
          v_mid[dim2] -= radius;
          double[] p4 = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] += radius;

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
   * @param <D> radius
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param rad radius
   * @return path element
   */
  public static <D extends NumberDistance<?, ?>> Element drawEuclidean(SVGPlot svgp, Projection2D proj, NumberVector<?, ?> mid, D rad) {
    final double radius = rad.doubleValue();
    double[] v_mid = mid.getColumnVector().getArrayRef(); // a copy
    BitSet dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = dims.nextSetBit(0); dim >= 0; dim = dims.nextSetBit(dim + 1)) {
      v_mid[dim] += radius;
      double[] p1 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] -= radius;
      v_mid[dim] -= radius;
      double[] p2 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] += radius;
      // delta vector
      double[] dt1 = new double[v_mid.length];
      dt1[dim] = radius;
      double[] d1 = proj.fastProjectRelativeDataToRenderSpace(dt1);
      for(int dim2 = dims.nextSetBit(0); dim2 >= 0; dim2 = dims.nextSetBit(dim2 + 1)) {
        if(dim < dim2) {
          v_mid[dim2] += radius;
          double[] p3 = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] -= radius;
          v_mid[dim2] -= radius;
          double[] p4 = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] += radius;
          // delta vector
          double[] dt2 = new double[v_mid.length];
          dt2[dim2] = radius;
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
   * @param <D> radius
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param rad radius
   * @param p L_p value
   * @return path element
   */
  public static <D extends NumberDistance<?, ?>> Element drawLp(SVGPlot svgp, Projection2D proj, NumberVector<?, ?> mid, D rad, double p) {
    final double radius = rad.doubleValue();
    final double[] v_mid = mid.getColumnVector().getArrayRef();
    final BitSet dims = proj.getVisibleDimensions2D();

    final double kappax, kappay;
    if(p > 1.) {
      final double kappal = Math.pow(0.5, 1. / p);
      kappax = Math.min(1.3, 4. * (2 * kappal - 1) / 3.);
      kappay = 0;
    }
    else if(p < 1.) {
      final double kappal = 1 - Math.pow(0.5, 1. / p);
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
      v_mid[dim] += radius;
      double[] pvp0 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] -= radius;
      v_mid[dim] -= radius;
      double[] pvm0 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] += radius;
      // delta vector
      double[] tvd0 = new double[v_mid.length];
      tvd0[dim] = radius;
      double[] vd0 = proj.fastProjectRelativeDataToRenderSpace(tvd0);
      for(int dim2 = dims.nextSetBit(0); dim2 >= 0; dim2 = dims.nextSetBit(dim2 + 1)) {
        if(dim < dim2) {
          v_mid[dim2] += radius;
          double[] pv0p = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] -= radius;
          v_mid[dim2] -= radius;
          double[] pv0m = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] += radius;
          // delta vector
          double[] tv0d = new double[v_mid.length];
          tv0d[dim2] = radius;
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
            // support points, p0 to 0p
            final double s_vp0_x = pvp0[0] - vd0[0] * kappay;
            final double s_vp0_y = pvp0[1] - vd0[1] * kappay;
            final double s_v0p_x = pv0p[0] - v0d[0] * kappay;
            final double s_v0p_y = pv0p[1] - v0d[1] * kappay;
            final double s_vm0_x = pvm0[0] + vd0[0] * kappay;
            final double s_vm0_y = pvm0[1] + vd0[1] * kappay;
            final double s_v0m_x = pv0m[0] + v0d[0] * kappay;
            final double s_v0m_y = pv0m[1] + v0d[1] * kappay;
            // Draw the star
            path.moveTo(pvp0[0], pvp0[1]);
            path.cubicTo(s_vp0_x, s_vp0_y, s_v0p_x, s_v0p_y, pv0p[0], pv0p[1]);
            path.cubicTo(s_v0p_x, s_v0p_y, s_vm0_x, s_vm0_y, pvm0[0], pvm0[1]);
            path.cubicTo(s_vm0_x, s_vm0_y, s_v0m_x, s_v0m_y, pv0m[0], pv0m[1]);
            path.cubicTo(s_v0m_x, s_v0m_y, s_vp0_x, s_vp0_y, pvp0[0], pvp0[1]);
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
   * @param <D> radius
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param rad radius
   * @return path element
   */
  public static <D extends NumberDistance<?, ?>> Element drawCross(SVGPlot svgp, Projection2D proj, NumberVector<?, ?> mid, D rad) {
    final double radius = rad.doubleValue();
    final double[] v_mid = mid.getColumnVector().getArrayRef();
    final BitSet dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = dims.nextSetBit(0); dim >= 0; dim = dims.nextSetBit(dim + 1)) {
      v_mid[dim] += radius;
      double[] p1 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] -= radius;
      path.moveTo(p1[0], p1[1]);
      v_mid[dim] -= radius;
      double[] p2 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] += radius;
      path.drawTo(p2[0], p2[1]);
      path.close();
    }
    return path.makeElement(svgp);
  }
}