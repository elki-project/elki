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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.batik.util.SVGConstants;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.CoreObjectsModel;
import elki.data.model.Model;
import elki.data.spatial.Polygon;
import elki.data.spatial.SpatialUtil;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.math.geometry.AlphaShape;
import elki.math.geometry.FilteredConvexHull2D;
import elki.utilities.datastructures.hierarchy.Hierarchy;
import elki.utilities.datastructures.iterator.ArrayListIter;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.pairs.DoubleObjPair;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.colors.ColorLibrary;
import elki.visualization.css.CSSClass;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.CanvasSize;
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
 * Visualizer for generating an SVG-Element containing the convex hull / alpha
 * shape of each cluster.
 *
 * @author Robert Rödler
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class ClusterHullVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Hull";

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
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    // We attach ourselves to the style library, not the clustering, so there is
    // only one hull.
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      context.addVis(p, new VisualizationTask(this, NAME, p, rel) //
          .level(VisualizationTask.LEVEL_DATA - 1).visibility(false) //
          .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SAMPLE).with(UpdateFlag.ON_STYLEPOLICY));
    });
  }

  /**
   * Instance.
   *
   * @author Robert Rödler
   * @author Erich Schubert
   *
   * @navhas - visualizes - Clustering
   * @assoc - - - GrahamScanConvexHull2D
   * @assoc - - - AlphaShape
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * Generic tags to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String CLUSTERHULL = "cluster-hull";

    /**
     * Constructor
     *
     * @param context Visualizer context
     * @param task Visualization task
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
      final ClusterStylingPolicy cpol = (ClusterStylingPolicy) spol;
      @SuppressWarnings("unchecked")
      Clustering<Model> clustering = (Clustering<Model>) cpol.getClustering();

      // Viewport size, for "relative size" computations
      final CanvasSize viewp = proj.estimateViewport();
      double projarea = viewp.getDiffX() * viewp.getDiffY();

      List<Cluster<Model>> clusters = clustering.getAllClusters();
      List<Cluster<Model>> topc = clustering.getToplevelClusters();
      Hierarchy<Cluster<Model>> hier = clustering.getClusterHierarchy();
      boolean flat = (clusters.size() == topc.size());
      // Heuristic value for transparency:
      double baseopacity = flat ? 0.5 : 0.5;

      // Convex hull mode:
      if(settings.alpha >= Double.POSITIVE_INFINITY) {
        // Build the convex hulls (reusing the hulls of nested clusters!)
        Map<Object, DoubleObjPair<Polygon>> hullmap = new HashMap<>(clusters.size());
        for(Cluster<Model> clu : topc) {
          buildHullsRecursively(clu, hier, hullmap);
        }

        // This way, we draw each cluster only once.
        // Unfortunately, not depth ordered (TODO!)
        for(Cluster<Model> clu : clusters) {
          DoubleObjPair<Polygon> pair = hullmap.get(clu),
              mpair = hullmap.get(clu.getModel());
          // Plot the convex hull:
          if(pair != null && pair.second != null && pair.second.size() > 1) {
            SVGPath path = new SVGPath(pair.second);
            // Approximate area (using bounding box)
            double hullarea = SpatialUtil.volume(pair.second);
            final double relativeArea = 1 - (hullarea / projarea);
            final double relativeSize = pair.first / rel.size();
            final double corefact = (mpair == null) ? 1.0 : .5;
            final double opacity = corefact * baseopacity * Math.sqrt(relativeSize * relativeArea);
            addCSSClasses(svgp, cpol.getStyleForCluster(clu), opacity);
            layer.appendChild(path.makeElement(svgp, CLUSTERHULL + cpol.getStyleForCluster(clu)));
          }
          // For core density models, over-plot the core:
          if(mpair != null && mpair.second != null && mpair.second.size() > 1) {
            SVGPath path = new SVGPath(mpair.second);
            // Approximate area (using bounding box)
            double hullarea = SpatialUtil.volume(mpair.second);
            final double relativeArea = 1 - (hullarea / projarea);
            final double relativeSize = mpair.first / rel.size();
            final double opacity = .5 * baseopacity * Math.sqrt(relativeSize * relativeArea);
            addCSSClasses(svgp, cpol.getStyleForCluster(clu), opacity);
            layer.appendChild(path.makeElement(svgp, CLUSTERHULL + cpol.getStyleForCluster(clu)));
          }
        }
      }
      else {
        // Alpha shape mode.
        // For alpha shapes we can't use the shortcut of convex hulls,
        // but have to revisit all child clusters.
        for(Cluster<Model> clu : clusters) {
          ArrayList<double[]> ps = new ArrayList<>();
          double weight = addRecursively(ps, hier, clu);
          List<Polygon> polys;
          if(ps.isEmpty()) {
            continue;
          }
          if(ps.size() > 2) {
            polys = (new AlphaShape(ps, settings.alpha * Projection.SCALE)).compute();
          }
          else {
            // Trivial polygon. Might still degenerate to a single point though.
            polys = new ArrayList<>(1);
            polys.add(new Polygon(ps));
          }
          for(Polygon p : polys) {
            addCSSClasses(svgp, cpol.getStyleForCluster(clu), baseopacity * weight / rel.size());
            layer.appendChild(new SVGPath(p).makeElement(svgp, CLUSTERHULL + cpol.getStyleForCluster(clu)));
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
    private DoubleObjPair<Polygon> buildHullsRecursively(Cluster<Model> clu, Hierarchy<Cluster<Model>> hier, Map<Object, DoubleObjPair<Polygon>> hulls) {
      final Model model = clu.getModel();
      final DBIDs ids = clu.getIDs();
      boolean coremodel = false;
      DBIDs cids = null;
      if(model instanceof CoreObjectsModel) {
        cids = ((CoreObjectsModel) model).getCoreObjects();
        coremodel = cids.size() > 0;
      }

      FilteredConvexHull2D hull = new FilteredConvexHull2D();
      FilteredConvexHull2D hull2 = coremodel ? new FilteredConvexHull2D() : null;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        final double[] projv = proj.fastProjectDataToRenderSpace(rel.get(iter));
        if(projv[0] != projv[0] || projv[1] != projv[1]) {
          continue; // NaN!
        }
        hull.add(projv);
        if(coremodel && cids.contains(iter)) {
          hull2.add(projv);
        }
      }
      double weight = ids.size(), cweight = coremodel ? cids.size() : 0.0;
      if(hier != null) {
        final int numc = hier.numChildren(clu);
        if(numc > 0) {
          for(It<Cluster<Model>> iter = hier.iterChildren(clu); iter.valid(); iter.advance()) {
            final Cluster<Model> iclu = iter.get();
            DoubleObjPair<Polygon> poly = hulls.get(iclu);
            if(poly == null) {
              poly = buildHullsRecursively(iclu, hier, hulls);
            }
            // Add inner convex hull to outer convex hull.
            for(ArrayListIter<double[]> vi = poly.second.iter(); vi.valid(); vi.advance()) {
              hull.add(vi.get());
            }
            // For a core model, include the inner core, too.
            if(coremodel) {
              DoubleObjPair<Polygon> ipoly = hulls.get(iclu.getModel());
              if(ipoly != null) {
                for(ArrayListIter<double[]> vi = ipoly.second.iter(); vi.valid(); vi.advance()) {
                  hull2.add(vi.get());
                }
                cweight += ipoly.first / numc;
              }
            }
            weight += poly.first / numc;
          }
        }
      }
      DoubleObjPair<Polygon> pair = new DoubleObjPair<>(weight, hull.getHull());
      hulls.put(clu, pair);
      if(coremodel) {
        hulls.put(model, new DoubleObjPair<>(cweight, hull2.getHull()));
      }
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
    private double addRecursively(ArrayList<double[]> hull, Hierarchy<Cluster<Model>> hier, Cluster<Model> clus) {
      final DBIDs ids = clus.getIDs();
      double weight = ids.size();
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double[] projP = proj.fastProjectDataToRenderSpace(rel.get(iter));
        if(projP[0] != projP[0] || projP[1] != projP[1]) {
          continue; // NaN!
        }
        hull.add(projP);
      }
      for(It<Cluster<Model>> iter = hier.iterChildren(clus); iter.valid(); iter.advance()) {
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
      final StyleLibrary style = context.getStyleLibrary();
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      CSSClass cls = new CSSClass(this, CLUSTERHULL + clusterID);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, .5 * style.getLineWidth(StyleLibrary.PLOT));

      final String color = colors.getColor(clusterID);
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
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Alpha-Value for alpha-shapes
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
