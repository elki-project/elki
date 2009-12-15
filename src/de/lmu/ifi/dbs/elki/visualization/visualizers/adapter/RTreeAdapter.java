package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.AbstractRTreeMBRVisualizer;

/**
 * Adapter that will look for an RTree to visualize
 * 
 * @author Erich Schubert
 */
public class RTreeAdapter implements AlgorithmAdapter {
  /**
   * Prototype for parameterization
   */
  private AbstractRTreeMBRVisualizer<?,?,?> mbrVisualizer = new AbstractRTreeMBRVisualizer<DoubleVector,RStarTreeNode,SpatialEntry>();
  
  /**
   * Constructor, following the {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style.
   */
  public RTreeAdapter() {
    super();
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
