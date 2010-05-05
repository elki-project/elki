package de.lmu.ifi.dbs.elki.visualization.svg;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;

/**
 * Utility class to draw hypercubes, wireframe and filled.
 * 
 * @author Erich Schubert
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
  public static Element drawWireframe(SVGPlot svgp, VisualizationProjection proj, double[] min, double[] max) {
    SVGPath path = new SVGPath();
    Vector s_min = proj.projectDataToScaledSpace(min);
    Vector s_max = proj.projectDataToScaledSpace(max);
    ArrayList<DoubleDoublePair> edges = getVisibleEdges(proj, s_min, s_max);
    Vector rv_min = proj.projectDataToRenderSpace(min);
    DoubleDoublePair corner = new DoubleDoublePair(rv_min.get(0), rv_min.get(1));
    recDrawEdges(path, corner, edges, 0, new BitSet());
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
  public static <V extends NumberVector<V,?>> Element drawWireframe(SVGPlot svgp, VisualizationProjection proj, V min, V max) {
    SVGPath path = new SVGPath();
    Vector s_min = proj.projectDataToScaledSpace(min);
    Vector s_max = proj.projectDataToScaledSpace(max);
    ArrayList<DoubleDoublePair> edges = getVisibleEdges(proj, s_min, s_max);
    Vector rv_min = proj.projectDataToRenderSpace(min);
    DoubleDoublePair corner = new DoubleDoublePair(rv_min.get(0), rv_min.get(1));
    recDrawEdges(path, corner, edges, 0, new BitSet());
    return path.makeElement(svgp);
  }

  /**
   * Get the visible (non-0) edges of a hypercube
   * 
   * @param proj Projection
   * @param s_min Minimum value (in scaled space)
   * @param s_max Maximum value (in scaled space)
   * @return Edge list
   */
  private static ArrayList<DoubleDoublePair> getVisibleEdges(VisualizationProjection proj, Vector s_min, Vector s_max) {
    Vector s_deltas = s_max.minus(s_min);
    ArrayList<DoubleDoublePair> r_edges = new ArrayList<DoubleDoublePair>();
    for(int i = 0; i < s_min.getDimensionality(); i++) {
      Vector delta = new Vector(s_min.getDimensionality());
      delta.set(i, s_deltas.get(i));
      delta = proj.projectRelativeScaledToRender(delta);
      if(delta.get(0) != 0 || delta.get(1) != 0) {
        r_edges.add(new DoubleDoublePair(delta.get(0), delta.get(1)));
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
  private static void recDrawEdges(SVGPath path, DoubleDoublePair r_min, List<DoubleDoublePair> r_edges, int off, BitSet b) {
    // Draw all "missing" edges
    for(int i = 0; i < r_edges.size(); i++) {
      if(!b.get(i)) {
        DoubleDoublePair dest = new DoubleDoublePair(r_min.first + r_edges.get(i).first, r_min.second + r_edges.get(i).second);
        path.moveTo(r_min.first, r_min.second);
        path.drawTo(dest.first, dest.second);
      }
    }
    // Recursion rule
    for(int i = off; i < r_edges.size(); i++) {
      if(!b.get(i)) {
        BitSet b2 = (BitSet) b.clone();
        b2.set(i);
        DoubleDoublePair dest = new DoubleDoublePair(r_min.first + r_edges.get(i).first, r_min.second + r_edges.get(i).second);
        recDrawEdges(path, dest, r_edges, i + 1, b2);
      }
    }
  }
}
