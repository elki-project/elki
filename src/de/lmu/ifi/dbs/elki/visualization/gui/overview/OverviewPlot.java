package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.PairUtil;
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
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerComparator;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.VisualizerChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d.P1DVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.UnprojectedVisFactory;

/**
 * Generate an overview plot for a set of visualizations.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * 
 * @apiviz.landmark
 * @apiviz.uses VisualizerComparator
 * @apiviz.has VisualizerContext
 * @apiviz.composedOf PlotMap
 * @apiviz.has DetailViewSelectedEvent
 * 
 * @param <NV> Number vector type
 */
public class OverviewPlot<NV extends NumberVector<NV, ?>> extends SVGPlot implements ContextChangeListener {
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
    context.addContextChangeListener(this);
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
    List<Pair<AnyResult, P1DVisFactory<?>>> vis1d = new ArrayList<Pair<AnyResult, P1DVisFactory<?>>>();
    List<Pair<AnyResult, P2DVisFactory<?>>> vis2d = new ArrayList<Pair<AnyResult, P2DVisFactory<?>>>();
    List<Pair<AnyResult, UnprojectedVisFactory<?>>> visup = new ArrayList<Pair<AnyResult, UnprojectedVisFactory<?>>>();
    for(Pair<AnyResult, VisFactory<?>> pair : context.iterVisualizers()) {
      VisFactory<?> v = pair.getSecond();
      if(P2DVisFactory.class.isAssignableFrom(v.getClass())) {
        vis2d.add(new Pair<AnyResult, P2DVisFactory<?>>(pair.getFirst(), (P2DVisFactory<?>) v));
      }
      else if(P1DVisFactory.class.isAssignableFrom(v.getClass())) {
        vis1d.add(new Pair<AnyResult, P1DVisFactory<?>>(pair.getFirst(), (P1DVisFactory<?>) v));
      }
      else if(UnprojectedVisFactory.class.isAssignableFrom(v.getClass())) {
        visup.add(new Pair<AnyResult, UnprojectedVisFactory<?>>(pair.getFirst(), (UnprojectedVisFactory<?>) v));
      }
      else {
        LoggingUtil.warning("Encountered visualization that is neither projected nor unprojected: " + v.getClass());
      }
    }
    // TODO: work on layers instead of visualizers!
    Comparator<Pair<? extends AnyResult, ? extends VisFactory<?>>> c = PairUtil.comparatorSecond(new VisualizerComparator());
    Collections.sort(vis1d, c);
    Collections.sort(vis2d, c);
    Collections.sort(visup, c);
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
    int dmax = Math.min(DatabaseUtil.dimensionality(dvdb), maxdim);
    if(vis2d.size() > 0) {
      for(int d1 = 1; d1 <= dmax; d1++) {
        for(int d2 = d1 + 1; d2 <= dmax; d2++) {
          Projection2D proj = new Simple2D(scales, d1, d2);

          for(Pair<AnyResult, P2DVisFactory<?>> pair : vis2d) {
            VisualizationInfo vi = new VisualizationProjectedInfo<Projection2D>(pair.getFirst(), pair.getSecond(), proj, 1., 1.);
            plotmap.addVis(d1 - 1, d2 - 2, 1., 1., vi);
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
        Projection2D proj = new AffineProjection(scales, p);
        for(Pair<AnyResult, P2DVisFactory<?>> pair : vis2d) {
          final double sizeh = Math.ceil((dmax - 1) / 2.0);
          VisualizationInfo vi = new VisualizationProjectedInfo<Projection2D>(pair.getFirst(), pair.getSecond(), proj, sizeh, sizeh);
          plotmap.addVis(Math.ceil((dmax - 1) / 2.0), 0.0, sizeh, sizeh, vi);
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
        Projection1D proj = new Simple1D(scales, d1);
        double ypos = -.1;
        for(Pair<AnyResult, P1DVisFactory<?>> pair : vis1d) {
          // TODO: 1d vis might have a different native scaling.
          double height = 0.66;
          VisualizationInfo vi = new VisualizationProjectedInfo<Projection1D>(pair.getFirst(), pair.getSecond(), proj, 1., height);
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
      // FIXME: use multiple columns!
      for(Pair<AnyResult, UnprojectedVisFactory<?>> pair : visup) {
        VisualizationInfo vi = new VisualizationUnprojectedInfo(pair.getFirst(), pair.getSecond(), 1., 1.);
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
    vistoelem = new HashMap<VisualizationInfo, Element>();

    final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());

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
          Visualization vis = vi.buildThumb(context, this, vi.getWidth(), vi.getHeight(), thumbsize);
          if(vis.getLayer() == null) {
            LoggingUtil.warning("Visualization returned empty layer: " + vis);
          }
          else {
            parent.appendChild(vis.getLayer());
          }
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
      final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());
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
              try {
                Visualization vis = vi.buildThumb(context, this, vi.getWidth(), vi.getHeight(), thumbsize);
                Element layer = vis.getLayer();
                if(layer != null) {
                  gg.appendChild(layer);
                }
              }
              catch(RuntimeException ex) {
                LoggingUtil.exception("Visualizer failed: ", ex);
              }
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
    List<VisualizationInfo> layers = plotmap.get(x, y);
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
    context.removeContextChangeListener(this);
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

  @Override
  public void contextChanged(ContextChangedEvent e) {
    if(e instanceof VisualizerChangedEvent) {
      // VisualizerChangedEvent vce = (VisualizerChangedEvent) e;
      // TODO: lazy refresh!
      refresh();
    }
  }
}