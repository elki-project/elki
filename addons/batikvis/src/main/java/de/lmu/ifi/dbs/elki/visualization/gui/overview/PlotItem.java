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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;

/**
 * Item to collect visualization tasks on a specific position on the plot map.
 *
 * Note: this is a {@code LinkedList<VisualizationTask>}!
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @composed - - - Projection
 * @composed - - - VisualizationTask
 * @composed - - - PlotItem
 */
public class PlotItem {
  /**
   * Position: x
   */
  public final double x;

  /**
   * Position: y
   */
  public final double y;

  /**
   * Size: width
   */
  public final double w;

  /**
   * Size: height
   */
  public final double h;

  /**
   * Projection (may be {@code null}!)
   */
  public final Projection proj;

  /**
   * The visualization tasks at this location
   */
  public List<VisualizationTask> tasks = new LinkedList<>();

  /**
   * Subitems to plot
   */
  public Collection<PlotItem> subitems = new LinkedList<>();

  /**
   * Constructor.
   *
   * @param w Position: w
   * @param h Position: h
   * @param proj Projection
   */
  public PlotItem(double w, double h, Projection proj) {
    this(0, 0, w, h, proj);
  }

  /**
   * Constructor.
   *
   * @param x Position: x
   * @param y Position: y
   * @param w Position: w
   * @param h Position: h
   * @param proj Projection
   */
  public PlotItem(double x, double y, double w, double h, Projection proj) {
    super();
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.proj = proj;
  }

  /**
   * Clone constructor.
   *
   * @param vis Existing plot item.
   */
  public PlotItem(PlotItem vis) {
    super();
    this.x = vis.x;
    this.y = vis.y;
    this.w = vis.w;
    this.h = vis.h;
    this.proj = vis.proj;
    this.tasks = new ArrayList<>(vis.tasks);
    this.subitems = new ArrayList<>(vis.subitems.size());
    for(PlotItem s : vis.subitems) {
      this.subitems.add(new PlotItem(s));
    }
  }

  /**
   * Sort all visualizers for their proper drawing order
   */
  public void sort() {
    Collections.sort(tasks);
    for(PlotItem subitem : subitems) {
      subitem.sort();
    }
  }

  /**
   * Add a task to the item.
   *
   * @param task Task to add
   */
  public void add(VisualizationTask task) {
    tasks.add(task);
  }

  /**
   * Number of tasks in this item.
   *
   * @return Number of tasks.
   */
  public int taskSize() {
    return tasks.size();
  }

  /**
   * Iterate (recursively) over all plot items, including itself.
   *
   * @return Iterator
   */
  public Iterator<PlotItem> itemIterator() {
    return new ItmItr();
  }

  @Override
  public String toString() {
    return "PlotItem [x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + ",proj=" + proj + "]";
  }

  /**
   * Recursive iterator
   *
   * @author Erich Schubert
   */
  private class ItmItr implements Iterator<PlotItem> {
    PlotItem next;

    Iterator<PlotItem> cur;

    Iterator<PlotItem> sub;

    /**
     * Constructor.
     */
    public ItmItr() {
      super();
      this.next = PlotItem.this;
      this.cur = null;
      this.sub = subitems.iterator();
    }

    @Override
    public boolean hasNext() {
      if(next != null) {
        return true;
      }
      if(cur != null && cur.hasNext()) {
        next = cur.next();
        return true;
      }
      if(sub.hasNext()) {
        cur = sub.next().itemIterator();
        return hasNext();
      }
      return false;
    }

    @Override
    public PlotItem next() {
      hasNext();
      PlotItem ret = next;
      next = null;
      return ret;
    }
  }
}