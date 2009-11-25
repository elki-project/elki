package experimentalcode.erich.visualization.visualizers.adapter;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.Result;
import experimentalcode.erich.visualization.visualizers.Visualizer;
import experimentalcode.erich.visualization.visualizers.VisualizerContext;

/**
 * Defines the requirements for an algorithm-adapter. <br />
 * Note: Any implementation is supposed to provide a constructor without
 * parameters (default constructor).
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <O> the type of {@link DatabaseObject}s the adapter will handle.
 */
public interface AlgorithmAdapter<O extends DatabaseObject> {
  /**
   * Returns <code>true</code> if the adapter can provide one or more
   * {@link Visualizer}s for the given Result, else false.
   * 
   * @param result the {@link Result} the adapter checks.
   * @return <code>true</code> if the adapter can provide one or more
   *         {@link Visualizer}s for the given Result, else false.
   */
  public boolean canVisualize(Result result);

  /**
   * Returns a collection of {@link Visualizer}s this adapter generally
   * provides.
   * 
   * @return a collection of {@link Visualizer}s this adapter generally
   *         provides.
   */
  public Collection<Visualizer> getProvidedVisualizers();

  /**
   * Returns a collection of {@link Visualizer}s this adapter provides,
   * depending on the given database and result.
   * 
   * @param context Context to store shared visualization properties.
   * @return a collection of {@link Visualizer}s this adapter provides,
   *         depending on the given database and result.
   */
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context);
}