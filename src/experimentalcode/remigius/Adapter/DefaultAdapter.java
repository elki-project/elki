package experimentalcode.remigius.Adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.Result;
import experimentalcode.remigius.Visualizers.Projection2DAxisVisualizer;
import experimentalcode.remigius.Visualizers.Projection2DClusteringVisualizer;
import experimentalcode.remigius.Visualizers.Projection2DDataDotVisualizer;
import experimentalcode.remigius.Visualizers.Projection1DHistogramVisualizer;
import experimentalcode.remigius.Visualizers.VisualizerContext;

public class DefaultAdapter<NV extends NumberVector<NV, ?>> extends AbstractAlgorithmAdapter<NV> {

  private Projection2DDataDotVisualizer<NV> projection2DDataDotVisualizer;

  private Projection2DClusteringVisualizer<NV> projection2DClusteringVisualizer;

  private Projection2DAxisVisualizer<NV> projection2DAxisVisualizer;

  private Projection1DHistogramVisualizer<NV> histoVisualizer;

  public DefaultAdapter() {
    super();
    projection2DDataDotVisualizer = new Projection2DDataDotVisualizer<NV>();
    projection2DClusteringVisualizer = new Projection2DClusteringVisualizer<NV>();
    projection2DAxisVisualizer = new Projection2DAxisVisualizer<NV>();
    histoVisualizer = new Projection1DHistogramVisualizer<NV>();
    providedVisualizers.add(projection2DDataDotVisualizer);
    providedVisualizers.add(projection2DClusteringVisualizer);
    providedVisualizers.add(projection2DAxisVisualizer);
    providedVisualizers.add(histoVisualizer);
  }

  @Override
  public boolean canVisualize(@SuppressWarnings("unused") Result r) {
    return true;
  }

  @Override
  protected void initVisualizer(VisualizerContext context) {
    projection2DAxisVisualizer.init(context);
    projection2DDataDotVisualizer.init(context);
    projection2DClusteringVisualizer.init(context);
    histoVisualizer.init(context);
    usableVisualizers.addAll(providedVisualizers);
  }
}