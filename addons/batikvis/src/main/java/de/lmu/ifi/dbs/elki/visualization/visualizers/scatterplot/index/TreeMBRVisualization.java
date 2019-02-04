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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.index;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize the bounding rectangles of an R-Tree based index.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class TreeMBRVisualization implements VisFactory {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String INDEX = "index";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Index MBRs";

  /**
   * Settings
   */
  protected Parameterizer settings;

  /**
   * Constructor.
   *
   * @param settings Settings
   */
  public TreeMBRVisualization(Parameterizer settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance<RStarTreeNode, SpatialEntry>(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewSiblings(context, start, AbstractRStarTree.class, ScatterPlotProjector.class, (tree, p) -> {
      final VisualizationTask task = new VisualizationTask(this, NAME, (Result) tree, p.getRelation()) //
          .level(VisualizationTask.LEVEL_BACKGROUND + 1).visibility(false);
      context.addVis(tree, task);
      context.addVis(p, task);
    });
  }

  /**
   * Instance for a particular tree
   *
   * @author Erich Schubert
   *
   * @navhas - visualizes - AbstractRStarTree
   * @assoc - - - SVGHyperCube
   *
   * @param <N> Tree node type
   * @param <E> Tree entry type
   */
  // TODO: listen for tree changes instead of data changes?
  public class Instance<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * The tree we visualize
     */
    protected AbstractRStarTree<N, E, ?> tree;

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
    @SuppressWarnings("unchecked")
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      this.tree = AbstractRStarTree.class.cast(task.getResult());
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
      int projdim = BitsUtil.cardinality(proj.getVisibleDimensions2D());
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      if(tree != null) {
        E root = tree.getRootEntry();
        for(int i = 0; i < tree.getHeight(); i++) {
          CSSClass cls = new CSSClass(this, INDEX + i);
          // Relative depth of this level. 1.0 = toplevel
          final double relDepth = 1. - (((double) i) / tree.getHeight());
          if(settings.fill) {
            cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
            cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * style.getLineWidth(StyleLibrary.PLOT));
            cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(i));
            cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.1 / (projdim - 1));
          }
          else {
            cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
            cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * style.getLineWidth(StyleLibrary.PLOT));
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
    private void visualizeRTreeEntry(SVGPlot svgp, Element layer, Projection2D proj, AbstractRStarTree<? extends N, E, ?> rtree, E entry, int depth) {
      SpatialComparable mbr = entry;

      if(settings.fill) {
        Element r = SVGHyperCube.drawFilled(svgp, INDEX + depth, proj, mbr);
        layer.appendChild(r);
      }
      else {
        Element r = SVGHyperCube.drawFrame(svgp, proj, mbr);
        SVGUtil.setCSSClass(r, INDEX + depth);
        layer.appendChild(r);
      }

      if(!(entry instanceof LeafEntry)) {
        N node = rtree.getNode(entry);
        for(int i = 0; i < node.getNumEntries(); i++) {
          E child = node.getEntry(i);
          if(!(child instanceof LeafEntry)) {
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
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Flag for half-transparent filling of bubbles.
     */
    public static final OptionID FILL_ID = new OptionID("index.fill", "Partially transparent filling of index pages.");

    protected boolean fill = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag fillF = new Flag(FILL_ID);
      if(config.grab(fillF)) {
        fill = fillF.isTrue();
      }
    }

    @Override
    protected TreeMBRVisualization makeInstance() {
      return new TreeMBRVisualization(this);
    }
  }
}
