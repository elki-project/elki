package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize the bounding rectangles of an rtree based index.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 * @param <N> Tree node type
 * @param <E> Tree entry type
 */
public class TreeMBRVisualizer<NV extends NumberVector<NV, ?>, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends Projection2DVisualizer<NV> {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String INDEX = "index";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Index MBRs";

  /**
   * The default constructor only registers parameters.
   */
  public TreeMBRVisualizer() {
    super();
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   */
  public void init(VisualizerContext context) {
    super.init(NAME, context);
  }

  @SuppressWarnings("unchecked")
  private AbstractRStarTree<NV, N, E> findRStarTree(VisualizerContext context) {
    Database<NV> database = context.getDatabase();
    if(database != null && SpatialIndexDatabase.class.isAssignableFrom(database.getClass())) {
      SpatialIndex<?, ?, ?> index = ((SpatialIndexDatabase<?, ?, ?>) database).getIndex();
      if(AbstractRStarTree.class.isAssignableFrom(index.getClass())) {
        return (AbstractRStarTree<NV, N, E>) index;
      }
    }
    return null;
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
    double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = super.setupCanvas(svgp, proj, margin, width, height);
    AbstractRStarTree<NV, N, E> rtree = findRStarTree(context);
    if(rtree != null) {
      E root = rtree.getRootEntry();
      try {
        for(int i = 0; i < rtree.getHeight(); i++) {
          CSSClass cls = new CSSClass(this, INDEX + i);
          cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
          // Relative depth of this level. 1.0 = toplevel
          final double relDepth = 1. - (((double) i) / rtree.getHeight());
          cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, relDepth * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
          svgp.getCSSClassManager().addClass(cls);
        }
      }
      catch(CSSNamingConflict e) {
        logger.exception("Could not add index visualization CSS classes.", e);
      }
      visualizeRTreeEntry(svgp, layer, proj, rtree, root, 0);
    }
    return layer;
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
  private void visualizeRTreeEntry(SVGPlot svgp, Element layer, VisualizationProjection proj, AbstractRStarTree<NV, ? extends N, E> rtree, E entry, int depth) {
    HyperBoundingBox mbr = entry.getMBR();

    Element r = SVGHyperCube.drawWireframe(svgp, proj, mbr.getMin(), mbr.getMax());
    SVGUtil.setCSSClass(r, INDEX + depth);
    layer.appendChild(r);

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

  /**
   * Test for a visualizable index in the context's database.
   * 
   * @param context Visualization context
   * @return whether there is a visualizable index
   */
  public boolean canVisualize(VisualizerContext context) {
    AbstractRStarTree<NV, ? extends N, E> rtree = findRStarTree(context);
    return (rtree != null);
  }
}