package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Runnable wrapper for appending XML-Elements to existing Elements.
 * 
 * @author Remigius Wojdanowski.
 * 
 */
public class NodeAppendChild implements Runnable {
  /**
   * Parent node to append to.
   */
  protected Element parent;

  /**
   * The child to be appended.
   */
  protected Element child;

  /**
   * The plot (for ID updates). May be {code null}.
   */
  protected SVGPlot plot;

  /**
   * The ID. May be {code null}.
   */
  protected String id;

  /**
   * Trivial constructor.
   * 
   * @param parent will become the parent of the appended Element.
   * @param child the Element to be appended.
   */
  public NodeAppendChild(Element parent, Element child) {
    this(parent, child, null, null);
  }

  /**
   * Full constructor.
   * 
   * @param parent Parent node to append the child to
   * @param child Child element
   * @param plot Plot to register the ID (may be {@code null})
   * @param id ID to register (may be {@code null}, requires plot to be given)
   */
  public NodeAppendChild(Element parent, Element child, SVGPlot plot, String id) {
    super();
    this.parent = parent;
    this.child = child;
    this.plot = plot;
    this.id = id;
  }

  @Override
  public void run() {
    parent.appendChild(child);
    if(plot != null && id != null) {
      plot.putIdElement(id, child);
    }
  }
}
