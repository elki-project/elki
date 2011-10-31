package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.index;

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
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;

/**
 * Visualize the bounding rectangles of an R-Tree based index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has AbstractRStarTree oneway - - visualizes
 * @apiviz.uses SVGHyperCube
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 * @param <N> Tree node type
 * @param <E> Tree entry type
 */
// TODO: listen for tree changes instead of data changes?
public class TreeMBRVisualization<NV extends NumberVector<NV, ?>, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends P2DVisualization<NV> implements DataStoreListener {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String INDEX = "index";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Index MBRs";

  /**
   * Fill parameter.
   */
  protected boolean fill = false;

  /**
   * The tree we visualize
   */
  protected AbstractRStarTree<N, E> tree;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   * @param fill Fill flag
   */
  @SuppressWarnings("unchecked")
  public TreeMBRVisualization(VisualizationTask task, boolean fill) {
    super(task);
    this.tree = AbstractRStarTree.class.cast(task.getResult());
    this.fill = fill;
    incrementalRedraw();
    context.addDataStoreListener(this);
  }

  @Override
  protected void redraw() {
    int projdim = proj.getVisibleDimensions2D().cardinality();
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    if(tree != null) {
      E root = tree.getRootEntry();
      for(int i = 0; i < tree.getHeight(); i++) {
        CSSClass cls = new CSSClass(this, INDEX + i);
        // Relative depth of this level. 1.0 = toplevel
        final double relDepth = 1. - (((double) i) / tree.getHeight());
        if(fill) {
          cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
          cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(i));
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.1 / (projdim - 1));
        }
        else {
          cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
          cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        }
        cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
      visualizeRTreeEntry(svgp, layer, proj, tree, root, 0);
    }
  }

  /**
   * Recursively draw the MBR rectangles.
   * 
   * @param svgp SVG Plot
   * @param layer Layer
   * @param proj Projection
   * @param rtree Rtree to visualize
   * @param entry Current entry
   * @param depth Current depth
   */
  private void visualizeRTreeEntry(SVGPlot svgp, Element layer, Projection2D proj, AbstractRStarTree<? extends N, E> rtree, E entry, int depth) {
    SpatialComparable mbr = entry;

    if(fill) {
      Element r = SVGHyperCube.drawFilled(svgp, INDEX + depth, proj, new Vector(SpatialUtil.getMin(mbr)), new Vector(SpatialUtil.getMax(mbr)));
      layer.appendChild(r);
    }
    else {
      Element r = SVGHyperCube.drawFrame(svgp, proj, new Vector(SpatialUtil.getMin(mbr)), new Vector(SpatialUtil.getMax(mbr)));
      SVGUtil.setCSSClass(r, INDEX + depth);
      layer.appendChild(r);
    }

    if(!entry.isLeafEntry()) {
      N node = rtree.getNode(entry);
      for(int i = 0; i < node.getNumEntries(); i++) {
        E child = node.getEntry(i);
        if(!child.isLeafEntry()) {
          visualizeRTreeEntry(svgp, layer, proj, rtree, child, depth + 1);
        }
      }
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDataStoreListener(this);
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * Factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses TreeMBRVisualization oneway - - «create»
   * 
   * @param <NV> vector type
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Flag for half-transparent filling of bubbles.
     * 
     * <p>
     * Key: {@code -index.fill}
     * </p>
     */
    public static final OptionID FILL_ID = OptionID.getOrCreateOptionID("index.fill", "Partially transparent filling of index pages.");

    /**
     * Fill parameter.
     */
    protected boolean fill = false;

    /**
     * Constructor.
     * 
     * @param fill
     */
    public Factory(boolean fill) {
      super();
      this.fill = fill;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new TreeMBRVisualization<NV, RStarTreeNode, SpatialEntry>(task, fill);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      ArrayList<AbstractRStarTree<RStarTreeNode, SpatialEntry>> trees = ResultUtil.filterResults(result, AbstractRStarTree.class);
      for(AbstractRStarTree<RStarTreeNode, SpatialEntry> tree : trees) {
        if(tree instanceof Result) {
          Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
          for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
            final VisualizationTask task = new VisualizationTask(NAME, (Result) tree, p.getRelation(), this);
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_BACKGROUND + 1);
            baseResult.getHierarchy().add((Result) tree, task);
            baseResult.getHierarchy().add(p, task);
          }
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
      protected boolean fill = false;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        Flag fillF = new Flag(FILL_ID);
        if(config.grab(fillF)) {
          fill = fillF.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<NV>(fill);
      }
    }
  }
}