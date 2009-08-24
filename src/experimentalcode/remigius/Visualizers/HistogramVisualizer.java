package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;

public class HistogramVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends ScalarVisualizer<NV, N>{

  private static final String NAME = "Histograms";
  private static final int BINS = 20;
  private double frac;
  
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
    
    // Visualizing a histogram.
    for (Integer id : database){
      double val = hist.get(database.get(id).getValue(dim).doubleValue());
      layer.appendChild(ShapeLibrary.createRow(svgp.getDocument(), getPositioned(database.get(id), dim), 1 - val, frac, val, dim, id));
    }
    
    return layer;
  }
}
