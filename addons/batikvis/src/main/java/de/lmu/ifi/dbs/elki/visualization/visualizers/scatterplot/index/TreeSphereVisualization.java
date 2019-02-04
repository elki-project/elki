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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree.MTreeNode;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize the bounding sphere of a metric index.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @stereotype factory
 * @composed - - - Mode
 * @navassoc - create - Instance
 */
public class TreeSphereVisualization implements VisFactory {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String INDEX = "index";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Index Spheres";

  /**
   * Drawing modes.
   */
  private enum Mode {
    MANHATTAN, EUCLIDEAN, LPCROSS
  }

  /**
   * Settings
   */
  protected Parameterizer settings;

  /**
   * Constructor.
   *
   * @param settings Settings
   */
  public TreeSphereVisualization(Parameterizer settings) {
    super();
    this.settings = settings;
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewSiblings(context, start, AbstractMTree.class, ScatterPlotProjector.class, (tree, p) -> {
      Relation<?> rel = p.getRelation();
      if(!canVisualize(rel, tree)) {
        return;
      }
      final VisualizationTask task = new VisualizationTask(this, NAME, tree, rel) //
          .level(VisualizationTask.LEVEL_BACKGROUND + 1).visibility(false);
      context.addVis(tree, task);
      context.addVis(p, task);
    });
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance<MTreeNode<Object>, MTreeEntry>(context, task, plot, width, height, proj);
  }

  /**
   * Get the "p" value of an Lp norm.
   *
   * @param tree Tree to visualize
   * @return p value
   */
  public static double getLPNormP(AbstractMTree<?, ?, ?, ?> tree) {
    // Note: we deliberately lose generics here, so the compilers complain
    // less on the next typecheck and cast!
    DistanceFunction<?> distanceFunction = tree.getDistanceFunction();
    if(LPNormDistanceFunction.class.isInstance(distanceFunction)) {
      return ((LPNormDistanceFunction) distanceFunction).getP();
    }
    return 0;
  }

  /**
   * Test for a visualizable index in the context's database.
   *
   * @param rel Vector relation
   * @param tree Tree to visualize
   * @return whether the tree is visualizable
   */
  public static boolean canVisualize(Relation<?> rel, AbstractMTree<?, ?, ?, ?> tree) {
    if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
      return false;
    }
    return getLPNormP(tree) > 0;
  }

  /**
   * Instance for a particular tree.
   *
   * @author Erich Schubert
   *
   * @navhas - visualizes - AbstractMTree
   * @assoc - - - SVGHyperSphere
   *
   * @param <N> Tree node type
   * @param <E> Tree entry type
   */
  // TODO: listen for tree changes!
  public class Instance<N extends AbstractMTreeNode<?, N, E>, E extends MTreeEntry> extends AbstractScatterplotVisualization implements DataStoreListener {
    protected double p;

    /**
     * Drawing mode (distance) to use
     */
    protected Mode dist = Mode.LPCROSS;

    /**
     * The tree we visualize
     */
    protected AbstractMTree<?, N, E, ?> tree;

    /**
     * Constructor
     *
     * @param context Visualizer context
     * @param task Task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    @SuppressWarnings("unchecked")
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      this.tree = AbstractMTree.class.cast(task.getResult());
      this.p = getLPNormP(this.tree);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
      int projdim = BitsUtil.cardinality(proj.getVisibleDimensions2D());
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      p = getLPNormP(tree);
      if(tree != null) {
        if(ManhattanDistanceFunction.class.isInstance(tree.getDistanceFunction())) {
          dist = Mode.MANHATTAN;
        }
        else if(EuclideanDistanceFunction.class.isInstance(tree.getDistanceFunction())) {
          dist = Mode.EUCLIDEAN;
        }
        else {
          dist = Mode.LPCROSS;
        }
        E root = tree.getRootEntry();
        final int mtheight = tree.getHeight();
        for(int i = 0; i < mtheight; i++) {
          CSSClass cls = new CSSClass(this, INDEX + i);
          // Relative depth of this level. 1.0 = toplevel
          final double relDepth = 1. - (((double) i) / mtheight);
          if(settings.fill) {
            cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
            cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * style.getLineWidth(StyleLibrary.PLOT));
            cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(i));
            cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.1 / (projdim - 1));
            cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
            cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
          }
          else {
            cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
            cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * style.getLineWidth(StyleLibrary.PLOT));
            cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
            cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
            cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
          }
          svgp.addCSSClassOrLogError(cls);
        }
        visualizeMTreeEntry(svgp, this.layer, proj, tree, root, 0);
      }
    }

    /**
     * Recursively draw the MBR rectangles.
     *
     * @param svgp SVG Plot
     * @param layer Layer
     * @param proj Projection
     * @param mtree Mtree to visualize
     * @param entry Current entry
     * @param depth Current depth
     */
    private void visualizeMTreeEntry(SVGPlot svgp, Element layer, Projection2D proj, AbstractMTree<?, N, E, ?> mtree, E entry, int depth) {
      DBID roid = entry.getRoutingObjectID();
      if(roid != null) {
        NumberVector ro = rel.get(roid);
        double rad = entry.getCoveringRadius();

        final Element r;
        if(dist == Mode.MANHATTAN) {
          r = SVGHyperSphere.drawManhattan(svgp, proj, ro, rad);
        }
        else if(dist == Mode.EUCLIDEAN) {
          r = SVGHyperSphere.drawEuclidean(svgp, proj, ro, rad);
        }
        // TODO: add visualizer for infinity norm?
        else {
          // r = SVGHyperSphere.drawCross(svgp, proj, ro, rad);
          r = SVGHyperSphere.drawLp(svgp, proj, ro, rad, p);
        }
        SVGUtil.setCSSClass(r, INDEX + (depth - 1));
        layer.appendChild(r);
      }

      if(!(entry instanceof LeafEntry)) {
        N node = mtree.getNode(entry);
        for(int i = 0; i < node.getNumEntries(); i++) {
          E child = node.getEntry(i);
          if(!(child instanceof LeafEntry)) {
            visualizeMTreeEntry(svgp, layer, proj, mtree, child, depth + 1);
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
    protected boolean fill = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag fillF = new Flag(TreeMBRVisualization.Parameterizer.FILL_ID);
      if(config.grab(fillF)) {
        fill = fillF.isTrue();
      }
    }

    @Override
    protected TreeSphereVisualization makeInstance() {
      return new TreeSphereVisualization(this);
    }
  }
}
