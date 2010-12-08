package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Abstract superclass for Visualizers (aka: Visualization Factories).
 * 
 * @author Remigius Wojdanowski
 */
public abstract class AbstractVisFactory<O extends DatabaseObject> implements VisFactory<O> {
  /**
   * Constructor.
   */
  protected AbstractVisFactory() {
    super();
  }

  @Override
  public Visualization makeVisualizationOrThumbnail(VisualizationTask task) {
    // Is this a thumbnail request?
    Boolean isthumb = task.get(VisualizationTask.THUMBNAIL, Boolean.class);
    if (isthumb != null && isthumb.booleanValue() && allowThumbnails(task)) {
      final Integer level = task.getGenerics(VisualizationTask.META_LEVEL, Integer.class);
      return new ThumbnailVisualization<DatabaseObject>(this, task, level, 0);
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
}