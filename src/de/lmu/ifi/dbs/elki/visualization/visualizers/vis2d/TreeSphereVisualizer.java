package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.MetricalIndexDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize the bounding sphere of a metric index.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 * @param <N> Tree node type
 * @param <E> Tree entry type
 */
public class TreeSphereVisualizer<NV extends NumberVector<NV, ?>, D extends NumberDistance<D, ?>, N extends AbstractMTreeNode<NV, D, N, E>, E extends MTreeEntry<D>> extends Projection2DVisualizer<NV> {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String INDEX = "index";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Index Spheres";

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
  private boolean fill = false;

  /**
   * Drawing modes.
   */
  private enum modi {
    MANHATTAN, EUCLIDEAN, LPCROSS
  }

  private modi dist = modi.LPCROSS;

  /**
   * The default constructor only registers parameters.
   * 
   * @param config Parameters
   */
  public TreeSphereVisualizer(Parameterization config) {
    super();
    if(config.grab(FILL_FLAG)) {
      fill = FILL_FLAG.getValue();
    }
    super.setLevel(Visualizer.LEVEL_BACKGROUND + 1);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   */
  public void init(VisualizerContext<? extends NV> context) {
    super.init(NAME, context);
  }

  @SuppressWarnings("unchecked")
  private AbstractMTree<NV, D, N, E> findMTree(VisualizerContext context) {
    Database<NV> database = context.getDatabase();
    if(database != null && MetricalIndexDatabase.class.isAssignableFrom(database.getClass())) {
      MetricalIndex<?, ?, ?, ?> index = ((MetricalIndexDatabase<?, ?, ?, ?>) database).getIndex();
      if(AbstractMTree.class.isAssignableFrom(index.getClass())) {
        if(index.getDistanceFunction() instanceof LPNormDistanceFunction) {
          return (AbstractMTree<NV, D, N, E>) index;
        }
      }
    }
    return null;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    int projdim = proj.computeVisibleDimensions2D().size();
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
    double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = Projection2DVisualization.setupCanvas(svgp, proj, margin, width, height);
    AbstractMTree<NV, D, N, E> mtree = findMTree(context);
    if(mtree != null) {
      if(ManhattanDistanceFunction.class.isInstance(mtree.getDistanceFunction())) {
        dist = modi.MANHATTAN;
      }
      else if(EuclideanDistanceFunction.class.isInstance(mtree.getDistanceFunction())) {
        dist = modi.EUCLIDEAN;
      }
      else {
        dist = modi.LPCROSS;
      }
      E root = mtree.getRootEntry();
      try {
        final int mtheight = mtree.getHeight();
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
          svgp.getCSSClassManager().addClass(cls);
        }
      }
      catch(CSSNamingConflict e) {
        logger.exception("Could not add index visualization CSS classes.", e);
      }
      visualizeMTreeEntry(svgp, layer, proj, mtree, root, 0);
    }
    Integer level = this.getMetadata().getGenerics(Visualizer.META_LEVEL, Integer.class);
    return new StaticVisualization(level, layer, width, height);
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
  private void visualizeMTreeEntry(SVGPlot svgp, Element layer, VisualizationProjection proj, AbstractMTree<NV, D, ? extends N, E> mtree, E entry, int depth) {
    Database<? extends NV> database = context.getDatabase();
    DBID roid = entry.getRoutingObjectID();
    if(roid != null) {
      NV ro = database.get(roid);
      D rad = entry.getCoveringRadius();

      final Element r;
      if(dist == modi.MANHATTAN) {
        r = SVGHyperSphere.drawManhattan(svgp, proj, ro, rad);
      }
      else if(dist == modi.EUCLIDEAN) {
        r = SVGHyperSphere.drawEuclidean(svgp, proj, ro, rad);
      }
      else {
        r = SVGHyperSphere.drawCross(svgp, proj, ro, rad);
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

  /**
   * Test for a visualizable index in the context's database.
   * 
   * @param context Visualization context
   * @return whether there is a visualizable index
   */
  public boolean canVisualize(VisualizerContext<? extends NV> context) {
    AbstractMTree<NV, D, ? extends N, E> rtree = findMTree(context);
    return (rtree != null);
  }
}