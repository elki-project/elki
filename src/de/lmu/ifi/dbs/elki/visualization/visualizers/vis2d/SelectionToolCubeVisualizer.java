package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.BitSet;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.DBIDSelection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.RangeSelection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;

/**
 * Factory for tool visualizations for selecting ranges and the inclosed objects
 * 
 * @author Heidi Kolb
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionToolCubeVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * The logger for this class.
   */
  protected static final Logging logger = Logging.getLogger(SelectionToolCubeVisualizer.class);
  
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Range Selection";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public SelectionToolCubeVisualizer() {
    super(NAME);
    super.metadata.put(Visualizer.META_TOOL, true);
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_TOOLS);
    super.metadata.put(Visualizer.META_NOTHUMB, true);
    super.metadata.put(Visualizer.META_NOEXPORT, true);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new ToolSelectionRangeVisualizer(context, svgp, proj, width, height);
  }

  /**
   * Tool-Visualization for the tool to select ranges
   * 
   * @author Heidi Kolb
   */
  public class ToolSelectionRangeVisualizer extends Projection2DVisualization<NV> implements DragableArea.DragListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    private static final String CSS_RANGEMARKER = "selectionRangeMarker";

    /**
     * Dimension
     */
    private int dim;

    /**
     * Element for selection rectangle
     */
    private Element rtag;

    /**
     * Element for the rectangle to add listeners
     */
    private Element etag;

    /**
     * The actual visualization instance, for a single projection
     * 
     * @param context The context
     * @param svgp The SVGPlot
     * @param proj The Projection
     * @param width The width
     * @param height The height
     */
    public ToolSelectionRangeVisualizer(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_INTERACTIVE);
      this.dim = context.getDatabase().dimensionality();
      context.addContextChangeListener(this);
      incrementalRedraw();
    }

    @Override
    public void destroy() {
      super.destroy();
      context.removeContextChangeListener(this);
    }

    @Override
    public void contextChanged(@SuppressWarnings("unused") ContextChangedEvent e) {
      synchronizedRedraw();
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);

      // rtag: tag for the selected rect
      rtag = svgp.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.addCSSClass(rtag, CSS_RANGEMARKER);
      layer.appendChild(rtag);

      // etag: sensitive area
      DragableArea drag = new DragableArea(svgp, -0.6 * StyleLibrary.SCALE, -0.7 * StyleLibrary.SCALE, 1.3 * StyleLibrary.SCALE, 1.4 * StyleLibrary.SCALE, this);
      etag = drag.getElement();
      layer.appendChild(etag);
    }

    /**
     * Delete the children of the element
     * 
     * @param container SVG-Element
     */
    private void deleteChildren(Element container) {
      while(container.hasChildNodes()) {
        container.removeChild(container.getLastChild());
      }
    }

    /**
     * Set the selected ranges and the mask for the actual dimensions in the
     * context
     * 
     * @param x1 x-value of the first dimension
     * @param x2 x-value of the second dimension
     * @param y1 y-value of the first dimension
     * @param y2 y-value of the second dimension
     */
    private void updateSelectionRectKoordinates(double x1, double x2, double y1, double y2, DoubleDoublePair[] ranges) {
      BitSet actDim = proj.getVisibleDimensions2D();
      Vector v1 = new Vector(dim);
      Vector v2 = new Vector(dim);
      v1.set(0, x1);
      v1.set(1, y1);
      v2.set(0, x2);
      v2.set(1, y2);

      Database<? extends NV> database = context.getDatabase();

      NV factory = database.getObjectFactory();

      NV nv1 = proj.projectRenderToDataSpace(v1, factory);
      NV nv2 = proj.projectRenderToDataSpace(v2, factory);

      for(int d = actDim.nextSetBit(0); d >= 0; d = actDim.nextSetBit(d + 1)) {
        ranges[d] = new DoubleDoublePair(Math.min(nv1.doubleValue(d + 1), nv2.doubleValue(d + 1)), Math.max(nv1.doubleValue(d + 1), nv2.doubleValue(d + 1)));
      }
    }

    @Override
    public boolean startDrag(@SuppressWarnings("unused") SVGPoint startPoint, @SuppressWarnings("unused") Event evt) {
      return true;
    }

    @Override
    public boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, @SuppressWarnings("unused") Event evt, @SuppressWarnings("unused") boolean inside) {
      deleteChildren(rtag);
      double x = Math.min(startPoint.getX(), dragPoint.getX());
      double y = Math.min(startPoint.getY(), dragPoint.getY());
      double width = Math.abs(startPoint.getX() - dragPoint.getX());
      double height = Math.abs(startPoint.getY() - dragPoint.getY());
      rtag.appendChild(svgp.svgRect(x, y, width, height));
      return true;
    }

    @Override
    public boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, @SuppressWarnings("unused") Event evt, @SuppressWarnings("unused") boolean inside) {
      deleteChildren(rtag);
      if(startPoint.getX() != dragPoint.getX() || startPoint.getY() != dragPoint.getY()) {
        updateSelection(proj, startPoint, dragPoint);
      }
      return true;
    }

    /**
     * Update the selection in the context.
     * 
     * @param proj The projection
     * @param p1 First Point of the selected rectangle
     * @param p2 Second Point of the selected rectangle
     */
    private void updateSelection(Projection proj, SVGPoint p1, SVGPoint p2) {
      Database<? extends NV> database = context.getDatabase();
      DBIDSelection selContext = context.getSelection();
      ModifiableDBIDs selection;
      if(selContext != null) {
        selection = DBIDUtil.newHashSet(selContext.getSelectedIds());
      }
      else {
        selection = DBIDUtil.newHashSet();
      }
      DoubleDoublePair[] ranges;

      if(p1 == null || p2 == null) {
        logger.warning("no rect selected: p1: " + p1 + " p2: " + p2);
      }
      else {
        double x1 = Math.min(p1.getX(), p2.getX());
        double x2 = Math.max(p1.getX(), p2.getX());
        double y1 = Math.max(p1.getY(), p2.getY());
        double y2 = Math.min(p1.getY(), p2.getY());

        if(selContext instanceof RangeSelection) {
          ranges = ((RangeSelection) selContext).getRanges();
        }
        else {
          ranges = new DoubleDoublePair[dim];
        }
        updateSelectionRectKoordinates(x1, x2, y1, y2, ranges);

        selection.clear();
        boolean idIn = true;
        for(DBID id : database) {
          NV dbTupel = database.get(id);
          idIn = true;
          for(int i = 0; i < dim; i++) {
            if(ranges != null && ranges[i] != null) {
              if(dbTupel.doubleValue(i + 1) < ranges[i].first || dbTupel.doubleValue(i + 1) > ranges[i].second) {
                idIn = false;
                break;
              }
            }
          }
          if(idIn == true) {
            selection.add(id);
          }
        }
        context.setSelection(new RangeSelection(selection, ranges));
      }
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    protected void addCSSClasses(SVGPlot svgp) {
      // Class for the range marking
      if(!svgp.getCSSClassManager().contains(CSS_RANGEMARKER)) {
        final CSSClass rcls = new CSSClass(this, CSS_RANGEMARKER);
        rcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
        rcls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.2");
        svgp.addCSSClassOrLogError(rcls);
      }
    }
  }
}