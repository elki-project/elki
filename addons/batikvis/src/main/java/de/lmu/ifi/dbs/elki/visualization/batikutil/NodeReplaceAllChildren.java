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

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Runnable wrapper to replace all children of a given node.
 *
 * @author Erich Schubert
 * @since 0.3
 */
public class NodeReplaceAllChildren extends NodeAppendChild {
  /**
   * Trivial constructor.
   *
   * @param parent will become the parent of the appended Element.
   * @param child the Element to be appended.
   */
  public NodeReplaceAllChildren(Element parent, Element child) {
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
  public NodeReplaceAllChildren(Element parent, Element child, SVGPlot plot, String id) {
    super(parent, child, plot, id);
  }

  @Override
  public void run() {
    // remove all existing children.
    while(parent.hasChildNodes()) {
      parent.removeChild(parent.getLastChild());
    }
    super.run();
  }
}
