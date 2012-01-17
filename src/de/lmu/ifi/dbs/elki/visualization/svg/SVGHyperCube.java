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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;

/**
 * Utility class to draw hypercubes, wireframe and filled.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses SVGPath
 * @apiviz.uses Projection2D
 */
public class SVGHyperCube {
  /**
   * Wireframe hypercube.
   * 
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param min First corner
   * @param max Opposite corner
   * @return path element
   */
  public static Element drawFrame(SVGPlot svgp, Projection2D proj, Vector min, Vector max) {
    SVGPath path = new SVGPath();
    ArrayList<double[]> edges = getVisibleEdges(proj, min, max);
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawEdges(path, rv_min, edges, 0, new BitSet());
    return path.makeElement(svgp);
  }

  /**
   * Wireframe hypercube.
   * 
   * @param <V> vector type
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param min First corner
   * @param max Opposite corner
   * @return path element
   */
  public static <V extends NumberVector<V, ?>> Element drawFrame(SVGPlot svgp, Projection2D proj, V min, V max) {
    SVGPath path = new SVGPath();
    ArrayList<double[]> edges = getVisibleEdges(proj, min.getColumnVector(), max.getColumnVector());
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawEdges(path, rv_min, edges, 0, new BitSet());
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
  public static Element drawFilled(SVGPlot svgp, String cls, Projection2D proj, Vector min, Vector max) {
    Element group = svgp.svgElement(SVGConstants.SVG_G_TAG);
    ArrayList<double[]> edges = getVisibleEdges(proj, min, max);
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawSides(svgp, group, cls, rv_min, edges, 0, new BitSet());
    return group;
  }

  /**
   * Filled hypercube.
   * 
   * @param <V> vector type
   * @param svgp SVG Plot
   * @param cls CSS class to use.
   * @param proj Visualization projection
   * @param min First corner
   * @param max Opposite corner
   * @return group element
   */
  public static <V extends NumberVector<V, ?>> Element drawFilled(SVGPlot svgp, String cls, Projection2D proj, V min, V max) {
    Element group = svgp.svgElement(SVGConstants.SVG_G_TAG);
    ArrayList<double[]> edges = getVisibleEdges(proj, min.getColumnVector(), max.getColumnVector());
    double[] rv_min = proj.fastProjectDataToRenderSpace(min);
    recDrawSides(svgp, group, cls, rv_min, edges, 0, new BitSet());
    return group;
  }

  /**
   * Get the visible (non-0) edges of a hypercube
   * 
   * @param proj Projection
   * @param s_min Minimum value (in scaled space)
   * @param s_max Maximum value (in scaled space)
   * @return Edge list
   */
  private static ArrayList<double[]> getVisibleEdges(Projection2D proj, Vector s_min, Vector s_max) {
    Vector s_deltas = s_max.minus(s_min);
    ArrayList<double[]> r_edges = new ArrayList<double[]>();
    for(int i = 0; i < s_min.getDimensionality(); i++) {
      Vector delta = new Vector(s_min.getDimensionality());
      delta.set(i, s_deltas.get(i));
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
   * @param r_min starting corner
   * @param r_edges edge vectors
   * @param off recursion offset (to avoid multi-recursion)
   * @param b bit set of drawn edges
   */
  private static void recDrawEdges(SVGPath path, double[] r_min, List<double[]> r_edges, int off, BitSet b) {
    // Draw all "missing" edges
    for(int i = 0; i < r_edges.size(); i++) {
      if(!b.get(i)) {
        double[] dest = new double[]{r_min[0] + r_edges.get(i)[0], r_min[1] + r_edges.get(i)[1]};
        path.moveTo(r_min[0], r_min[1]);
        path.drawTo(dest[0], dest[1]);
      }
    }
    // Recursion rule
    for(int i = off; i < r_edges.size(); i++) {
      if(!b.get(i)) {
        BitSet b2 = (BitSet) b.clone();
        b2.set(i);
        double[] dest = new double[]{r_min[0] + r_edges.get(i)[0], r_min[1] + r_edges.get(i)[1]};
        recDrawEdges(path, dest, r_edges, i + 1, b2);
      }
    }
  }

  /**
   * Recursive helper for hypercube drawing.
   * 
   * @param plot Plot
   * @param group Group element
   * @param cls CSS class
   * @param r_min starting corner
   * @param r_edges edge vectors
   * @param off recursion offset (to avoid multi-recursion)
   * @param b bit set of drawn edges
   */
  private static void recDrawSides(SVGPlot plot, Element group, String cls, double[] r_min, List<double[]> r_edges, int off, BitSet b) {
    // Draw all "missing" sides
    for(int i = 0; i < r_edges.size() - 1; i++) {
      double[] deltai = r_edges.get(i);
      for(int j = i + 1; j < r_edges.size(); j++) {
        if(!b.get(i) && !b.get(j)) {
          double[] deltaj = r_edges.get(j);
          StringBuffer pbuf = new StringBuffer();
          pbuf.append(SVGUtil.fmt(r_min[0])).append(",");
          pbuf.append(SVGUtil.fmt(r_min[1])).append(" ");
          pbuf.append(SVGUtil.fmt(r_min[0] + deltai[0])).append(",");
          pbuf.append(SVGUtil.fmt(r_min[1] + deltai[1])).append(" ");
          pbuf.append(SVGUtil.fmt(r_min[0] + deltai[0] + deltaj[0])).append(",");
          pbuf.append(SVGUtil.fmt(r_min[1] + deltai[1] + deltaj[1])).append(" ");
          pbuf.append(SVGUtil.fmt(r_min[0] + deltaj[0])).append(",");
          pbuf.append(SVGUtil.fmt(r_min[1] + deltaj[1]));
          
          Element poly = plot.svgElement(SVGConstants.SVG_POLYGON_TAG);
          SVGUtil.setAtt(poly, SVGConstants.SVG_POINTS_ATTRIBUTE, pbuf.toString());
          SVGUtil.setCSSClass(poly, cls);
          group.appendChild(poly);
        }
      }
    }
    // Recursion rule
    for(int i = off; i < r_edges.size(); i++) {
      if(!b.get(i)) {
        BitSet b2 = (BitSet) b.clone();
        b2.set(i);
        double[] dest = new double[]{r_min[0] + r_edges.get(i)[0], r_min[1] + r_edges.get(i)[1]};
        recDrawSides(plot, group, cls, dest, r_edges, i + 1, b2);
      }
    }
  }
}