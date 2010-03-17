package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Generates a SVG-Element containing a histogram representing the distribution
 * of the database's objects.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class Projection1DHistogramVisualizer<NV extends NumberVector<NV, ?>> extends Projection1DVisualizer<NV> {
  /**
   * OptionID for {@link #STYLE_ROW_FLAG}.
   */
  public static final OptionID STYLE_ROW_ID = OptionID.getOrCreateOptionID("histogram.stack", "Alternative style: stacked bars.");

  /**
   * Flag to specify the "row" rendering style.
   * 
   * <p>
   * Key: {@code -histogram.row}
   * </p>
   */
  private final Flag STYLE_ROW_FLAG = new Flag(STYLE_ROW_ID);

  /**
   * Internal storage of the row flag.
   */
  private boolean row;

  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Histograms";

  /**
   * Number of bins to use in histogram.
   */
  // TODO: make configurable!
  private static final int BINS = 20;

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String BIN = "bin";

  /**
   * Constructor.
   */
  public Projection1DHistogramVisualizer(Parameterization config) {
    if(config.grab(STYLE_ROW_FLAG)) {
      row = STYLE_ROW_FLAG.getValue();
    }
  }

  /**
   * Initialization.
   * 
   * @param context context.
   */
  public void init(VisualizerContext context) {
    super.init(NAME, context);
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
    // if(row) {
    // allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY,
    // SVGConstants.CSS_BLACK_VALUE);
    // allInOne.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 1.0);
    // }
    // else {
    allInOne.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
    allInOne.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.005 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
    allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    // }
    try {
      svgp.getCSSClassManager().addClass(allInOne);
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("Could not add allInOne histogram CSS class.", e);
    }

    for(int clusterID = 0; clusterID < numc; clusterID++) {
      CSSClass bin = new CSSClass(svgp, BIN + clusterID);

      if(row) {
        bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(clusterID));
      }
      else {
        bin.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(clusterID));
        bin.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.005 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
        bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      }

      try {
        svgp.getCSSClassManager().addClass(bin);
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("Could not create histogram CSS classes.", e);
      }
    }
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    Element layer = super.setupCanvas(svgp, width, height);

    Clustering<Model> clustering = context.getOrCreateDefaultClustering();
    final List<Cluster<Model>> allClusters = clustering.getAllClusters();

    setupCSS(svgp, allClusters.size());

    // Get the database.
    Database<NV> database = context.getDatabase();

    // Creating histograms
    MinMax<Double> minmax = new MinMax<Double>();
    final double frac = 1. / database.size();
    final int cols = allClusters.size() + 1;
    AggregatingHistogram<double[], double[]> histogram = new AggregatingHistogram<double[], double[]>(BINS, 0, 1, new AggregatingHistogram.Adapter<double[], double[]>() {
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

    int clusterID = 0;
    for(Cluster<Model> cluster : allClusters) {
      double[] inc = new double[cols];
      inc[clusterID + 1] = frac;
      for(int id : cluster.getIDs()) {
        double pos = proj.projectDataToRenderSpace(database.get(id)).get(0);
        histogram.aggregate(pos, inc);
      }
      clusterID += 1;
    }
    // Actual data distribution.
    double[] inc = new double[cols];
    inc[0] = frac;
    for(int id : database) {
      double pos = proj.projectDataToRenderSpace(database.get(id)).get(0);
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
      SVGSimpleLinearAxis.drawAxis(svgp, layer, yscale, -1, 1, -1, -1, true, false, context.getStyleLibrary());

      // draw axes that are non-trivial
      Vector orig = proj.projectScaledToRender(new Vector(database.dimensionality()));
      for(int d = 1; d <= database.dimensionality(); d++) {
        Vector v = new Vector(database.dimensionality());
        v.set(d - 1, 1);
        // projected endpoint of axis
        Vector ax = proj.projectScaledToRender(v);
        if(ax.get(0) != orig.get(0)) {
          SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getScale(d), orig.get(0), 1, ax.get(0), 1, true, true, context.getStyleLibrary());
        }
      }
      // SVGSimpleLinearAxis.drawAxis(svgp, layer, xscale, -1, 1, 1, 1, true,
      // true);
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("CSS class exception in axis class.", e);
    }

    double binwidth = histogram.getBinsize();
    // Visualizing
    if(row) {
      for(Pair<Double, double[]> bin : histogram) {
        double lpos = xscale.getScaled(bin.getFirst() - binwidth / 2);
        double rpos = xscale.getScaled(bin.getFirst() + binwidth / 2);
        double stack = 0.0;
        for(int key = 1; key < cols; key++) {
          double val = yscale.getScaled(bin.getSecond()[key]);
          Element row = SVGUtil.svgRect(svgp.getDocument(), lpos * 2 - 1, 1 - (val + stack) * 2, (rpos - lpos) * 2, val * 2);
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
      for(int i = 0; i < cols; i++) {
        paths[i] = new SVGPath(left * 2 - 1, 1);
      }

      // draw histogram lines
      for(Pair<Double, double[]> bin : histogram) {
        left = xscale.getScaled(bin.getFirst() - binwidth / 2);
        right = xscale.getScaled(bin.getFirst() + binwidth / 2);
        for(int i = 0; i < cols; i++) {
          double val = yscale.getScaled(bin.getSecond()[i]);
          paths[i].lineTo(left * 2 - 1, 1 - val * 2);
          paths[i].lineTo(right * 2 - 1, 1 - val * 2);
        }
      }
      // close and insert all lines.
      for(int i = 0; i < cols; i++) {
        paths[i].lineTo(right * 2 - 1, 1);
        Element elem = paths[i].makeElement(svgp);
        SVGUtil.addCSSClass(elem, BIN + (i - 1));
        layer.appendChild(elem);
      }
    }
    return layer;
  }
}