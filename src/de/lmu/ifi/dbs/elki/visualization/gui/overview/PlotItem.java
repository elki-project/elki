package de.lmu.ifi.dbs.elki.visualization.gui.overview;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * 
 * @apiviz.composedOf Projection
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
   * The visualizations at this location
   */
  public List<VisualizationTask> visualizations = new LinkedList<VisualizationTask>();

  /**
   * Subitems to plot
   */
  public Collection<PlotItem> subitems = new LinkedList<PlotItem>();

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
   * Sort all visualizers for their proper drawing order
   */
  public void sort() {
    Collections.sort(visualizations);
    for(PlotItem subitem : subitems) {
      subitem.sort();
    }
  }

  /**
   * Iterate (recursively) over all visualizations.
   * 
   * @return Iterator
   */
  public Iterator<VisualizationTask> visIterator() {
    return new VisItr();
  }

  /**
   * Iterate (recursively) over all plot items, including itself.
   * 
   * @return Iterator
   */
  public Iterator<PlotItem> itemIterator() {
    return new ItmItr();
  }

  /**
   * Recursive iterator
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class VisItr implements Iterator<VisualizationTask> {
    Iterator<VisualizationTask> cur;

    Iterator<PlotItem> sub;

    /**
     * Constructor.
     */
    public VisItr() {
      super();
      this.cur = visualizations.iterator();
      this.sub = subitems.iterator();
    }

    @Override
    public boolean hasNext() {
      if(cur.hasNext()) {
        return true;
      }
      if(sub.hasNext()) {
        cur = sub.next().visIterator();
        return hasNext();
      }
      return false;
    }

    @Override
    public VisualizationTask next() {
      hasNext();
      return cur.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Recursive iterator
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
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
      if (cur != null && cur.hasNext()) {
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

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}