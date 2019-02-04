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

import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;


/**
 * Add a CSS class to the event target.
 * 
 * @author Erich Schubert
 * @since 0.2
 *
 */
public class AddCSSClass implements EventListener {
  /**
   * Class to set
   */
  private String cssclass;

  /**
   * Constructor
   * @param cssclass class to set
   */
  public AddCSSClass(String cssclass) {
    super();
    this.cssclass = cssclass;
  }

  /**
   * Event handler
   */
  @Override
  public void handleEvent(Event evt) {
    Element e = (Element) evt.getTarget();
    SVGUtil.addCSSClass(e, cssclass);
  }
}