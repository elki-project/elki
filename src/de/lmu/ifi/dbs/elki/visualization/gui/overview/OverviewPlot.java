package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.batikutil.CSSHoverClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;
import de.lmu.ifi.dbs.elki.visualization.projections.AffineProjection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection1D;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projections.Simple1D;
import de.lmu.ifi.dbs.elki.visualization.projections.Simple2D;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d.P1DVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.LabelVisFactory;

/**
 * Generate an overview plot for a set of visualizations.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * 
 * @apiviz.landmark
 * @apiviz.has VisualizerContext
 * @apiviz.composedOf PlotMap
 * @apiviz.has DetailViewSelectedEvent
 * @apiviz.uses DetailView
 * @apiviz.uses de.lmu.ifi.dbs.elki.visualization.projections.Projection
 * 
 * @param <NV> Number vector type
 */
public class OverviewPlot<NV extends NumberVector<NV, ?>> extends SVGPlot implements ResultListener {
  /**
   * Maximum number of dimensions to visualize.
   * 
   * TODO: Erich: add scrolling function for higher dimensionality!
   */
  public static final int MAX_DIMENSIONS_DEFAULT = 10;

  /**
   * Stores the maximum number of dimensions to show.
   */
  private int maxdim = MAX_DIMENSIONS_DEFAULT;

  /**
   * Visualizer context
   */
  private VisualizerContext<? extends DatabaseObject> context;

  /**
   * Database we work on.
   */
  private Database<? extends DatabaseObject> db;

  /**
   * Result we work on. Currently unused, but kept for future requirements.
   */
  @SuppressWarnings("unused")
  private Result result;

  /**
   * Map of coordinates to plots.
   */
  protected PlotMap<NV> plotmap;

  /**
   * Action listeners for this plot.
   */
  private java.util.Vector<ActionListener> actionListeners = new java.util.Vector<ActionListener>();

  /**
   * Constructor.
   * 
   * @param db Database
   * @param result Result to visualize
   * @param maxdim Maximum number of dimensions
   * @param context Visualizer context
   */
  public OverviewPlot(Database<? extends DatabaseObject> db, Result result, int maxdim, VisualizerContext<? extends DatabaseObject> context) {
    super();
    this.maxdim = maxdim;
    this.db = db;
    this.result = result;
    this.context = context;
    // register context listener
    context.addResultListener(this);
  }

  /**
   * Screen size (used for thumbnail sizing)
   */
  public int screenwidth = 2000;

  /**
   * Screen size (used for thumbnail sizing)
   */
  public int screenheight = 2000;

  /**
   * React to mouse hover events
   */
  private EventListener hoverer;

  /**
   * Lookup
   */
  private HashMap<Pair<PlotItem, VisualizationTask>, Element> vistoelem;

  /**
   * Layer for plot thumbnail
   */
  private Element plotlayer;

  /**
   * Layer for hover elements
   */
  private Element hoverlayer;

  /**
   * The CSS class used on "selectable" rectangles.
   */
  private CSSClass selcss;

  /**
   * Screen ratio
   */
  private double ratio = 1.0;

