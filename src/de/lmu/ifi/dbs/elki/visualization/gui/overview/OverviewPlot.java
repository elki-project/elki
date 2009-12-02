package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d.Projection1DVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Generate an overview plot for a set of visualizations.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 */
public class OverviewPlot<NV extends NumberVector<NV, ?>> extends SVGPlot {
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
   */
  public OverviewPlot(Database<? extends DatabaseObject> db, MultiResult result) {
    super();
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
  }

  /**
   * Current thumbnail thread.
   */
  private ThumbnailThread thumbnails = null;
  
  /**
   * Screen size (used for thumbnail sizing)
   */
  public int screenwidth = 1000;

  /**
   * Screen size (used for thumbnail sizing)
   */
  public int screenheight = 1000;

  /**
   * Refresh the overview plot.
   */
  public void refresh() {
    // setup the hover CSS classes.
    CSSClass selcss = new CSSClass(this, "s");
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
    EventListener hoverer = new CSSHoverClass(hovcss.getName(), null, true);

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
    if(vis2d.size() > 0) {
      int dim = db.dimensionality();
      for(int d1 = 1; d1 <= dim; d1++) {
        for(int d2 = d1 + 1; d2 <= dim; d2++) {
          VisualizationProjection proj = new VisualizationProjection(dvdb, scales, d1, d2);

          for(Projection2DVisualizer<?> v : vis2d) {
            VisualizationInfo vi = new VisualizationProjectedInfo(v, proj);
            plotmap.addVis(d1 - 1, d2 - 2, 1.0, 1.0, vi);
          }
        }
      }
    }
    if(vis1d.size() > 0) {
      int dim = db.dimensionality();
      for(int d1 = 1; d1 <= dim; d1++) {
        VisualizationProjection proj = new VisualizationProjection(dvdb, scales, d1, (d1 == 1 ? 2 : 1));
        double ypos = 0;
        for(Projection1DVisualizer<?> v : vis1d) {
          VisualizationInfo vi = new VisualizationProjectedInfo(v, proj);
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
        VisualizationInfo vi = new VisualizationUnprojectedInfo(v);
        // TODO: might have different scaling.
        plotmap.addVis(-1, pos, 1., 1., vi);
        pos += 1.0;
      }
    }
    // Reset thumbnailer.
    if(thumbnails != null) {
      thumbnails.shutdown = true;
    }
    thumbnails = new ThumbnailThread();
    // Recalculate bounding box.
    String vb = plotmap.minmaxx.getMin() + " " + plotmap.minmaxy.getMin() + " " + plotmap.getWidth() + " " + plotmap.getHeight();
    // Reset root bounding box.
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / plotmap.getWidth() * plotmap.getHeight()) + "cm");
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, vb);

    // Layers.
    Element plotlayer = this.svgElement(SVGConstants.SVG_G_TAG);
    Element hoverlayer = this.svgElement(SVGConstants.SVG_G_TAG);

    // TODO: kill all children in document root except style, defs etc?
    for(Entry<DoubleDoublePair, ArrayList<VisualizationInfo>> e : plotmap.entrySet()) {
      double x = e.getKey().getFirst();
      double y = e.getKey().getSecond();
      Element g = this.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(g, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate(" + x + " " + y + ")");
      /*
       * //SVG element instead of G, works much worse in Inkscape Element g =
       * this.svgElement(SVGConstants.SVG_SVG_TAG); SVGUtil.setAtt(g,
       * SVGConstants.SVG_X_ATTRIBUTE, x); SVGUtil.setAtt(g,
       * SVGConstants.SVG_Y_ATTRIBUTE, y); SVGUtil.setAtt(g,
       * SVGConstants.SVG_WIDTH_ATTRIBUTE, 1); SVGUtil.setAtt(g,
       * SVGConstants.SVG_HEIGHT_ATTRIBUTE, 1); SVGUtil.setAtt(g,
       * SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 1 1");
       */
      for(VisualizationInfo vi : e.getValue()) {
        Element gg = this.svgElement(SVGConstants.SVG_G_TAG);
        gg.appendChild(SVGUtil.svgWaitIcon(this.getDocument(), 0, 0, 1, 1));
        g.appendChild(gg);
        thumbnails.queue(gg, vi);
      }
      plotlayer.appendChild(g);
      Element h = this.svgRect(x, y, 1, 1);
      SVGUtil.addCSSClass(h, selcss.getName());
      // link hoverer.
      EventTarget targ = (EventTarget) h;
      targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
      targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
      targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
      targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new SelectPlotEvent(x, y), false);

      hoverlayer.appendChild(h);
    }
    getRoot().appendChild(plotlayer);
    getRoot().appendChild(hoverlayer);
    updateStyleElement();
    thumbnails.start();
  }

  /**
   * Generate a single Thumbnail.
   * 
   * @param g Parent element to insert the thumbnail into.
   * @param vi Visualization.
   */
  protected void generateThumbnail(final Element g, VisualizationInfo vi) {
    int thumbwidth = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());
    File thumb = vi.makeThumbnail(t, thumbwidth);
    final Element i = this.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(i, SVGConstants.SVG_X_ATTRIBUTE, 0);
    SVGUtil.setAtt(i, SVGConstants.SVG_Y_ATTRIBUTE, 0);
    SVGUtil.setAtt(i, SVGConstants.SVG_WIDTH_ATTRIBUTE, 1);
    SVGUtil.setAtt(i, SVGConstants.SVG_HEIGHT_ATTRIBUTE, 1);
    i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, thumb.toURI().toString());
    this.scheduleUpdate(new NodeReplaceChild(g, i));
  }

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

    /**
     * Queue of thumbnails to generate.
     */
    Queue<Pair<Element, VisualizationInfo>> queue = new ConcurrentLinkedQueue<Pair<Element, VisualizationInfo>>();

    @Override
    public void run() {
      while(!queue.isEmpty()) {
        Pair<Element, VisualizationInfo> ti = queue.poll();
        generateThumbnail(ti.first, ti.second);
      }
    }

    /**
     * Enqueue a new Thumbnail task.
     * 
     * @param parent Parent element
     * @param vi Visualiation info
     */
    public void queue(Element parent, VisualizationInfo vi) {
      queue.add(new Pair<Element, VisualizationInfo>(parent, vi));
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
    plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 1 1");

    List<VisualizationInfo> layers = plotmap.get(x, y);

    for(VisualizationInfo vi : layers) {
      Element e = vi.build(plot);
      plot.getRoot().appendChild(e);
    }
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
}
