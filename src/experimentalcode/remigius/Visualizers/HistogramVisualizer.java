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
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import experimentalcode.remigius.ShapeLibrary;

/**
 * Generates a SVG-Element containing a histogram representing the distribution of the database's objects.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV>
 * @param <N>
 */
public class HistogramVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends ScalarVisualizer<NV, N> {

  public static final OptionID STYLE_ROW_ID = OptionID.getOrCreateOptionID("histogram.row", "Alternative style: Rows.");

  private final Flag STYLE_ROW_PARAM = new Flag(STYLE_ROW_ID);

  private boolean row;

  private static final String NAME = "Histograms";

  private static final int BINS = 20;

  private Clustering<Model> clustering;

  private double frac;

  private Result result;

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

  public void init(Database<NV> database, Result result, Clustering<Model> clustering) {
    init(database, NAME);
    this.frac = 1. / database.size();
    this.result = result;
    this.clustering = clustering;
  }

  private void setupCSS(SVGPlot svgp) {

    // creating IDs manually because cluster often return a null-ID.
    int clusterID = 0;

    CSSClass allInOne = new CSSClass(svgp, ShapeLibrary.BIN);
    for (Cluster<Model> cluster : clustering.getAllClusters()){

      CSSClass bin = new CSSClass(svgp, ShapeLibrary.BIN + clusterID);
      String coloredElement;

      if (row){
        bin.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 1.0);
        coloredElement = SVGConstants.CSS_FILL_PROPERTY;
      } else {
        bin.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.001);
        bin.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.0);
        allInOne.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.001);
        allInOne.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.0);
        coloredElement = SVGConstants.CSS_STROKE_PROPERTY;
      }
      
      allInOne.setStatement(coloredElement, "black");
      if (clustering.getAllClusters().size() == 1){
        bin.setStatement(coloredElement, "black");
      } else {
        bin.setStatement(coloredElement, COLORS.getColor(clusterID));
      }

      try {
        svgp.getCSSClassManager().addClass(bin);
        svgp.updateStyleElement();
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
      }
      clusterID += 1;
    }
  }

  //  private void setupCSS(SVGPlot svgp, int clusterID) {
  //    CSSClass bin = new CSSClass(svgp, ShapeLibrary.BIN + clusterID);
  //    bin.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0.5");
  //    bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, COLORS.getColor(clusterID));
  //
  //    try {
  //      svgp.getCSSClassManager().addClass(bin);
  //      svgp.updateStyleElement();
  //    }
  //    catch(CSSNamingConflict e) {
  //      LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
  //    }
  //  }

  @Override
  public Element visualize(SVGPlot svgp) {
    setupCSS(svgp);
    Element layer = ShapeLibrary.createSVG(svgp.getDocument());

    Map<Integer, AggregatingHistogram<Double, Double>> hists = new HashMap<Integer, AggregatingHistogram<Double, Double>>();

    // Creating histograms
    int clusterID = 0;
    MinMax<Double> minmax = new MinMax<Double>();

    AggregatingHistogram<Double, Double> allInOne = AggregatingHistogram.DoubleSumHistogram(BINS, scales[dim].getMin(), scales[dim].getMax());
    for(Cluster<Model> cluster : clustering.getAllClusters()) {
      AggregatingHistogram<Double, Double> hist = AggregatingHistogram.DoubleSumHistogram(BINS, scales[dim].getMin(), scales[dim].getMax());
      for(int id : cluster.getIDs()) {
        hist.aggregate(database.get(id).getValue(dim).doubleValue(), frac);
        allInOne.aggregate(database.get(id).getValue(dim).doubleValue(), frac);
      }

      for(int bin = 0; bin < hist.getNumBins(); bin++) {
        double valAllInOne = allInOne.get(bin * (scales[dim].getMax() - scales[dim].getMin()) / BINS);
        minmax.put(valAllInOne);
      }
      hists.put(clusterID, hist);
      clusterID += 1;
    }

    LinearScale scale = new LinearScale(minmax.getMin(), minmax.getMax());

    // Axis
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scale, 0, 1, 0, 0, true, false);
      svgp.updateStyleElement();
    }
    catch(CSSNamingConflict e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // Visualizing

    if (row){
      for(int bin = 0; bin < BINS; bin++) {
        double lastVal = 0;
        for(int key = 0; key < hists.size(); key++) {
          AggregatingHistogram<Double, Double> hist = hists.get(key);

          double val = hist.get((bin * (scales[dim].getMax() - scales[dim].getMin()) / BINS))/minmax.getMax();
          layer.appendChild(ShapeLibrary.createRow(svgp.getDocument(), getPositioned(bin * hist.getBinsize(), dim), 1-(val + lastVal), 1./BINS, val, dim, key, bin));
          lastVal += val;
        }
      }
    } else {
      for(int key = 0; key < hists.size(); key++) {
        AggregatingHistogram<Double, Double> hist = hists.get(key);
        Element path = ShapeLibrary.createPath(svgp.getDocument(), 0, 1, key, Integer.toString(key));
        double right = 0;
        for(int bin = 0; bin < hist.getNumBins(); bin++) {
          double val = hist.get((bin * (scales[dim].getMax() - scales[dim].getMin()) / BINS))/minmax.getMax();
          double left = getPositioned(bin * hist.getBinsize(), dim);
          right = getPositioned(bin * hist.getBinsize(), dim) + (1./BINS);
          ShapeLibrary.addLine(path, left, 1 - val);
          ShapeLibrary.addLine(path, right, 1 - val);
        }
        ShapeLibrary.addLine(path, right, 1);
        layer.appendChild(path);
      }
    }
    return layer;
  }
}
