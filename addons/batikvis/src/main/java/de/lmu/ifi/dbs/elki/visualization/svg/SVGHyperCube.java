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

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;

/**
 * Utility class to draw hypercubes, wireframe and filled.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - SVGPath
 * @assoc - - - Projection2D
 */
public final class SVGHyperCube {
  /**
   * Private constructor. Static methods only.
   */
  private SVGHyperCube() {
    // Do not use.
  }

  /**
   * Wireframe hypercube.
   *
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param min First corner
   * @param max Opposite corner
   * @return path element
   */
  public static Element drawFrame(SVGPlot svgp, Projection2D proj, double[] min, double[] max) {
    SVGPath path = new SVGPath();
    ArrayList<double[]> edges = getVisibleEdges(proj, min, max);
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawEdges(path, rv_min[0], rv_min[1], edges, BitsUtil.zero(edges.size()));
    return path.makeElement(svgp);
  }

  /**
   * Wireframe hypercube.
   *
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param min First corner
   * @param max Opposite corner
   * @return path element
   */
  public static Element drawFrame(SVGPlot svgp, Projection2D proj, NumberVector min, NumberVector max) {
    SVGPath path = new SVGPath();
    ArrayList<double[]> edges = getVisibleEdges(proj, min, max);
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawEdges(path, rv_min[0], rv_min[1], edges, BitsUtil.zero(edges.size()));
    return path.makeElement(svgp);
  }

  /**
   * Wireframe hypercube.
   *
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param box Bounding box
   * @return path element
   */
  public static Element drawFrame(SVGPlot svgp, Projection2D proj, SpatialComparable box) {
    SVGPath path = new SVGPath();
    ArrayList<double[]> edges = getVisibleEdges(proj, box);
    final int dim = box.getDimensionality();
    double[] min = new double[dim];
    for(int i = 0; i < dim; i++) {
      min[i] = box.getMin(i);
    }
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawEdges(path, rv_min[0], rv_min[1], edges, BitsUtil.zero(edges.size()));
    return path.makeElement(svgp);
  }

  /**
   * Filled hypercube.
   *
   * @param svgp SVG Plot
   * @param cls CSS class to use.
   * @param proj Visualization projection
   * @param min First corner
   * @param max Opposite corner
   * @return group element
   */
  public static Element drawFilled(SVGPlot svgp, String cls, Projection2D proj, double[] min, double[] max) {
    Element group = svgp.svgElement(SVGConstants.SVG_G_TAG);
    ArrayList<double[]> edges = getVisibleEdges(proj, min, max);
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawSides(svgp, group, cls, rv_min[0], rv_min[1], edges, 0, BitsUtil.zero(edges.size()));
    return group;
  }

  /**
   * Filled hypercube.
   *
   * @param svgp SVG Plot
   * @param cls CSS class to use.
   * @param proj Visualization projection
   * @param min First corner
   * @param max Opposite corner
   * @return group element
   */
  public static Element drawFilled(SVGPlot svgp, String cls, Projection2D proj, NumberVector min, NumberVector max) {
    Element group = svgp.svgElement(SVGConstants.SVG_G_TAG);
    ArrayList<double[]> edges = getVisibleEdges(proj, min, max);
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawSides(svgp, group, cls, rv_min[0], rv_min[1], edges, 0, BitsUtil.zero(edges.size()));
    return group;
  }

  /**
   * Filled hypercube.
   *
   * @param svgp SVG Plot
   * @param cls CSS class to use.
   * @param proj Visualization projection
   * @param box Bounding box
   * @return group element
   */
  public static Element drawFilled(SVGPlot svgp, String cls, Projection2D proj, SpatialComparable box) {
    Element group = svgp.svgElement(SVGConstants.SVG_G_TAG);
    ArrayList<double[]> edges = getVisibleEdges(proj, box);
    final int dim = box.getDimensionality();
    double[] min = new double[dim];
    for(int i = 0; i < dim; i++) {
      min[i] = box.getMin(i);
    }
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawSides(svgp, group, cls, rv_min[0], rv_min[1], edges, 0, BitsUtil.zero(edges.size()));
    return group;
  }

  /**
   * Get the visible (non-0) edges of a hypercube
   *
   * @param proj Projection
   * @param s_min Minimum value (in data space)
   * @param s_max Maximum value (in data space)
   * @return Edge list
   */
  private static ArrayList<double[]> getVisibleEdges(Projection2D proj, double[] s_min, double[] s_max) {
    final int dim = s_min.length;
    double[] s_deltas = VMath.minus(s_max, s_min);
    ArrayList<double[]> r_edges = new ArrayList<>(dim);
    for(int i = 0; i < dim; i++) {
      double[] delta = new double[dim];
      delta[i] = s_deltas[i];
      double[] deltas = proj.fastProjectRelativeDataToRenderSpace(delta);
      if(deltas[0] != 0 || deltas[1] != 0) {
        r_edges.add(deltas);
      }
    }
    return r_edges;
  }

