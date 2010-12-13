package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

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
public class TreeMBRVisualization<NV extends NumberVector<NV, ?>, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends P2DVisualization<NV> implements DataStoreListener<NV> {
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
  protected AbstractRStarTree<NV, N, E> rtree;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   * @param fill Fill flag
   */
  public TreeMBRVisualization(VisualizationTask task, boolean fill) {
    super(task);
    this.rtree = task.getResult();
    this.fill = fill;
    incrementalRedraw();
    context.addDataStoreListener(this);
  }

  @Override
  protected void redraw() {
    int projdim = proj.getVisibleDimensions2D().cardinality();
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    if(rtree != null) {
      E root = rtree.getRootEntry();
      for(int i = 0; i < rtree.getHeight(); i++) {
        CSSClass cls = new CSSClass(this, INDEX + i);
        // Relative depth of this level. 1.0 = toplevel
        final double relDepth = 1. - (((double) i) / rtree.getHeight());
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
      visualizeRTreeEntry(svgp, layer, proj, rtree, root, 0);
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
  private void visualizeRTreeEntry(SVGPlot svgp, Element layer, Projection2D proj, AbstractRStarTree<NV, ? extends N, E> rtree, E entry, int depth) {
    HyperBoundingBox mbr = entry.getMBR();

    if(fill) {
      Element r = SVGHyperCube.drawFilled(svgp, INDEX + depth, proj, new Vector(mbr.getMin()), new Vector(mbr.getMax()));
      layer.appendChild(r);
    }
    else {
      Element r = SVGHyperCube.drawFrame(svgp, proj, new Vector(mbr.getMin()), new Vector(mbr.getMax()));
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
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<NV> e) {
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
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> {
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
      super();
      config = config.descend(this);
      if(config.grab(FILL_FLAG)) {
        fill = FILL_FLAG.getValue();
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new TreeMBRVisualization<NV, RStarTreeNode, SpatialEntry>(task, fill);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      ArrayList<AbstractRStarTree<NV, RStarTreeNode, SpatialEntry>> trees = ResultUtil.filterResults(result, AbstractRStarTree.class);
      for(AbstractRStarTree<NV, RStarTreeNode, SpatialEntry> tree : trees) {
        final VisualizationTask task = new VisualizationTask(NAME, context, tree, this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_BACKGROUND + 1);
        context.addVisualizer(tree, task);
      }
    }

    @Override
    public Object getVisualizationType() {
      return P2DVisualization.class;
    }
  }
}