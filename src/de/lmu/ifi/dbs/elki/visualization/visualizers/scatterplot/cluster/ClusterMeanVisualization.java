package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.cluster;

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
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize the mean of a KMeans-Clustering
 * 
 * @author Heidi Kolb
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
   * Settings
   */
  protected Parameterizer settings;

  /**
   * Constructor.
   * 
   * @param settings Settings
   */
  public ClusterMeanVisualization(Parameterizer settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    // Find clusterings we can visualize:
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
    for(Clustering<?> c : clusterings) {
      if(c.getAllClusters().size() > 0) {
        // Does the cluster have a model with cluster means?
        if(testMeanModel(c)) {
          Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
          for(ScatterPlotProjector<?> p : ps) {
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
     * CSS class name for center of the means
     */
    private static final String CSS_MEAN_STAR = "mean-star";

    /**
     * Clustering to visualize.
     */
    Clustering<Model> clustering;

    /**
     * Constructor.
     * 
     * @param task Visualization task
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.clustering = task.getResult();
      incrementalRedraw();
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);

      MarkerLibrary ml = context.getStyleLibrary().markers();
      double marker_size = context.getStyleLibrary().getSize(StyleLibrary.MARKERPLOT);

      Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
      for(int cnum = 0; ci.hasNext(); cnum++) {
        Cluster<Model> clus = ci.next();
        Model model = clus.getModel();
        double[] mean;
        if(model instanceof MeanModel) {
          @SuppressWarnings("unchecked")
          MeanModel<? extends NumberVector<?>> mmodel = (MeanModel<? extends NumberVector<?>>) model;
          mean = proj.fastProjectDataToRenderSpace(mmodel.getMean());
        }
        else if(model instanceof MedoidModel) {
          MedoidModel mmodel = (MedoidModel) model;
          mean = proj.fastProjectDataToRenderSpace(rel.get(mmodel.getMedoid()));
        }
        else {
          continue;
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

        if(settings.stars) {
          SVGPath star = new SVGPath();
          for(DBIDIter id = clus.getIDs().iter(); id.valid(); id.advance()) {
            double[] obj = proj.fastProjectDataToRenderSpace(rel.get(id));
            star.moveTo(mean);
            star.drawTo(obj);
          }
          Element stare = star.makeElement(svgp);
          SVGUtil.setCSSClass(stare, CSS_MEAN_STAR + "_" + cnum);
          layer.appendChild(stare);
        }
      }
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      if(!svgp.getCSSClassManager().contains(CSS_MEAN_CENTER)) {
        CSSClass center = new CSSClass(this, CSS_MEAN_CENTER);
        center.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.DEFAULT));
        center.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.AXIS_TICK) * .5);
        svgp.addCSSClassOrLogError(center);
      }
      if(!svgp.getCSSClassManager().contains(CSS_MEAN)) {
        CSSClass center = new CSSClass(this, CSS_MEAN);
        center.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.7");
        svgp.addCSSClassOrLogError(center);
      }
      if(settings.stars) {
        ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

        Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
        for(int cnum = 0; ci.hasNext(); cnum++) {
          ci.next();
          if(!svgp.getCSSClassManager().contains(CSS_MEAN_STAR + "_" + cnum)) {
            CSSClass center = new CSSClass(this, CSS_MEAN_STAR + "_" + cnum);
            center.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(cnum));
            center.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
            center.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.7");
            svgp.addCSSClassOrLogError(center);
          }
        }
      }
    }
  }

  /**
   * Test if the given clustering has a mean model.
   * 
   * @param c Clustering to inspect
   * @return true when the clustering has a mean or medoid model.
   */
  private static boolean testMeanModel(Clustering<?> c) {
    Model firstmodel = c.getAllClusters().get(0).getModel();
    if(firstmodel instanceof MeanModel<?>) {
      return true;
    }
    if(firstmodel instanceof MedoidModel) {
      return true;
    }
    return false;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option ID for visualization of cluster means.
     * 
     * <pre>
     * -cluster.stars
     * </pre>
     */
    public static final OptionID STARS_ID = OptionID.getOrCreateOptionID("cluster.stars", "Visualize mean-based clusters using stars.");

    /**
     * Whether to draw cluster stars
     */
    protected boolean stars = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag starsF = new Flag(STARS_ID);
      if(config.grab(starsF)) {
        stars = starsF.getValue();
      }
    }

    @Override
    protected ClusterMeanVisualization makeInstance() {
      return new ClusterMeanVisualization(this);
    }
  }
}