  /**
   * Get the visible (non-0) edges of a hypercube
   *
   * @param proj Projection
   * @param s_min Minimum value (in data space)
   * @param s_max Maximum value (in data space)
   * @return Edge list
   */
  private static ArrayList<double[]> getVisibleEdges(Projection2D proj, NumberVector s_min, NumberVector s_max) {
    final int dim = s_min.getDimensionality();
    double[] s_deltas = new double[dim];
    for(int i = 0; i < dim; i++) {
      s_deltas[i] = s_max.doubleValue(i) - s_min.doubleValue(i);
    }
    ArrayList<double[]> r_edges = new ArrayList<>(dim);
    for(int i = 0; i < dim; i++) {
      double[] delta = new double[dim];
      delta[i] = s_deltas[i];
      double[] deltas = proj.fastProjectRelativeDataToRenderSpace(delta);
      if(deltas[0] != 0 || deltas[1] != 0) {
        r_edges.add(deltas);
      }
    }
    return r_edges;
  }

  /**
   * Get the visible (non-0) edges of a hypercube
   *
   * @param proj Projection
   * @param box Box object
   * @return Edge list
   */
  private static ArrayList<double[]> getVisibleEdges(Projection2D proj, SpatialComparable box) {
    final int dim = box.getDimensionality();
    double[] s_deltas = new double[dim];
    for(int i = 0; i < dim; i++) {
      s_deltas[i] = box.getMax(i) - box.getMin(i);
    }
    ArrayList<double[]> r_edges = new ArrayList<>(dim);
    for(int i = 0; i < dim; i++) {
      double[] delta = new double[dim];
      delta[i] = s_deltas[i];
      double[] deltas = proj.fastProjectRelativeDataToRenderSpace(delta);
      if(deltas[0] != 0 || deltas[1] != 0) {
        r_edges.add(deltas);
      }
    }
    return r_edges;
  }

  /**
   * Recursive helper for hypercube drawing.
   *
   * @param path path
   * @param minx starting corner
   * @param miny starting corner
   * @param r_edges edge vectors
   * @param b bit set of drawn edges
   */
  private static void recDrawEdges(SVGPath path, double minx, double miny, List<double[]> r_edges, long[] b) {
    // Draw all "missing" edges
    for(int i = 0; i < r_edges.size(); i++) {
      if(BitsUtil.get(b, i)) {
        continue;
      }
      final double[] edge = r_edges.get(i);
      final double x_i = minx + edge[0], y_i = miny + edge[1];
      if(!isFinite(x_i) || !isFinite(y_i)) {
        continue;
      }
      path.moveTo(minx, miny).drawTo(x_i, y_i);
      // Recursion
      BitsUtil.setI(b, i);
      recDrawEdges(path, x_i, y_i, r_edges, b);
      BitsUtil.clearI(b, i);
    }
  }

  /**
   * Recursive helper for hypercube drawing.
   *
   * @param plot Plot
   * @param group Group element
   * @param cls CSS class
   * @param minx starting corner
   * @param miny starting corner
   * @param r_edges edge vectors
   * @param off recursion offset (to avoid multi-recursion)
   * @param b bit set of drawn edges
   */
  private static void recDrawSides(SVGPlot plot, Element group, String cls, double minx, double miny, List<double[]> r_edges, int off, long[] b) {
    StringBuilder pbuf = new StringBuilder();
    // Draw all "missing" sides
    for(int i = 0; i < r_edges.size() - 1; i++) {
      if(BitsUtil.get(b, i)) {
        continue;
      }
      double[] deltai = r_edges.get(i);
      final double xi = minx + deltai[0];
      if(!isFinite(xi)) {
        continue;
      }
      final double yi = miny + deltai[1];
      if(!isFinite(yi)) {
        continue;
      }
      for(int j = i + 1; j < r_edges.size(); j++) {
        if(BitsUtil.get(b, j)) {
          continue;
        }
        double[] deltaj = r_edges.get(j);
        final double dxj = deltaj[0];
        if(!isFinite(xi)) {
          continue;
        }
        final double dyj = deltaj[1];
        if(!isFinite(dxj)) {
          continue;
        }
        pbuf.delete(0, pbuf.length()) // Clear
            .append(SVGUtil.fmt(minx)).append(',') //
            .append(SVGUtil.fmt(miny)).append(' ') //
            .append(SVGUtil.fmt(xi)).append(',') //
            .append(SVGUtil.fmt(yi)).append(' ') //
            .append(SVGUtil.fmt(xi + dxj)).append(',') //
            .append(SVGUtil.fmt(yi + dyj)).append(' ') //
            .append(SVGUtil.fmt(minx + dxj)).append(',') //
            .append(SVGUtil.fmt(miny + dyj));

        Element poly = plot.svgElement(SVGConstants.SVG_POLYGON_TAG, cls);
        SVGUtil.setAtt(poly, SVGConstants.SVG_POINTS_ATTRIBUTE, pbuf.toString());
        group.appendChild(poly);
      }
      // Recursion
      BitsUtil.setI(b, i);
      recDrawSides(plot, group, cls, xi, yi, r_edges, i + 1, b);
      BitsUtil.clearI(b, i);
    }
  }

  /**
   * Finite (and not NaN) double values.
   *
   * @param v Value
   * @return true, when finite.
   */
  private static boolean isFinite(double v) {
    return v < Double.POSITIVE_INFINITY && v > Double.NEGATIVE_INFINITY;
  }
}