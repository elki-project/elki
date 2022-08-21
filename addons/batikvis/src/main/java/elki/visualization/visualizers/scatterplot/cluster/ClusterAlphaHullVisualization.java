/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
import java.util.Arrays;
import java.util.List;

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
import elki.utilities.datastructures.hierarchy.Hierarchy;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
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
 * Visualizer generating the alpha shape of each cluster.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class ClusterAlphaHullVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Alpha Shape";

  /**
   * Alpha parameter
   */
  double alpha;

  /**
   * Constructor.
   *
   * @param alpha Alpha parameter
   */
  public ClusterAlphaHullVisualization(double alpha) {
    super();
    this.alpha = alpha;
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    // We attach ourselves to the style library, not the clustering, so there is
    // only one hull at a time.
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
   * @author Erich Schubert
   *
   * @navhas - visualizes - Clustering
   * @assoc - - - AlphaShape
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * Prefix for IDs, CSS classes etc.
     */
    public static final String CLUSTERHULL = "alpha-shape";

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
      Hierarchy<Cluster<Model>> hier = clustering.getClusterHierarchy();
      // For alpha shapes we can't use the shortcut of convex hulls,
      // but have to revisit all child clusters.
      for(Cluster<Model> clu : clusters) {
        ArrayList<double[]> ps = new ArrayList<>();
        double weight = addRecursively(ps, hier, clu);
        if(ps.isEmpty()) {
          continue;
        }
        List<Polygon> polys = ps.size() < 2 ? Arrays.asList(new Polygon[] { new Polygon(ps) }) : //
            (new AlphaShape(ps, alpha * Projection.SCALE)).compute();
        double hullarea = 0.;
        for(Polygon p : polys) {
          hullarea += SpatialUtil.volume(p);
        }
        final double opacity = 0.5 * Math.sqrt((weight / rel.size()) * (1 - (hullarea / projarea)));
        addCSSClasses(svgp, cpol.getStyleForCluster(clu), opacity);
        SVGPath path = new SVGPath();
        for(Polygon p : polys) {
          path.moveTo(p.get(0));
          for(int i = 1; i < p.size(); i++) {
            path.drawTo(p.get(i));
          }
          path.close();
        }
        layer.appendChild(path.makeElement(svgp, CLUSTERHULL + cpol.getStyleForCluster(clu)));
      }
    }

    /**
     * Recursively add a cluster and its children for alpha shapes.
     *
     * @param hull Hull to add to
     * @param hier Cluster hierarchy
     * @param clus Current cluster
     * @return Weight for visualization
     */
    private double addRecursively(ArrayList<double[]> hull, Hierarchy<Cluster<Model>> hier, Cluster<Model> clus) {
      final DBIDs ids = clus.getIDs();
      final Model model = clus.getModel();
      DBIDs cids = null;
      if(model instanceof CoreObjectsModel) {
        cids = ((CoreObjectsModel) model).getCoreObjects();
        cids = cids.isEmpty() ? null : cids;
      }
      double weight = ids.size();
      for(DBIDIter iter = cids != null ? cids.iter() : ids.iter(); iter.valid(); iter.advance()) {
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
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 2 * Math.abs(alpha) * Projection.SCALE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, opac);
      cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, .5);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
      cls.setStatement(SVGConstants.CSS_FILL_RULE_PROPERTY, SVGConstants.SVG_EVEN_ODD_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Alpha-Value for alpha-shapes
     */
    public static final OptionID ALPHA_ID = new OptionID("hull.alpha", "Alpha value for hull drawing (in projected space!).");

    /**
     * Alpha value
     */
    double alpha = Double.POSITIVE_INFINITY;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(ALPHA_ID, 0.05) //
          .grab(config, x -> alpha = x);
    }

    @Override
    public ClusterAlphaHullVisualization make() {
      return new ClusterAlphaHullVisualization(alpha);
    }
  }
}
