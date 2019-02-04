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
package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

/**
 * Base class for a materialized Visualization.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 * @navhas - - - Element
 */
public interface Visualization {
  /**
   * Get the SVG layer of the given visualization.
   *
   * @return layer
   */
  Element getLayer();

  /**
   * Request an update of the visualization.
   */
  void incrementalRedraw();

  /**
   * Request a full redrawing of the visualization.
   */
  void fullRedraw();

  /**
   * Destroy the visualization. Called after the elements have been removed from
   * the document.
   *
   * Implementations should remove their listeners etc.
   */
  void destroy();
}
