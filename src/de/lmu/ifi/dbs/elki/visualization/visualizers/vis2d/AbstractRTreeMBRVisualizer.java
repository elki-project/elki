package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize the bounding rectangles of an index
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class AbstractRTreeMBRVisualizer<NV extends NumberVector<NV, ?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends Projection2DVisualizer<NV> {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String INDEX = "index";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Index";

  /**
   * The default constructor only registers parameters.
   */
  public AbstractRTreeMBRVisualizer() {
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

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  @SuppressWarnings("unchecked")
  public static <NV extends NumberVector<NV, ?>, N extends SpatialNode<N,E>, E extends SpatialEntry> AbstractRStarTree<NV, ? extends N, E> findRStarTree(VisualizerContext context) {
    Database<NV> database = context.getDatabase();
    if(SpatialIndexDatabase.class.isAssignableFrom(database.getClass())) {
      SpatialIndex<?, ?, ?> index = ((SpatialIndexDatabase<?, ?, ?>) database).getIndex();
      if(AbstractRStarTree.class.isAssignableFrom(index.getClass())) {
        return (AbstractRStarTree<NV, ? extends N, E>) index;
      }
    }
    return null;
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj) {
    Element layer = super.setupCanvas(svgp, proj);
    AbstractRStarTree<NV, ? extends N, E> rtree = findRStarTree(context);
    if(rtree != null) {
      visualizeRTree(svgp, layer, proj, rtree);
    }
    return layer;
  }

  private void visualizeRTree(SVGPlot svgp, Element layer, VisualizationProjection proj, AbstractRStarTree<NV, ? extends N, E> rtree) {
    E root = rtree.getRootEntry();
    try {
      for (int i = 0; i < rtree.getHeight(); i++) {
        CSSClass cls = new CSSClass(this, INDEX + i);
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getColorLibrary().getColor(i));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.001);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        svgp.getCSSClassManager().addClass(cls);
      }
    }
    catch(CSSNamingConflict e) {
      logger.exception("Could not add index visualization CSS classes.", e);
    }
    visualizeRTreeEntry(svgp, layer, proj, rtree, root, 0);
  }

  private void visualizeRTreeEntry(SVGPlot svgp, Element layer, VisualizationProjection proj, AbstractRStarTree<NV, ? extends N, E> rtree, E entry, int depth) {
    HyperBoundingBox mbr = entry.getMBR();
    Vector min = proj.projectDataToRenderSpace(mbr.getMin());
    Vector max = proj.projectDataToRenderSpace(mbr.getMax());
    double minx = Math.min(min.get(0), max.get(0));
    double maxx = Math.max(min.get(0), max.get(0));
    double miny = Math.min(min.get(1), max.get(1));
    double maxy = Math.max(min.get(1), max.get(1));

    Element r = svgp.svgRect(minx, miny, maxx - minx, maxy - miny);
    SVGUtil.setCSSClass(r, INDEX + depth);
    layer.appendChild(r);

    if(!entry.isLeafEntry()) {
      N node = rtree.getNode(entry);
      for(int i = 0; i < node.getNumEntries(); i++) {
        E child = node.getEntry(i);
        visualizeRTreeEntry(svgp, layer, proj, rtree, child, depth + 1);
      }
    }
  }

  public boolean canVisualize(VisualizerContext context) {
    AbstractRStarTree<NV, ? extends N, E> rtree = findRStarTree(context);
    return (rtree != null);
  }
}
