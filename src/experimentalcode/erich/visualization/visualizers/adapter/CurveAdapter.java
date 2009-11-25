package experimentalcode.erich.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.adapter.AlgorithmAdapter;
import experimentalcode.erich.visualization.visualizers.CurveVisualizer;

public class CurveAdapter implements AlgorithmAdapter {
  private CurveVisualizer curveVisualizer;
  
  public CurveAdapter() {
    super();
    curveVisualizer = new CurveVisualizer();
  }

  @Override
  public boolean canVisualize(Result result) {
    IterableResult<Pair<Double, Double>> curve = CurveVisualizer.findCurveResult(result);
    return (curve != null);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(curveVisualizer);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(1);
    IterableResult<Pair<Double, Double>> curve = CurveVisualizer.findCurveResult(context.getResult());
    curveVisualizer.init(context);
    if(curve != null) {
      usableVisualizers.add(curveVisualizer);
    }
    return usableVisualizers;
  }
}
