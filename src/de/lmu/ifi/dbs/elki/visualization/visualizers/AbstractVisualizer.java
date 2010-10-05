package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.datastructures.AnyMap;

/**
 * Abstract superclass for Visualizers (aka: Visualization Factories).
 * 
 * @author Remigius Wojdanowski
 */
public abstract class AbstractVisualizer<O extends DatabaseObject> implements Visualizer {
  /**
   * Visualizer context to use
   */
  protected VisualizerContext<? extends O> context;

  /**
   * Meta data storage
   */
  protected AnyMap<String> metadata;

  // TODO: Move name, level etc. to full class members,
  // now that the API is becoming somewhat stable?

  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   * @param level the visualizer level
   */
  protected AbstractVisualizer(String name, int level) {
    this.metadata = new AnyMap<String>();
    this.metadata.put(Visualizer.META_NAME, name);
    this.metadata.put(Visualizer.META_LEVEL, level);
  }

  /**
   * Constructor with default level.
   * 
   * @param name a short name characterizing this Visualizer
   */
  protected AbstractVisualizer(String name) {
    this(name, Visualizer.LEVEL_STATIC);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   */
  public void init(VisualizerContext<? extends O> context) {
    this.context = context;
  }

  /**
   * Convenience method to update the visualizer level.
   * 
   * @param level new level.
   */
  protected void setLevel(int level) {
    this.metadata.put(Visualizer.META_LEVEL, level);
  }

  @Override
  public AnyMap<String> getMetadata() {
    return metadata;
  }

  /**
   * Override the visualizer name.
   * 
   * @param name Visualizer name
   */
  public void setName(String name) {
    metadata.put(Visualizer.META_NAME, name);
  }

  /**
   * Get the visualizer name.
   * 
   * @return the visualizer name
   */
  public String getName() {
    return metadata.getGenerics(Visualizer.META_NAME, String.class);
  }
}
