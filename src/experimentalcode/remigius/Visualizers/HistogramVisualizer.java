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
  
  public void setup(Database<O> database, VisualizationManager<O> visManager) {
    init(database, visManager, NAME);
    setupCSS();
  }

  private void setupCSS() {

    // TODO: Set CSS.
  }

  @Override
  public Element visualize(SVGPlot svgp) {
    Element layer = ShapeLibrary.createSVG(svgp.getDocument());
    // TODO: Fix Bins & aggregated values.
    int bins = 5;
    AggregatingHistogram<Double, Double> hist = AggregatingHistogram.DoubleSumHistogram(bins, scales[dim].getMin(), scales[dim].getMax());
    for (Integer id : database.getIDs()){
      hist.aggregate(database.get(id).getValue(dim), 0.5);
    }

    // TODO: Introduce a proper shape & scaling.
    for (Integer id : database.getIDs()){
      layer.appendChild(ShapeLibrary.createRow(svgp.getDocument(), database.get(id).getValue(dim), 1, 0.009/bins, hist.get(database.get(id).getValue(dim)), dim, id));
    }
    
    return layer;
  }
}
