package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.datastructures.AnyMap;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Abstract superclass for Visualizers (aka: Visualization Factories).
 * 
 * @author Remigius Wojdanowski
 */
public abstract class AbstractVisFactory<O extends DatabaseObject> implements VisFactory<O> {
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
  protected AbstractVisFactory(String name, int level) {
    this.metadata = new AnyMap<String>();
    this.metadata.put(VisFactory.META_NAME, name);
    this.metadata.put(VisFactory.META_LEVEL, level);
  }

  /**
   * Constructor with default level.
   * 
   * @param name a short name characterizing this Visualizer
   */
  protected AbstractVisFactory(String name) {
    this(name, VisFactory.LEVEL_STATIC);
  }

  /**
   * Convenience method to get the visualizer level
   * 
   * @return current level.
   */
  protected int getLevel() {
    return this.metadata.get(VisFactory.META_LEVEL, Integer.class);
  }

  /**
   * Convenience method to update the visualizer level.
   * 
   * @param level new level.
   */
  protected void setLevel(int level) {
    this.metadata.put(VisFactory.META_LEVEL, level);
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
    metadata.put(VisFactory.META_NAME, name);
  }

  /**
   * Get the visualizer name.
   * 
   * @return the visualizer name
   */
  public String getName() {
    return metadata.getGenerics(VisFactory.META_NAME, String.class);
  }

  @Override
  public Visualization makeVisualizationOrThumbnail(VisualizationTask task) {
    // Is this a thumbnail request?
    Boolean isthumb = task.get(VisualizationTask.THUMBNAIL, Boolean.class);
    if (isthumb != null && isthumb.booleanValue() && allowThumbnails(task)) {
      return new ThumbnailVisualization<DatabaseObject>(this, task, getLevel(), 0);
    }
    return makeVisualization(task);
  }
  
  @Override
  abstract public Visualization makeVisualization(VisualizationTask task);

  /**
   * Test whether to do a thumbnail or a full rendering.
   * 
   * Override this with "false" to disable thumbnails!
   * 
   * @param task Task requested 
   */
  public boolean allowThumbnails(VisualizationTask task) {
    return true;
  }

  @Override
  public String getLongName() {
    return getName();
  }

  @Override
  public String getShortName() {
    return getName();
  }
}