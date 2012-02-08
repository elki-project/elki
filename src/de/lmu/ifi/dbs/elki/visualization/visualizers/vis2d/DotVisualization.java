package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

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

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Generates a SVG-Element containing "dots" as markers representing the
 * Database's objects.
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.has NumberVector - - visualizes
 */
public class DotVisualization extends P2DVisualization implements DataStoreListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Data Dots";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String MARKER = "marker";

  /**
   * Constructor.
   * 
   * @param task Task to visualize
   */
  public DotVisualization(VisualizationTask task) {
    super(task);
    context.addDataStoreListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDataStoreListener(this);
  }

  @Override
  public void redraw() {
    // draw data
    double dot_size = context.getStyleLibrary().getSize(StyleLibrary.DOTPLOT);
    for(DBID id : sample.getSample()) {
      try {
        double[] v = proj.fastProjectDataToRenderSpace(rel.get(id));
        Element dot = svgp.svgCircle(v[0], v[1], dot_size);
        SVGUtil.addCSSClass(dot, MARKER);
        layer.appendChild(dot);
      }
      catch(ObjectNotFoundException e) {
        // ignore.
      }

    }
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * The visualization factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses DotVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new DotVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      ArrayList<Clustering<?>> cs = ResultUtil.filterResults(result, Clustering.class);
      boolean hasClustering = (cs.size() > 0);

      Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(result, ScatterPlotProjector.class);
      for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
        final VisualizationTask task = new VisualizationTask(NAME, p.getRelation(), p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 1);
        if(hasClustering) {
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
        }
        // baseResult.getHierarchy().add(p.getRelation(), task);
        baseResult.getHierarchy().add(p, task);
      }
    }
  }
}