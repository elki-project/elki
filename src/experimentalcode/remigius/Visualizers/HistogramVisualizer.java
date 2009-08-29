package experimentalcode.remigius.Visualizers;

import java.util.List;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;

public class HistogramVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends ScalarVisualizer<NV, N>{

  public static final OptionID STYLE_ROW_ID = OptionID.getOrCreateOptionID("histogram.row", "Alternative style: Rows.");
  private final Flag STYLE_ROW_PARAM = new Flag(STYLE_ROW_ID);
  private boolean row;

  private static final String NAME = "Histograms";
  private static final int BINS = 50;
  private double frac;

  public HistogramVisualizer(){
    addOption(STYLE_ROW_PARAM);
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    row = STYLE_ROW_PARAM.getValue();
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  public void setup(Database<NV> database, VisualizationManager<NV> visManager) {
    init(database, visManager, NAME);
    this.frac = 1. / database.size();
    setupCSS();
  }

  private void setupCSS() {

    // TODO: Set CSS.
  }

  @Override
  public Element visualize(SVGPlot svgp) {
    Element layer = ShapeLibrary.createSVG(svgp.getDocument());

    // Creating a histogram.
    AggregatingHistogram<Double, Double> hist = AggregatingHistogram.DoubleSumHistogram(BINS, scales[dim].getMin(), scales[dim].getMax());
    for (Integer id : database){
      hist.aggregate(database.get(id).getValue(dim).doubleValue(), frac);
    }

    if (row) {
      // 1 row per bin
      for (int bin = 0; bin < hist.getNumBins(); bin++){
        // TODO: calculating the value *must* be simpler. Something is wrong here.
        double val = hist.get(bin*(scales[dim].getMax() - scales[dim].getMin())/BINS);
        layer.appendChild(ShapeLibrary.createRow(svgp.getDocument(), getPositioned(bin*hist.getBinsize(), dim), 1 - val, hist.getBinsize(), val, dim, bin));
      }
    } else {
      // 1 Line
      Element path = ShapeLibrary.createPath(svgp.getDocument(), 1, 1, "");
      StringBuffer strb = new StringBuffer();
      for (int bin = 0; bin < hist.getNumBins(); bin++){
        double val = hist.get(bin*(scales[dim].getMax() - scales[dim].getMin())/BINS);
        ShapeLibrary.addLine(path, getPositioned(bin*hist.getBinsize(), dim), 1-val);
      }
      layer.appendChild(path);
    }
    return layer;
  }
}
