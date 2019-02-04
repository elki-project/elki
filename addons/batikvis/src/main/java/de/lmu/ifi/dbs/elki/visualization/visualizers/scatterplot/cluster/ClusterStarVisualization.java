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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.cluster;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.PrototypeModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize the mean of a KMeans-Clustering using stars.
 *
 * @author Heidi Kolb
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class ClusterStarVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Stars";

  /**
   * Constructor.
   */
  public ClusterStarVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(final VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      context.addVis(p, new VisualizationTask(this, NAME, p, rel) //
          .level(VisualizationTask.LEVEL_DATA + 1).visibility(false) //
          .with(UpdateFlag.ON_STYLEPOLICY));
    });
  }

  /**
   * Instance.
   *
   * @author Heidi Kolb
   *
   * @navhas - visualizes - MeanModel
   * @navhas - visualizes - MedoidModel
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * CSS class name for center of the means
     */
    private static final String CSS_MEAN_STAR = "mean-star";

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
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StylingPolicy spol = context.getStylingPolicy();
      if(!(spol instanceof ClusterStylingPolicy)) {
        return;
      }
      @SuppressWarnings("unchecked")
      Clustering<Model> clustering = (Clustering<Model>) ((ClusterStylingPolicy) spol).getClustering();
      if(clustering.getAllClusters().size() == 0) {
        return;
      }

      StyleLibrary slib = context.getStyleLibrary();
      ColorLibrary colors = slib.getColorSet(StyleLibrary.PLOT);

      Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
      for(int cnum = 0; ci.hasNext(); cnum++) {
        Cluster<Model> clus = ci.next();
        Model model = clus.getModel();
        double[] mean = null;
        try {
          if(model instanceof PrototypeModel) {
            Object prototype = ((PrototypeModel<?>) model).getPrototype();
            if(prototype instanceof double[]) {
              mean = proj.fastProjectDataToRenderSpace((double[]) prototype);
            }
            else if(prototype instanceof DBIDRef) {
              mean = proj.fastProjectDataToRenderSpace(rel.get((DBIDRef) prototype));
            }
          }
          if(mean == null) {
            continue;
          }
        }
        catch(ObjectNotFoundException e) {
          continue; // Element not found.
        }

        if(!svgp.getCSSClassManager().contains(CSS_MEAN_STAR + "_" + cnum)) {
          CSSClass center = new CSSClass(this, CSS_MEAN_STAR + "_" + cnum);
          center.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(cnum));
          center.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, slib.getLineWidth(StyleLibrary.PLOT));
          center.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.7");
          svgp.addCSSClassOrLogError(center);
        }

        SVGPath star = new SVGPath();
        for(DBIDIter id = clus.getIDs().iter(); id.valid(); id.advance()) {
          star.moveTo(proj.fastProjectDataToRenderSpace(rel.get(id)));
          star.drawTo(mean);
        }
        layer.appendChild(star.makeElement(svgp, CSS_MEAN_STAR + "_" + cnum));
      }
      svgp.updateStyleElement();
    }
  }
}
