package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

/**
 * Base class for a materialized Visualization.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has Element oneway
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
}