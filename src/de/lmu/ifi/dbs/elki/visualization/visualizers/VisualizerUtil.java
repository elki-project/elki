package de.lmu.ifi.dbs.elki.visualization.visualizers;

/**
 * Visualizer utilities.
 * 
 * @author Erich Schubert
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
  public static boolean isVisible(Visualizer vis) {
    // Currently enabled?
    Boolean enabled = vis.getMetadata().getGenerics(Visualizer.META_VISIBLE, Boolean.class);
    if(enabled == null) {
      enabled = vis.getMetadata().getGenerics(Visualizer.META_VISIBLE_DEFAULT, Boolean.class);
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
  public static boolean isTool(Visualizer vis) {
    // Currently enabled?
    Boolean tool = vis.getMetadata().getGenerics(Visualizer.META_TOOL, Boolean.class);
    return (tool != null) && tool;
  }
}