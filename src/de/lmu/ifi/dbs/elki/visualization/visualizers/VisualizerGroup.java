package de.lmu.ifi.dbs.elki.visualization.visualizers;

/**
 * Representing a group of Visualizers.
 * 
 * @author Erich Schubert
 */
public class VisualizerGroup extends java.util.Vector<VisualizerTreeItem> implements VisualizerTreeItem {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Visualizer group name
   */
  private final String name;
  
  /**
   * Constructor
   * 
   * @param name Group name
   */
  public VisualizerGroup(String name) {
    super();
    this.name = name;
  }

  /**
   * Get the visuaizer group name
   * 
   * @return group name
   */
  @Override
  public String getName() {
    return name;
  }
}
