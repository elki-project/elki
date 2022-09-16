/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.visualization.visualizers.scatterplot.cluster;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.clustering.optics.ClusterOrder;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreListener;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.relation.Relation;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.css.CSSClass;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projector.ScatterPlotProjector;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;
import elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Cluster order visualizer: connect objects via the spanning tree the cluster
 * order represents.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
// TODO: draw sample only?
public class ClusterOrderVisualization implements VisFactory {
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
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewSiblings(context, start, ClusterOrder.class, ScatterPlotProjector.class, (co, p) -> {
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      final VisualizationTask task = new VisualizationTask(this, NAME, co, rel) //
          .level(VisualizationTask.LEVEL_DATA - 1).visibility(false) //
          .with(UpdateFlag.ON_DATA);
      context.addVis(co, task);
      context.addVis(p, task);
    });
  }

  /**
   * Instance
   *
   * @author Erich Schubert
   *
   * @navhas - visualizes - ClusterOrder
   */
  // TODO: listen for CLUSTER ORDER changes.
  public static class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * CSS class name
     */
    private static final String CSSNAME = "predecessor";

    /**
     * The result we visualize
     */
    protected ClusterOrder result;

    /**
     * Visualize the cluster order sequence, not the predecessor.
     */
    protected boolean order = false;

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      result = task.getResult();
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
      CSSClass cls = new CSSClass(this, CSSNAME);
      style.lines().formatCSSClass(cls, -1, style.getLineWidth(StyleLibrary.CLUSTERORDER));
      cls.setStatement(SVGConstants.CSS_MARKER_END_PROPERTY, "url(#opticshead)");
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.5);
      svgp.addCSSClassOrLogError(cls);

      if(svgp.getIdElement("opticshead") == null) {
        Element head = svgp.svgElement(SVGConstants.SVG_MARKER_TAG);
        head.setAttribute(SVGConstants.SVG_ID_ATTRIBUTE, "opticshead");
        head.setAttribute(SVGConstants.SVG_ORIENT_ATTRIBUTE, SVGConstants.SVG_AUTO_VALUE);
        head.setAttribute(SVGConstants.SVG_MARKER_WIDTH_ATTRIBUTE, "4");
        head.setAttribute(SVGConstants.SVG_MARKER_HEIGHT_ATTRIBUTE, "4");
        head.setAttribute(SVGConstants.SVG_REF_X_ATTRIBUTE, SVGUtil.fmt(4 - 2 * style.getLineWidth(StyleLibrary.CLUSTERORDER)));
        head.setAttribute(SVGConstants.SVG_REF_Y_ATTRIBUTE, "2");
        Element path = svgp.svgElement(SVGConstants.SVG_PATH_TAG);
        path.setAttribute(SVGConstants.SVG_D_ATTRIBUTE, "M0,0 L2,2 L0,4 L4,2 Z");
        path.setAttribute(SVGConstants.CSS_COLOR_PROPERTY, style.getColorSet(StyleLibrary.PLOT).getColor(-1));
        head.appendChild(path);
        svgp.getDefs().appendChild(head);
      }

      DBIDVar prev = DBIDUtil.newVar();
      for(DBIDIter it = result.iter(); it.valid(); it.advance()) {
        if(!order) {
          result.getPredecessor(it, prev);
        }
        if(prev.isEmpty()) {
          if(order) {
            prev.set(it);
          }
          continue;
        }
        double[] thisVec = proj.fastProjectDataToRenderSpace(rel.get(it));
        double[] prevVec = proj.fastProjectDataToRenderSpace(rel.get(prev));
        if(order) {
          prev.set(it);
        }

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
