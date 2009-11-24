package experimentalcode.erich.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import experimentalcode.erich.visualization.visualizers.Visualizer;
import experimentalcode.erich.visualization.visualizers.VisualizerContext;
import experimentalcode.erich.visualization.visualizers.vis1d.Projection1DHistogramVisualizer;
import experimentalcode.erich.visualization.visualizers.vis2d.AxisVisualizer;
import experimentalcode.erich.visualization.visualizers.vis2d.ClusteringVisualizer;
import experimentalcode.erich.visualization.visualizers.vis2d.DataDotVisualizer;

public class DefaultAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter<NV> {

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
  }

  @Override
  public boolean canVisualize(@SuppressWarnings("unused") Result r) {
    return true;
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(4);
    providedVisualizers.add(dataDotVisualizer);
    providedVisualizers.add(clusteringVisualizer);
    providedVisualizers.add(axisVisualizer);
    providedVisualizers.add(histoVisualizer);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(3);
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
    return usableVisualizers;
  }
}