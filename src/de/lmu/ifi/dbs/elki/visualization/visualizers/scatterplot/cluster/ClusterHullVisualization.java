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

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayListIter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
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
    for (Clustering<?> c : clusterings) {
      Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
      for (ScatterPlotProjector<?> p : ps) {
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

      List<Cluster<Model>> clusters = clustering.getAllClusters();
      List<Cluster<Model>> topc = clustering.getToplevelClusters();
      Hierarchy<Cluster<Model>> hier = clustering.getClusterHierarchy();
      boolean flat = (clusters.size() == topc.size());
      // Heuristic value for transparency:
      double baseopacity = flat ? 0.5 : 0.5;

      // TODO: store this, and share across visualizers!
      // Fix a cluster to integer ID mapping:
      TObjectIntMap<Cluster<Model>> cnums = new TObjectIntHashMap<>();
      {
        int cnum = 0;
        for (Cluster<Model> clus : clusters) {
          cnums.put(clus, cnum);
          cnum++;
        }
      }

      // Convex hull mode:
      if (settings.alpha >= Double.POSITIVE_INFINITY) {
        // Build the convex hulls (reusing the hulls of nested clusters!)
        Map<Cluster<Model>, DoubleObjPair<Polygon>> hullmap = new HashMap<>(clusters.size());
        for (Cluster<Model> clu : topc) {
          buildHullsRecursively(clu, hier, hullmap);
        }

        // This way, we draw each cluster only once.
        // Unfortunately, not depth ordered (TODO!)
        for (Cluster<Model> clu : clusters) {
          DoubleObjPair<Polygon> pair = hullmap.get(clu);
          // Plot the convex hull:
          if (pair.second != null && pair.second.size() > 1) {
            SVGPath path = new SVGPath(pair.second);
            // Approximate area (using bounding box)
            double hullarea = SpatialUtil.volume(pair.second);
            final double relativeArea = 1 - (hullarea / projarea);
            final double relativeSize = pair.first / rel.size();
            final double opacity = baseopacity * Math.sqrt(relativeSize * relativeArea);
            addCSSClasses(svgp, cnums.get(clu), opacity);

            Element hulls = path.makeElement(svgp);
            SVGUtil.addCSSClass(hulls, CLUSTERHULL + cnums.get(clu));
            layer.appendChild(hulls);
          }
        }
      } else {
        // Alpha shape mode.
        // For alpha shapes we can't use the shortcut of convex hulls,
        // but have to revisit all child clusters.
        for (Cluster<Model> clu : clusters) {
          ArrayList<Vector> ps = new ArrayList<>();
          double weight = addRecursively(ps, hier, clu);
          List<Polygon> polys;
          if (ps.size() < 1) {
            continue;
          }
          if (ps.size() > 2) {
            polys = (new AlphaShape(ps, settings.alpha * Projection.SCALE)).compute();
          } else {
            // Trivial polygon. Might still degenerate to a single point though.
            polys = new ArrayList<>(1);
            polys.add(new Polygon(ps));
          }
          for (Polygon p : polys) {
            SVGPath path = new SVGPath(p);
            Element hulls = path.makeElement(svgp);
            addCSSClasses(svgp, cnums.get(clu), baseopacity * weight / rel.size());
            SVGUtil.addCSSClass(hulls, CLUSTERHULL + cnums.get(clu));
            layer.appendChild(hulls);
          }
        }
      }
    }

    /**
     * Recursively step through the clusters to build the hulls.
     * 
     * @param clu Current cluster
     * @param hier Clustering hierarchy
     * @param hulls Hull map
     */
    private DoubleObjPair<Polygon> buildHullsRecursively(Cluster<Model> clu, Hierarchy<Cluster<Model>> hier, Map<Cluster<Model>, DoubleObjPair<Polygon>> hulls) {
      GrahamScanConvexHull2D hull = new GrahamScanConvexHull2D();
      final DBIDs ids = clu.getIDs();
      for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double[] projP = proj.fastProjectDataToRenderSpace(rel.get(iter));
        hull.add(new Vector(projP));
      }
      double weight = ids.size();
      if (hier != null && hulls != null) {
        final int numc = hier.numChildren(clu);
        if (numc > 0) {
          for (Iter<Cluster<Model>> iter = hier.iterChildren(clu); iter.valid(); iter.advance()) {
            DoubleObjPair<Polygon> poly = hulls.get(iter.get());
            if (poly == null) {
              poly = buildHullsRecursively(iter.get(), hier, hulls);
            }
            // Add inner convex hull to outer convex hull.
            for (ArrayListIter<Vector> vi = poly.second.iter(); vi.valid(); vi.advance()) {
              hull.add(vi.get());
            }
            weight += poly.first / numc;
          }
        }
      }
      DoubleObjPair<Polygon> pair = new DoubleObjPair<>(weight, hull.getHull());
      hulls.put(clu, pair);
      return pair;
    }

    /**
     * Recursively add a cluster and its children.
     * 
     * @param hull Hull to add to
     * @param hier Cluster hierarchy
     * @param clus Current cluster
     * @return Weight for visualization
     */
    private double addRecursively(ArrayList<Vector> hull, Hierarchy<Cluster<Model>> hier, Cluster<Model> clus) {
      final DBIDs ids = clus.getIDs();
      double weight = ids.size();
      for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double[] projP = proj.fastProjectDataToRenderSpace(rel.get(iter));
        hull.add(new Vector(projP));
      }
      for (Iter<Cluster<Model>> iter = hier.iterChildren(clus); iter.valid(); iter.advance()) {
        weight += .5 * addRecursively(hull, hier, iter.get());
      }
      return weight;
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp, int clusterID, double opac) {
      final StyleLibrary style = context.getStyleResult().getStyleLibrary();
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      CSSClass cls = new CSSClass(this, CLUSTERHULL + clusterID);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));

      final String color;
      if (clustering.getAllClusters().size() == 1) {
        color = "black";
      } else {
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
    public static final OptionID ALPHA_ID = new OptionID("hull.alpha", "Alpha value for hull drawing (in projected space!).");

    /**
     * Alpha value
     */
    double alpha = Double.POSITIVE_INFINITY;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, Double.POSITIVE_INFINITY);
      if (config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }
    }

    @Override
    protected ClusterHullVisualization makeInstance() {
      return new ClusterHullVisualization(this);
    }
  }
}
