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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;

/**
 * Visualize the mean of a KMeans-Clustering
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has MeanModel oneway - - visualizes
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class ClusterMeanVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Means";

  /**
   * CSS class name for center of the means
   */
  private final static String CSS_MEAN_CENTER = "mean-center";

  /**
   * CSS class name for center of the means
   */
  private final static String CSS_MEAN = "mean-marker";

  /**
   * Clustering to visualize.
   */
  Clustering<MeanModel<NV>> clustering;

  /**
   * Constructor.
   *
   * @param task Visualization task
   */
  public ClusterMeanVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
    context.addContextChangeListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);

    MarkerLibrary ml = context.getStyleLibrary().markers();
    double marker_size = context.getStyleLibrary().getSize(StyleLibrary.MARKERPLOT);

    Iterator<Cluster<MeanModel<NV>>> ci = clustering.getAllClusters().iterator();
    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<MeanModel<NV>> clus = ci.next();
      double[] mean = proj.fastProjectDataToRenderSpace(clus.getModel().getMean());

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
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    if(!svgp.getCSSClassManager().contains(CSS_MEAN_CENTER)) {
      CSSClass center = new CSSClass(svgp, CSS_MEAN_CENTER);
      center.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.DEFAULT));
      center.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.AXIS_TICK) / 2);
      svgp.addCSSClassOrLogError(center);
    }
    if(!svgp.getCSSClassManager().contains(CSS_MEAN)) {
      CSSClass center = new CSSClass(svgp, CSS_MEAN);
      center.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.7");
      svgp.addCSSClassOrLogError(center);
    }
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing a marker for
   * the mean in a KMeans-Clustering
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusterMeanVisualization oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusterMeanVisualization<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        if(c.getAllClusters().size() > 0) {
          // Does the cluster have a model with cluster means?
          Clustering<MeanModel<NV>> mcls = findMeanModel(c);
          if(mcls != null) {
            Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
            for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
              final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
              task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 1);
              baseResult.getHierarchy().add(c, task);
              baseResult.getHierarchy().add(p, task);
            }
          }
        }
      }
    }

    /**
     * Test if the given clustering has a mean model.
     * 
     * @param <NV> Vector type
     * @param c Clustering to inspect
     * @return the clustering cast to return a mean model, null otherwise.
     */
    @SuppressWarnings("unchecked")
    private static <NV extends NumberVector<NV, ?>> Clustering<MeanModel<NV>> findMeanModel(Clustering<?> c) {
      if(c.getAllClusters().get(0).getModel() instanceof MeanModel<?>) {
        return (Clustering<MeanModel<NV>>) c;
      }
      return null;
    }
  }
}