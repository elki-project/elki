package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
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
   * Configuration cache
   */
  private MergedParameterization reconfig;

  /**
   * Constructor.
   * 
   * @param config Parameters
   */
  public RStarTreeAdapter(Parameterization config) {
    super();
    config = config.descend(this);
    reconfig = new MergedParameterization(config);
    mbrVisualizer = new TreeMBRVisualizer<NV, RStarTreeNode, SpatialEntry>(reconfig, null);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(mbrVisualizer);
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends NV> context, AnyResult result) {
    ArrayList<AbstractRStarTree<NV, RStarTreeNode, SpatialEntry>> trees = ResultUtil.filterResults(result, AbstractRStarTree.class);
    for(AbstractRStarTree<NV, RStarTreeNode, SpatialEntry> tree : trees) {
      reconfig.rewind();
      TreeMBRVisualizer<NV, RStarTreeNode, SpatialEntry> treeVis = new TreeMBRVisualizer<NV, RStarTreeNode, SpatialEntry>(reconfig, tree);
      treeVis.init(context);
      context.addVisualization(tree, treeVis);
    }
  }
}