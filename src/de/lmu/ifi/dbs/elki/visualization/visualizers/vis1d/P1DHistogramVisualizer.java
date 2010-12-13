package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Generates a SVG-Element containing a histogram representing the distribution
 * of the database's objects.
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.has NumberVector oneway - - visualizes
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
// FIXME: make non-static, react to database changes!
// FIXME: cache histogram instead of recomputing it.
public class P1DHistogramVisualizer<NV extends NumberVector<NV, ?>> extends P1DVisualization<NV> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Projection Histograms";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String BIN = "bin";

  /**
   * The clustering we use for colorization
   */
  protected static final String KEY_CLUSTERING = "clustering";

  /**
   * Internal storage of the curves flag.
   */
  private boolean curves;

  /**
   * Number of bins to use in the histogram.
   */
  private int bins;

  /**
   * The database we visualize
   */
  private Database<? extends NV> database;

  /**
   * The clustering we visualize
   */
  private Clustering<Model> clustering;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   * @param curves Curves flag
   * @param bins Number of bins
   */
  public P1DHistogramVisualizer(VisualizationTask task, boolean curves, int bins) {
    super(task);
    this.curves = curves;
    this.bins = bins;
    this.database = task.getResult();
    this.clustering = task.getGenerics(KEY_CLUSTERING, Clustering.class);
  }

  @Override
  protected void redraw() {
    double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    double xsize = Projection.SCALE * task.getWidth() / task.getHeight();
    double ysize = Projection.SCALE;

    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), xsize, ysize, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    final List<Cluster<Model>> allClusters = clustering != null ? clustering.getAllClusters() : null;
    final int numc = allClusters != null ? allClusters.size() : 0;

    setupCSS(svgp, numc);

    // Creating histograms
    MinMax<Double> minmax = new MinMax<Double>();
    final double frac = 1. / database.size();
    final int cols = numc + 1;
    AggregatingHistogram<double[], double[]> histogram = new AggregatingHistogram<double[], double[]>(bins, -.5, .5, new AggregatingHistogram.Adapter<double[], double[]>() {
      @Override
      public double[] aggregate(double[] existing, double[] data) {
        for(int i = 0; i < existing.length; i++) {
          existing[i] += data[i];
        }
        return existing;
      }

      @Override
      public double[] make() {
        return new double[cols];
      }
    });

    if(allClusters != null) {
      int clusterID = 0;
      for(Cluster<Model> cluster : allClusters) {
        double[] inc = new double[cols];
        inc[clusterID + 1] = frac;
        for(DBID id : cluster.getIDs()) {
          try {
            double pos = proj.fastProjectDataToRenderSpace(database.get(id)) / Projection.SCALE;
            histogram.aggregate(pos, inc);
          }
          catch(ObjectNotFoundException e) {
            // Ignore. The object was probably deleted from the database
          }
        }
        clusterID += 1;
      }
    }
    // Actual data distribution.
    double[] inc = new double[cols];
    inc[0] = frac;
    for(DBID id : database) {
      double pos = proj.fastProjectDataToRenderSpace(database.get(id)) / Projection.SCALE;
      histogram.aggregate(pos, inc);
    }
    // for scaling, get the maximum occurring value in the bins:
    for(Pair<Double, double[]> bin : histogram) {
      for(double val : bin.second) {
        minmax.put(val);
      }
    }

    LinearScale yscale = new LinearScale(0, minmax.getMax());
    LinearScale xscale = new LinearScale(histogram.getCoverMinimum(), histogram.getCoverMaximum());

    // Axis. TODO: Use AxisVisualizer for this?
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, yscale, 0, ysize, 0, 0, true, false, context.getStyleLibrary());

      // draw axes that are non-trivial
      final int dimensionality = DatabaseUtil.dimensionality(database);
      double orig = proj.fastProjectScaledToRender(new Vector(dimensionality));
      for(int d = 0; d < dimensionality; d++) {
        Vector v = new Vector(dimensionality);
        v.set(d, 1);
        // projected endpoint of axis
        double ax = proj.fastProjectScaledToRender(v);
        if(ax != orig) {
          final double left = (orig / Projection.SCALE + 0.5) * xsize;
          final double right = (ax / Projection.SCALE + 0.5) * xsize;
          SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getScale(d), left, ysize, right, ysize, true, true, context.getStyleLibrary());
        }
      }
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("CSS class exception in axis class.", e);
    }

    double binwidth = histogram.getBinsize();
    // Visualizing
    if(!curves) {
      for(Pair<Double, double[]> bin : histogram) {
        double lpos = xscale.getScaled(bin.getFirst() - binwidth / 2);
        double rpos = xscale.getScaled(bin.getFirst() + binwidth / 2);
        double stack = 0.0;
        final int start = numc > 0 ? 1 : 0;
        for(int key = start; key < cols; key++) {
          double val = yscale.getScaled(bin.getSecond()[key]);
          Element row = SVGUtil.svgRect(svgp.getDocument(), xsize * lpos, ysize * (1 - (val + stack)), xsize * (rpos - lpos), ysize * val);
          stack = stack + val;
          SVGUtil.addCSSClass(row, BIN + (key - 1));
          layer.appendChild(row);
        }
      }
    }
    else {
      double left = xscale.getScaled(histogram.getCoverMinimum());
      double right = left;

      SVGPath[] paths = new SVGPath[cols];
      double[] lasty = new double[cols];
      for(int i = 0; i < cols; i++) {
        paths[i] = new SVGPath(xsize * left, ysize * 1);
        lasty[i] = 0;
      }

      // draw histogram lines
      for(Pair<Double, double[]> bin : histogram) {
        left = xscale.getScaled(bin.getFirst() - binwidth / 2);
        right = xscale.getScaled(bin.getFirst() + binwidth / 2);
        for(int i = 0; i < cols; i++) {
          double val = yscale.getScaled(bin.getSecond()[i]);
          if(lasty[i] != val) {
            paths[i].lineTo(xsize * left, ysize * (1 - lasty[i]));
            paths[i].lineTo(xsize * left, ysize * (1 - val));
            paths[i].lineTo(xsize * right, ysize * (1 - val));
            lasty[i] = val;
          }
        }
      }
      // close and insert all lines.
      for(int i = 0; i < cols; i++) {
        if(lasty[i] != 0) {
          paths[i].lineTo(xsize * right, ysize * (1 - lasty[i]));
        }
        paths[i].lineTo(xsize * right, ysize * 1);
        Element elem = paths[i].makeElement(svgp);
        SVGUtil.addCSSClass(elem, BIN + (i - 1));
        layer.appendChild(elem);
      }
    }
  }

  /**
   * Generate the needed CSS classes.
   * 
   * @param svgp Plot context
   * @param numc Number of classes we need.
   */
  private void setupCSS(SVGPlot svgp, int numc) {
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    CSSClass allInOne = new CSSClass(svgp, BIN + -1);
    if(!curves) {
      allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      allInOne.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 1.0);
    }
    else {
      allInOne.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      allInOne.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    }
    svgp.addCSSClassOrLogError(allInOne);

    for(int clusterID = 0; clusterID < numc; clusterID++) {
      CSSClass bin = new CSSClass(svgp, BIN + clusterID);

      if(!curves) {
        bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(clusterID));
      }
      else {
        bin.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(clusterID));
        bin.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
        bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      }

      svgp.addCSSClassOrLogError(bin);
    }
  }

  /**
   * Visualizer factory for 1D histograms
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses P1DHistogramVisualizer oneway - - «create»
   * 
   * @param <NV> Number vector type
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> {
    /**
     * OptionID for {@link #STYLE_CURVES_FLAG}.
     */
    public static final OptionID STYLE_CURVES_ID = OptionID.getOrCreateOptionID("projhistogram.curves", "Use curves instead of the stacked histogram style.");

    /**
     * Flag to specify the "curves" rendering style.
     * 
     * <p>
     * Key: {@code -histogram.curves}
     * </p>
     */
    private final Flag STYLE_CURVES_FLAG = new Flag(STYLE_CURVES_ID);

    /**
     * Internal storage of the curves flag.
     */
    private boolean curves;

    /**
     * Number of bins to use in histogram.
     */
    private static final int DEFAULT_BINS = 50;

    /**
     * Option ID for parameter {@link #HISTOGRAM_BINS_PARAM}
     */
    public static final OptionID HISTOGRAM_BINS_ID = OptionID.getOrCreateOptionID("projhistogram.bins", "Number of bins in the distribution histogram");

    /**
     * Parameter to specify the number of bins to use in histogram.
     * 
     * <p>
     * Key: {@code -projhistogram.bins} Default: 20
     * </p>
     */
    private final IntParameter HISTOGRAM_BINS_PARAM = new IntParameter(HISTOGRAM_BINS_ID, new GreaterEqualConstraint(2), DEFAULT_BINS);

    /**
     * Number of bins to use in the histogram.
     */
    private int bins = DEFAULT_BINS;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super();
      config = config.descend(this);
      if(config.grab(STYLE_CURVES_FLAG)) {
        curves = STYLE_CURVES_FLAG.getValue();
      }
      if(config.grab(HISTOGRAM_BINS_PARAM)) {
        bins = HISTOGRAM_BINS_PARAM.getValue();
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new P1DHistogramVisualizer<NV>(task, curves, bins);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      ArrayList<Database<?>> databases = ResultUtil.filterResults(result, Database.class);
      for(Database<?> database : databases) {
        if(!VisualizerUtil.isNumberVectorDatabase(database)) {
          return;
        }
        // register self
        final VisualizationTask task = new VisualizationTask(NAME, context, database, this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
        context.addVisualizer(database, task);
      }
      ArrayList<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        // register self
        final VisualizationTask task = new VisualizationTask(NAME, context, context.getDatabase(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
        task.put(KEY_CLUSTERING, c);
        context.addVisualizer(c, task);
      }
    }

    @Override
    public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }

    @Override
    public Object getVisualizationType() {
      return P1DVisualization.class;
    }
  }
}