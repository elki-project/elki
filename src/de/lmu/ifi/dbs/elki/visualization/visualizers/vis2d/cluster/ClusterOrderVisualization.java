package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.cluster;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import java.util.Iterator;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;

/**
 * Cluster order visualizer.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ClusterOrderResult oneway - - visualizes
 */
// TODO: listen for CLUSTER ORDER changes.
public class ClusterOrderVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DataStoreListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Predecessor Graph";

  /**
   * CSS class name
   */
  private static final String CSSNAME = "predecessor";

  /**
   * The result we visualize
   */
  protected ClusterOrderResult<?> result;

  public ClusterOrderVisualization(VisualizationTask task) {
    super(task);
    result = task.getResult();
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
    CSSClass cls = new CSSClass(this, CSSNAME);
    context.getStyleLibrary().lines().formatCSSClass(cls, 0, context.getStyleLibrary().getLineWidth(StyleLibrary.CLUSTERORDER));

    svgp.addCSSClassOrLogError(cls);

    for(ClusterOrderEntry<?> ce : result) {
      DBID thisId = ce.getID();
      DBID prevId = ce.getPredecessorID();
      if(thisId == null || prevId == null) {
        continue;
      }
      double[] thisVec = proj.fastProjectDataToRenderSpace(rel.get(thisId));
      double[] prevVec = proj.fastProjectDataToRenderSpace(rel.get(prevId));

      Element arrow = svgp.svgLine(prevVec[0], prevVec[1], thisVec[0], thisVec[1]);
      SVGUtil.setCSSClass(arrow, cls.getName());

      layer.appendChild(arrow);
    }
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * Visualize an OPTICS cluster order by drawing connection lines.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusterOrderVisualization oneway - - «create»
   * 
   * @param <NV> object type
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusterOrderVisualization<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Collection<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(result, ClusterOrderResult.class);
      for(ClusterOrderResult<DoubleDistance> co : cos) {
        Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
        for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
          final VisualizationTask task = new VisualizationTask(NAME, co, p.getRelation(), this);
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
          baseResult.getHierarchy().add(co, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }
  }
}