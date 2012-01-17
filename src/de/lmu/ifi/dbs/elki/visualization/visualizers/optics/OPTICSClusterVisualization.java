package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize the clusters and cluster hierarchy found by OPTICS on the OPTICS
 * Plot.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Clustering oneway - - «visualizes»
 * 
 * @param <D> Distance type (actually unused)
 */
public class OPTICSClusterVisualization<D extends Distance<D>> extends AbstractOPTICSVisualization<D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OPTICSClusterVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Cluster Ranges";

  /**
   * CSS class for markers
   */
  protected static final String CSS_BRACKET = "opticsBracket";

  /**
   * Optics clustering we visualize
   */
  public static final String CLUSTERING = "OPTICSClustering";

  /**
   * Our clustering
   */
  Clustering<OPTICSModel> clus;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public OPTICSClusterVisualization(VisualizationTask task) {
    super(task);
    this.clus = task.getGenerics(CLUSTERING, Clustering.class);
    context.addResultListener(this);
    incrementalRedraw();
  }

  /**
   * Find the first OPTICS clustering child of a result.
   * 
   * @param result Result to start searching at
   * @return OPTICS clustering
   */
  @SuppressWarnings("unchecked")
  protected static Clustering<OPTICSModel> findOPTICSClustering(Result result) {
    Iterator<Clustering<?>> cs = ResultUtil.filteredResults(result, Clustering.class);
    while (cs.hasNext()) {
      Clustering<?> clus = cs.next();
      if(clus.getToplevelClusters().size() == 0) {
        continue;
      }
      try {
        Cluster<?> firstcluster = clus.getToplevelClusters().iterator().next();
        if(firstcluster.getModel() instanceof OPTICSModel) {
          return (Clustering<OPTICSModel>) clus;
        }
      } catch(Exception e) {
        // Empty clustering? Shouldn't happen.
        logger.warning("Clustering with no cluster detected.", e);
      }
    }
    return null;
  }

  @Override
  protected void redraw() {
    makeLayerElement();
    addCSSClasses();
    drawClusters(clus.getToplevelClusters(), 1);
  }

  /**
   * Recursively draw clusters
   * 
   * @param clusters Current set of clusters
   * @param depth Recursion depth
   */
  private void drawClusters(List<Cluster<OPTICSModel>> clusters, int depth) {
    final double scale = StyleLibrary.SCALE;
    for(Cluster<OPTICSModel> cluster : clusters) {
      try {
        OPTICSModel model = cluster.getModel();
        final double x1 = plotwidth * ((model.getStartIndex() + .25) / this.optics.getResult().getClusterOrder().size());
        final double x2 = plotwidth * ((model.getEndIndex() + .75) / this.optics.getResult().getClusterOrder().size());
        final double y = plotheight + depth * scale * 0.01;
        Element e = svgp.svgLine(x1, y, x2, y);
        SVGUtil.addCSSClass(e, CSS_BRACKET);
        layer.appendChild(e);
      }
      catch(ClassCastException e) {
        logger.warning("Expected OPTICSModel, got: " + cluster.getModel().getClass().getSimpleName());
      }
      // Descend
      final List<Cluster<OPTICSModel>> children = cluster.getChildren();
      if(children != null) {
        drawClusters(children, depth + 1);
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
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getStyleLibrary().getColor(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(cls);
    }
  }

  /**
   * Factory class for OPTICS plot selections.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses OPTICSPlotSelectionVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Iterator<OPTICSProjector<?>> ops = ResultUtil.filteredResults(result, OPTICSProjector.class);
      for(OPTICSProjector<?> p : IterableUtil.fromIterator(ops)) {
        final Clustering<OPTICSModel> ocl = findOPTICSClustering(baseResult);
        if(ocl != null) {
          final VisualizationTask task = new VisualizationTask(NAME, p, null, this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
          task.put(CLUSTERING, ocl);
          baseResult.getHierarchy().add(p, task);
        }
      }
      // TODO: also run when a new clustering is added, instead of just new projections?
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new OPTICSClusterVisualization<DoubleDistance>(task);
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}