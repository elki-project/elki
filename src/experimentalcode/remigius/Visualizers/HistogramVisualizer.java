package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;

public class HistogramVisualizer<O extends DoubleVector> extends ScalarVisualizer<O>{

  private static final String NAME = "Histograms";
  private static final int BINS = 20;
  private double frac;
  
  public void setup(Database<O> database, VisualizationManager<O> visManager) {
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
      hist.aggregate(database.get(id).getValue(dim), frac);
    }
    
    // Visualizing a histogram.
    for (Integer id : database){
      double val = hist.get(database.get(id).getValue(dim));
      layer.appendChild(ShapeLibrary.createRow(svgp.getDocument(), getPositioned(database.get(id), dim), 1 - val, frac, val, dim, id));
    }
    
    return layer;
  }
}
