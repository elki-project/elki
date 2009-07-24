package experimentalcode.remigius.Visualizers;

import java.util.Iterator;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.lisa.HistogramResult;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;
import experimentalcode.remigius.visualization.ScalarVisualization;

public class HistogramVisualizer<O extends DatabaseObject> extends ScalarVisualizer<O>{

  private HistogramResult<O> histResult;

  public void setup(Database<O> database, HistogramResult<O> histResult, VisualizationManager<O> visManager) {
    init(database, visManager);
    this.histResult = histResult;
    setupCSS();
  }

  private void setupCSS() {

  }
  
  @Override
  protected ScalarVisualization visualize(SVGPlot svgp, Element layer) {
    
    Iterator<Pair<Double, O>> iter = histResult.getHistogram().iterator();
    while (iter.hasNext()){
      Pair<Double, O> p = iter.next();
      layer.appendChild(ShapeLibrary.createBubble(svgp.getDocument(), dim, 0, p.getFirst(), 0, (int)Math.random(), dim, 0, toString()));
    }
    return new ScalarVisualization(layer, dim);
  }
}
