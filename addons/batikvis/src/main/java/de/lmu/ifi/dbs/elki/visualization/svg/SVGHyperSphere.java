/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.svg;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import net.jafama.FastMath;

/**
 * Utility class to draw hypercubes, wireframe and filled.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - SVGPath
 * @assoc - - - Projection2D
 */
public final class SVGHyperSphere {
  /**
   * Private constructor. Static methods only.
   */
  private SVGHyperSphere() {
    // Do not use.
  }

  /**
   * Factor used for approximating circles with cubic beziers.
   *
   * kappa = 4 * (Math.sqrt(2)-1)/3
   */
  public static final double EUCLIDEAN_KAPPA = 0.5522847498;

  /**
   * Wireframe "manhattan" hypersphere
   *
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param radius radius
   * @return path element
   */
  public static Element drawManhattan(SVGPlot svgp, Projection2D proj, NumberVector mid, double radius) {
    final double[] v_mid = mid.toArray(); // a copy
    final long[] dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = BitsUtil.nextSetBit(dims, 0); dim >= 0; dim = BitsUtil.nextSetBit(dims, dim + 1)) {
      v_mid[dim] += radius;
      double[] p1 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] -= radius * 2;
      double[] p2 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] += radius;
      for(int dim2 = BitsUtil.nextSetBit(dims, 0); dim2 >= 0; dim2 = BitsUtil.nextSetBit(dims, dim2 + 1)) {
        if(dim < dim2) {
          v_mid[dim2] += radius;
          double[] p3 = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] -= radius * 2;
          double[] p4 = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] += radius;

          path.moveTo(p1[0], p1[1]).drawTo(p3[0], p3[1]) //
              .moveTo(p1[0], p1[1]).drawTo(p4[0], p4[1]) //
              .moveTo(p2[0], p2[1]).drawTo(p3[0], p3[1]) //
              .moveTo(p2[0], p2[1]).drawTo(p4[0], p4[1]) //
              .close();
        }
      }
    }
    return path.makeElement(svgp);
  }

  /**
   * Wireframe "euclidean" hypersphere
   *
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param radius radius
   * @return path element
   */
  public static Element drawEuclidean(SVGPlot svgp, Projection2D proj, NumberVector mid, double radius) {
    double[] v_mid = mid.toArray(); // a copy
    long[] dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = BitsUtil.nextSetBit(dims, 0); dim >= 0; dim = BitsUtil.nextSetBit(dims, dim + 1)) {
      v_mid[dim] += radius;
      double[] p1 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] -= radius * 2;
      double[] p2 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] += radius;
      // delta vector
      double[] dt1 = new double[v_mid.length];
      dt1[dim] = radius;
      double[] d1 = proj.fastProjectRelativeDataToRenderSpace(dt1);
      for(int dim2 = BitsUtil.nextSetBit(dims, 0); dim2 >= 0; dim2 = BitsUtil.nextSetBit(dims, dim2 + 1)) {
        if(dim < dim2) {
          v_mid[dim2] += radius;
          double[] p3 = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] -= radius * 2;
          double[] p4 = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] += radius;
          // delta vector
          double[] dt2 = new double[v_mid.length];
          dt2[dim2] = radius;
          double[] d2 = proj.fastProjectRelativeDataToRenderSpace(dt2);

          path.moveTo(p1[0], p1[1]) //
              .cubicTo(p1[0] + d2[0] * EUCLIDEAN_KAPPA, p1[1] + d2[1] * EUCLIDEAN_KAPPA, p3[0] + d1[0] * EUCLIDEAN_KAPPA, p3[1] + d1[1] * EUCLIDEAN_KAPPA, p3[0], p3[1]) //
              .cubicTo(p3[0] - d1[0] * EUCLIDEAN_KAPPA, p3[1] - d1[1] * EUCLIDEAN_KAPPA, p2[0] + d2[0] * EUCLIDEAN_KAPPA, p2[1] + d2[1] * EUCLIDEAN_KAPPA, p2[0], p2[1]) //
              .cubicTo(p2[0] - d2[0] * EUCLIDEAN_KAPPA, p2[1] - d2[1] * EUCLIDEAN_KAPPA, p4[0] - d1[0] * EUCLIDEAN_KAPPA, p4[1] - d1[1] * EUCLIDEAN_KAPPA, p4[0], p4[1]) //
              .cubicTo(p4[0] + d1[0] * EUCLIDEAN_KAPPA, p4[1] + d1[1] * EUCLIDEAN_KAPPA, p1[0] - d2[0] * EUCLIDEAN_KAPPA, p1[1] - d2[1] * EUCLIDEAN_KAPPA, p1[0], p1[1]) //
              .close();
        }
      }
    }
    return path.makeElement(svgp);
  }

  /**
   * Wireframe "Lp" hypersphere
   *
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param radius radius
   * @param p L_p value
   * @return path element
   */
  public static Element drawLp(SVGPlot svgp, Projection2D proj, NumberVector mid, double radius, double p) {
    final double[] v_mid = mid.toArray();
    final long[] dims = proj.getVisibleDimensions2D();

    double kappax = p > 1 ? Math.min(1.3, 4. * (2 * FastMath.pow(0.5, 1. / p) - 1) / 3.) : 0;
    double kappay = p < 1 ? Math.min(1.3, 4. * (2 * (1 - FastMath.pow(0.5, 1. / p)) - 1) / 3.) : 0;

    SVGPath path = new SVGPath();
    for(int dim = BitsUtil.nextSetBit(dims, 0); dim >= 0; dim = BitsUtil.nextSetBit(dims, dim + 1)) {
      v_mid[dim] += radius;
      double[] pvp0 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] -= radius * 2;
      double[] pvm0 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] += radius;
      // delta vector
      double[] tvd0 = new double[v_mid.length];
      tvd0[dim] = radius;
      double[] vd0 = proj.fastProjectRelativeDataToRenderSpace(tvd0);
      for(int dim2 = BitsUtil.nextSetBit(dims, 0); dim2 >= 0; dim2 = BitsUtil.nextSetBit(dims, dim2 + 1)) {
        if(dim < dim2) {
          v_mid[dim2] += radius;
          double[] pv0p = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] -= radius * 2;
          double[] pv0m = proj.fastProjectDataToRenderSpace(v_mid);
          v_mid[dim2] += radius;
          // delta vector
          double[] tv0d = new double[v_mid.length];
          tv0d[dim2] = radius;
          double[] v0d = proj.fastProjectRelativeDataToRenderSpace(tv0d);

          if(p > 1) {
            // p > 1
            // support points, p0 to 0p
            final double s_pp1_x = pvp0[0] + v0d[0] * kappax;
            final double s_pp1_y = pvp0[1] + v0d[1] * kappax;
            final double s_pp2_x = pv0p[0] + vd0[0] * kappax;
            final double s_pp2_y = pv0p[1] + vd0[1] * kappax;
            // support points, 0p to m0
            final double s_mp1_x = pv0p[0] - vd0[0] * kappax;
            final double s_mp1_y = pv0p[1] - vd0[1] * kappax;
            final double s_mp2_x = pvm0[0] + v0d[0] * kappax;
            final double s_mp2_y = pvm0[1] + v0d[1] * kappax;
            // support points, m0 to 0m
            final double s_mm1_x = pvm0[0] - v0d[0] * kappax;
            final double s_mm1_y = pvm0[1] - v0d[1] * kappax;
            final double s_mm2_x = pv0m[0] - vd0[0] * kappax;
            final double s_mm2_y = pv0m[1] - vd0[1] * kappax;
            // support points, 0m to p0
            final double s_pm1_x = pv0m[0] + vd0[0] * kappax;
            final double s_pm1_y = pv0m[1] + vd0[1] * kappax;
            final double s_pm2_x = pvp0[0] - v0d[0] * kappax;
            final double s_pm2_y = pvp0[1] - v0d[1] * kappax;
            path.moveTo(pvp0[0], pvp0[1]) //
                .cubicTo(s_pp1_x, s_pp1_y, s_pp2_x, s_pp2_y, pv0p[0], pv0p[1]) //
                .cubicTo(s_mp1_x, s_mp1_y, s_mp2_x, s_mp2_y, pvm0[0], pvm0[1]) //
                .cubicTo(s_mm1_x, s_mm1_y, s_mm2_x, s_mm2_y, pv0m[0], pv0m[1]) //
                .cubicTo(s_pm1_x, s_pm1_y, s_pm2_x, s_pm2_y, pvp0[0], pvp0[1]) //
                .close();
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
            path.moveTo(pvp0[0], pvp0[1]) //
                .cubicTo(s_vp0_x, s_vp0_y, s_v0p_x, s_v0p_y, pv0p[0], pv0p[1]) //
                .cubicTo(s_v0p_x, s_v0p_y, s_vm0_x, s_vm0_y, pvm0[0], pvm0[1]) //
                .cubicTo(s_vm0_x, s_vm0_y, s_v0m_x, s_v0m_y, pv0m[0], pv0m[1]) //
                .cubicTo(s_v0m_x, s_v0m_y, s_vp0_x, s_vp0_y, pvp0[0], pvp0[1]) //
                .close();
          }
          else {
            // p == 1 - Manhattan
            path.moveTo(pvp0[0], pvp0[1]) //
                .lineTo(pv0p[0], pv0p[1]) //
                .lineTo(pvm0[0], pvm0[1]) //
                .lineTo(pv0m[0], pv0m[1]) //
                .lineTo(pvp0[0], pvp0[1]) //
                .close();
          }
        }
      }
    }
    return path.makeElement(svgp);
  }

  /**
   * Wireframe "cross" hypersphere
   *
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param radius radius
   * @return path element
   */
  public static Element drawCross(SVGPlot svgp, Projection2D proj, NumberVector mid, double radius) {
    final double[] v_mid = mid.toArray();
    final long[] dims = proj.getVisibleDimensions2D();

    SVGPath path = new SVGPath();
    for(int dim = BitsUtil.nextSetBit(dims, 0); dim >= 0; dim = BitsUtil.nextSetBit(dims, dim + 1)) {
      v_mid[dim] += radius;
      double[] p1 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] -= radius * 2;
      double[] p2 = proj.fastProjectDataToRenderSpace(v_mid);
      v_mid[dim] += radius;
      path.moveTo(p1[0], p1[1]).drawTo(p2[0], p2[1]).close();
    }
    return path.makeElement(svgp);
  }
}
