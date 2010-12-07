package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree.MTreeNode;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize the bounding sphere of a metric index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has AbstractMTree oneway - - visualizes
 * @apiviz.uses SVGHyperSphere
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 * @param <N> Tree node type
 * @param <E> Tree entry type
 */
// TODO: listen for tree changes!
public class TreeSphereVisualization<NV extends NumberVector<NV, ?>, D extends NumberDistance<D, ?>, N extends AbstractMTreeNode<NV, D, N, E>, E extends MTreeEntry<D>> extends P2DVisualization<NV> implements DataStoreListener<NV> {
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
   * 
   * @apiviz.exclude
   */
  private enum Modus {
    MANHATTAN, EUCLIDEAN, LPCROSS
  }

  protected double p;

  /**
   * Drawing mode (distance) to use
   */
  protected Modus dist = Modus.LPCROSS;

  /**
   * The tree we visualize
   */
  protected AbstractMTree<NV, D, N, E> tree;

  /**
   * Fill parameter.
   */
  protected boolean fill = false;

  public TreeSphereVisualization(VisualizationTask task, boolean fill) {
    super(task, VisFactory.LEVEL_BACKGROUND);
    this.tree = task.getResult();
    this.p = getLPNormP(this.tree);
    this.fill = fill;
    incrementalRedraw();
    context.addDataStoreListener(this);
  }

  /**
   * Get the "p" value of an Lp norm.
   * 
   * @param tree Tree to visualize
   * @return p value
   */
  public static Double getLPNormP(AbstractMTree<?, ?, ?, ?> tree) {
    // Note: we deliberately lose generics here, so the compilers complain less
    // on the next typecheck and cast!
    DistanceFunction<?, ?> distanceFunction = tree.getDistanceQuery().getDistanceFunction();
    if(LPNormDistanceFunction.class.isInstance(distanceFunction)) {
      return ((LPNormDistanceFunction) distanceFunction).getP();
    }
    return null;
  }

  /**
   * Test for a visualizable index in the context's database.
   * 
   * @param tree Tree to visualize
   * @return whether the tree is visualizable
   */
  public static boolean canVisualize(AbstractMTree<?, ?, ?, ?> tree) {
    Double p = getLPNormP(tree);
    return (p != null);
  }

  @Override
  protected void redraw() {
    int projdim = proj.getVisibleDimensions2D().cardinality();
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    p = getLPNormP(tree);
    if(tree != null) {
      if(ManhattanDistanceFunction.class.isInstance(tree.getDistanceQuery())) {
        dist = Modus.MANHATTAN;
      }
      else if(EuclideanDistanceFunction.class.isInstance(tree.getDistanceQuery())) {
        dist = Modus.EUCLIDEAN;
      }
      else {
        dist = Modus.LPCROSS;
      }
      E root = tree.getRootEntry();
      final int mtheight = tree.getHeight();
      for(int i = 0; i < mtheight; i++) {
        CSSClass cls = new CSSClass(this, INDEX + i);
        // Relative depth of this level. 1.0 = toplevel
        final double relDepth = 1. - (((double) i) / mtheight);
        if(fill) {
          cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
          cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(i));
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.1 / (projdim - 1));
          cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
          cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        }
        else {
          cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
          cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
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
  private void visualizeMTreeEntry(SVGPlot svgp, Element layer, Projection2D proj, AbstractMTree<NV, D, ? extends N, E> mtree, E entry, int depth) {
    Database<? extends NV> database = context.getDatabase();
    DBID roid = entry.getRoutingObjectID();
    if(roid != null) {
      NV ro = database.get(roid);
      D rad = entry.getCoveringRadius();

      final Element r;
      if(dist == Modus.MANHATTAN) {
        r = SVGHyperSphere.drawManhattan(svgp, proj, ro, rad);
      }
      else if(dist == Modus.EUCLIDEAN) {
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

    if(!entry.isLeafEntry()) {
      N node = mtree.getNode(entry);
      for(int i = 0; i < node.getNumEntries(); i++) {
        E child = node.getEntry(i);
        if(!child.isLeafEntry()) {
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

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<NV> e) {
    synchronizedRedraw();
  }

  /**
   * Factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses TreeSphereVisualization oneway - - «create»
   * 
   * @param <NV>
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
    /**
     * OptionID for {@link #FILL_FLAG}.
     */
    public static final OptionID FILL_ID = OptionID.getOrCreateOptionID("index.fill", "Partially transparent filling of index pages.");

    /**
     * Flag for half-transparent filling of bubbles.
     * 
     * <p>
     * Key: {@code -index.fill}
     * </p>
     */
    private final Flag FILL_FLAG = new Flag(FILL_ID);

    /**
     * Fill parameter.
     */
    protected boolean fill = false;

    /**
     * The default constructor only registers parameters.
     * 
     * @param config Parameters
     */
    public Factory(Parameterization config) {
      super(NAME, VisFactory.LEVEL_BACKGROUND + 1);
      config = config.descend(this);
      if(config.grab(FILL_FLAG)) {
        fill = FILL_FLAG.getValue();
      }
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      // FIXME: check for vector spaces - essentially, we need a "good" database, too!
      ArrayList<AbstractMTree<NV, DoubleDistance, MTreeNode<NV, DoubleDistance>, MTreeEntry<DoubleDistance>>> trees = ResultUtil.filterResults(result, AbstractMTree.class);
      for(AbstractMTree<NV, DoubleDistance, MTreeNode<NV, DoubleDistance>, MTreeEntry<DoubleDistance>> tree : trees) {
        if(canVisualize(tree)) {
          context.addVisualizer(tree, new VisualizationTask(context, tree, this));
        }
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new TreeSphereVisualization<NV, DoubleDistance, MTreeNode<NV, DoubleDistance>, MTreeEntry<DoubleDistance>>(task, fill);
    }
  }
}