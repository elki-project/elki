package de.lmu.ifi.dbs.elki.visualization.visualizers;

/**
 * Interface for both visualizers and visualizer groups.
 * 
 * @author Erich Schubert
 */
public interface VisualizerTreeItem {
  /**
   * Get the visualizer or visualizer group name.
   * 
   * @return Name
   */
  public String getName();
}
