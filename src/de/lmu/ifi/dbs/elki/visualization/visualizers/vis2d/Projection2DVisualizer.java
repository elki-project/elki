package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisualizer;

/**
 * Produces visualizations of 2-dimensional projections. <br>
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public abstract class Projection2DVisualizer<NV extends NumberVector<NV, ?>> extends AbstractVisualizer implements ProjectedVisualizer {
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
  public Element setupCanvas(SVGPlot svgp, VisualizationProjection proj, double margin, double width, double height) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, proj.estimateTransformString(margin, width, height));
    return layer;
  }
}