package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.ReferencePointsResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ReferencePointsVisualizer;

/**
 * Adapter to generate a reference points visualizer when reference points were found in the data.
 * 
 * @author Erich Schubert
 *
 * @param <NV> Object type
 */
public class ReferencePointsAdapter<NV extends NumberVector<NV,?>> implements AlgorithmAdapter {
  /**
   * Prototype for parameterization
   */
  private ReferencePointsVisualizer<NV> referencePointsVisualizer = new ReferencePointsVisualizer<NV>();
  
  /**
   * Constructor.
   */
  public ReferencePointsAdapter() {
    super();
  }

  @Override
  public boolean canVisualize(VisualizerContext context) {
    Collection<ReferencePointsResult<NV>> cos = ResultUtil.filterResults(context.getResult(), ReferencePointsResult.class);
    return (cos.size() > 0);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(referencePointsVisualizer);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    Collection<ReferencePointsResult<NV>> cos = ResultUtil.filterResults(context.getResult(), ReferencePointsResult.class);
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(cos.size());
    for (ReferencePointsResult<NV> co : cos) {
      ReferencePointsVisualizer<NV> rpVis = new ReferencePointsVisualizer<NV>();
      rpVis.init(context, co);
      usableVisualizers.add(rpVis);
    }
    return usableVisualizers;
  }
}
