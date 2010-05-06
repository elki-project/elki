package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree.MTreeNode;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.TreeSphereVisualizer;

/**
 * Adapter that will look for an AbstractMTree to visualize
 * 
 * @author Erich Schubert
 */
public class MTreeAdapter implements AlgorithmAdapter {
  /**
   * Prototype for parameterization
   */
  private TreeSphereVisualizer<?,?,?,?> mbrVisualizer;
  
  /**
   * Constructor.
   * 
   * @param config Parameters
   */
  public MTreeAdapter(Parameterization config) {
    super();
    mbrVisualizer = new TreeSphereVisualizer<DoubleVector,DoubleDistance,MTreeNode<DoubleVector,DoubleDistance>,MTreeEntry<DoubleDistance>>(config);
  }

  @Override
  public boolean canVisualize(VisualizerContext context) {
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
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(1);
    mbrVisualizer.init(context);
    usableVisualizers.add(mbrVisualizer);
    return usableVisualizers;
  }
}