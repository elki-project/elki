package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerTree;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ClusterOrderVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.OPTICSPlotVisualizer;

/**
 * Visualize a cluster order by connecting the points with arrows
 * 
 * @author Erich Schubert
 */
public class ClusterOrderAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter<NV> {
  /**
   * Prototype for parameterization
   */
  private ClusterOrderVisualizer<?> clusterorderVisualizer = new ClusterOrderVisualizer<DoubleVector>();

  /**
   * Prototype for parameterization
   */
  private OPTICSPlotVisualizer<?> opticsplotVisualizer = new OPTICSPlotVisualizer<DoubleDistance>();

  /**
   * Constructor.
   */
  public ClusterOrderAdapter() {
    super();
  }

  @Override
  public boolean canVisualize(VisualizerContext<? extends NV> context) {
    Collection<ClusterOrderResult<?>> cos = ResultUtil.filterResults(context.getResult(), ClusterOrderResult.class);
    return (cos.size() > 0);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(2);
    providedVisualizers.add(clusterorderVisualizer);
    providedVisualizers.add(opticsplotVisualizer);
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends NV> context, VisualizerTree<? extends NV> vistree) {
    Collection<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(context.getResult(), ClusterOrderResult.class);
    for(ClusterOrderResult<DoubleDistance> co : cos) {
      ClusterOrderVisualizer<NV> coVis = new ClusterOrderVisualizer<NV>();
      coVis.init(context, co);
      coVis.getMetadata().put(Visualizer.META_VISIBLE_DEFAULT, false);
      vistree.addVisualization(co, coVis);
      if(OPTICSPlot.canPlot(co)) {
        OPTICSPlotVisualizer<DoubleDistance> opVis = new OPTICSPlotVisualizer<DoubleDistance>();
        opVis.init(context, co);
        vistree.addVisualization(co, opVis);
      }
    }
  }
}