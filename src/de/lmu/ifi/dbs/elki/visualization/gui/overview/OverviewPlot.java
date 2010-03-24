package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.batikutil.CSSHoverClass;
import de.lmu.ifi.dbs.elki.visualization.batikutil.NodeReplaceChild;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.Thumbnailer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.RedrawListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d.Projection1DVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Generate an overview plot for a set of visualizations.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * @param <NV> Number vector type
 */
public class OverviewPlot<NV extends NumberVector<NV, ?>> extends SVGPlot implements RedrawListener {
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
   * Visualizations
   */
  private Collection<Visualizer> vis = new ArrayList<Visualizer>();

  /**
   * Database we work on.
   */
  private Database<? extends DatabaseObject> db;

  /**
   * Result we work on. Currently unused, but kept for future requirements.
   */
  @SuppressWarnings("unused")
  private MultiResult result;

  /**
   * Map of coordinates to plots.
   */
  protected PlotMap<NV> plotmap;

  /**
   * Thumbnailer to use.
   */
  static Thumbnailer t = new Thumbnailer();

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
   */
  public OverviewPlot(Database<? extends DatabaseObject> db, MultiResult result, int maxdim) {
    super();
    this.maxdim = maxdim;
    this.db = db;
    this.result = result;
  }

  /**
   * Add vis to the plot. Call {@link #refresh} when done.
   * 
   * @param vs vis.
   */
  public void addVisualizations(Collection<Visualizer> vs) {
    vis.addAll(vs);
    for(Visualizer v : vs) {
      v.addRedrawListener(this);
    }
  }

  /**
   * Current thumbnail thread.
   */
  private ThumbnailThread thumbnails = null;

  /**
   * Queue of thumbnails to generate.
   */
  Queue<Pair<Element, VisualizationInfo>> queue = new ConcurrentLinkedQueue<Pair<Element, VisualizationInfo>>();

  /**
   * Screen size (used for thumbnail sizing)
   */
  public int screenwidth = 1000;

  /**
   * Screen size (used for thumbnail sizing)
   */
  public int screenheight = 1000;

  /**
   * React to mouse hover events
   */
  private EventListener hoverer;

  /**
   * Lookup
   */
  private HashMap<VisualizationInfo, Element> vistoelem;

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
   * Flag to indicate shutdown.
   */
  private boolean stopped;

