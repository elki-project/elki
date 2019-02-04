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

import static de.lmu.ifi.dbs.elki.visualization.svg.VoronoiDraw.drawDelaunay;
import static de.lmu.ifi.dbs.elki.visualization.svg.VoronoiDraw.drawFakeVoronoi;
import static de.lmu.ifi.dbs.elki.visualization.svg.VoronoiDraw.drawVoronoi;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.datastore.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationMenuAction;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualizer drawing Voronoi cells for k-means clusterings.
 *
 * See also: {@link de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd
 * KMeans clustering}
 * 
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @stereotype factory
 * @composed - - - Mode
 * @has - - - SwitchModeAction
 * @navassoc - create - Instance
 */
public class VoronoiVisualization implements VisFactory {
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
   */
  public enum Mode {
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
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      final VisualizationTask task = new VisualizationTask(this, NAME, p, p.getRelation()) //
          .level(VisualizationTask.LEVEL_DATA + 3).with(UpdateFlag.ON_STYLEPOLICY);
      context.addVis(p, task);
      context.addVis(p, new SwitchModeAction(task, context));
    });
  }

  /**
   * Menu item to change visualization styles.
   *
   * @author Erich Schubert
   */
  public class SwitchModeAction implements VisualizationMenuAction {
    /**
     * Task we represent.
     */
    private VisualizationTask task;

    /**
     * Visualizer context.
     */
    private VisualizerContext context;

    /**
     * Constructor.
     *
     * @param task Task
     * @param context Visualizer context
     */
    public SwitchModeAction(VisualizationTask task, VisualizerContext context) {
      super();
      this.task = task;
      this.context = context;
    }

    @Override
    public String getMenuName() {
      return "Switch Voronoi Mode";
    }

    @Override
    public void activate() {
      switch(mode){
      case VORONOI:
        mode = Mode.DELAUNAY;
        break;
      case DELAUNAY:
        mode = Mode.V_AND_D;
        break;
      case V_AND_D:
        mode = Mode.VORONOI;
        break;
      }
      context.visChanged(task);
    }
  }

  /**
   * Instance.
   *
   * @author Robert Rödler
   * @author Erich Schubert
   *
   * @navhas - visualizes - KMeansModel
   * @navhas - visualizes - MedoidModel
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * The Voronoi diagram.
     */
    Element voronoi;

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task VisualizationTask
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
      if(clustering.getAllClusters().size() <= 1) {
        return;
      }

      final int dim = proj.getInputDimensionality();
      if(dim != 2) {
        return;
      }

      addCSSClasses(svgp);
      final List<Cluster<Model>> clusters = clustering.getAllClusters();

      // Collect cluster means
      ArrayList<double[]> means = new ArrayList<>(clusters.size());
      {
        for(Cluster<Model> clus : clusters) {
          Model model = clus.getModel();
          double[] mean;
          try {
            if(model instanceof KMeansModel) {
              mean = ((KMeansModel) model).getMean();
              if(mean == null || mean.length != dim) {
                continue;
              }
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
              mean = v.toArray();
              if(mean.length != dim) {
                continue;
              }
            }
            else {
              continue;
            }
          }
          catch(ObjectNotFoundException e) {
            continue; // Element not found.
          }
          means.add(mean);
        }
      }

      if(means.size() < 2) {
        return; // Cannot visualize
      }
      if(means.size() == 2) {
        if(mode == Mode.VORONOI || mode == Mode.V_AND_D) {
          layer.appendChild(drawFakeVoronoi(proj, means).makeElement(svgp, KMEANSBORDER));
        }
        if(mode == Mode.DELAUNAY || mode == Mode.V_AND_D) {
          layer.appendChild(new SVGPath(proj.fastProjectDataToRenderSpace(means.get(0)))//
              .drawTo(proj.fastProjectDataToRenderSpace(means.get(1))).makeElement(svgp, KMEANSBORDER));
        }
      }
      else {
        // Compute Delaunay Triangulation
        ArrayList<Triangle> delaunay = new SweepHullDelaunay2D(means).getDelaunay();
        if(mode == Mode.VORONOI || mode == Mode.V_AND_D) {
          layer.appendChild(drawVoronoi(proj, delaunay, means).makeElement(svgp, KMEANSBORDER));
        }
        if(mode == Mode.DELAUNAY || mode == Mode.V_AND_D) {
          layer.appendChild(drawDelaunay(proj, delaunay, means).makeElement(svgp, KMEANSBORDER));
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
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Mode for drawing: Voronoi, Delaunay, both.
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
