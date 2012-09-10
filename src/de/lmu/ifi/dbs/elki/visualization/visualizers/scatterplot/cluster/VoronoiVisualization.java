package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.cluster;

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
import java.util.Collection;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.VoronoiDraw;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualizer drawing Voronoi cells for k-means clusterings.
 * 
 * See also: {@link de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd
 * KMeans clustering}
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class VoronoiVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "k-means Voronoi cells";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  private static final String KMEANSBORDER = "kmeans-border";

  /**
   * Visualization mode.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static enum Mode {
    VORONOI, DELAUNAY, V_AND_D
  }

  /**
   * Settings
   */
  private Parameterizer settings;

  /**
   * Constructor
   * 
   * @param settings Drawing mode
   */
  public VoronoiVisualization(Parameterizer settings) {
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
            if(RelationUtil.dimensionality(p.getRelation()) == 2) {
              final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
              task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 3);
              baseResult.getHierarchy().add(p, task);
              baseResult.getHierarchy().add(c, task);
            }
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
   * Instance
   * 
   * @author Robert Rödler
   * @author Erich Schubert
   * 
   * @apiviz.has MeanModel oneway - - visualizes
   * @apiviz.has MedoidModel oneway - - visualizes
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * The result we work on
     */
    Clustering<Model> clustering;

    /**
     * The Voronoi diagram
     */
    Element voronoi;

    /**
     * Constructor
     * 
     * @param task VisualizationTask
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.clustering = task.getResult();
      incrementalRedraw();
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);
      final List<Cluster<Model>> clusters = clustering.getAllClusters();

      if(clusters.size() < 2) {
        return;
      }

      // Collect cluster means
      if(clusters.size() == 2) {
        ArrayList<double[]> means = new ArrayList<double[]>(clusters.size());
        {
          for(Cluster<Model> clus : clusters) {
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
            means.add(mean);
          }
        }
        if(settings.mode == Mode.VORONOI || settings.mode == Mode.V_AND_D) {
          Element path = VoronoiDraw.drawFakeVoronoi(proj, means).makeElement(svgp);
          SVGUtil.addCSSClass(path, KMEANSBORDER);
          layer.appendChild(path);
        }
        if(settings.mode == Mode.DELAUNAY || settings.mode == Mode.V_AND_D) {
          Element path = new SVGPath(means.get(0)).drawTo(means.get(1)).makeElement(svgp);
          SVGUtil.addCSSClass(path, KMEANSBORDER);
          layer.appendChild(path);
        }
      }
      else {
        ArrayList<Vector> vmeans = new ArrayList<Vector>(clusters.size());
        ArrayList<double[]> means = new ArrayList<double[]>(clusters.size());
        {
          for(Cluster<Model> clus : clusters) {
            Model model = clus.getModel();
            Vector mean;
            if(model instanceof MeanModel) {
              @SuppressWarnings("unchecked")
              MeanModel<? extends NumberVector<?>> mmodel = (MeanModel<? extends NumberVector<?>>) model;
              mean = mmodel.getMean().getColumnVector();
            }
            else if(model instanceof MedoidModel) {
              MedoidModel mmodel = (MedoidModel) model;
              mean = rel.get(mmodel.getMedoid()).getColumnVector();
            }
            else {
              continue;
            }
            vmeans.add(mean);
            means.add(mean.getArrayRef());
          }
        }
        // Compute Delaunay Triangulation
        ArrayList<Triangle> delaunay = new SweepHullDelaunay2D(vmeans).getDelaunay();
        if(settings.mode == Mode.VORONOI || settings.mode == Mode.V_AND_D) {
          Element path = VoronoiDraw.drawVoronoi(proj, delaunay, means).makeElement(svgp);
          SVGUtil.addCSSClass(path, KMEANSBORDER);
          layer.appendChild(path);
        }
        if(settings.mode == Mode.DELAUNAY || settings.mode == Mode.V_AND_D) {
          Element path = VoronoiDraw.drawDelaunay(proj, delaunay, means).makeElement(svgp);
          SVGUtil.addCSSClass(path, KMEANSBORDER);
          layer.appendChild(path);
        }
      }
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      // Class for the distance markers
      if(!svgp.getCSSClassManager().contains(KMEANSBORDER)) {
        CSSClass cls = new CSSClass(this, KMEANSBORDER);
        cls = new CSSClass(this, KMEANSBORDER);
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) * .5);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
    }
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
     * Mode for drawing: Voronoi, Delaunay, both
     * 
     * <p>
     * Key: {@code -voronoi.mode}
     * </p>
     */
    public static final OptionID MODE_ID = OptionID.getOrCreateOptionID("voronoi.mode", "Mode for drawing the voronoi cells (and/or delaunay triangulation)");

    protected Mode mode;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      EnumParameter<Mode> modeP = new EnumParameter<Mode>(MODE_ID, Mode.class, Mode.VORONOI);
      if(config.grab(modeP)) {
        mode = modeP.getValue();
      }
    }

    @Override
    protected VoronoiVisualization makeInstance() {
      return new VoronoiVisualization(this);
    }
  }
}