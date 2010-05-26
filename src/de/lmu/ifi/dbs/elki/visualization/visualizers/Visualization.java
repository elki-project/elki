package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.Collection;

import org.w3c.dom.Element;

/**
 * Base class for a materialized Visualization.
 * 
 * @author Erich Schubert
 */
public interface Visualization {
  /**
   * Get the SVG layers of the given visualization.
   * 
   * @return layers
   */
  public Collection<VisualizationLayer> getLayers();

  /**
   * Modify visibility
   * 
   * @param vis
   */
  public void setVisible(boolean vis);

  /**
   * Destroy the visualization. Called after the elements have been removed from the document.
   * 
   * Implementations should remove their listeners etc.
   */
  public void destroy();

  /**
   * Representation of a single visualization layer.
   * 
   * @author Erich Schubert
   *
   */
  public class VisualizationLayer implements Comparable<VisualizationLayer> {
    /**
     * The visualization level
     */
    public Integer level;

    /**
     * The SVG element of the layer
     */
    public Element layer;
    
    /**
     * Constructor.
     * 
     * @param level Level
     * @param layer Layer
     */
    public VisualizationLayer(Integer level, Element layer) {
      this.level = level;
      this.layer = layer;
    }

    @Override
    public int compareTo(VisualizationLayer o) {
      if(this.level != null && o.level != null && this.level != o.level) {
        return this.level - o.level;
      }
      return 0;
    }
  }
}