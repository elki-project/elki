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
package elki.visualization.visualizers.scatterplot.cluster;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.model.PrototypeModel;
import elki.database.datastore.ObjectNotFoundException;
import elki.database.ids.DBIDRef;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.css.CSSClass;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projector.ScatterPlotProjector;
import elki.visualization.style.ClusterStylingPolicy;
import elki.visualization.style.StyleLibrary;
import elki.visualization.style.StylingPolicy;
import elki.visualization.style.marker.MarkerLibrary;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;
import elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize the mean of a KMeans-Clustering
 *
 * @author Heidi Kolb
 * @since 0.7.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class ClusterMeanVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Means";

  /**
   * Constructor.
   */
  public ClusterMeanVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(final VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      context.addVis(p, new VisualizationTask(this, NAME, p, p.getRelation())//
          .level(VisualizationTask.LEVEL_DATA + 1).with(UpdateFlag.ON_STYLEPOLICY));
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
    private static final String CSS_MEAN_CENTER = "mean-center";

    /**
     * CSS class name for center of the means
     */
    private static final String CSS_MEAN = "mean-marker";

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
      MarkerLibrary ml = slib.markers();
      double marker_size = slib.getSize(StyleLibrary.MARKERPLOT);

      // Small crosses for mean:
      if(!svgp.getCSSClassManager().contains(CSS_MEAN_CENTER)) {
        CSSClass center = new CSSClass(this, CSS_MEAN_CENTER);
        center.setStatement(SVGConstants.CSS_STROKE_PROPERTY, slib.getTextColor(StyleLibrary.DEFAULT));
        center.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, slib.getLineWidth(StyleLibrary.AXIS_TICK) * .5);
        svgp.addCSSClassOrLogError(center);
      }
      // Markers for the mean:
      if(!svgp.getCSSClassManager().contains(CSS_MEAN)) {
        CSSClass center = new CSSClass(this, CSS_MEAN);
        center.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.7");
        svgp.addCSSClassOrLogError(center);
      }

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

        // add a greater Marker for the mean
        Element meanMarker = ml.useMarker(svgp, layer, mean[0], mean[1], cnum, marker_size * 3);
        SVGUtil.setAtt(meanMarker, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_MEAN);

        // Add a fine cross to mark the exact location of the mean.
        Element meanMarkerCenter = svgp.svgLine(mean[0] - .7, mean[1], mean[0] + .7, mean[1]);
        SVGUtil.setAtt(meanMarkerCenter, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_MEAN_CENTER);
        Element meanMarkerCenter2 = svgp.svgLine(mean[0], mean[1] - .7, mean[0], mean[1] + .7);
        SVGUtil.setAtt(meanMarkerCenter2, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_MEAN_CENTER);

        layer.appendChild(meanMarkerCenter);
        layer.appendChild(meanMarkerCenter2);
      }
      svgp.updateStyleElement();
    }
  }
}
