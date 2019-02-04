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

import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Do a hover effect using a CSS class.
 *
 * @author Erich Schubert
 * @since 0.2
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
   *
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
    if (SVGConstants.SVG_EVENT_MOUSEOVER.equals(evt.getType())) {
      if (overclass != null) {
        SVGUtil.addCSSClass(e, overclass);
      }
      if (outclass != null) {
        SVGUtil.removeCSSClass(e, outclass);
      }
    }
    if (SVGConstants.SVG_EVENT_MOUSEOUT.equals(evt.getType())) {
      if (overclass != null) {
        SVGUtil.removeCSSClass(e, overclass);
      }
      if (outclass != null) {
        SVGUtil.addCSSClass(e, outclass);
      }
    }
    if (clickisout && SVGConstants.SVG_EVENT_CLICK.equals(evt.getType())) {
      if (overclass != null) {
        SVGUtil.removeCSSClass(e, overclass);
      }
      if (outclass != null) {
        SVGUtil.addCSSClass(e, outclass);
      }
    }
  }
}
