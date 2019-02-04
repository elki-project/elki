/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
 * @since 0.4.0
 * 
 * @has - - - DragListener
 * @has - - - Element
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
   * A listener to notify on drags (when not subclassing).
   */
  protected DragListener listener = null;

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
    makeInvisible();
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
    makeInvisible();
    this.coordref = coordref;
    enableStart();
  }

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
   * @param listener Drag listener
   */
  public DragableArea(SVGPlot plot, double x, double y, double w, double h, DragListener listener) {
    this.svgp = plot;
    this.element = plot.svgRect(x, y, w, h);
    makeInvisible();
    this.coordref = this.element;
    this.listener = listener;
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
   * @param listener Drag listener
   */
  public DragableArea(SVGPlot plot, Element coordref, double x, double y, double w, double h, DragListener listener) {
    this.svgp = plot;
    this.element = plot.svgRect(x, y, w, h);
    makeInvisible();
    this.coordref = coordref;
    this.listener = listener;
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
    if (evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEDOWN)) {
      SVGPoint dragPoint = getCoordinates(evt);
      if (startDrag(dragPoint, evt)) {
        // LoggingUtil.warning("Starting drag: "+dragPoint);
        startDragPoint = dragPoint;
        enableStop();
      }
    } else if (evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEMOVE)) {
      if (startDragPoint != null) {
        SVGPoint dragPoint = getCoordinates(evt);
        if (!duringDrag(startDragPoint, dragPoint, evt, evt.getTarget() == element)) {
          // cancel the drag operation
          startDragPoint = null;
          disableStop();
        }
      }
    } else if (evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEUP)) {
      if (startDragPoint != null) {
        SVGPoint dragPoint = getCoordinates(evt);
        if (endDrag(startDragPoint, dragPoint, evt, evt.getTarget() == element)) {
          // LoggingUtil.warning("Drag completed: "+dragPoint);
          startDragPoint = null;
          disableStop();
        }
      }
    } else if (evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEOUT)) {
      // When leaving the document with the mouse!
      if (startDragPoint != null && evt.getTarget() == evt.getCurrentTarget()) {
        // LoggingUtil.warning("Mouseout: "+evt.getTarget().toString());
        SVGPoint dragPoint = getCoordinates(evt);
        if (endDrag(startDragPoint, dragPoint, evt, false)) {
          // LoggingUtil.warning("Drag completed: "+dragPoint);
          startDragPoint = null;
          disableStop();
        }
      }
    } else {
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
   * @param evt The event object
   * @return {@code true} to start the drag operation
   */
  protected boolean startDrag(SVGPoint startPoint, Event evt) {
    if (listener != null) {
      return listener.startDrag(startPoint, evt);
    }
    return true;
  }

  /**
   * Method called during drags.
   * 
   * @param startPoint Drag starting point
   * @param dragPoint Drag end point
   * @param evt The event object
   * @param inside Inside the tracked element
   * @return {@code true} to continue the drag
   */
  protected boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside) {
    if (listener != null) {
      return listener.duringDrag(startPoint, dragPoint, evt, inside);
    }
    return true;
  }

  /**
   * Method called when a drag was ended.
   * 
   * @param startPoint Drag starting point
   * @param dragPoint Drag end point
   * @param evt The event object
   * @param inside Success flag
   * @return {@code true} to complete the drag
   */
  protected boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside) {
    if (listener != null) {
      return listener.endDrag(startPoint, dragPoint, evt, inside);
    }
    return true;
  }

  /**
   * Make the rectangle invisible.
   */
  public void makeInvisible() {
    CSSClass cls = new CSSClass(this, "unused");
    cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0");
    cls.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);
    SVGUtil.setAtt(element, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
  }

  /**
   * Make the rectangle visible, for debug purposes.
   */
  public void makeVisible() {
    CSSClass cls = new CSSClass(this, "unused");
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_GREEN_VALUE);
    cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0.2");
    cls.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);
    SVGUtil.setAtt(element, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
  }

  /**
   * Listener interface for drag events.
   * 
   * @author Erich Schubert
   * 
   */
  public interface DragListener {
    /**
     * Action to do on drag start.
     * 
     * @param startPoint Point where the drag was started.
     * @param evt The event object
     * @return {@code true} to start the drag operation
     */
    boolean startDrag(SVGPoint startPoint, Event evt);

    /**
     * Method called during drags.
     * 
     * @param startPoint Drag starting point
     * @param dragPoint Drag end point
     * @param evt The event object
     * @param inside Inside the tracked element
     * @return {@code true} to continue the drag
     */
    boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside);

    /**
     * Method called when a drag was ended.
     * 
     * @param startPoint Drag starting point
     * @param dragPoint Drag end point
     * @param evt The event object
     * @param inside Whether the end point was inside the area
     * @return {@code true} to complete the drag
     */
    boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside);
  }
}
