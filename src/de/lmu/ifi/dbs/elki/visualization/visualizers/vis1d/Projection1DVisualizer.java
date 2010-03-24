package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisualizer;

/**
 * Produces visualizations of 1-dimensional projections.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public abstract class Projection1DVisualizer<NV extends NumberVector<NV, ?>> extends AbstractVisualizer implements ProjectedVisualizer {
  /**
   * Setup a canvas (wrapper) element for the visualization.
   * 
   * @param svgp Plot context
   * @param proj Projection to use
   * @param width Width
   * @param height Height
   * @return Wrapper element with appropriate view box.
   */
  public Element setupCanvasMargin(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final double ratio = width / height;
    final double zoom = width * 0.85 / VisualizationProjection.SCALE;
    final double offx = 0.13 * VisualizationProjection.SCALE;
    final double offy = 0.02 / ratio * VisualizationProjection.SCALE;
    String transform = "scale(" + SVGUtil.fmt(zoom) + ") translate(" + SVGUtil.fmt(offx) + " " + SVGUtil.fmt(offy) + ")";
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    return layer;
  }
}