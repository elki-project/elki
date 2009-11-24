package experimentalcode.erich.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.erich.visualization.visualizers.AbstractVisualizer;
import experimentalcode.erich.visualization.visualizers.ProjectedVisualizer;

/**
 * Produces visualizations of 2-dimensional projections. <br>
 * Note that a Projection2DVisualizer is <b>not</b> sub-classing Projection1DVisualizer.
 * This only happens because use of the instanceof-operator to distinguish those
 * classes is slightly easier now.
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
   * @return wrapper element with appropriate view box.
   */
  public Element setupCanvas(SVGPlot svgp, VisualizationProjection proj) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_SVG_TAG);
    // Use the projections viewport by default.
    SVGUtil.setAtt(layer, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, proj.estimateViewportString(0.2));
    
    return layer;
  }
}