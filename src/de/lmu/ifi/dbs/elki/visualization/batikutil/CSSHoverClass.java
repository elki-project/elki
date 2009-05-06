package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;


/**
 * Do a hover effect using a CSS class.
 * 
 * @author Erich Schubert
 *
 */
public class CSSHoverClass implements EventListener {
  /**
   * Class to set when over
   */
  private String overclass;

  /**
   * Class to set when out
   */
  private String outclass;
  
  /**
   * Consider a click as 'out'?
   */
  private boolean clickisout;

  /**
   * Constructor
   * @param overclass class to set when over
   * @param outclass class to set when out
   * @param clickisout consider a click to be an 'out' event
   */
  public CSSHoverClass(String overclass, String outclass, boolean clickisout) {
    super();
    this.overclass = overclass;
    this.outclass = outclass;
    this.clickisout = clickisout;
  }

  /**
   * Constructor without 'clickisout' option.
   * 
   * @param overclass class to set when over
   * @param outclass class to set when out
   */
  public CSSHoverClass(String overclass, String outclass) {
    this(overclass, outclass, false);
  }

  /**
   * Event handler
   */
  @Override
  public void handleEvent(Event evt) {
    Element e = (Element) evt.getTarget();
    if (evt.getType() == SVGConstants.SVG_EVENT_MOUSEOVER) {
      if (overclass != null) {
        SVGUtil.addCSSClass(e, overclass);
      }
      if (outclass != null) {
        SVGUtil.removeCSSClass(e, outclass);
      }
    }
    if (evt.getType() == SVGConstants.SVG_EVENT_MOUSEOUT) {
      if (overclass != null) {
        SVGUtil.removeCSSClass(e, overclass);
      }
      if (outclass != null) {
        SVGUtil.addCSSClass(e, outclass);
      }
    }
    if (clickisout && evt.getType() == SVGConstants.SVG_EVENT_CLICK) {
      if (overclass != null) {
        SVGUtil.removeCSSClass(e, overclass);
      }
      if (outclass != null) {
        SVGUtil.addCSSClass(e, outclass);
      }
    }
  }
}