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

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize the mean of a KMeans-Clustering
 *
 * @author Heidi Kolb
 * @since 0.5.0
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class ClusterMeanVisualization extends AbstractVisFactory {
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
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(final VisualizerContext context, Object start) {
    Hierarchy.Iter<ScatterPlotProjector<?>> it = VisualizationTree.filter(context, start, ScatterPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ScatterPlotProjector<?> p = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, p, p.getRelation(), ClusterMeanVisualization.this);
      task.level = VisualizationTask.LEVEL_DATA + 1;
      task.addUpdateFlags(VisualizationTask.ON_STYLEPOLICY);
      context.addVis(p, task);
    }
  }

  /**
   * Instance.
   *
   * @author Heidi Kolb
   *
   * @apiviz.has MeanModel oneway - - visualizes
   * @apiviz.has MedoidModel oneway - - visualizes
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
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
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
          if(model instanceof MeanModel) {
            final double[] mmean = ((MeanModel) model).getMean();
            if(mmean == null) {
              continue;
            }
            mean = proj.fastProjectDataToRenderSpace(mmean);
          }
          else if(model instanceof MedoidModel) {
            DBID medoid = ((MedoidModel) model).getMedoid();
            if(medoid == null) {
              continue;
            }
            NumberVector v = rel.get(medoid);
            if(v == null) {
              continue;
            }
            mean = proj.fastProjectDataToRenderSpace(v);
          }
          else {
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