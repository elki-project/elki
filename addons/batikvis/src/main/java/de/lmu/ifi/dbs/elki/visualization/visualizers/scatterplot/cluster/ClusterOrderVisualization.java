package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.cluster;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Cluster order visualizer: connect objects via the spanning tree the cluster
 * order represents.
 *
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
// TODO: draw sample only?
public class ClusterOrderVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Predecessor Graph";

  /**
   * Constructor.
   */
  public ClusterOrderVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewSiblings(context, start, ClusterOrder.class, ScatterPlotProjector.class, //
    new VisualizationTree.Handler2<ClusterOrder, ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, ClusterOrder co, ScatterPlotProjector<?> p) {
        final VisualizationTask task = new VisualizationTask(NAME, context, co, p.getRelation(), ClusterOrderVisualization.this);
        task.initDefaultVisibility(false);
        task.level = VisualizationTask.LEVEL_DATA - 1;
        task.addUpdateFlags(VisualizationTask.ON_DATA);
        context.addVis(co, task);
        context.addVis(p, task);
      }
    });
  }

  /**
   * Instance
   *
   * @author Erich Schubert
   *
   * @apiviz.has ClusterOrderResult oneway - - visualizes
   */
  // TODO: listen for CLUSTER ORDER changes.
  public class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * CSS class name
     */
    private static final String CSSNAME = "predecessor";

    /**
     * The result we visualize
     */
    protected ClusterOrder result;

    /**
     * Constructor.
     *
     * @param task Visualization task.
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      result = task.getResult();
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
      CSSClass cls = new CSSClass(this, CSSNAME);
      style.lines().formatCSSClass(cls, 0, style.getLineWidth(StyleLibrary.CLUSTERORDER));

      svgp.addCSSClassOrLogError(cls);

      DBIDVar prev = DBIDUtil.newVar();
      for(DBIDIter it = result.iter(); it.valid(); it.advance()) {
        result.getPredecessor(it, prev);
        if(prev.isEmpty()) {
          continue;
        }
        double[] thisVec = proj.fastProjectDataToRenderSpace(rel.get(it));
        double[] prevVec = proj.fastProjectDataToRenderSpace(rel.get(prev));

        if(thisVec[0] != thisVec[0] || thisVec[1] != thisVec[1]) {
          continue; // NaN!
        }
        if(prevVec[0] != prevVec[0] || prevVec[1] != prevVec[1]) {
          continue; // NaN!
        }
        // FIXME: add arrow decorations!
        Element arrow = svgp.svgLine(prevVec[0], prevVec[1], thisVec[0], thisVec[1]);
        SVGUtil.setCSSClass(arrow, cls.getName());

        layer.appendChild(arrow);
      }
    }
  }
}