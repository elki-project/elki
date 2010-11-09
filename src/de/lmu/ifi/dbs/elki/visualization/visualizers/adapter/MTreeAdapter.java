package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree.MTreeNode;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.TreeSphereVisualizer;

/**
 * Adapter that will look for an AbstractMTree to visualize
 * 
 * @author Erich Schubert
 */
public class MTreeAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter<NV> {
  /**
   * Prototype for parameterization
   */
  private TreeSphereVisualizer<NV, ?, ?, ?> mbrVisualizer;

  /**
   * Configuration cache
   */
  private MergedParameterization reconfig;

  /**
   * Constructor.
   * 
   * @param config Parameters
   */
  public MTreeAdapter(Parameterization config) {
    super();
    config = config.descend(this);
    reconfig = new MergedParameterization(config);
    mbrVisualizer = new TreeSphereVisualizer<NV, DoubleDistance, MTreeNode<NV, DoubleDistance>, MTreeEntry<DoubleDistance>>(reconfig, null);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    // FIXME: parameter handling is not very nice here.
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(mbrVisualizer);
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends NV> context, AnyResult result) {
    ArrayList<AbstractMTree<NV, DoubleDistance, MTreeNode<NV, DoubleDistance>, MTreeEntry<DoubleDistance>>> trees = ResultUtil.filterResults(result, AbstractMTree.class);
    for(AbstractMTree<NV, DoubleDistance, MTreeNode<NV, DoubleDistance>, MTreeEntry<DoubleDistance>> tree : trees) {
      reconfig.rewind();
      TreeSphereVisualizer<NV, DoubleDistance, MTreeNode<NV, DoubleDistance>, MTreeEntry<DoubleDistance>> treeVis = new TreeSphereVisualizer<NV, DoubleDistance, MTreeNode<NV, DoubleDistance>, MTreeEntry<DoubleDistance>>(reconfig, tree);
      treeVis.init(context);
      context.addVisualization(tree, treeVis);
    }
  }
}