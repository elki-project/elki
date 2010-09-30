package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerTree;

/**
 * Defines the requirements for an algorithm-adapter. <br />
 * Note: Any implementation is supposed to provide a constructor without
 * parameters (default constructor).
 * 
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 */
public interface AlgorithmAdapter<O extends DatabaseObject> extends Parameterizable {
  /**
   * Returns <code>true</code> if the adapter can provide one or more
   * {@link Visualizer}s for the given Result, else false.
   * 
   * @param context Context to store shared visualization properties.
   * @return <code>true</code> if the adapter can provide one or more
   *         {@link Visualizer}s for the given Result, else false.
   */
  public boolean canVisualize(VisualizerContext<? extends O> context);

  /**
   * Returns a collection of {@link Visualizer}s this adapter generally
   * provides.
   * 
   * @return a collection of {@link Visualizer}s this adapter generally
   *         provides.
   */
  public Collection<Visualizer> getProvidedVisualizers();

  // FIXME: document
  public void addVisualizers(VisualizerContext<? extends O> context, VisualizerTree<? extends O> vistree);
}