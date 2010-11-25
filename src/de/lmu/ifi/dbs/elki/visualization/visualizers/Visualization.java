package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.Comparator;

import org.w3c.dom.Element;

/**
 * Base class for a materialized Visualization.
 * 
 * @author Erich Schubert
 */
public interface Visualization {
  /**
   * Get the SVG layer of the given visualization.
   * 
   * @return layer
   */
  public Element getLayer();

  /**
   * Destroy the visualization. Called after the elements have been removed from the document.
   * 
   * Implementations should remove their listeners etc.
   */
  public void destroy();
  
  /**
   * Get the visualization level
   * 
   * @return level
   */
  public Integer getLevel();

  /**
   * Representation of a single visualization layer.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public class VisualizationComparator implements Comparator<Visualization> {
    @Override
    public int compare(Visualization o1, Visualization o2) {
      if(o1.getLevel() != null && o2.getLevel() != null && o1.getLevel() != o2.getLevel()) {
        return o1.getLevel() - o2.getLevel();
      }
      return 0;
    }
  }
}