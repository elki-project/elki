package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
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
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

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
  protected boolean fill = false;

  /**
   * Drawing modes.
   */
  private enum modi {
    MANHATTAN, EUCLIDEAN, LPCROSS
  }

  protected double p;

  /**
   * Drawing mode (distance) to use
   */
  protected modi dist = modi.LPCROSS;

  /**
   * The default constructor only registers parameters.
   * 
   * @param config Parameters
   */
  public TreeSphereVisualizer(Parameterization config) {
    super(NAME, Visualizer.LEVEL_BACKGROUND + 1);
    config = config.descend(this);
    if(config.grab(FILL_FLAG)) {
      fill = FILL_FLAG.getValue();
    }
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_METADATA);
  }

  @SuppressWarnings("unchecked")
  protected Pair<AbstractMTree<NV, D, N, E>, Double> findMTree(VisualizerContext<? extends NV> context) {
    Database<? extends NV> database = context.getDatabase();
    if(database != null && MetricalIndexDatabase.class.isAssignableFrom(database.getClass())) {
      MetricalIndex<?, ?, ?, ?> index = ((MetricalIndexDatabase<?, ?, ?, ?>) database).getIndex();
      if(AbstractMTree.class.isAssignableFrom(index.getClass())) {
        if(index.getDistanceQuery() instanceof LPNormDistanceFunction) {
          AbstractMTree<NV, D, N, E> tree = (AbstractMTree<NV, D, N, E>) index;
          double p = ((LPNormDistanceFunction) index.getDistanceQuery()).getP();
          return new Pair<AbstractMTree<NV, D, N, E>, Double>(tree, p);
        }
      }
    }
    return null;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new TreeSphereVisualization(context, svgp, proj, width, height);
  }

  @Override
  public Visualization makeThumbnail(SVGPlot svgp, Projection2D proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV, Projection2D>(this, context, svgp, proj, width, height, tresolution, ThumbnailVisualization.ON_DATA);
  }

  /**
   * Test for a visualizable index in the context's database.
   * 
   * @param context Visualization context
   * @return whether there is a visualizable index
   */
  public boolean canVisualize(VisualizerContext<? extends NV> context) {
    Pair<AbstractMTree<NV, D, N, E>, Double> rtree = findMTree(context);
    return (rtree != null);
  }

  /**
   * M-Tree visualization.
   * 
   * @author Erich Schubert
   */
  // TODO: listen for tree changes!
  protected class TreeSphereVisualization extends Projection2DVisualization<NV> implements DatabaseListener<NV> {
    /**
     * Constructor.
     * 
     * @param context Context
     * @param svgp Plot
     * @param proj Projection
     * @param width Width
     * @param height Height
     */
    public TreeSphereVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_BACKGROUND);
      incrementalRedraw();
      context.addDatabaseListener(this);
    }

    @Override
    protected void redraw() {
      int projdim = proj.getVisibleDimensions2D().cardinality();
      ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

      Pair<AbstractMTree<NV, D, N, E>, Double> indexinfo = findMTree(context);
      AbstractMTree<NV, D, N, E> mtree = indexinfo.first;
      p = indexinfo.second;
      if(mtree != null) {
        if(ManhattanDistanceFunction.class.isInstance(mtree.getDistanceQuery())) {
          dist = modi.MANHATTAN;
        }
        else if(EuclideanDistanceFunction.class.isInstance(mtree.getDistanceQuery())) {
          dist = modi.EUCLIDEAN;
        }
        else {
          dist = modi.LPCROSS;
        }
        E root = mtree.getRootEntry();
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
          svgp.addCSSClassOrLogError(cls);
        }
        visualizeMTreeEntry(svgp, this.layer, proj, mtree, root, 0);
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
        if(dist == modi.MANHATTAN) {
          r = SVGHyperSphere.drawManhattan(svgp, proj, ro, rad);
        }
        else if(dist == modi.EUCLIDEAN) {
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
      context.removeDatabaseListener(this);
    }

    @Override
    public void objectsChanged(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }

    @Override
    public void objectsInserted(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }

    @Override
    public void objectsRemoved(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }
  }
}