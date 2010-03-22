package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
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
   * @param width Width
   * @param height Height
   * @return Wrapper element with appropriate view box.
   */
  public Element setupCanvas(SVGPlot svgp, double width, double height) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_SVG_TAG);
    // Some default for the view box.
    double ratio = width / height;
    double left = -1.2 * ratio;
    double top = -1.1;
    double vwidth = 2.3 * ratio;
    double vheight = 2.2;
    // TODO: use transform() instead.
    SVGUtil.setAtt(layer, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, left + " " + top + " " + vwidth + " " + vheight);
    return layer;
  }
}