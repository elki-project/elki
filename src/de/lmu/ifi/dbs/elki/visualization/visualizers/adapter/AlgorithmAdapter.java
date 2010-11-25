package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Defines the requirements for an algorithm-adapter. <br />
 * Note: Any implementation is supposed to provide a constructor without
 * parameters (default constructor).
 * 
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.result.AnyResult
 * @apiviz.has de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer oneway - - produces
 */
public interface AlgorithmAdapter<O extends DatabaseObject> extends Parameterizable {
  /**
   * Returns a collection of {@link Visualizer}s this adapter generally
   * provides, for parameterization.
   * 
   * @return a collection of {@link Visualizer}s this adapter generally
   *         provides.
   */
  public Collection<Visualizer> getProvidedVisualizers();

  /**
   * Add visualizers for the given result (tree) to the context.
   * 
   * @param context Context to work with
   * @param result Result to process
   */
  public void addVisualizers(VisualizerContext<? extends O> context, AnyResult result);
}