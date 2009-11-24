package experimentalcode.remigius.Adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import experimentalcode.erich.visualization.visualizers.VisualizerContext;
import experimentalcode.erich.visualization.visualizers.vis1d.Projection1DHistogramVisualizer;
import experimentalcode.erich.visualization.visualizers.vis2d.AxisVisualizer;
import experimentalcode.erich.visualization.visualizers.vis2d.ClusteringVisualizer;
import experimentalcode.erich.visualization.visualizers.vis2d.DataDotVisualizer;

public class DefaultAdapter<NV extends NumberVector<NV, ?>> extends AbstractAlgorithmAdapter<NV, Object> {

  private DataDotVisualizer<NV> dataDotVisualizer;

  private ClusteringVisualizer<NV> clusteringVisualizer;

  private AxisVisualizer<NV> axisVisualizer;

  private Projection1DHistogramVisualizer<NV> histoVisualizer;

  public DefaultAdapter() {
    super();
    dataDotVisualizer = new DataDotVisualizer<NV>();
    clusteringVisualizer = new ClusteringVisualizer<NV>();
    axisVisualizer = new AxisVisualizer<NV>();
    histoVisualizer = new Projection1DHistogramVisualizer<NV>();
    providedVisualizers.add(dataDotVisualizer);
    providedVisualizers.add(clusteringVisualizer);
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
    dataDotVisualizer.init(context);
    clusteringVisualizer.init(context);
    histoVisualizer.init(context);
    
    usableVisualizers.add(axisVisualizer);
    if (ResultUtil.getClusteringResults(context.getResult()).size() > 0) {
      usableVisualizers.add(clusteringVisualizer);
    } else {
      usableVisualizers.add(dataDotVisualizer);
    }
    usableVisualizers.add(histoVisualizer);    
  }
}