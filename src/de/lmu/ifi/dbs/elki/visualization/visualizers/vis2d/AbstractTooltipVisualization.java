package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * General base class for a tooltip visualizer.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Number Vector
 */
// TODO: can we improve performance by not adding as many hovers?
public abstract class AbstractTooltipVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DataStoreListener {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_HIDDEN = "tooltip_hidden";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_VISIBLE = "tooltip_visible";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_STICKY = "tooltip_sticky";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_AREA = "tooltip_area";

  public AbstractTooltipVisualization(VisualizationTask task) {
    super(task);
    context.addDataStoreListener(this);
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDataStoreListener(this);
  }

  @Override
  public void redraw() {
    setupCSS(svgp);

    double dotsize = 2 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT);

    EventListener hoverer = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        handleHoverEvent(evt);
      }
    };

    for(DBID id : rel.iterDBIDs()) {
      double[] v = proj.fastProjectDataToRenderSpace(rel.get(id));
      Element tooltip = makeTooltip(id, v[0], v[1], dotsize);
      SVGUtil.addCSSClass(tooltip, TOOLTIP_HIDDEN);

      // sensitive area.
      Element area = svgp.svgCircle(v[0], v[1], dotsize);
      SVGUtil.addCSSClass(area, TOOLTIP_AREA);

      EventTarget targ = (EventTarget) area;
      targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
      targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
      targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);

      // NOTE: do not change the sequence in which these are inserted!
      layer.appendChild(area);
      layer.appendChild(tooltip);
    }
  }

  abstract protected Element makeTooltip(DBID id, double x, double y, double dotsize);

  /**
   * Handle the hover events.
   * 
   * @param evt Event.
   */
  protected void handleHoverEvent(Event evt) {
    if(evt.getTarget() instanceof Element) {
      Element e = (Element) evt.getTarget();
      Node next = e.getNextSibling();
      if(next != null && next instanceof Element) {
        toggleTooltip((Element) next, evt.getType());
      }
      else {
        LoggingUtil.warning("Tooltip sibling not found.");
      }
    }
    else {
      LoggingUtil.warning("Got event for non-Element?!?");
    }
  }

  /**
   * Toggle the Tooltip of an element.
   * 
   * @param elem Element
   * @param type Event type
   */
  protected void toggleTooltip(Element elem, String type) {
    String csscls = elem.getAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE);
    if(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE.equals(type)) {
      if(TOOLTIP_HIDDEN.equals(csscls)) {
        SVGUtil.setAtt(elem, SVGConstants.SVG_CLASS_ATTRIBUTE, TOOLTIP_VISIBLE);
      }
    }
    else if(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE.equals(type)) {
      if(TOOLTIP_VISIBLE.equals(csscls)) {
        SVGUtil.setAtt(elem, SVGConstants.SVG_CLASS_ATTRIBUTE, TOOLTIP_HIDDEN);
      }
    }
    else if(SVGConstants.SVG_CLICK_EVENT_TYPE.equals(type)) {
      if(TOOLTIP_STICKY.equals(csscls)) {
        SVGUtil.setAtt(elem, SVGConstants.SVG_CLASS_ATTRIBUTE, TOOLTIP_HIDDEN);
      }
      if(TOOLTIP_HIDDEN.equals(csscls) || TOOLTIP_VISIBLE.equals(csscls)) {
        SVGUtil.setAtt(elem, SVGConstants.SVG_CLASS_ATTRIBUTE, TOOLTIP_STICKY);
      }
    }
  }

  /**
   * Registers the Tooltip-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
   */
  abstract protected void setupCSS(SVGPlot svgp);

  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
  }
}