  /**
   * Recompute the layout of visualizations.
   */
  private void arrangeVisualizations() {
    // split the visualizers into three sets.
    // FIXME: THIS IS VERY UGLY, and needs to be refactored.
    // (This is a remainder of merging adapters and visualizationfactories)
    List<VisualizationTask> vis = new ArrayList<VisualizationTask>();
    for(VisualizationTask task : context.iterVisualizers()) {
      vis.add(task);
    }
    // We'll use three regions for now:
    // 2D projections starting at 0,0 and going right and down.
    // 1D projections starting at 0, -1 and going right
    // Other projections starting at -1, min() and going down.
    plotmap = new PlotMap<NV>();
    // FIXME: ugly cast used here.
    Database<NV> dvdb = uglyCastDatabase();
    LinearScale[] scales = null;
    scales = Scales.calcScales(dvdb);
    int dmax = Math.min(DatabaseUtil.dimensionality(dvdb), maxdim);
    for(int d1 = 1; d1 <= dmax; d1++) {
      for(int d2 = d1 + 1; d2 <= dmax; d2++) {
        Projection2D proj = new Simple2D(scales, d1, d2);

        for(VisualizationTask task : vis) {
          if(task.getVisualizationType() == P2DVisualization.class) {
            plotmap.addVis(d1 - 1, d2 - 2, 1., 1., proj, task);
          }
        }
      }
    }
    if(dmax >= 3) {
      AffineTransformation p = AffineProjection.axisProjection(DatabaseUtil.dimensionality(dvdb), 1, 2);
      p.addRotation(0, 2, Math.PI / 180 * -10.);
      p.addRotation(1, 2, Math.PI / 180 * 15.);
      // Wanna try 4d? go ahead:
      // p.addRotation(0, 3, Math.PI / 180 * -20.);
      // p.addRotation(1, 3, Math.PI / 180 * 30.);
      final double sizeh = Math.ceil((dmax - 1) / 2.0);
      Projection2D proj = new AffineProjection(scales, p);
      for(VisualizationTask task : vis) {
        if(task.getVisualizationType() == P2DVisualization.class) {
          plotmap.addVis(Math.ceil((dmax - 1) / 2.0), 0.0, sizeh, sizeh, proj, task);
        }
      }
    }
    // insert column numbers
    for(int d1 = 1; d1 <= dmax; d1++) {
      VisualizationTask colvi = new VisualizationTask("", context, null, new LabelVisFactory(Integer.toString(d1)), null, this, 1, .1);
      colvi.put(VisualizationTask.META_NODETAIL, true);
      plotmap.addVis(d1 - 1, -.1, 1., .1, null, colvi);
    }
    // insert row numbers
    for(int d1 = 2; d1 <= dmax; d1++) {
      VisualizationTask colvi = new VisualizationTask("", context, null, new LabelVisFactory(Integer.toString(d1)), null, this, .1, 1);
      colvi.put(VisualizationTask.META_NODETAIL, true);
      plotmap.addVis(-.1, d1 - 2, .1, 1., null, colvi);
    }
    {
      int dim = dmax;
      for(int d1 = 1; d1 <= dim; d1++) {
        Projection1D proj = new Simple1D(scales, d1);
        double ypos = -.1;
        for(VisualizationTask task : vis) {
          if(task.getVisualizationType() == P1DVisualization.class) {
            // TODO: 1d vis might have a different native scaling.
            double height = 0.5;
            plotmap.addVis(d1 - 1, ypos - height, 1.0, height, proj, task);
            //ypos = ypos - height;
          }
        }
      }
    }
    {
      HashMap<Object, double[]> stackmap = new HashMap<Object, double[]>();
      // find starting position.
      Double pos = plotmap.minmaxy.getMin();
      if(pos == null) {
        pos = 0.0;
      }
      // FIXME: use multiple columns!
      for(VisualizationTask task : vis) {
        if(task.getVisualizationType() == P1DVisualization.class) {
          continue;
        }
        if(task.getVisualizationType() == P2DVisualization.class) {
          continue;
        }
        double[] p = null;
        if(task.getVisualizationType() != Visualization.class) {
          p = stackmap.get(task.getVisualizationType());
        }
        if(p == null) {
          p = new double[] { -1.1, pos };
          pos += 1.0;
          stackmap.put(task.getVisualizationType(), p);
        }
        // TODO: might have different scaling preferences
        plotmap.addVis(p[0], p[1], 1., 1., null, task);
      }
    }
  }

  /**
   * Refresh the overview plot.
   */
  public void reinitialize() {
    setupHoverer();
    arrangeVisualizations();
    recalcViewbox();
    // TODO: cancel pending thumbnail requests!

    // Layers.
    if(plotlayer != null) {
      if(plotlayer.getParentNode() != null) {
        plotlayer.getParentNode().removeChild(plotlayer);
      }
      plotlayer = null;
    }
    if(hoverlayer != null) {
      if(hoverlayer.getParentNode() != null) {
        hoverlayer.getParentNode().removeChild(hoverlayer);
      }
      hoverlayer = null;
    }
    plotlayer = this.svgElement(SVGConstants.SVG_G_TAG);
    hoverlayer = this.svgElement(SVGConstants.SVG_G_TAG);
    vistoelem = new HashMap<Pair<PlotItem, VisualizationTask>, Element>();

    final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());

