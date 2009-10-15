package experimentalcode.remigius.Visualizers;

import java.util.logging.Level;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.erich.visualization.VisualizationProjection;
import experimentalcode.remigius.ShapeLibrary;

/**
 * Produces visualizations of 2-dimensional projections. <br>
 * Note that a PlanarVisualizer is <b>not</b> sub-classing ScalarVisualizer.
 * This only happens because use of the instanceof-operator to distinguish those
 * classes is slightly easier now.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV>
 */
public abstract class PlanarVisualizer<NV extends NumberVector<NV, ?>> extends NumberVectorVisualizer<NV> {
  /**
   * the dimension to appear as horizontal dimension.
   */
  protected int dimx;

  /**
   * the dimension to appear as vertical dimension.
   */
  protected int dimy;

  /**
   * Setting up parameters individual to each run of the visualization.
   * 
   * @param dimx
   * @param dimy
   */
  public void setup(int dimx, int dimy, VisualizationProjection<NV> proj) {
    this.dimx = dimx;
    this.dimy = dimy;
    super.setup(proj);
  }
  
  @Override
  public Element visualize(SVGPlot svgp) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_SVG_TAG);
    // Use the projections viewport by default.
    SVGUtil.setAtt(layer, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, proj.estimateViewportString(0.2));
    
    return layer;
  }
}
