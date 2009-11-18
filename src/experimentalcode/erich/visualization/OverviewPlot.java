package experimentalcode.erich.visualization;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.remigius.Visualizers.Projection1DVisualizer;
import experimentalcode.remigius.Visualizers.Projection2DVisualizer;
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

  public void refresh() {
    // split the visualizers into three sets.
    Collection<Projection1DVisualizer<NV>> vis1d = new ArrayList<Projection1DVisualizer<NV>>(vis.size());
    Collection<Projection2DVisualizer<NV>> vis2d = new ArrayList<Projection2DVisualizer<NV>>(vis.size());
    Collection<Visualizer> visot = new ArrayList<Visualizer>(vis.size());
    for(Visualizer v : vis) {
      if(Projection2DVisualizer.class.isAssignableFrom(v.getClass())) {
        vis2d.add((Projection2DVisualizer<NV>) v);
      }
      else if(Projection1DVisualizer.class.isAssignableFrom(v.getClass())) {
        vis1d.add((Projection1DVisualizer<NV>) v);
      }
      else {
        visot.add(v);
      }
    }
    // We'll use three regions for now:
    // 2D projections starting at 0,0 and going right and down.
    // 1D projections starting at 0, -1 and going right
    // Other projections starting at -1, min() and going down.
    PlotMap plotmap = new PlotMap();
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
          VisualizationProjection<NV> proj = new VisualizationProjection<NV>(dvdb, scales, d1, d2);

          for(Projection2DVisualizer<NV> v : vis2d) {
            VisualizationInfo vi = new Visualization2DInfo(v, proj);
            plotmap.addVis(d1 - 1, d2 - 2, vi);
          }
        }
      }
    }
    if(vis1d.size() > 0) {
      int dim = db.dimensionality();
      for(int d1 = 1; d1 <= dim; d1++) {
        VisualizationProjection<NV> proj = new VisualizationProjection<NV>(dvdb, scales, d1, (d1 == 1 ? 2 : 1));
        int ypos = 1;
        for(Projection1DVisualizer<NV> v : vis1d) {
          VisualizationInfo vi = new Visualization1DInfo(v, proj);
          plotmap.addVis(d1 - 1, -ypos, vi);
          ypos = ypos + 1;
        }
      }
    }
    if(visot.size() > 0) {
      // find starting position.
      int pos = plotmap.miny;
      if(pos == Integer.MAX_VALUE) {
        pos = 0;
      }
      for (Visualizer v : visot) {
        VisualizationInfo vi = new VisualizationInfo(v);
        plotmap.addVis(-1, pos, vi);
        pos++;
      }
    }

    final int plotw = plotmap.maxx - plotmap.minx + 1;
    final int ploth = plotmap.maxy - plotmap.miny + 1;
    String vb = plotmap.minx + " " + plotmap.miny + " " + plotw + " " + ploth;
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, vb);
    for (int x = plotmap.minx; x <= plotmap.maxx; x++) {
      for (int y = plotmap.miny; y <= plotmap.maxy; y++) {
        int num = 0;
        List<VisualizationInfo> visl = plotmap.get(x, y);
        if (visl != null) {
          num = visl.size();
        }
        Element t = SVGUtil.svgText(getDocument(), x + .5, y + .5, ""+num);
        t.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: .2; fill: black");
        getRoot().appendChild(t);
      }
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

  class PlotMap extends HashMap<IntIntPair, ArrayList<VisualizationInfo>> {
    private static final long serialVersionUID = 1L;

    int minx = Integer.MAX_VALUE;

    int miny = Integer.MAX_VALUE;

    int maxx = Integer.MIN_VALUE;

    int maxy = Integer.MIN_VALUE;

    PlotMap() {
      super();
    }

    void addVis(int x, int y, VisualizationInfo v) {
      ArrayList<VisualizationInfo> l = this.get(new IntIntPair(x, y));
      if(l == null) {
        l = new ArrayList<VisualizationInfo>();
        this.put(new IntIntPair(x, y), l);
      }
      l.add(v);
      // Update min/max
      minx = Math.min(minx, x);
      miny = Math.min(miny, y);
      maxx = Math.max(maxx, x);
      maxy = Math.max(maxy, y);
    }

    List<VisualizationInfo> get(int x, int y) {
      return this.get(new IntIntPair(x, y));
    }
  }

  class VisualizationInfo {
    Double x = null;

    Double y = null;

    int depth = 0;

    File thumbnail = null;

    Visualizer vis = null;

    VisualizationInfo(final Visualizer vis) {
      this.vis = vis;
    }
  }

  class Visualization2DInfo extends VisualizationInfo {
    VisualizationProjection<NV> proj;

    public Visualization2DInfo(Projection2DVisualizer<NV> vis, VisualizationProjection<NV> proj) {
      super(vis);
      this.proj = proj;
    }
  }

  class Visualization1DInfo extends VisualizationInfo {
    VisualizationProjection<NV> proj;

    public Visualization1DInfo(Projection1DVisualizer<NV> vis, VisualizationProjection<NV> proj) {
      super(vis);
      this.proj = proj;
    }
  }
}
