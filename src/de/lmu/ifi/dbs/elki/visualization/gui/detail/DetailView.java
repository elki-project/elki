package de.lmu.ifi.dbs.elki.visualization.gui.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.batik.util.SVGConstants;

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
   * Map from visualizers to layers
   */
  Map<Visualizer, Visualization> layermap = new HashMap<Visualizer, Visualization>();

  /**
   * The created width
   */
  private double width;

  /**
   * The created height
   */
  private double height;

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
    // while (getRoot().hasChildNodes()) {
    // getRoot().removeChild(getRoot().getFirstChild());
    // }
    destroyVisualizations();

    // Collections.sort(layers, new VisualizationInfoComparator());
    width = getRatio();
    height = 1.0;

    ArrayList<Visualization> layers = new ArrayList<Visualization>(visi.size());
    // TODO: center/arrange visualizations?
    for(VisualizationInfo vi : visi) {
      if(vi.isVisible()) {
        Visualization v = vi.build(this, width, height);
        layers.add(v);
        layermap.put(vi.getVisualizer(), v);
      }
    }
    // Sort layers
    // TODO: final-static comparator?
    Collections.sort(layers, new Visualization.VisualizationComparator());
    // Arrange
    for(Visualization layer : layers) {
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
    for(Entry<Visualizer, Visualization> v : layermap.entrySet()) {
      v.getValue().destroy();
    }
    layermap.clear();
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
    if(e instanceof VisualizerChangedEvent) {
      VisualizerChangedEvent vce = (VisualizerChangedEvent) e;
      Visualizer v = vce.getVisualizer();
      if(VisualizerUtil.isVisible(v)) {
        Visualization vis = layermap.get(v);
        if(vis != null) {
          this.scheduleUpdate(new AttributeModifier(vis.getLayer(), SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_VISIBLE_VALUE));
        }
        else {
          //LoggingUtil.warning("Need to recreate a missing layer for " + v);
          for(VisualizationInfo vi : visi) {
            if(vi.getVisualizer() == v) {
              vis = vi.build(this, width, height);
              layermap.put(v, vis);
              this.scheduleUpdate(new InsertVisualization(vis));
            }
          }
        }
      }
      else {
        Visualization vis = layermap.get(v);
        if(vis != null) {
          this.scheduleUpdate(new AttributeModifier(vis.getLayer(), SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_HIDDEN_VALUE));
        }
        else {
          LoggingUtil.warning("Need to hide a nonexistant layer for " + v);
        }
      }
    }
  }

  /**
   * Class used to insert a new visualization layer
   * 
   * @author Erich Schubert
   */
  protected class InsertVisualization implements Runnable {
    /**
     * The visualization to insert.
     */
    Visualization vis;
    
    /**
     * Visualization.
     * 
     * @param vis
     */
    public InsertVisualization(Visualization vis) {
      super();
      this.vis = vis;
    }

    @Override
    public void run() {
      DetailView.this.getRoot().appendChild(vis.getLayer());
      updateStyleElement();
   }
  }
}