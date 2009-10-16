package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.utilities.AnyMap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

/**
 * Abstract superclass for Visualizers.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <O> the type of object this Visualizer will process.
 */
public abstract class AbstractVisualizer extends AbstractParameterizable implements Visualizer {
  /**
   * Visualizer context to use
   */
  protected VisualizerContext context;

  /**
   * Meta data storage
   */
  protected AnyMap<String> metadata;

  /**
   * Initializes this Visualizer.
   * 
   * @param db contains all objects to be processed.
   * @param level indicates when to execute this Visualizer.
   * @param name a short name characterizing this Visualizer
   */
  protected void init(int level, String name, VisualizerContext context) {
    this.metadata = new AnyMap<String>();
    this.metadata.put(Visualizer.META_LEVEL, level);
    this.metadata.put(Visualizer.META_NAME, name);
    this.context = context;
  }

  @Override
  public AnyMap<String> getMetadata() {
    return metadata;
  }
}
