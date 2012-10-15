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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.geometry.AlphaShape;
import de.lmu.ifi.dbs.elki.math.geometry.GrahamScanConvexHull2D;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
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
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualizer for generating an SVG-Element containing the convex hull / alpha
 * shape of each cluster.
 * 
 * @author Robert Rödler
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class ClusterHullVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Hull Visualization";

  /**
   * Settings
   */
  Parameterizer settings;

  /**
   * Constructor.
   * 
   * @param settings Settings
   */
  public ClusterHullVisualization(Parameterizer settings) {
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
      Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
      for(ScatterPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
        task.level = VisualizationTask.LEVEL_DATA - 1;
        task.initDefaultVisibility(false);
        baseResult.getHierarchy().add(c, task);
        baseResult.getHierarchy().add(p, task);
      }
    }
  }

  /**
   * Instance.
   * 
   * @author Robert Rödler
   * @author Erich Schubert
   * 
   * @apiviz.has Clustering oneway - - visualizes
   * @apiviz.uses GrahamScanConvexHull2D
   * @apiviz.uses AlphaShape
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * Generic tags to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String CLUSTERHULL = "cluster-hull";

    /**
     * The result we work on
     */
    Clustering<Model> clustering;

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
      // Viewport size, for "relative size" computations
      final CanvasSize viewp = proj.estimateViewport();
      double projarea = viewp.getDiffX() * viewp.getDiffY();

      double opacity = 0.25;

      Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();

      for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
        Cluster<?> clus = ci.next();
        final DBIDs ids = clus.getIDs();

        if(settings.alpha >= Double.POSITIVE_INFINITY) {
          GrahamScanConvexHull2D hull = new GrahamScanConvexHull2D();

          for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
            double[] projP = proj.fastProjectDataToRenderSpace(rel.get(iter));
            hull.add(new Vector(projP));
          }
          Polygon chres = hull.getHull();

          // Plot the convex hull:
          if(chres != null && chres.size() > 1) {
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
          for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
            double[] projP = proj.fastProjectDataToRenderSpace(rel.get(iter));
            ps.add(new Vector(projP));
          }
          List<Polygon> polys = (new AlphaShape(ps, settings.alpha * Projection.SCALE)).compute();
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
        alpha = alphaP.doubleValue();
      }
    }

    @Override
    protected ClusterHullVisualization makeInstance() {
      return new ClusterHullVisualization(this);
    }
  }
}