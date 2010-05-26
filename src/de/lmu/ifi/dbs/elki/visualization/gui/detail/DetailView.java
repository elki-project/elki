package de.lmu.ifi.dbs.elki.visualization.gui.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.visualization.gui.overview.VisualizationInfo;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Manages a detail view.
 * 
 * @author Erich Schubert
 */
public class DetailView extends SVGPlot {
  /**
   * Meta information on the visualizers contained.
   */
  List<VisualizationInfo> vis;

  /**
   * Ratio of this view.
   */
  double ratio = 1.0;

  /**
   * Constructor.
   * 
   * @param layers Visualizations to use
   * @param ratio Plot ratio
   */
  public DetailView(List<VisualizationInfo> vis, double ratio) {
    super();
    this.vis = vis;
    this.ratio = ratio;
    
    redraw();
  }

  // TODO: protected?
  public void redraw() {
    // TODO: Clear root children
    // Warning: do not remove style and similar elements!
    //while (getRoot().hasChildNodes()) {
    //  getRoot().removeChild(getRoot().getFirstChild());
    //}
    
    //Collections.sort(layers, new VisualizationInfoComparator());
    double width = getRatio();
    double height = 1.0;

    ArrayList<Visualization.VisualizationLayer> layers = new ArrayList<Visualization.VisualizationLayer>(vis.size());
    // TODO: center/arrange visualizations?
    for(VisualizationInfo vi : vis) {
      if(vi.isVisible()) {
        Visualization v = vi.build(this, width, height);
        // TODO: listeners?
        for (Visualization.VisualizationLayer l : v.getLayers()) {
          layers.add(l);
        }
      }
    }
    // Sort layers
    Collections.sort(layers);
    // Arrange
    for (Visualization.VisualizationLayer layer : layers) {
      this.getRoot().appendChild(layer.layer);
    }
  
    double ratio = width / height;
    getRoot().setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    getRoot().setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / ratio) + "cm");
    getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + width + " " + height);
  
    updateStyleElement();
  }

  /**
   * Get the plot ratio.
   * 
   * @return the current ratio
   */
  public double getRatio() {
    return ratio;
  }

  /**
   * Set the plot ratio
   * 
   * @param ratio the new ratio to set
   */
  public void setRatio(double ratio) {
    // TODO: trigger refresh?
    this.ratio = ratio;
  }
}