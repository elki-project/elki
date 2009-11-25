package experimentalcode.erich.visualization.visualizers.vis1d;

import java.util.ArrayList;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.erich.visualization.visualizers.VisualizerContext;

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
   * OptionID for {@link #STYLE_ROW_PARAM}.
   */
  public static final OptionID STYLE_ROW_ID = OptionID.getOrCreateOptionID("histogram.row", "Alternative style: Rows.");

  /**
   * Flag to specify the "row" rendering style.
   * 
   * <p>
   * Key: {@code -histogram.row}
   * </p>
   */
  private final Flag STYLE_ROW_PARAM = new Flag(STYLE_ROW_ID);

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
   * Constructor, as per {@link Parameterizable} API.
   */
  public Projection1DHistogramVisualizer() {
    addOption(STYLE_ROW_PARAM);
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    row = STYLE_ROW_PARAM.getValue();
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
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
    CSSClass allInOne = new CSSClass(svgp, BIN + -1);
    if(row) {
      allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY, "black");
      allInOne.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 1.0);
    }
    else {
      allInOne.setStatement(SVGConstants.CSS_STROKE_PROPERTY, "black");
      allInOne.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.005);
      allInOne.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.0);
    }
    try {
      svgp.getCSSClassManager().addClass(allInOne);
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("Could not add allInOne histogram CSS class.", e);
    }

    for(int clusterID = 0; clusterID < numc; clusterID++) {
      CSSClass bin = new CSSClass(svgp, BIN + clusterID);

      if(row) {
        bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getColorLibrary().getColor(clusterID));
        bin.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 1.0);
      }
      else {
        bin.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getColorLibrary().getColor(clusterID));
        bin.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.005);
        bin.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.0);
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
  public Element visualize(SVGPlot svgp, VisualizationProjection proj) {
    Element layer = super.setupCanvas(svgp);

    Clustering<Model> clustering = context.getOrCreateDefaultClustering();
    final List<Cluster<Model>> allClusters = clustering.getAllClusters();

    setupCSS(svgp, allClusters.size());

    ArrayList<AggregatingHistogram<Double, Double>> hists = new ArrayList<AggregatingHistogram<Double, Double>>(allClusters.size());

    // Get the database.
    Database<NV> database = context.getDatabase();

    // Creating histograms
    MinMax<Double> minmax = new MinMax<Double>();
    final double frac = 1. / database.size();
    // TODO: change this into a double[] histogram to have the same bins everywhere!
    AggregatingHistogram<Double, Double> allInOne = AggregatingHistogram.DoubleSumHistogram(BINS, 0, 1);

    int clusterID = 0;
    for(Cluster<Model> cluster : allClusters) {
      AggregatingHistogram<Double, Double> hist = AggregatingHistogram.DoubleSumHistogram(BINS, 0, 1);
      for(int id : cluster.getIDs()) {
        double pos = proj.projectDataToRenderSpace(database.get(id)).get(0);
        hist.aggregate(pos, frac);
        allInOne.aggregate(pos, frac);
      }
      assert(hists.size() == clusterID);
      hists.add(hist);
      // for scaling, get the maximum occurring value in the bins:
      for(Pair<Double, Double> bin : hist) {
        minmax.put(bin.getSecond());
      }
      
      clusterID += 1;
    }
    // for scaling, get the maximum occurring value in the bins:
    for(Pair<Double, Double> bin : allInOne) {
      minmax.put(bin.getSecond());
    }

    LinearScale scale = new LinearScale(0, minmax.getMax());

    // Axis. TODO: Use AxisVisualizer for this.
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scale, -1, 1, -1, -1, true, false);
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("CSS class exception in axis class.", e);
    }

    // Visualizing
    if(row) {
      // FIXME: stacking is nontrivial with this kind of ordering (and because
      // of using one histogram each! - so the "row" mode is currently broken.
      for(int key = 0; key < hists.size(); key++) {
        AggregatingHistogram<Double, Double> hist = hists.get(key);
        double binsize = hist.getBinsize();
        for(Pair<Double, Double> bin : hist) {
          double lpos = bin.getFirst() - binsize / 2;
          double rpos = bin.getFirst() + binsize / 2;
          double val = bin.getSecond() / scale.getMax();
          Element row = SVGUtil.svgRect(svgp.getDocument(), lpos, 1 - val, rpos - lpos, val);
          SVGUtil.addCSSClass(row, BIN + key);
          layer.appendChild(row);
        }
      }
    }
    else {
      layer.appendChild(drawLine(svgp, -1, allInOne, scale.getMax()));
      for(int key = 0; key < hists.size(); key++) {
        layer.appendChild(drawLine(svgp, key, hists.get(key), scale.getMax()));
      }
    }
    return layer;
  }

  /**
   * Helper to draw a single histogram line.
   * 
   * @param svgp Plot context.
   * @param color Color number
   * @param hist Histogram to plot
   * @param max Maximum value for scaling.
   * @return New SVG path
   */
  private Element drawLine(SVGPlot svgp, int color, AggregatingHistogram<Double, Double> hist, double max) {
    SVGPath path = new SVGPath(hist.getCoverMinimum(), 1);
    double left = 0;
    double right = 0;
    double binwidth = hist.getBinsize();
    for(Pair<Double, Double> bin : hist) {
      double val = bin.getSecond() / max;
      left = bin.getFirst() - binwidth / 2;
      right = bin.getFirst() + binwidth / 2;
      path.lineTo(left, 1 - val * 2);
      path.lineTo(right, 1 - val * 2);
    }
    path.lineTo(right, hist.getCoverMaximum());
    Element elem = path.makeElement(svgp);
    SVGUtil.addCSSClass(elem, BIN + color);
    return elem;
  }
}
