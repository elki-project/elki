package experimentalcode.remigius.Adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.Result;
import experimentalcode.remigius.Visualizers.AxisVisualizer;
import experimentalcode.remigius.Visualizers.DotVisualizer;
import experimentalcode.remigius.Visualizers.HistogramVisualizer;
import experimentalcode.remigius.Visualizers.VisualizerContext;

public class DefaultAdapter<NV extends NumberVector<NV, ?>> extends AbstractAlgorithmAdapter<NV> {

  private DotVisualizer<NV> dotVisualizer;

  private AxisVisualizer<NV> axisVisualizer;

  private HistogramVisualizer<NV> histoVisualizer;

  public DefaultAdapter() {
    super();
    dotVisualizer = new DotVisualizer<NV>();
    axisVisualizer = new AxisVisualizer<NV>();
    histoVisualizer = new HistogramVisualizer<NV>();
    providedVisualizers.add(dotVisualizer);
    providedVisualizers.add(axisVisualizer);
    providedVisualizers.add(histoVisualizer);
  }

  @Override
  public boolean canVisualize(@SuppressWarnings("unused") Result r) {
    return true;
  }

  @Override
  protected void initVisualizer(VisualizerContext context) {
    axisVisualizer.init(context);
    dotVisualizer.init(context);
    histoVisualizer.init(context, clustering);
    usableVisualizers.addAll(providedVisualizers);
  }
}