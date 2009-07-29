package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * This helper class will replace a node in an SVG plot,
 * as soon as the tree is unlocked by the rendering engine.
 * 
 * @author Erich Schubert
 *
 */
public class NodeReplacer implements Runnable {
  private SVGPlot plot;
  private String id;
  private Element newe;
  
  /**
   * Setup a SVG node replacement.
   * 
   * @param newe New element
   * @param plot SVG plot to process
   * @param id Node ID to replace
   */
  public NodeReplacer(Element newe, SVGPlot plot, String id) {
    super();
    this.newe = newe;
    this.plot = plot;
    this.id = id;
  }

  @Override
  public void run() {
    Element olde = plot.getIdElement(id);
    if (olde != null) {
      olde.getParentNode().replaceChild(newe, olde);
      plot.putIdElement(id, newe);
    }
  }
}
