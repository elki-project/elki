package experimentalcode.remigius.Visualizers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.remigius.ShapeLibrary;

/**
 * Generates a SVG-Element containing a histogram representing the distribution
 * of the database's objects.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV>
 */
public class HistogramVisualizer<NV extends NumberVector<NV, ?>> extends Projection1DVisualizer<NV> {

  public static final OptionID STYLE_ROW_ID = OptionID.getOrCreateOptionID("histogram.row", "Alternative style: Rows.");

  private final Flag STYLE_ROW_PARAM = new Flag(STYLE_ROW_ID);

  private boolean row;

  private static final String NAME = "Histograms";

  private static final int BINS = 20;

  private Clustering<Model> clustering;

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String BIN = "bin";

  public HistogramVisualizer() {
    addOption(STYLE_ROW_PARAM);
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    row = STYLE_ROW_PARAM.getValue();
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  public void init(VisualizerContext context, Clustering<Model> clustering) {
    init(0, NAME, context);
    this.clustering = clustering;
  }

  private void setupCSS(SVGPlot svgp) {

    // creating IDs manually because cluster often return a null-ID.
    int clusterID = 0;

    CSSClass allInOne = new CSSClass(svgp, BIN + -1);
    for(Cluster<Model> cluster : clustering.getAllClusters()) {

      CSSClass bin = new CSSClass(svgp, BIN + clusterID);
      String coloredElement;

      if(row) {
        bin.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 1.0);
        coloredElement = SVGConstants.CSS_FILL_PROPERTY;
      }
      else {
        bin.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.005);
        bin.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.0);
        allInOne.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.005);
        allInOne.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.0);
        coloredElement = SVGConstants.CSS_STROKE_PROPERTY;
      }

      allInOne.setStatement(coloredElement, "black");
      if(clustering.getAllClusters().size() == 1) {
        bin.setStatement(coloredElement, "black");
      }
      else {
        bin.setStatement(coloredElement, context.getColorLibrary().getColor(clusterID));
      }

      try {
        svgp.getCSSClassManager().addClass(bin);
        svgp.getCSSClassManager().addClass(allInOne);
        svgp.updateStyleElement();
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
      }
      clusterID += 1;
    }
  }

  @Override
  public Element visualize(SVGPlot svgp) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_SVG_TAG);
    SVGUtil.setAtt(layer, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "-1.2 -1.2 2.4 2.4");
    setupCSS(svgp);

    Map<Integer, AggregatingHistogram<Double, Double>> hists = new HashMap<Integer, AggregatingHistogram<Double, Double>>();

    // Creating histograms
    int clusterID = 0;
    MinMax<Double> minmax = new MinMax<Double>();
    final double frac = 1. / database.size();

    AggregatingHistogram<Double, Double> allInOne = AggregatingHistogram.DoubleSumHistogram(BINS, 0, 1);
    for(Cluster<Model> cluster : clustering.getAllClusters()) {
      AggregatingHistogram<Double, Double> hist = AggregatingHistogram.DoubleSumHistogram(BINS, 0, 1);
      for(int id : cluster.getIDs()) {
        double pos = getProjected(database.get(id), 0);
        hist.aggregate(pos, frac);
        allInOne.aggregate(pos, frac);
      }
      hists.put(clusterID, hist);
      clusterID += 1;

      // for scaling, get the maximum occurring value in the bins:
      for(Pair<Double, Double> bin : hist) {
        minmax.put(bin.getSecond());
      }
    }

    LinearScale scale = new LinearScale(0, minmax.getMax());

    // Axis. TODO: Use AxisVisualizer for this.
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scale, -1, 1, -1, -1, true, false);
      svgp.updateStyleElement();
    }
    catch(CSSNamingConflict e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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

  private Element drawLine(SVGPlot svgp, int color, AggregatingHistogram<Double, Double> hist, double max) {
    Element path = ShapeLibrary.createPath(svgp.getDocument(), hist.getCoverMinimum(), 1, color);
    SVGUtil.addCSSClass(path, BIN + color);
    double left = 0;
    double right = 0;
    double binwidth = hist.getBinsize();
    for(Pair<Double, Double> bin : hist) {
      double val = bin.getSecond() / max;
      left = bin.getFirst() - binwidth / 2;
      right = bin.getFirst() + binwidth / 2;
      ShapeLibrary.addLine(path, left, 1 - val * 2);
      ShapeLibrary.addLine(path, right, 1 - val * 2);
    }
    ShapeLibrary.addLine(path, right, hist.getCoverMaximum());
    return path;
  }
}
