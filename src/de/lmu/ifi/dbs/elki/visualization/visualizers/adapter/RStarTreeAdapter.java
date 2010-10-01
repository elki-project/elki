package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerTree;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.TreeMBRVisualizer;

/**
 * Adapter that will look for an AbstractRStarTree to visualize
 * 
 * @author Erich Schubert
 */
public class RStarTreeAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter<NV> {
  /**
   * Prototype for parameterization
   */
  private TreeMBRVisualizer<NV, ?, ?> mbrVisualizer;

  /**
   * Constructor.
   * 
   * @param config Parameters
   */
  public RStarTreeAdapter(Parameterization config) {
    super();
    config = config.descend(this);
    mbrVisualizer = new TreeMBRVisualizer<NV, RStarTreeNode, SpatialEntry>(config);
  }

  @Override
  public boolean canVisualize(VisualizerContext<? extends NV> context) {
    return mbrVisualizer.canVisualize(context);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    // FIXME: parameter handling is not very nice here.
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(mbrVisualizer);
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends NV> context, VisualizerTree<? extends NV> vistree) {
    mbrVisualizer.init(context);
    vistree.addVisualization(context.getDatabase(), mbrVisualizer);
  }
}