    // TODO: kill all children in document root except style, defs etc?
    for(PlotItem it : plotmap.values()) {
      boolean hasDetails = false;
      Element g = this.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(g, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate(" + it.x + " " + it.y + ")");
      for(VisualizationTask task : it) {
        Element parent = this.svgElement(SVGConstants.SVG_G_TAG);
        g.appendChild(parent);
        makeThumbnail(thumbsize, it, task, parent);
        vistoelem.put(new Pair<PlotItem, VisualizationTask>(it, task), parent);

        if(VisualizerUtil.detailsEnabled(task)) {
          hasDetails = true;
        }
      }
      plotlayer.appendChild(g);
      if(hasDetails) {
        Element hover = this.svgRect(it.x, it.y, it.w, it.h);
        SVGUtil.addCSSClass(hover, selcss.getName());
        // link hoverer.
        EventTarget targ = (EventTarget) hover;
        targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new SelectPlotEvent(it.x, it.y), false);

        hoverlayer.appendChild(hover);
      }
    }
    getRoot().appendChild(plotlayer);
    getRoot().appendChild(hoverlayer);
    updateStyleElement();
  }

  /**
   * Produce thumbnail for a visualizer.
   * 
   * @param thumbsize Thumbnail size
   * @param it Plot item
   * @param task Task
   * @param parent Parent element to draw to
   */
  private void makeThumbnail(final int thumbsize, PlotItem it, VisualizationTask task, Element parent) {
    if(VisualizerUtil.isVisible(task) && VisualizerUtil.thumbnailEnabled(task)) {
      VisualizationTask thumbtask = task.clone(this, it.proj, it.w, it.h);
      thumbtask.put(VisualizationTask.THUMBNAIL, true);
      thumbtask.put(VisualizationTask.THUMBNAIL_RESOLUTION, thumbsize);
      Visualization vis = thumbtask.getFactory().makeVisualizationOrThumbnail(thumbtask);
      if(vis.getLayer() == null) {
        LoggingUtil.warning("Visualization returned empty layer: " + vis);
      }
      else {
        parent.appendChild(vis.getLayer());
      }
    }
  }

  /**
   * Do a refresh (when visibilities have changed).
   */
  public void refresh() {
    if(vistoelem == null || plotlayer == null || hoverlayer == null) {
      reinitialize();
    }
    else {
      final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());
      for(PlotItem it : plotmap.values()) {
        for(VisualizationTask task : it) {
          Element gg = vistoelem.get(new Pair<PlotItem, VisualizationTask>(it, task));
          if(gg == null) {
            LoggingUtil.warning("No container element found for " + task);
            continue;
          }
          if(VisualizerUtil.thumbnailEnabled(task) && VisualizerUtil.isVisible(task)) {
            // unhide when hidden.
            if(gg.hasAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY)) {
              gg.removeAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY);
            }
            // if not yet rendered, add a thumbnail
            if(!gg.hasChildNodes()) {
              makeThumbnail(thumbsize, it, task, gg);
            }
          }
          else {
            // hide if there is anything to hide.
            if(gg != null && gg.hasChildNodes()) {
              gg.setAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_HIDDEN_VALUE);
            }
            // TODO: unqueue pending thumbnails
          }
        }
      }
    }
  }

  /**
   * Recompute the view box of the plot.
   */
  private void recalcViewbox() {
    // Recalculate bounding box.
    String vb = plotmap.minmaxx.getMin() + " " + plotmap.minmaxy.getMin() + " " + plotmap.getWidth() + " " + plotmap.getHeight();
    // Reset root bounding box.
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / plotmap.getWidth() * plotmap.getHeight()) + "cm");
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, vb);
  }

  /**
   * Setup the CSS hover effect.
   */
  private void setupHoverer() {
    // setup the hover CSS classes.
    selcss = new CSSClass(this, "s");
    selcss.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
    selcss.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    selcss.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0");
    selcss.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);
    CSSClass hovcss = new CSSClass(this, "h");
    hovcss.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0.25");
    addCSSClassOrLogError(selcss);
    addCSSClassOrLogError(hovcss);
    // Hover listener.
    hoverer = new CSSHoverClass(hovcss.getName(), null, true);
  }

  /**
   * Ugly cast of the database to a default number vector.
   * 
   * @return Database
   */
  @SuppressWarnings("unchecked")
  private Database<NV> uglyCastDatabase() {
    return (Database<NV>) db;
  }

  /**
   * Event triggered when a plot was selected.
   * 
   * @param x X coordinate
   * @param y Y coordinate
   * @return sub plot
   */
  public DetailView makeDetailView(double x, double y) {
    PlotItem layers = plotmap.get(x, y);
    return new DetailView(context, layers, ratio);
  }

  /**
   * Adds an {@link ActionListener} to the plot.
   * 
   * @param actionListener the {@link ActionListener} to be added
   */
  public void addActionListener(ActionListener actionListener) {
    actionListeners.add(actionListener);
  }

  /**
   * When a subplot was selected, forward the event to listeners.
   * 
   * @param x X coordinate
   * @param y Y coordinate
   */
  protected void triggerSubplotSelectEvent(double x, double y) {
    // forward event to all listeners.
    for(ActionListener actionListener : actionListeners) {
      actionListener.actionPerformed(new DetailViewSelectedEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, x, y));
    }
  }

  /**
   * Event when a plot was selected.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public class SelectPlotEvent implements EventListener {
    /**
     * X coordinate of box.
     */
    double x;

    /**
     * Y coordinate of box.
     */
    double y;

    /**
     * Constructor.
     * 
     * @param x coordinate
     * @param y coordinate
     */
    public SelectPlotEvent(double x, double y) {
      super();
      this.x = x;
      this.y = y;
    }

    @Override
    public void handleEvent(@SuppressWarnings("unused") Event evt) {
      triggerSubplotSelectEvent(x, y);
    }
  }

  /**
   * Cancel the overview, i.e. stop the thumbnailer
   */
  @Override
  public void dispose() {
    context.removeResultListener(this);
    super.dispose();
  }

  /**
   * @return the ratio
   */
  public double getRatio() {
    return ratio;
  }

  /**
   * @param ratio the ratio to set
   */
  public void setRatio(double ratio) {
    this.ratio = ratio;
  }

  @SuppressWarnings("unused")
  @Override
  public void resultAdded(Result child, Result parent) {
    // TODO: be lazy
    if (child instanceof VisualizationTask) {
      reinitialize();
    }
    refresh();
  }

  @SuppressWarnings("unused")
  @Override
  public void resultChanged(Result current) {
    // TODO: be lazy
    refresh();
  }

  @SuppressWarnings("unused")
  @Override
  public void resultRemoved(Result child, Result parent) {
    // TODO: be lazy
    refresh();
  }
}