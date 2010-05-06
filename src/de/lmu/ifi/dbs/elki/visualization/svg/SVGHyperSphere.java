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
    //Vector r_mid = proj.projectDataToRenderSpace(mid);
    List<Integer> dims = proj.computeVisibleDimensions2D();
    
    SVGPath path = new SVGPath();
    for (Integer dim : dims) {
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