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
package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.cluster;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.cluster.ClusterHullVisualization;

/**
 * Generates a SVG-Element that visualizes the area covered by a cluster.
 *
 * @author Robert Rödler
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
// TODO: make parameterizable: rounded.
public class ClusterOutlineVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Hull (Parallel Coordinates)";

  /**
   * Settings
   */
  Parameterizer settings;

  /**
   * Constructor.
   *
   * @param settings Settings
   */
  public ClusterOutlineVisualization(Parameterizer settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    // We use the style library, not individual clusterings!
    VisualizationTree.findVis(context, start).filter(ParallelPlotProjector.class).forEach(p -> {
      Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      context.addVis(p, new VisualizationTask(this, NAME, p, rel) //
          .level(VisualizationTask.LEVEL_DATA - 1).visibility(false) //
          .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_STYLEPOLICY));
    });
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance
   *
   * @author Robert Rödler
   * @author Erich Schubert
   */
  public class Instance extends AbstractParallelVisualization<NumberVector> implements DataStoreListener {
    /**
     * Generic tags to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String CLUSTERAREA = "Clusteroutline";

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
      super.fullRedraw();
      final StylingPolicy spol = context.getStylingPolicy();
      if(!(spol instanceof ClusterStylingPolicy)) {
        return;
      }
      final ClusterStylingPolicy cpol = (ClusterStylingPolicy) spol;
      @SuppressWarnings("unchecked")
      Clustering<Model> clustering = (Clustering<Model>) cpol.getClustering();

      int dim = proj.getVisibleDimensions();

      DoubleMinMax[] mms = DoubleMinMax.newArray(dim);
      DoubleMinMax[] midmm = DoubleMinMax.newArray(dim - 1);

      // Heuristic value for transparency:
      double baseopacity = .5;

      Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
      for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
        Cluster<?> clus = ci.next();
        final DBIDs ids = clus.getIDs();
        if(ids.size() < 1) {
          continue;
        }
        for(int i = 0; i < dim; i++) {
          mms[i].reset();
          if(i < dim - 1) {
            midmm[i].reset();
          }
        }

        // Process points
        // TODO: do this just once, cache the result somewhere appropriately?
        for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
          double[] yPos = proj.fastProjectDataToRenderSpace(relation.get(id));
          for(int i = 0; i < dim; i++) {
            mms[i].put(yPos[i]);
            if(i > 0) {
              midmm[i - 1].put((yPos[i] + yPos[i - 1]) / 2.);
            }
          }
        }

        SVGPath path = new SVGPath();
        if(!settings.bend) {
          // Straight lines
          for(int i = 0; i < dim; i++) {
            path.drawTo(getVisibleAxisX(i), mms[i].getMax());
            if(i < dim - 1) {
              path.drawTo(getVisibleAxisX(i + .5), midmm[i].getMax());
            }
          }
          for(int i = dim - 1; i >= 0; i--) {
            if(i < dim - 1) {
              path.drawTo(getVisibleAxisX(i + .5), midmm[i].getMin());
            }
            path.drawTo(getVisibleAxisX(i), mms[i].getMin());
          }
        }
        else {
          // Maxima
          path.drawTo(getVisibleAxisX(0), mms[0].getMax());
          for(int i = 1; i < dim; i++) {
            path.quadTo(getVisibleAxisX(i - .5), midmm[i - 1].getMax(), getVisibleAxisX(i), mms[i].getMax());
          }
          // Minima
          path.drawTo(getVisibleAxisX(dim - 1), mms[dim - 1].getMin());
          for(int i = dim - 1; i > 0; i--) {
            path.quadTo(getVisibleAxisX(i - .5), midmm[i - 1].getMin(), getVisibleAxisX(i - 1), mms[i - 1].getMin());
          }
        }
        path.close();

        // TODO: improve the visualization by adjusting the opacity by the
        // cluster extends on each axis (maybe use a horizontal gradient?)
        double weight = 0.;
        for(int i = 0; i < dim; i++) {
          weight += mms[i].getDiff();
        }
        weight = (weight > 0.) ? (dim * StyleLibrary.SCALE) / weight : 1.;

        addCSSClasses(svgp, cpol.getStyleForCluster(clus), baseopacity * weight * ids.size() / relation.size());
        layer.appendChild(path.makeElement(svgp, CLUSTERAREA + cnum));
      }
    }

    /**
     * Adds the required CSS-Classes
     *
     * @param svgp SVG-Plot
     * @param clusterID Cluster ID to style
     * @param opac Opacity
     */
    private void addCSSClasses(SVGPlot svgp, int clusterID, double opac) {
      final StyleLibrary style = context.getStyleLibrary();
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      CSSClass cls = new CSSClass(this, CLUSTERAREA + clusterID);
      final String color = colors.getColor(clusterID);

      // cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY,
      // context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) / 2.0);
      // cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
      cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, opac);

      svgp.addCSSClassOrLogError(cls);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option string to draw straight lines for hull.
     */
    public static final OptionID STRAIGHT_ID = new OptionID("parallel.clusteroutline.straight", "Draw straight lines");

    /**
     * Alpha value
     */
    double alpha = Double.POSITIVE_INFINITY;

    /**
     * Use bend curves
     */
    private boolean bend = true;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter alphaP = new DoubleParameter(ClusterHullVisualization.Parameterizer.ALPHA_ID, Double.POSITIVE_INFINITY);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      Flag bendP = new Flag(STRAIGHT_ID);
      if(config.grab(bendP)) {
        bend = bendP.isFalse();
      }
    }

    @Override
    protected ClusterOutlineVisualization makeInstance() {
      return new ClusterOutlineVisualization(this);
    }
  }
}