  /**
   * Recompute the layout of visualizations.
   */
  private void arrangeVisualizations() {
    // split the visualizers into three sets.
    Collection<Projection1DVisualizer<?>> vis1d = new ArrayList<Projection1DVisualizer<?>>(vis.size());
    Collection<Projection2DVisualizer<?>> vis2d = new ArrayList<Projection2DVisualizer<?>>(vis.size());
    Collection<UnprojectedVisualizer> visup = new ArrayList<UnprojectedVisualizer>(vis.size());
    for(Visualizer v : vis) {
      if(Projection2DVisualizer.class.isAssignableFrom(v.getClass())) {
        vis2d.add((Projection2DVisualizer<?>) v);
      }
      else if(Projection1DVisualizer.class.isAssignableFrom(v.getClass())) {
        vis1d.add((Projection1DVisualizer<?>) v);
      }
      else if(UnprojectedVisualizer.class.isAssignableFrom(v.getClass())) {
        visup.add((UnprojectedVisualizer) v);
      }
      else {
        LoggingUtil.exception("Encountered visualization that is neither projected nor unprojected!", new Throwable());
      }
    }
    // We'll use three regions for now:
    // 2D projections starting at 0,0 and going right and down.
    // 1D projections starting at 0, -1 and going right
    // Other projections starting at -1, min() and going down.
    plotmap = new PlotMap<NV>();
    // FIXME: ugly cast used here.
    Database<NV> dvdb = uglyCastDatabase();
    LinearScale[] scales = null;
    if(vis2d.size() > 0 || vis1d.size() > 0) {
      scales = Scales.calcScales(dvdb);
    }
    int dmax = Math.min(db.dimensionality(), maxdim);
    if(vis2d.size() > 0) {
      for(int d1 = 1; d1 <= dmax; d1++) {
        for(int d2 = d1 + 1; d2 <= dmax; d2++) {
          VisualizationProjection proj = new VisualizationProjection(dvdb, scales, d1, d2);

          for(Projection2DVisualizer<?> v : vis2d) {
            VisualizationInfo vi = new VisualizationProjectedInfo(v, proj, 1., 1.);
            plotmap.addVis(d1 - 1, d2 - 2, 1.0, 1.0, vi);
          }
        }
      }
      if(dmax >= 3) {
        AffineTransformation p = VisualizationProjection.axisProjection(dmax, 1, 2);
        p.addRotation(0, 2, Math.PI / 180 * -40. / dmax);
        p.addRotation(1, 2, Math.PI / 180 * 50. / dmax);
        VisualizationProjection proj = new VisualizationProjection(dvdb, scales, p);
        for(Projection2DVisualizer<?> v : vis2d) {
          final double sizel = Math.floor((dmax - 1) / 2.0);
          final double sizeh = Math.ceil((dmax - 1) / 2.0);
          VisualizationInfo vi = new VisualizationProjectedInfo(v, proj, sizeh, sizeh);
          plotmap.addVis(Math.ceil((dmax - 1) / 2.0), 0.0, sizel, sizel, vi);
        }
      }
    }
    // insert column numbers
    if(vis1d.size() > 0 || vis2d.size() > 0) {
      for(int d1 = 1; d1 <= dmax; d1++) {
        VisualizationInfo colvi = new VisualizationLabel(Integer.toString(d1), 1, .1);
        plotmap.addVis(d1 - 1, -.1, 1., .1, colvi);
      }
    }
    // insert row numbers
    if(vis2d.size() > 0) {
      for(int d1 = 2; d1 <= dmax; d1++) {
        VisualizationInfo colvi = new VisualizationLabel(Integer.toString(d1), .1, 1);
        plotmap.addVis(-.1, d1 - 2, .1, 1., colvi);
      }
    }
    if(vis1d.size() > 0) {
      int dim = dmax;
      for(int d1 = 1; d1 <= dim; d1++) {
        VisualizationProjection proj = new VisualizationProjection(dvdb, scales, d1, (d1 == 1 ? 2 : 1));
        double ypos = -.1;
        for(Projection1DVisualizer<?> v : vis1d) {
          VisualizationInfo vi = new VisualizationProjectedInfo(v, proj, 1., 1.);
          // TODO: 1d vis might have a different native scaling.
          double height = 1.0;
          plotmap.addVis(d1 - 1, ypos - height, 1.0, height, vi);
          ypos = ypos - height;
        }
      }
    }
    if(visup.size() > 0) {
      // find starting position.
      Double pos = plotmap.minmaxy.getMin();
      if(pos == null) {
        pos = 0.0;
      }
      for(UnprojectedVisualizer v : visup) {
        VisualizationInfo vi = new VisualizationUnprojectedInfo(v, 1., 1.);
        // TODO: might have different scaling.
        plotmap.addVis(-1.1, pos, 1., 1., vi);
        pos += 1.0;
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
    stopThumbnailer();

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
    vistoelem = new HashMap<VisualizationInfo, Element>();

    // TODO: kill all children in document root except style, defs etc?
    for(Entry<DoubleDoublePair, ArrayList<VisualizationInfo>> e : plotmap.entrySet()) {
      boolean hasDetails = false;
      double x = e.getKey().getFirst();
      double y = e.getKey().getSecond();
      double w = 0.0;
      double h = 0.0;
      Element g = this.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(g, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate(" + x + " " + y + ")");
      for(VisualizationInfo vi : e.getValue()) {
        Element parent = g;
        if(e.getValue().size() > 1) {
          parent = this.svgElement(SVGConstants.SVG_G_TAG);
          g.appendChild(parent);
        }
        w = Math.max(w, vi.getWidth());
        h = Math.max(h, vi.getHeight());
        if(vi.isVisible() && vi.thumbnailEnabled()) {
          addOrQueueThumbnail(vi, parent);
        }
        vistoelem.put(vi, parent);

        if(vi.hasDetails()) {
          hasDetails = true;
        }
      }
      plotlayer.appendChild(g);
      if(hasDetails) {
        Element hover = this.svgRect(x, y, w, h);
        SVGUtil.addCSSClass(hover, selcss.getName());
        // link hoverer.
        EventTarget targ = (EventTarget) hover;
        targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new SelectPlotEvent(x, y), false);

        hoverlayer.appendChild(hover);
      }
    }
    getRoot().appendChild(plotlayer);
    getRoot().appendChild(hoverlayer);
    updateStyleElement();
  }

  /**
   * Do a refresh (when visibilities have changed).
   */
  public void refresh() {
    if(vistoelem == null || plotlayer == null || hoverlayer == null) {
      reinitialize();
    }
    else {
      for(Entry<DoubleDoublePair, ArrayList<VisualizationInfo>> e : plotmap.entrySet()) {
        for(VisualizationInfo vi : e.getValue()) {
          Element gg = vistoelem.get(vi);
          if(vi.thumbnailEnabled() && vi.isVisible()) {
            // unhide when hidden.
            if(gg.hasAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY)) {
              gg.removeAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY);
            }
            // if not yet rendered, add a thumbnail
            if(!gg.hasChildNodes()) {
              addOrQueueThumbnail(vi, gg);
            }
          }
          else {
            // hide if there is anything to hide.
            if(gg.hasChildNodes()) {
              gg.setAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_HIDDEN_VALUE);
            }
            // TODO: unqueue pending thumbnails
          }
        }
      }
    }
  }

  /**
   * Queue a thumbnail for generation.
   * 
   * @param vi VisualizationInfo
   * @param gg Thumbnail element
   */
  private void addOrQueueThumbnail(VisualizationInfo vi, Element gg) {
    Element elem = vi.makeElement(this);
    if(elem != null) {
      gg.appendChild(elem);
    }
    else {
      gg.appendChild(SVGUtil.svgWaitIcon(this.getDocument(), 0, 0, vi.getWidth(), vi.getHeight()));
      queueThumbnail(vi, gg);
    }
  }

  /**
   * Queue a thumbnail for generation.
   * 
   * @param vi VisualizationInfo
   * @param gg Thumbnail element
   */
  private synchronized void queueThumbnail(VisualizationInfo vi, Element gg) {
    queue.add(new Pair<Element, VisualizationInfo>(gg, vi));
    if(thumbnails == null || !thumbnails.isAlive()) {
      thumbnails = new ThumbnailThread();
      thumbnails.start();
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
   * Stop a running thumbnailer.
   */
  private void stopThumbnailer() {
    // Stop thumbnail
    if(thumbnails != null) {
      thumbnails.shutdown = true;
      thumbnails.interrupt();
      thumbnails = null;
    }
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
    try {
      getCSSClassManager().addClass(selcss);
      getCSSClassManager().addClass(hovcss);
    }
    catch(CSSNamingConflict e) {
      throw new RuntimeException("Unresolved conflict in CSS.", e);
    }
    // Hover listener.
    hoverer = new CSSHoverClass(hovcss.getName(), null, true);
  }

  /**
   * Generate a single Thumbnail.
   * 
   * @param g Parent element to insert the thumbnail into.
   * @param vi Visualization.
   */
  protected void generateThumbnail(final Element g, VisualizationInfo vi) {
    int thumbwidth = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());
    if(stopped) {
      return;
    }
    vi.generateThumbnail(t, thumbwidth);
    final Element i = vi.makeElement(this);
    if(stopped) {
      return;
    }
    this.scheduleUpdate(new NodeReplaceChild(g, i));
  }

  // TODO: don't restart the thumbnailer, but clear the queue.
  /**
   * Thread to update thumbnails in the background.
   * 
   * @author Erich Schubert
   */
  protected class ThumbnailThread extends Thread {
    /**
     * Flag to signal shutdown.
     */
    boolean shutdown = false;

    @Override
    public void run() {
      while(!queue.isEmpty() && !shutdown) {
        Pair<Element, VisualizationInfo> ti = queue.poll();
        generateThumbnail(ti.first, ti.second);
      }
    }
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
  public SVGPlot makeDetailPlot(double x, double y) {
    SVGPlot plot = new SVGPlot();

    List<VisualizationInfo> layers = plotmap.get(x, y);
    //double width = Double.MIN_VALUE;
    //double height = Double.MIN_VALUE;

    for(VisualizationInfo vi : layers) {
      if(vi.isVisible()) {
        Element e = vi.build(plot, 1, 1);
        plot.getRoot().appendChild(e);
        //width = Math.max(width, vi.getWidth());
        //height = Math.max(height, vi.getHeight());
      }
    }
    
    double ratio = 1.0; //width / height;
    plot.getRoot().setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    plot.getRoot().setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / ratio) + "cm");
    plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + ratio + " 1");

    plot.updateStyleElement();
    return plot;
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
      actionListener.actionPerformed(new SubplotSelectedEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, x, y));
    }
  }

  /**
   * Event when a plot was selected.
   * 
   * @author Erich Schubert
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
  public void dispose() {
    stopped = true;
    stopThumbnailer();
  }

  @Override
  public void triggerRedraw(Visualizer caller) {
    // FIXME: ERICH: When this is called while a thumbnail is already being
    // generated, it will miss the last update, so we need some timestamping.
    for(Entry<VisualizationInfo, Element> ent : vistoelem.entrySet()) {
      VisualizationInfo vis = ent.getKey();
      if(vis.isVisible() && vis.thumbnailEnabled() && vis.getVisualization() == caller) {
        if(vis.thumbnail != null) {
          vis.thumbnail = null;
          queueThumbnail(vis, ent.getValue());
        }
      }
    }
  }
}