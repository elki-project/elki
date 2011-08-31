package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

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
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.ConvexHull2D;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualizer for generating an SVG-Element containing the convex hull of each
 * cluster.
 * 
 * @author Robert Rödler
 * 
 * @apiviz.has Clustering oneway - - visualizes
 * @apiviz.uses ConvexHull2D
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class ClusterConvexHullVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Convex Hull Visualization";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CONVEXHULL = "convexHull";

  /**
   * The result we work on
   */
  Clustering<Model> clustering;

  /**
   * The hulls
   */
  Element hulls;

  /**
   * Constructor
   * 
   * @param task VisualizationTask
   */
  public ClusterConvexHullVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
    context.addContextChangeListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    // Viewport size, for "relative size" computations
    final Pair<DoubleMinMax, DoubleMinMax> viewp = proj.estimateViewport();
    double projarea = (viewp.getFirst().getDiff()) * (viewp.getSecond().getDiff());

    double opacity = 0.25;

    Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();

    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<?> clus = ci.next();

      final DBIDs ids = clus.getIDs();
      ConvexHull2D hull = new ConvexHull2D();

      for(DBID clpnum : ids) {
        double[] projP = proj.fastProjectDataToRenderSpace(rep.get(clpnum).getColumnVector());
        hull.add(new Vector(projP));
      }
      Polygon chres = hull.getHull();

      // Plot the convex hull:
      if(chres != null) {
        SVGPath path = new SVGPath(chres);
        // Approximate area (using bounding box)
        double hullarea = SpatialUtil.volume(chres);
        final double relativeArea = (projarea - hullarea) / projarea;
        final double relativeSize = (double) ids.size() / rep.size();
        opacity = Math.sqrt(relativeSize * relativeArea);

        hulls = path.makeElement(svgp);
        addCSSClasses(svgp, cnum, opacity);
        SVGUtil.addCSSClass(hulls, CONVEXHULL + cnum);
        layer.appendChild(hulls);
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

    CSSClass cls = new CSSClass(this, CONVEXHULL + clusterID);
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
   * hull of a cluster.
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusterConvexHullVisualization oneway - - «create»
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
      return new ClusterConvexHullVisualization<NV>(task);
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
  }
}