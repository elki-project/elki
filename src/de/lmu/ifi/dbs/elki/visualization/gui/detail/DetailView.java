package de.lmu.ifi.dbs.elki.visualization.gui.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.batikutil.AttributeModifier;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.VisualizationInfo;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.VisualizerChangedEvent;

/**
 * Manages a detail view.
 * 
 * @author Erich Schubert
 */
public class DetailView extends SVGPlot implements ContextChangeListener {
  /**
   * Meta information on the visualizers contained.
   */
  List<VisualizationInfo> visi;

  /**
   * Ratio of this view.
   */
  double ratio = 1.0;

  /**
   * The visualizer context
   */
  VisualizerContext<? extends DatabaseObject> context;

  /**
   * The actual visualization instances
   */
  List<Visualization> visv = new java.util.Vector<Visualization>();
  
  /**
   * Map from visualizers to layers
   */
  Map<Visualizer, Element> layermap = new HashMap<Visualizer, Element>();
  
  /**
   * Constructor.
   * 
   * @param vis Visualizations to use
   * @param ratio Plot ratio
   */
  public DetailView(VisualizerContext<? extends DatabaseObject> context, List<VisualizationInfo> vis, double ratio) {
    super();
    this.context = context;
    this.visi = vis;
    this.ratio = ratio;
    
    redraw();
    context.addContextChangeListener(this);
  }

  // TODO: protected?
  protected void redraw() {
    // TODO: Clear root children
    // Warning: do not remove style and similar elements!
    //while (getRoot().hasChildNodes()) {
    //  getRoot().removeChild(getRoot().getFirstChild());
    //}
    destroyVisualizations();
    
    //Collections.sort(layers, new VisualizationInfoComparator());
    double width = getRatio();
    double height = 1.0;

    ArrayList<Visualization> layers = new ArrayList<Visualization>(visi.size());
    // TODO: center/arrange visualizations?
    for(VisualizationInfo vi : visi) {
      if(vi.isVisible()) {
        Visualization v = vi.build(this, width, height);
        visv.add(v);
        layers.add(v);
        layermap.put(vi.getVisualizer(), v.getLayer());
      }
    }
    // Sort layers
    // TODO: final-static comparator?
    Collections.sort(layers, new Visualization.VisualizationComparator());
    // Arrange
    for (Visualization layer : layers) {
      this.getRoot().appendChild(layer.getLayer());
    }
  
    double ratio = width / height;
    getRoot().setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    getRoot().setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / ratio) + "cm");
    getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + width + " " + height);
  
    updateStyleElement();
  }
  
  /**
   * Cleanup function. To remove listeners.
   */
  public void destroy() {
    destroyVisualizations();
  }

  private void destroyVisualizations() {
    for (Visualization v : visv) {
      v.destroy();
    }
    visv.clear();
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

  @Override
  public void contextChanged(ContextChangedEvent e) {
    if (e instanceof VisualizerChangedEvent) {
      VisualizerChangedEvent vce = (VisualizerChangedEvent) e;
      Visualizer v = vce.getVisualizer();
      if (VisualizerUtil.isVisible(v)) {
        Element layer = layermap.get(v);
        if (layer != null) {
          this.scheduleUpdate(new AttributeModifier(layer, SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_VISIBLE_VALUE));
        } else {
          LoggingUtil.warning("Need to recreate a missing layer for "+v);
        }
      } else {
        Element layer = layermap.get(v);
        if (layer != null) {
          this.scheduleUpdate(new AttributeModifier(layer, SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_HIDDEN_VALUE));
        } else {
          LoggingUtil.warning("Need to hide a nonexistant layer for "+v);
        }
      }
    }
  }
}