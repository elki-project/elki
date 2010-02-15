package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ClusterOrderVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.OPTICSPlotVisualizer;

/**
 * Visualize a cluster order by connecting the points with arrows
 * 
 * @author Erich Schubert
 */
public class ClusterOrderAdapter implements AlgorithmAdapter {
  /**
   * Prototype for parameterization
   */
  private ClusterOrderVisualizer<?> clusterorderVisualizer = new ClusterOrderVisualizer<DoubleVector>();
  
  /**
   * Prototype for parameterization
   */
  private OPTICSPlotVisualizer<?> opticsplotVisualizer = new OPTICSPlotVisualizer<DoubleDistance>();
  
  /**
   * Constructor, following the {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style.
   */
  public ClusterOrderAdapter() {
    super();
  }

  @Override
  public boolean canVisualize(VisualizerContext context) {
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
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    Collection<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(context.getResult(), ClusterOrderResult.class);
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(cos.size() * 2);
    for (ClusterOrderResult<DoubleDistance> co : cos) {
      ClusterOrderVisualizer<?> coVis = new ClusterOrderVisualizer<DoubleVector>();
      coVis.init(context, co);
      usableVisualizers.add(coVis);
      OPTICSPlotVisualizer<DoubleDistance> opVis = new OPTICSPlotVisualizer<DoubleDistance>();
      opVis.init(context, co);
      usableVisualizers.add(opVis);
    }
    return usableVisualizers;
  }
}