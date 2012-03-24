package experimentalcode.students.roedler.parallelCoordinates.visualizer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
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
import experimentalcode.students.roedler.parallelCoordinates.gui.MenuOwner;
import experimentalcode.students.roedler.parallelCoordinates.gui.SubMenu;
import experimentalcode.students.roedler.parallelCoordinates.svg.menu.CheckboxMenuItem;

/**
 * Generates a SVG-Element that visualizes cluster intervals.
 * 
 * @author Robert Rödler
 */
public class ClusteringOutlineVisualization extends AbstractParallelVisualization<NumberVector<?, ?>> implements DataStoreListener, MenuOwner {
  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERAREA = "Clusteroutline";

  /**
   * The result we visualize
   */
  private Clustering<Model> clustering;

  /**
   * selected cluster
   */
  private boolean[] clustervis;

  /**
   * menu items
   */
  CheckboxMenuItem items[];

  List<Integer> list;

  boolean rounded;

  private static final double KAPPA = SVGHyperSphere.EUCLIDEAN_KAPPA;

  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public ClusteringOutlineVisualization(VisualizationTask task, List<Integer> list, boolean rounded) {
    super(task);
    this.clustering = task.getResult();
    this.list = list;
    this.rounded = rounded;
    context.addDataStoreListener(this);
    context.addResultListener(this);
    init();
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    context.removeDataStoreListener(this);
    context.removeResultListener(this);
    super.destroy();
  }

  private void init() {
    clustervis = new boolean[clustering.getAllClusters().size()];
    for(int i = 0; i < clustervis.length; i++) {
      clustervis[i] = false;
    }
    if(list != null) {
      for(Integer i : list) {
        if(i < clustervis.length) {
          clustervis[i] = true;
        }
      }
    }
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

      if(!clustervis[cnum]) {
        continue;
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
      String color;
      int clusterID = 0;

      for(@SuppressWarnings("unused")
      Cluster<?> cluster : clustering.getAllClusters()) {
        CSSClass cls = new CSSClass(this, CLUSTERAREA + clusterID);
        // cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY,
        // context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) / 2.0);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.5);
        if(clustering.getAllClusters().size() == 1) {
          color = "black";
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

  @Override
  public SubMenu getMenu() {
    SubMenu myMenu = new SubMenu(CLUSTERAREA, this);
    int clus = clustering.getAllClusters().size();

    items = new CheckboxMenuItem[clus];

    for(int num = 0; num < clus; num++) {
      items[num] = myMenu.addCheckBoxItem("Cluster " + num, Integer.toString(num), clustervis[num]);
    }
    // myMenu.addCheckBoxItem("rounded", "rounded", rounded);

    /*
     * myMenu.addSeparator();
     * 
     * myMenu.addItem("Select all", "-1"); myMenu.addItem("Unselect all", "-2");
     * myMenu.addItem("Invert all", "-3");
     */
    return myMenu;
  }

  @Override
  public void menuPressed(String id, boolean checked) {
    if(id == "rounded") {
      rounded = checked;
      incrementalRedraw();
      return;
    }

    int iid = Integer.parseInt(id);
    /*
     * if (iid < 0){ if (iid == -1){ for(int i = 0; i < clustervis.length; i++){
     * items[i].setSelected(true); clustervis[i] = true; } } if (iid == -2){
     * for(int i = 0; i < clustervis.length; i++){ items[i].setSelected(false);
     * clustervis[i] = false; } } if (iid == -3){ for(int i = 0; i <
     * clustervis.length; i++){ items[i].setSelected(!clustervis[i]);
     * clustervis[i] = !clustervis[i]; } } incrementalRedraw(); return; }
     */

    clustervis[iid] = checked;
    incrementalRedraw();
  }

  /**
   * Factory for axis visualizations
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AxisVisualization oneway - - «create»
   * 
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Cluster Outline";

    public static final OptionID VISIBLE_ID = OptionID.getOrCreateOptionID("parallel.clusteroutline.visible", "Select visible Clusteroutlines");

    public static final OptionID FILL_ID = OptionID.getOrCreateOptionID("parallel.clusteroutline.rounded", "Draw lines rounded");

    /**
     * selected cluster
     */
    private List<Integer> list;

    private boolean rounded;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory(List<Integer> list, boolean rounded) {
      super();
      this.list = list;
      this.rounded = rounded;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusteringOutlineVisualization(task, list, rounded);
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
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 4);
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

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      protected List<Integer> p;

      protected boolean rounded = false;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        // Flag fillF = new Flag(FILL_ID);
        // fillF.setDefaultValue(true);
        // if(config.grab(fillF)) {
        // rounded = fillF.getValue();
        // }
        final IntListParameter visL = new IntListParameter(VISIBLE_ID, true);
        if(config.grab(visL)) {
          p = visL.getValue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(p, rounded);
      }
    }
  }
}
