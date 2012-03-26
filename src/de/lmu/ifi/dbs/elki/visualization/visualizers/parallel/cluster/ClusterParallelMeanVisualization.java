package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.cluster;

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

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;

/**
 * Generates a SVG-Element that visualizes cluster means.
 * 
 * @author Robert Rödler
 */
public class ClusterParallelMeanVisualization extends AbstractParallelVisualization<NumberVector<?, ?>> implements DataStoreListener {
  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERMEAN = "Clustermean";

  /**
   * The result we visualize
   */
  private Clustering<MeanModel<? extends NumberVector<?, ?>>> clustering;

  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public ClusterParallelMeanVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
    context.addDataStoreListener(this);
    context.addResultListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    context.removeDataStoreListener(this);
    context.removeResultListener(this);
    super.destroy();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);

    Iterator<Cluster<MeanModel<? extends NumberVector<?, ?>>>> ci = clustering.getAllClusters().iterator();
    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<MeanModel<? extends NumberVector<?, ?>>> clus = ci.next();
      NumberVector<?, ?> mean = clus.getModel().getMean();
      if(mean == null) {
        continue;
      }

      double[] pmean = proj.fastProjectDataToRenderSpace(mean);

      SVGPath path = new SVGPath();
      for(int i = 0; i < pmean.length; i++) {
        path.drawTo(getVisibleAxisX(i), pmean[i]);
      }

      Element meanline = path.makeElement(svgp);
      SVGUtil.addCSSClass(meanline, CLUSTERMEAN + cnum);
      layer.appendChild(meanline);
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    if(!svgp.getCSSClassManager().contains(CLUSTERMEAN)) {
      ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
      int clusterID = 0;

      for(@SuppressWarnings("unused")
      Cluster<?> cluster : clustering.getAllClusters()) {
        CSSClass cls = new CSSClass(this, CLUSTERMEAN + clusterID);
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) * 2);

        final String color;
        if(clustering.getAllClusters().size() == 1) {
          color = SVGConstants.CSS_BLACK_VALUE;
        }
        else {
          color = colors.getColor(clusterID);
        }

        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);

        svgp.addCSSClassOrLogError(cls);
        clusterID++;
      }
    }
  }

  /**
   * Factory for axis visualizations
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusterParallelMeanVisualization oneway - - «create»
   * 
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Cluster Means";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusterParallelMeanVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find clusterings we can visualize:
      Iterator<Clustering<?>> clusterings = ResultUtil.filteredResults(result, Clustering.class);
      while(clusterings.hasNext()) {
        Clustering<?> c = clusterings.next();
        if(c.getAllClusters().size() > 0) {
          // Does the cluster have a model with cluster means?
          Clustering<MeanModel<? extends NumberVector<?, ?>>> mcls = findMeanModel(c);
          if(mcls != null) {
            Iterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ParallelPlotProjector.class);
            while(ps.hasNext()) {
              ParallelPlotProjector<?> p = ps.next();
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
     * @param c Clustering to inspect
     * @return the clustering cast to return a mean model, null otherwise.
     */
    @SuppressWarnings("unchecked")
    private static Clustering<MeanModel<? extends NumberVector<?, ?>>> findMeanModel(Clustering<?> c) {
      if(c.getAllClusters().get(0).getModel() instanceof MeanModel<?>) {
        return (Clustering<MeanModel<? extends NumberVector<?, ?>>>) c;
      }
      return null;
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}