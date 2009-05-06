package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;


/**
 * Remove a CSS class to the event target.
 * 
 * @author Erich Schubert
 *
 */
public class RemoveCSSClass implements EventListener {
  /**
   * Class to set
   */
  private String cssclass;

  /**
   * Constructor
   * @param cssclass class to set
   */
  public RemoveCSSClass(String cssclass) {
    super();
    this.cssclass = cssclass;
  }

  /**
   * Event handler
   */
  @Override
  public void handleEvent(Event evt) {
    Element e = (Element) evt.getTarget();
    SVGUtil.removeCSSClass(e, cssclass);
  }
}