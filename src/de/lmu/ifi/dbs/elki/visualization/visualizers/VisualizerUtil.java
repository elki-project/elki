package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Visualizer utilities.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses VisFactory - - inspects
 */
public final class VisualizerUtil {
  /**
   * Utility function to test for Visualizer visibility.
   * 
   * TODO: make this context dependent?
   * 
   * @param vis Visualizer to test
   * @return true when visible
   */
  public static boolean isVisible(VisualizationTask task) {
    // Currently enabled?
    Boolean enabled = task.getGenerics(VisualizationTask.META_VISIBLE, Boolean.class);
    if(enabled == null) {
      enabled = task.getGenerics(VisualizationTask.META_VISIBLE_DEFAULT, Boolean.class);
    }
    if(enabled == null) {
      enabled = true;
    }
    return enabled;
  }

  /**
   * Utility function to test for a visualizer being a "tool".
   * 
   * @param vis Visualizer to test
   * @return true for a tool
   */
  public static boolean isTool(VisualizationTask vis) {
    // Currently enabled?
    Boolean tool = vis.getGenerics(VisualizationTask.META_TOOL, Boolean.class);
    return (tool != null) && tool;
  }

  /**
   * Test if the database contains number vectors.
   * 
   * @param database Database
   * @return true when the factory is for number vectors.
   */
  // FIXME: move to DatabaseUtil
  public static boolean isNumberVectorDatabase(Database<?> database) {
    if (NumberVector.class.isInstance(database.getObjectFactory())) {
      return true;
    }
    return false;
  }

  public static boolean thumbnailEnabled(VisualizationTask vi) {
    // FIXME: TODO
    return true;
  }

  public static boolean detailsEnabled(VisualizationTask vi) {
    // FIXME: TODO
    return true;
  }
}