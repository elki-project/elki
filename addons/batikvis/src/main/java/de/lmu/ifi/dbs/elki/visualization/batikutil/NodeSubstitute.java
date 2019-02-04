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
 * This helper class will replace a node in an SVG plot. This is a Runnable to
 * be put on the update queue.
 *
 * @author Erich Schubert
 * @since 0.2
 */
public class NodeSubstitute implements Runnable {
  /**
   * Existing element to replace.
   */
  private Element prev;

  /**
   * Replacement element.
   */
  private Element newe;

  /**
   * Setup a SVG node replacement.
   *
   * @param prev Existing element
   * @param newe New element
   */
  public NodeSubstitute(Element prev, Element newe) {
    super();
    this.prev = prev;
    this.newe = newe;
  }

  @Override
  public void run() {
    prev.getParentNode().replaceChild(newe, prev);
    // Note: no warning if it is not possible!
  }
}
