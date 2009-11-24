package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Runnable wrapper to replace all children of a given node.
 * 
 * @author Erich Schubert
 * 
 */
public class NodeReplaceChild extends NodeAppendChild {
  /**
   * Trivial constructor.
   * 
   * @param parent will become the parent of the appended Element.
   * @param child the Element to be appended.
   */
  public NodeReplaceChild(Element parent, Element child) {
    super(parent, child, null, null);
  }

  /**
   * Full constructor.
   * 
   * @param parent Parent node to append the child to
   * @param child Child element
   * @param plot Plot to register the ID (may be {@code null})
   * @param id ID to register (may be {@code null}, requires plot to be given)
   */
  public NodeReplaceChild(Element parent, Element child, SVGPlot plot, String id) {
    super(parent, child, plot, id);
  }

  @Override
  public void run() {
    // remove all existing children.
    while (parent.hasChildNodes()) {
      parent.removeChild(parent.getFirstChild());
    }
    super.run();
  }
}
