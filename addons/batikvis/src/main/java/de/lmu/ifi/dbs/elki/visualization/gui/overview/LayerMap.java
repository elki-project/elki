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
package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.util.*;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Class to help keeping track of the materialized layers of the different
 * visualizations.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @has - - - PlotItem
 * @has - - - VisualizationTask
 */
public class LayerMap {
  /**
   * The actual map
   */
  private HashMap<Pair<PlotItem, VisualizationTask>, Pair<Element, Visualization>> map = new HashMap<>();

  /**
   * Helper function for building a key object
   *
   * @param item Plot item
   * @param task Visualization Task
   * @return Key
   */
  private Pair<PlotItem, VisualizationTask> key(PlotItem item, VisualizationTask task) {
    return new Pair<>(item, task);
  }

  /**
   * Helper function to build a value pair
   *
   * @param elem Container element
   * @param vis Visualization
   * @return Value object
   */
  private Pair<Element, Visualization> value(Element elem, Visualization vis) {
    return new Pair<>(elem, vis);
  }

  /**
   * Get the visualization referenced by a item/key combination.
   *
   * @param item Plot ttem
   * @param task Visualization task
   * @return Visualization
   */
  public Visualization getVisualization(PlotItem item, VisualizationTask task) {
    Pair<Element, Visualization> pair = map.get(key(item, task));
    return pair == null ? null : pair.second;
  }

  /**
   * Get the container element referenced by a item/key combination.
   *
   * @param item Plot item
   * @param task Visualization task
   * @return Container element
   */
  public Element getContainer(PlotItem item, VisualizationTask task) {
    Pair<Element, Visualization> pair = map.get(key(item, task));
    return pair == null ? null : pair.first;
  }

  /**
   * Iterate over values
   *
   * @return Value iterable
   */
  public Iterable<Pair<Element, Visualization>> values() {
    return map.values();
  }

  /**
   * Clear a map
   */
  public void clear() {
    map.clear();
  }

  /**
   * Put a new combination into the map.
   *
   * @param it Plot item
   * @param task Visualization Task
   * @param elem Container element
   * @param vis Visualization
   */
  public void put(PlotItem it, VisualizationTask task, Element elem, Visualization vis) {
    map.put(key(it, task), value(elem, vis));
  }

  /**
   * Remove a combination.
   *
   * @param it Plot item
   * @param task Visualization task
   * @return Previous value
   */
  public Pair<Element, Visualization> remove(PlotItem it, VisualizationTask task) {
    return map.remove(key(it, task));
  }

  /**
   * Put a new item into the visualizations
   *
   * @param it Plot item
   * @param task Visualization task
   * @param pair Pair object
   */
  public void put(PlotItem it, VisualizationTask task, Pair<Element, Visualization> pair) {
    map.put(key(it, task), pair);
  }

  /**
   * Get a pair from the map
   *
   * @param it Plot item
   * @param task Visualization task
   * @return Pair object
   */
  public Pair<Element, Visualization> get(PlotItem it, VisualizationTask task) {
    return map.get(key(it, task));
  }
}