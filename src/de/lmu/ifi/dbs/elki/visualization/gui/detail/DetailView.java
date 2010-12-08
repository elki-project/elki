package de.lmu.ifi.dbs.elki.visualization.gui.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.batikutil.AttributeModifier;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.VisualizerChangedEvent;

/**
 * Manages a detail view.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has VisualizerContext
 * @apiviz.has Visualization
 */
public class DetailView extends SVGPlot implements ContextChangeListener {
  /**
   * Meta information on the visualizers contained.
   */
  List<VisualizationTask> visi;

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
  Map<VisualizationTask, Visualization> layermap = new HashMap<VisualizationTask, Visualization>();

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
  public DetailView(VisualizerContext<? extends DatabaseObject> context, List<VisualizationTask> vis, double ratio) {
    super();
    this.context = context;
    this.visi = vis;
    this.ratio = ratio;
    
    Collections.sort(this.visi);

    // TODO: only do this when there is an interactive visualizer?
    setDisableInteractions(true);
    addBackground(context);
    
    redraw();
    context.addContextChangeListener(this);
  }

  /**
   * Create a background node.
   * 
   * @param context
   */
  private void addBackground(VisualizerContext<? extends DatabaseObject> context) {
    // Make a background
    CSSClass cls = new CSSClass(this, "background");
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getBackgroundColor(StyleLibrary.PAGE));
    Element bg = this.svgElement(SVGConstants.SVG_RECT_TAG);
    SVGUtil.setAtt(bg, SVGConstants.SVG_X_ATTRIBUTE, "0");
    SVGUtil.setAtt(bg, SVGConstants.SVG_Y_ATTRIBUTE, "0");
    SVGUtil.setAtt(bg, SVGConstants.SVG_WIDTH_ATTRIBUTE, "100%");
    SVGUtil.setAtt(bg, SVGConstants.SVG_HEIGHT_ATTRIBUTE, "100%");
    addCSSClassOrLogError(cls);
    SVGUtil.setCSSClass(bg, cls.getName());

    // Insert the background as first element.
    Element root = getDocument().getRootElement();
    if (root.hasChildNodes()) {
      root.insertBefore(bg, root.getFirstChild());
    } else {
      root.appendChild(bg);
    }
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
    for(VisualizationTask task : visi) {
      if(VisualizerUtil.isVisible(task)) {
        Visualization v = task.getFactory().makeVisualization(task.clone(this));
        layers.add(v);
        layermap.put(task, v);
      }
    }
    // Arrange
    for(Visualization layer : layers) {
      if (layer.getLayer() != null) {
        this.getRoot().appendChild(layer.getLayer());
      } else {
        LoggingUtil.warning("NULL layer seen.");
      }
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
    for(Entry<VisualizationTask, Visualization> v : layermap.entrySet()) {
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
      VisualizationTask v = vce.getVisualizer();
      if(VisualizerUtil.isVisible(v)) {
        Visualization vis = layermap.get(v);
        if(vis != null) {
          this.scheduleUpdate(new AttributeModifier(vis.getLayer(), SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_VISIBLE_VALUE));
        }
        else {
          //LoggingUtil.warning("Need to recreate a missing layer for " + v);
          for(VisualizationTask task : visi) {
            // FIXME:
            if(task == v) {
              vis = task.getFactory().makeVisualization(task.clone(this));
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
   * 
   * @apiviz.exclude
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