package experimentalcode.remigius.Adapter;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import experimentalcode.remigius.VisualizationManager;
import experimentalcode.remigius.Visualizers.Visualizer;

/**
 * Defines the requirements for an algorithm-adapter.
 * Note: Any implementation is supposed to provide a constructor 
 * without parameters (default constructor).
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> the type of {@link DatabaseObject}s the adapter will handle.
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
   * Returns a collection of {@link Visualizer}s this adapter provides.
   * 
   * @return a collection of {@link Visualizer}s this adapter provides.
   */
  public Collection<Visualizer> getProvidedVisualizers();
  
  public Collection<Visualizer> getUsableVisualizers();

  /**
   * Initializes all provided {@link Visualizer}s.
   * 
   * @param database the {@link Database} which was processed to obtain the
   *        result.
   * @param result the {@link Result} to be visualized.
   */
  public void init(Database<O> database, Result result);
}