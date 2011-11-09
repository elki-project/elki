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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.geometry.AlphaShape;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.CanvasSize;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;

/**
 * Visualizer for generating an SVG-Element containing the convex hull / alpha
 * shape of each cluster.
 * 
 * @author Robert Rödler
 * @author Erich Schubert
 * 
 * @apiviz.has Clustering oneway - - visualizes
 * @apiviz.uses ConvexHull2D
 * @apiviz.uses AlphaShape
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class ClusterHullVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Hull Visualization";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERHULL = "cluster-hull";

  /**
   * The result we work on
   */
  Clustering<Model> clustering;

  /**
   * Alpha value
   */
  double alpha = Double.POSITIVE_INFINITY;

  /**
   * Constructor
   * 
   * @param task VisualizationTask
   * @param alpha Alpha value
   */
  public ClusterHullVisualization(VisualizationTask task, double alpha) {
    super(task);
    this.clustering = task.getResult();
    this.alpha = alpha;
    context.addContextChangeListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    // Viewport size, for "relative size" computations
    final CanvasSize viewp = proj.estimateViewport();
    double projarea = viewp.getDiffX() * viewp.getDiffY();

    double opacity = 0.25;

    Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();

    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<?> clus = ci.next();
      final DBIDs ids = clus.getIDs();

      if(alpha >= Double.POSITIVE_INFINITY) {
        SweepHullDelaunay2D hull = new SweepHullDelaunay2D();

        for(DBID clpnum : ids) {
          double[] projP = proj.fastProjectDataToRenderSpace(rel.get(clpnum));
          hull.add(new Vector(projP));
        }
        Polygon chres = hull.getHull();

        // Plot the convex hull:
        if(chres != null) {
          SVGPath path = new SVGPath(chres);
          // Approximate area (using bounding box)
          double hullarea = SpatialUtil.volume(chres);
          final double relativeArea = (projarea - hullarea) / projarea;
          final double relativeSize = (double) ids.size() / rel.size();
          opacity = Math.sqrt(relativeSize * relativeArea);

          Element hulls = path.makeElement(svgp);
          addCSSClasses(svgp, cnum, opacity);
          SVGUtil.addCSSClass(hulls, CLUSTERHULL + cnum);
          layer.appendChild(hulls);
        }
      }
      else {
        ArrayList<Vector> ps = new ArrayList<Vector>(ids.size());
        for(DBID clpnum : ids) {
          double[] projP = proj.fastProjectDataToRenderSpace(rel.get(clpnum));
          ps.add(new Vector(projP));
        }
        List<Polygon> polys = (new AlphaShape(ps, alpha * Projection.SCALE)).compute();
        for(Polygon p : polys) {
          SVGPath path = new SVGPath(p);
          Element hulls = path.makeElement(svgp);
          addCSSClasses(svgp, cnum, 0.5);
          SVGUtil.addCSSClass(hulls, CLUSTERHULL + cnum);
          layer.appendChild(hulls);
        }
      }
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp, int clusterID, double opac) {
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    CSSClass cls = new CSSClass(this, CLUSTERHULL + clusterID);
    cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));

    final String color;
    if(clustering.getAllClusters().size() == 1) {
      color = "black";
    }
    else {
      color = colors.getColor(clusterID);
    }
    cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
    cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, opac);

    svgp.addCSSClassOrLogError(cls);
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing the convex
   * hull or alpha shape of a cluster.
   * 
   * @author Robert Rödler
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusterHullVisualization oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Alpha value
     */
    double alpha = Double.POSITIVE_INFINITY;

    /**
     * Constructor.
     * 
     * @param alpha Alpha value
     */
    public Factory(double alpha) {
      super();
      this.alpha = alpha;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusterHullVisualization<NV>(task, alpha);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
        for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
          final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
          baseResult.getHierarchy().add(c, task);
          baseResult.getHierarchy().add(p, task);
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
    public static class Parameterizer<NV extends NumberVector<NV, ?>> extends AbstractParameterizer {
      /**
       * Alpha-Value for alpha-shapes
       * 
       * <p>
       * Key: {@code -hull.alpha}
       * </p>
       */
      public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("hull.alpha", "Alpha value for hull drawing (in projected space!).");

      /**
       * Alpha value
       */
      double alpha = Double.POSITIVE_INFINITY;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, Double.POSITIVE_INFINITY);
        if(config.grab(alphaP)) {
          alpha = alphaP.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<NV>(alpha);
      }
    }
  }
}