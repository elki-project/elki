package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;
import experimentalcode.remigius.visualization.ScalarVisualization;

public class HistogramVisualizer<O extends DoubleVector> extends ScalarVisualizer<O>{

  public void setup(Database<O> database, VisualizationManager<O> visManager) {
    init(database, visManager);
    setupCSS();
  }

  private void setupCSS() {

    // TODO: Set CSS
  }

  @Override
  protected ScalarVisualization visualize(SVGPlot svgp, Element layer) {

    AggregatingHistogram<Double, Double> hist = AggregatingHistogram.DoubleSumHistogram(5, 0, 100);
    for (Integer id : database.getIDs()){
      hist.aggregate(database.get(id).getValue(dim), 0.5);
    }

    // TODO: Introduce a proper shape
    for (Integer id : database.getIDs()){
      layer.appendChild(ShapeLibrary.createBubble(svgp.getDocument(), database.get(id).getValue(dim), 1, 0.001*hist.get(database.get(id).getValue(dim)), 0, id, dim, 0, toString()));
    }
    
    return new ScalarVisualization(layer, dim);
  }
}
