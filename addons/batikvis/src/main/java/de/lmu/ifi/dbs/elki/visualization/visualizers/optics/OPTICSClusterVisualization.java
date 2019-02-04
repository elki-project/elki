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
package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

import java.util.HashMap;
import java.util.Map;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize the clusters and cluster hierarchy found by OPTICS on the OPTICS
 * Plot.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class OPTICSClusterVisualization implements VisFactory {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICSClusterVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Cluster Ranges";

  /**
   * Constructor.
   */
  public OPTICSClusterVisualization() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findVis(context, result).filter(OPTICSProjector.class).forEach(p -> {
      // Child results, once OPTICSXi runs after the actual OPTICS algorithm.
      VisualizationTree.findNewResults(context, p.getResult()).filter(Clustering.class).forEach(clus -> {
        if(clus.getToplevelClusters().size() == 0) {
          return;
        }
        try {
          Cluster<?> firstcluster = ((Clustering<?>) clus).getToplevelClusters().iterator().next();
          if(firstcluster.getModel() instanceof OPTICSModel) {
            context.addVis(p, new VisualizationTask(this, NAME, clus, null) //
                .level(VisualizationTask.LEVEL_DATA));
            // TODO: use and react to style policy!
          }
        }
        catch(Exception e) {
          // Empty clustering? Shouldn't happen.
          LOG.warning("Clustering with no cluster detected.", e);
        }
      });
      // Also check parents, the current default behavior of OPTICSXi.
      context.getHierarchy().iterAncestors(p.getResult()).filter(Clustering.class).forEach(clus -> {
        if(clus.getToplevelClusters().size() == 0) {
          return;
        }
        try {
          Cluster<?> firstcluster = ((Clustering<?>) clus).getToplevelClusters().iterator().next();
          if(firstcluster.getModel() instanceof OPTICSModel) {
            context.addVis(p, new VisualizationTask(this, NAME, clus, null) //
                .level(VisualizationTask.LEVEL_DATA));
            // TODO: use and react to style policy!
          }
        }
        catch(Exception e) {
          // Empty clustering? Shouldn't happen.
          LOG.warning("Clustering with no cluster detected.", e);
        }
      });
    });
    // TODO: run when a new clustering is added, instead of projections?
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance.
   *
   * @author Erich Schubert
   *
   * @navassoc - visualizes - Clustering
   */
  public class Instance extends AbstractOPTICSVisualization {
    /**
     * CSS class for markers
     */
    protected static final String CSS_BRACKET = "opticsBracket";

    /**
     * Our clustering
     */
    Clustering<OPTICSModel> clus;

    /**
     * Constructor.
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
      this.clus = task.getResult();
      addListeners();
    }

    @Override
    public void fullRedraw() {
      makeLayerElement();
      addCSSClasses();

      ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
      HashMap<Cluster<?>, String> colormap = new HashMap<>();
      int cnum = 0;
      for(Cluster<?> c : clus.getAllClusters()) {
        colormap.put(c, colors.getColor(cnum));
        cnum++;
      }
      drawClusters(clus, clus.iterToplevelClusters(), 1, colormap);
    }

    /**
     * Recursively draw clusters
     *
     * @param clusters Current set of clusters
     * @param depth Recursion depth
     * @param colormap Color mapping
     */
    private void drawClusters(Clustering<OPTICSModel> clustering, It<Cluster<OPTICSModel>> clusters, int depth, Map<Cluster<?>, String> colormap) {
      final double scale = StyleLibrary.SCALE;

      for(; clusters.valid(); clusters.advance()) {
        Cluster<OPTICSModel> cluster = clusters.get();
        try {
          OPTICSModel model = cluster.getModel();
          final double x1 = plotwidth * ((model.getStartIndex() + .25) / this.optics.getResult().size());
          final double x2 = plotwidth * ((model.getEndIndex() + .75) / this.optics.getResult().size());
          final double y = plotheight + depth * scale * 0.01;
          Element e = svgp.svgLine(x1, y, x2, y);
          SVGUtil.addCSSClass(e, CSS_BRACKET);
          String color = colormap.get(cluster);
          if(color != null) {
            SVGUtil.setAtt(e, SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_STROKE_PROPERTY + ":" + color);
          }
          layer.appendChild(e);
        }
        catch(ClassCastException e) {
          LOG.warning("Expected OPTICSModel, got: " + cluster.getModel().getClass().getSimpleName());
        }
        // Descend
        final It<Cluster<OPTICSModel>> children = clustering.getClusterHierarchy().iterChildren(cluster);
        if(children != null) {
          drawClusters(clustering, children, depth + 1, colormap);
        }
      }
    }

    /**
     * Adds the required CSS-Classes
     */
    private void addCSSClasses() {
      // Class for the markers
      if(!svgp.getCSSClassManager().contains(CSS_BRACKET)) {
        final CSSClass cls = new CSSClass(this, CSS_BRACKET);
        final StyleLibrary style = context.getStyleLibrary();
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, style.getColor(StyleLibrary.PLOT));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
        svgp.addCSSClassOrLogError(cls);
      }
    }
  }
}
