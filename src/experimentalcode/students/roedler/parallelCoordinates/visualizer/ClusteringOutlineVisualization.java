package experimentalcode.students.roedler.parallelCoordinates.visualizer;

import java.util.Collection;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;

/**
 * Generates a SVG-Element that visualizes cluster intervals.
 * 
 * @author Robert Rödler
 */
public class ClusteringOutlineVisualization extends AbstractParallelVisualization<NumberVector<?, ?>> implements DataStoreListener {
  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERAREA = "Clusteroutline";

  /**
   * The result we visualize
   */
  private Clustering<Model> clustering;

  boolean rounded;

  private static final double KAPPA = SVGHyperSphere.EUCLIDEAN_KAPPA;

  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public ClusteringOutlineVisualization(VisualizationTask task, boolean rounded) {
    super(task);
    this.clustering = task.getResult();
    this.rounded = rounded;
    context.addDataStoreListener(this);
    context.addResultListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    context.removeDataStoreListener(this);
    context.removeResultListener(this);
    super.destroy();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    int dim = proj.getVisibleDimensions();

    DoubleMinMax[] mms = DoubleMinMax.newArray(dim);
    DoubleMinMax[] midmm = DoubleMinMax.newArray(dim - 1);

    Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<?> clus = ci.next();
      for(int i = 0; i < dim; i++) {
        mms[i].reset();
      }
      for(int i = 0; i < dim - 1; i++) {
        midmm[i].reset();
      }

      for(DBID objId : clus.getIDs()) {
        double[] yPos = getYPositions(objId);

        for(int i = 0; i < dim; i++) {
          mms[i].put(yPos[i]);
          if(i > 0) {
            midmm[i - 1].put((yPos[i] + yPos[i - 1]) / 2.);
          }
        }
      }

      SVGPath path = new SVGPath();
      boolean first = true;
      if(rounded) {
        for(int i = 0; i < dim; i++) {
          if(first) {
            path.drawTo(getVisibleAxisX(i), mms[i].getMax());
            first = false;
          }
          else {
            double lx = getVisibleAxisX(i - 1);
            double mx = getVisibleAxisX(i - .5);
            double rx = getVisibleAxisX(i);
            double lef = mms[i - 1].getMax();
            double mid = midmm[i - 1].getMax();
            double rig = mms[i].getMax();
            path.cubicTo(mx, mid + (mid - lef) * KAPPA, mx - (mx - lx) * KAPPA, mid, mx, mid);
            path.cubicTo(mx + (rx - mx) * KAPPA, mid, rx, rig - (mid - rig) * KAPPA, rx, rig);
          }
        }
        first = true;
        for(int i = dim - 1; i > 0; i--) {
          if(first) {
            path.drawTo(getVisibleAxisX(i), mms[i].getMin());
            first = false;
          }
          else {
            double lx = getVisibleAxisX(i - 1);
            double mx = getVisibleAxisX(i - .5);
            double rx = getVisibleAxisX(i);
            double lef = mms[i - 1].getMin();
            double mid = midmm[i - 1].getMin();
            double rig = mms[i].getMin();
            path.cubicTo(rx + (rx - mx) * KAPPA, mid, mx, mid - (mid - rig) * KAPPA, mx, mid);
            path.cubicTo(mx - (mx - lx) * KAPPA, mid, mx, mid - (mid - lef) * KAPPA, lx, lef);
          }
        }
      }
      else {
        for(int i = 0; i < dim; i++) {
          path.drawTo(getVisibleAxisX(i), mms[i].getMax());
          if(i < dim - 1) {
            path.drawTo(getVisibleAxisX(i + .5), midmm[i].getMax());
          }
        }
        for(int i = dim - 1; i >= 0; i--) {
          if(i < dim - 1) {
            path.drawTo(getVisibleAxisX(i + .5), midmm[i].getMin());
          }
          path.drawTo(getVisibleAxisX(i), mms[i].getMin());
        }
      }
      path.close();

      Element intervals = path.makeElement(svgp);
      SVGUtil.addCSSClass(intervals, CLUSTERAREA + cnum);
      layer.appendChild(intervals);
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    if(!svgp.getCSSClassManager().contains(CLUSTERAREA)) {
      ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
      int clusterID = 0;

      for(@SuppressWarnings("unused")
      Cluster<?> cluster : clustering.getAllClusters()) {
        CSSClass cls = new CSSClass(this, CLUSTERAREA + clusterID);
        // cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY,
        // context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) / 2.0);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.5);
        final String color;
        if(clustering.getAllClusters().size() == 1) {
          color = SVGConstants.CSS_BLACK_VALUE;
        }
        else {
          color = colors.getColor(clusterID);
        }
        // cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);

        svgp.addCSSClassOrLogError(cls);
        clusterID++;
      }
    }
  }

  /**
   * Factory for axis visualizations
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusteringOutlineVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Cluster Outline";

    public static final OptionID FILL_ID = OptionID.getOrCreateOptionID("parallel.clusteroutline.rounded", "Draw lines rounded");

    private boolean rounded = true;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusteringOutlineVisualization(task, rounded);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        if(c.getAllClusters().size() > 0) {
          IterableIterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ParallelPlotProjector.class);
          for(ParallelPlotProjector<?> p : ps) {
            final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA -1);
            task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
            baseResult.getHierarchy().add(c, task);
            baseResult.getHierarchy().add(p, task);
          }
        }
      }
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}