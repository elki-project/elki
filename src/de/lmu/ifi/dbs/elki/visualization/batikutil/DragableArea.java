package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * A simple dragable area for Batik.
 * 
 * @author Erich Schubert
 * 
 */
public class DragableArea implements EventListener {
  /**
   * Our element node.
   */
  final protected Element element;

  /**
   * The coordinate system node.
   */
  final protected Element coordref;

  /**
   * The plot we are attached to.
   */
  final protected SVGPlot svgp;

  /**
   * The point where the drag started.
   */
  protected SVGPoint startDragPoint = null;

  /**
   * Constructor for a dragable area. use getElement() to get the DOM node.
   * 
   * Note: always remember to call 'destroy()' to remove listeners!
   * 
   * @param plot Plot we'll be added to
   * @param x X position
   * @param y Y position
   * @param w Width
   * @param h Height
   */
  public DragableArea(SVGPlot plot, double x, double y, double w, double h) {
    this.svgp = plot;
    this.element = plot.svgRect(x, y, w, h);
    makeVisible();
    this.coordref = this.element;
    enableStart();
  }

  /**
   * Constructor for a dragable area. use getElement() to get the DOM node.
   * 
   * Note: always remember to call 'destroy()' to remove listeners!
   * 
   * @param plot Plot we'll be added to
   * @param coordref Element defining the coordinate system
   * @param x X position
   * @param y Y position
   * @param w Width
   * @param h Height
   */
  public DragableArea(SVGPlot plot, Element coordref, double x, double y, double w, double h) {
    this.svgp = plot;
    this.element = plot.svgRect(x, y, w, h);
    makeVisible();
    this.coordref = coordref;
    enableStart();
  }

  /**
   * Remove the listeners
   */
  public void destroy() {
    disableStart();
    disableStop();
  }

  /**
   * The DOM element.
   * 
   * @return the element
   */
  public Element getElement() {
    return element;
  }

  /**
   * Enable capturing of 'mousedown' events.
   */
  public void enableStart() {
    EventTarget targ = (EventTarget) element;
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEDOWN, this, false);
  }

  /**
   * Disable capturing of 'mousedown' events.
   */
  public void disableStart() {
    EventTarget targ = (EventTarget) element;
    targ.removeEventListener(SVGConstants.SVG_EVENT_MOUSEDOWN, this, false);
  }

  /**
   * Enable capturing of 'mousemove' and 'mouseup' events.
   */
  protected void enableStop() {
    EventTarget targ = svgp.getDocument().getRootElement();
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEMOVE, this, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEUP, this, false);
    // FIXME: listen on the background object!
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEOUT, this, false);
  }

  /**
   * Disable capturing of 'mousemove' and 'mouseup' events.
   */
  protected void disableStop() {
    EventTarget targ = svgp.getDocument().getRootElement();
    targ.removeEventListener(SVGConstants.SVG_EVENT_MOUSEMOVE, this, false);
    targ.removeEventListener(SVGConstants.SVG_EVENT_MOUSEUP, this, false);
    // FIXME: listen on the background object!
    targ.removeEventListener(SVGConstants.SVG_EVENT_MOUSEOUT, this, false);
  }

  @Override
  public void handleEvent(Event evt) {
    if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEDOWN)) {
      SVGPoint dragPoint = getCoordinates(evt);
      if(startDrag(dragPoint)) {
        // LoggingUtil.warning("Starting drag: "+dragPoint);
        startDragPoint = dragPoint;
        enableStop();
      }
    }
    else if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEMOVE)) {
      if(startDragPoint != null) {
        SVGPoint dragPoint = getCoordinates(evt);
        if(!duringDrag(startDragPoint, dragPoint, evt.getTarget() == element)) {
          // cancel the drag operation
          startDragPoint = null;
          disableStop();
        }
      }
    }
    else if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEUP)) {
      if(startDragPoint != null) {
        SVGPoint dragPoint = getCoordinates(evt);
        if(endDrag(startDragPoint, dragPoint, evt.getTarget() == element)) {
          // LoggingUtil.warning("Drag completed: "+dragPoint);
          startDragPoint = null;
          disableStop();
        }
      }
    }
    else if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEOUT)) {
      // When leaving the document with the mouse!
      LoggingUtil.warning("Mouseout: "+evt.getTarget().toString());
      if(startDragPoint != null && evt.getTarget() == evt.getCurrentTarget()) {
        //LoggingUtil.warning("Mouseout: "+evt.getTarget().toString());
        SVGPoint dragPoint = getCoordinates(evt);
        if(endDrag(startDragPoint, dragPoint, false)) {
          // LoggingUtil.warning("Drag completed: "+dragPoint);
          startDragPoint = null;
          disableStop();
        }
      }
    }
    else {
      LoggingUtil.warning("Unrecognized event: " + evt);
    }
  }

  /**
   * Return the event coordinates for this event.
   * 
   * @param evt Event
   * @return Coordinates
   */
  protected SVGPoint getCoordinates(Event evt) {
    return SVGUtil.elementCoordinatesFromEvent(this.svgp.getDocument(), this.coordref, evt);
  }

  /**
   * Action to do on drag start.
   * 
   * @param startPoint Point where the drag was started.
   * @return {@code true} to start the drag operation
   */
  protected boolean startDrag(SVGPoint startPoint) {
    // By default, we do nothing.
    return true;
  }

  /**
   * Method called during drags.
   * 
   * @param startPoint Drag starting point
   * @param dragPoint Drag end point
   * @param inside Inside the tracked element
   * @return {@code true} to continue the drag
   */
  protected boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, boolean inside) {
    // By default, we do nothing
    return true;
  }

  /**
   * Method called when a drag was ended.
   * 
   * @param startPoint Drag starting point
   * @param dragPoint Drag end point
   * @param success Success flag
   * @return {@code true} to complete the drag
   */
  protected boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, boolean success) {
    // By default, do nothing, but complete drag
    return true;
  }

  /**
   * Make the rectangle visible, for debug purposes.
   */
  public void makeVisible() {
    CSSClass cls = new CSSClass(this, "unused");
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_GREEN_VALUE);
    cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0.2");
    SVGUtil.setAtt(element, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
  }
}