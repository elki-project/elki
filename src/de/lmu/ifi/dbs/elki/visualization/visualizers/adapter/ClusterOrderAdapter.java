package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ClusterOrderVisualizer;

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
   * Constructor, following the {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style.
   */
  public ClusterOrderAdapter() {
    super();
  }

  @Override
  public boolean canVisualize(Result result) {
    Collection<ClusterOrderResult<?>> cos = ResultUtil.filterResults(result, ClusterOrderResult.class);
    return (cos.size() > 0);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    // FIXME: parameter handling is not very nice here.
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(clusterorderVisualizer);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    Collection<ClusterOrderResult<?>> cos = ResultUtil.filterResults(context.getResult(), ClusterOrderResult.class);
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(cos.size());
    ArrayList<String> params = clusterorderVisualizer.getParameters();
    for (ClusterOrderResult<?> co : cos) {
      ClusterOrderVisualizer<?> coVis = new ClusterOrderVisualizer<DoubleVector>();
      // setup parameters.
      try {
        coVis.setParameters(params);
      }
      catch(ParameterException e) {
        LoggingUtil.exception("Error setting parameters for cluster order visualizer.", e);
      }
      coVis.init(context, co);
      usableVisualizers.add(coVis);
    }
    return usableVisualizers;
  }
}
