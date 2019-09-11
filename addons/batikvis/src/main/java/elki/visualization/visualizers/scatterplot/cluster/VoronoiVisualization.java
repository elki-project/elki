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

import static elki.visualization.svg.VoronoiDraw.drawDelaunay;
import static elki.visualization.svg.VoronoiDraw.drawFakeVoronoi;
import static elki.visualization.svg.VoronoiDraw.drawVoronoi;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.data.model.MedoidModel;
import elki.data.model.Model;
import elki.database.datastore.ObjectNotFoundException;
import elki.database.ids.DBID;
import elki.math.geometry.SweepHullDelaunay2D;
import elki.math.geometry.SweepHullDelaunay2D.Triangle;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.visualization.VisualizationMenuAction;
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
import elki.visualization.svg.SVGPath;
import elki.visualization.svg.SVGPlot;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;
import elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualizer drawing Voronoi cells for k-means clusterings.
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
  public static class Par implements Parameterizer {
    /**
     * Mode for drawing: Voronoi, Delaunay, both.
     */
    public static final OptionID MODE_ID = new OptionID("voronoi.mode", "Mode for drawing the voronoi cells (and/or delaunay triangulation)");

    /**
     * Drawing mode.
     */
    protected Mode mode;

    @Override
    public void configure(Parameterization config) {
      new EnumParameter<Mode>(MODE_ID, Mode.class, Mode.VORONOI) //
          .grab(config, x -> mode = x);
    }

    @Override
    public VoronoiVisualization make() {
      return new VoronoiVisualization(mode);
    }
  }
}
