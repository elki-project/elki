package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * Default class to handle 2D projected visualizations.
 * 
 * @author Erich Schubert
 */
public abstract class P2DVisualization<NV extends NumberVector<NV, ?>> extends AbstractVisualization<NV> {
  /**
   * The current projection
   */
  final protected Projection2D proj;
  
  /**
   * Constructor.
   *
   * @param task Visualization task
   * @param level Level
   */
  public P2DVisualization(VisualizationTask task, Integer level) {
    super(task, level);
    this.proj = task.getProj();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    this.layer = setupCanvas(svgp, proj, margin, task.getWidth(), task.getHeight());
  }

  /**
   * Utility function to setup a canvas element for the visualization.
   * 
   * @param svgp Plot element
   * @param proj Projection to use
   * @param margin Margin to use
   * @param width Width
   * @param height Height
   * @return wrapper element with appropriate view box.
   */
  public static Element setupCanvas(SVGPlot svgp, Projection2D proj, double margin, double width, double height) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, proj.estimateTransformString(margin, width, height));
    return layer;
  }
}