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

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
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
    /**
     * Draw Voronoi cells.
     */
    VORONOI, //
    /**
     * Draw Delaunay triangulation.
     */
    DELAUNAY, //
    /**
     * Draw both Delaunay and Voronoi.
     */
    V_AND_D
  }

  /**
   * Visualization mode.
   */
  private Mode mode;

  /**
   * Constructor.
   *
   * @param mode Visualization mod
   */
  public VoronoiVisualization(Mode mode) {
    super();
    this.mode = mode;
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNew(context, start, ScatterPlotProjector.class, new VisualizationTree.Handler1<ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, ScatterPlotProjector<?> p) {
        final VisualizationTask task = new VisualizationTask(NAME, context, p, p.getRelation(), VoronoiVisualization.this);
        task.level = VisualizationTask.LEVEL_DATA + 3;
        task.addUpdateFlags(VisualizationTask.ON_STYLEPOLICY);
        context.addVis(p, task);
      }
    });
  }

  /**
   * Instance.
   *
   * @author Robert Rödler
   * @author Erich Schubert
   *
   * @apiviz.has MeanModel oneway - - visualizes
   * @apiviz.has MedoidModel oneway - - visualizes
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * The Voronoi diagram.
     */
    Element voronoi;

    /**
     * Constructor.
     *
     * @param task VisualizationTask
     */
    public Instance(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
    }

    @Override
    protected void redraw() {
      super.redraw();
      final StylingPolicy spol = context.getStylingPolicy();
      if(!(spol instanceof ClusterStylingPolicy)) {
        return;
      }
      @SuppressWarnings("unchecked")
      Clustering<Model> clustering = (Clustering<Model>) ((ClusterStylingPolicy) spol).getClustering();
      if(clustering.getAllClusters().size() <= 1) {
        return;
      }

      addCSSClasses(svgp);
      final List<Cluster<Model>> clusters = clustering.getAllClusters();

      // Collect cluster means
      ArrayList<Vector> vmeans = new ArrayList<>(clusters.size());
      ArrayList<double[]> means = new ArrayList<>(clusters.size());
      {
        for(Cluster<Model> clus : clusters) {
          Model model = clus.getModel();
          Vector mean;
          if(model instanceof EMModel) {
            continue; // Does not make much sense
          }
          else if(model instanceof MeanModel) {
            MeanModel mmodel = (MeanModel) model;
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

      if(means.size() < 2) {
        return; // Cannot visualize
      }
      if(means.size() == 2) {
        if(mode == Mode.VORONOI || mode == Mode.V_AND_D) {
          Element path = VoronoiDraw.drawFakeVoronoi(proj, means).makeElement(svgp);
          SVGUtil.addCSSClass(path, KMEANSBORDER);
          layer.appendChild(path);
        }
        if(mode == Mode.DELAUNAY || mode == Mode.V_AND_D) {
          Element path = new SVGPath(proj.fastProjectDataToRenderSpace(means.get(0)))//
          .drawTo(proj.fastProjectDataToRenderSpace(means.get(1))).makeElement(svgp);
          SVGUtil.addCSSClass(path, KMEANSBORDER);
          layer.appendChild(path);
        }
      }
      else {
        // Compute Delaunay Triangulation
        ArrayList<Triangle> delaunay = new SweepHullDelaunay2D(vmeans).getDelaunay();
        if(mode == Mode.VORONOI || mode == Mode.V_AND_D) {
          Element path = VoronoiDraw.drawVoronoi(proj, delaunay, means).makeElement(svgp);
          SVGUtil.addCSSClass(path, KMEANSBORDER);
          layer.appendChild(path);
        }
        if(mode == Mode.DELAUNAY || mode == Mode.V_AND_D) {
          Element path = VoronoiDraw.drawDelaunay(proj, delaunay, means).makeElement(svgp);
          SVGUtil.addCSSClass(path, KMEANSBORDER);
          layer.appendChild(path);
        }
      }
    }

    /**
     * Adds the required CSS-Classes.
     *
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      // Class for the distance markers
      if(!svgp.getCSSClassManager().contains(KMEANSBORDER)) {
        final StyleLibrary style = context.getStyleLibrary();
        CSSClass cls = new CSSClass(this, KMEANSBORDER);
        cls = new CSSClass(this, KMEANSBORDER);
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) * .5);
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
     * Mode for drawing: Voronoi, Delaunay, both.
     *
     * <p>
     * Key: {@code -voronoi.mode}
     * </p>
     */
    public static final OptionID MODE_ID = new OptionID("voronoi.mode", "Mode for drawing the voronoi cells (and/or delaunay triangulation)");

    /**
     * Drawing mode.
     */
    protected Mode mode;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      EnumParameter<Mode> modeP = new EnumParameter<>(MODE_ID, Mode.class, Mode.VORONOI);
      if(config.grab(modeP)) {
        mode = modeP.getValue();
      }
    }

    @Override
    protected VoronoiVisualization makeInstance() {
      return new VoronoiVisualization(mode);
    }
  }
}
