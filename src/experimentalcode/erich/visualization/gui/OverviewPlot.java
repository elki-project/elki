package experimentalcode.erich.visualization.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.Thumbnailer;
import experimentalcode.remigius.Visualizers.Projection1DVisualizer;
import experimentalcode.remigius.Visualizers.Projection2DVisualizer;
import experimentalcode.remigius.Visualizers.UnprojectedVisualizer;
import experimentalcode.remigius.Visualizers.Visualizer;

/**
 * Generate an overview plot for a set of vis
 * 
 * @author Erich Schubert
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
   * Result we work on.
   */
  @SuppressWarnings("unused")
  private MultiResult result;

  /**
   * Map of coordinates to plots.
   */
  protected PlotMap plotmap;

  /**
   * Constructor.
   */
  public OverviewPlot(Database<? extends DatabaseObject> db, MultiResult result) {
    super();
    this.db = db;
    this.result = result;
  }

  /**
   * Add vis to the plot.
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

  public void refresh() {
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
    plotmap = new PlotMap();
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
            VisualizationInfo vi = new Visualization2DInfo(v, proj);
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
          VisualizationInfo vi = new Visualization1DInfo(v, proj);
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
    final double plotw = plotmap.minmaxx.getMax() - plotmap.minmaxx.getMin();
    final double ploth = plotmap.minmaxy.getMax() - plotmap.minmaxy.getMin();
    String vb = plotmap.minmaxx.getMin() + " " + plotmap.minmaxy.getMin() + " " + plotw + " " + ploth;
    // Reset root bounding box.
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / plotw * ploth) + "cm");
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, vb);
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
        thumbnails.queue(new Pair<Element, VisualizationInfo>(gg, vi));
      }
      getRoot().appendChild(g);
    }
    thumbnails.start();
  }

  /**
   * Generate a single Thumbnail.
   * 
   * @param g Parent element to insert the thumbnail into.
   * @param vi Visualization.
   */
  protected void generateThumbnail(final Element g, VisualizationInfo vi) {
    File thumb = vi.makeThumbnail();
    final Element i = this.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(i, SVGConstants.SVG_X_ATTRIBUTE, 0);
    SVGUtil.setAtt(i, SVGConstants.SVG_Y_ATTRIBUTE, 0);
    SVGUtil.setAtt(i, SVGConstants.SVG_WIDTH_ATTRIBUTE, 1);
    SVGUtil.setAtt(i, SVGConstants.SVG_HEIGHT_ATTRIBUTE, 1);
    i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, thumb.toURI().toString());
    this.scheduleUpdate(new Runnable() {
      @Override
      public void run() {
        // Replace children.
        while(g.getFirstChild() != null) {
          g.removeChild(g.getFirstChild());
        }
        g.appendChild(i);
      }
    });
  }

  class ThumbnailThread extends Thread {
    boolean shutdown = false;

    Queue<Pair<Element, VisualizationInfo>> queue = new ConcurrentLinkedQueue<Pair<Element, VisualizationInfo>>();

    @Override
    public void run() {
      while(!queue.isEmpty()) {
        Pair<Element, VisualizationInfo> ti = queue.poll();
        generateThumbnail(ti.first, ti.second);
      }
    }

    public void queue(Pair<Element, VisualizationInfo> thumbnailTask) {
      queue.add(thumbnailTask);
    }
  }

  /**
   * Ugly cast of the database to a default number vector.
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  private Database<NV> uglyCastDatabase() {
    return (Database<NV>) db;
  }

  class PlotMap extends HashMap<DoubleDoublePair, ArrayList<VisualizationInfo>> {
    private static final long serialVersionUID = 1L;

    MinMax<Double> minmaxx = new MinMax<Double>();

    MinMax<Double> minmaxy = new MinMax<Double>();

    PlotMap() {
      super();
    }

    void addVis(double x, double y, double w, double h, VisualizationInfo v) {
      ArrayList<VisualizationInfo> l = this.get(new DoubleDoublePair(x, y));
      if(l == null) {
        l = new ArrayList<VisualizationInfo>();
        this.put(new DoubleDoublePair(x, y), l);
      }
      l.add(v);
      // Update min/max
      minmaxx.put(x);
      minmaxx.put(x + w);
      minmaxy.put(y);
      minmaxy.put(y + h);
    }

    List<VisualizationInfo> get(double x, double y) {
      return this.get(new DoubleDoublePair(x, y));
    }
  }

  static Thumbnailer t = new Thumbnailer();

  abstract class VisualizationInfo {

    File thumbnail = null;

    abstract Element build(SVGPlot plot);

    File getThumbnailIfGenerated() {
      return thumbnail;
    }

    File makeThumbnail() {
      SVGPlot plot = new SVGPlot();
      plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 1 1");
      Element e = build(plot);
      plot.getRoot().appendChild(e);
      synchronized(t) {
        thumbnail = t.thumbnail(plot, 512);
      }
      return thumbnail;
    }
  }

  class VisualizationUnprojectedInfo extends VisualizationInfo {
    private UnprojectedVisualizer vis;

    public VisualizationUnprojectedInfo(UnprojectedVisualizer vis) {
      this.vis = vis;
    }

    @Override
    public Element build(SVGPlot plot) {
      synchronized(vis) {
        return vis.visualize(plot);
      }
    }
  }

  class Visualization2DInfo extends VisualizationInfo {
    VisualizationProjection proj;

    Projection2DVisualizer<?> vis;

    public Visualization2DInfo(Projection2DVisualizer<?> vis, VisualizationProjection proj) {
      super();
      this.vis = vis;
      this.proj = proj;
    }

    @Override
    public Element build(SVGPlot plot) {
      synchronized(vis) {
        return vis.visualize(plot, proj);
      }
    }
  }

  class Visualization1DInfo extends VisualizationInfo {
    VisualizationProjection proj;

    Projection1DVisualizer<?> vis;

    public Visualization1DInfo(Projection1DVisualizer<?> vis, VisualizationProjection proj) {
      super();
      this.vis = vis;
      this.proj = proj;
    }

    @Override
    public Element build(SVGPlot plot) {
      synchronized(vis) {
        return vis.visualize(plot, proj);
      }
    }
  }
}
