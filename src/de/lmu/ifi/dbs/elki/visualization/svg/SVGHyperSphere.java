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
   * Constant for euclidean
   */
  private static final double halfsqrt2 = Math.sqrt(2) / 2.;

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
          path.cubicTo(v1.get(0) + d2.get(0) / 2, v1.get(1) + d2.get(1) / 2, v3.get(0) + d1.get(0) / 2, v3.get(1) + d1.get(1) / 2, v3.get(0), v3.get(1));
          path.cubicTo(v3.get(0) - d1.get(0) / 2, v3.get(1) - d1.get(1) / 2, v2.get(0) + d2.get(0) / 2, v2.get(1) + d2.get(1) / 2, v2.get(0), v2.get(1));
          path.cubicTo(v2.get(0) - d2.get(0) / 2, v2.get(1) - d2.get(1) / 2, v4.get(0) - d1.get(0) / 2, v4.get(1) - d1.get(1) / 2, v4.get(0), v4.get(1));
          path.cubicTo(v4.get(0) + d1.get(0) / 2, v4.get(1) + d1.get(1) / 2, v1.get(0) - d2.get(0) / 2, v1.get(1) - d2.get(1) / 2, v1.get(0), v1.get(1));
          // path.moveTo(v3.get(0), v3.get(1));
          // path.moveTo(v2.get(0), v2.get(1));
          // path.moveTo(v4.get(0), v4.get(1));
          path.close();
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