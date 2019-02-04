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

/**
 * Runnable wrapper for modifying XML-Attributes.
 * 
 * @author Remigius Wojdanowski
 * @since 0.3
 * 
 */
// FIXME: Unused? Remove?
public class AttributeModifier implements Runnable {

  /**
   * Provides the attribute to be modified.
   */
  private Element e;

  /**
   * The name of the attribute to be modified.
   */
  private String attribute;

  /**
   * The new value of the attribute.
   */
  private String newValue;

  /**
   * Trivial constructor.
   * 
   * @param e provides the attribute to be modified.
   * @param attribute the name of the attribute to be modified.
   * @param newValue the new value of the attribute.
   */
  public AttributeModifier(Element e, String attribute, String newValue) {
    this.e = e;
    this.attribute = attribute;
    this.newValue = newValue;
  }

  @Override
  public void run() {
    if(newValue != null) {
      e.setAttribute(attribute, newValue);
    }
    else {
      e.removeAttribute(attribute);
    }
  }
}
