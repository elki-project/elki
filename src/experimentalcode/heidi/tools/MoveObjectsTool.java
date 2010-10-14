package experimentalcode.heidi.tools;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea.DragListener;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Factory for tool visualizations for changing objects in the database
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class MoveObjectsTool<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Move Objects";

  /**
   * Constructor
   */
  public MoveObjectsTool() {
    super(NAME);
    super.metadata.put(Visualizer.META_TOOL, true);
    super.metadata.put(Visualizer.META_NOTHUMB, true);
    super.metadata.put(Visualizer.META_NOEXPORT, true);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new MoveObjectsToolVisualization<NV>(context, svgp, proj, width, height);
  }

  /**
   * Tool to move the currently selected objects.
   * 
   * @author Heidi Kolb
   * @author Erich Schubert
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class MoveObjectsToolVisualization<NV extends NumberVector<NV, ?>> extends Projection2DVisualization<NV> implements DragListener {
    /**
     * CSS tag for our event rectangle
     */
    protected static final String CSS_ARROW = "moveArrow";

    /**
     * Element for the rectangle to add listeners
     */
    private Element etag;

    /**
     * Element to contain the drag arrow
     */
    private Element rtag;

    /**
     * The actual visualization instance, for a single projection
     * 
     * @param context The context
     * @param svgp The SVGPlot
     * @param proj The Projection
     * @param width The width
     * @param height The height
     */
    public MoveObjectsToolVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_INTERACTIVE);
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

      rtag = svgp.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.addCSSClass(rtag, CSS_ARROW);
      layer.appendChild(rtag);

      DragableArea drag = new DragableArea(svgp, -0.6 * StyleLibrary.SCALE, -0.7 * StyleLibrary.SCALE, 1.3 * StyleLibrary.SCALE, 1.4 * StyleLibrary.SCALE, this);
      etag = drag.getElement();
      layer.appendChild(etag);
    }

    /**
     * Updates the object with the given DBID It will be moved depending on the
     * given Vector
     * 
     * @param dbid - DBID of the object to move
     * @param movingVector - Vector for moving object
     */
    @SuppressWarnings("unchecked")
    // TODO: in DBIDUtil?
    private void updateDB(DBID dbid, Vector movingVector) {
      Database<NV> database = (Database<NV>) context.getDatabase();

      NV obj = database.get(dbid);
      String objectLabel = database.getObjectLabel(dbid);
      ClassLabel classLabel = database.getClassLabel(dbid);

      Vector v = proj.projectDataToRenderSpace(obj);
      double v0 = v.get(0);
      double v1 = v.get(1);
      v.set(0, v0 + movingVector.get(0));
      v.set(1, v1 + movingVector.get(1));
      NV nv = proj.projectRenderToDataSpace(v, obj);
      nv.setID(obj.getID());

      try {
        synchronized(database) {
          database.delete(dbid);
          database.insert(new Pair<NV, DatabaseObjectMetadata>(nv, null));
          // restore class and object labels
          database.setClassLabel(dbid, classLabel);
          database.setObjectLabel(dbid, objectLabel);
        }
      }
      catch(UnableToComplyException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
      }
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
     * Adds the required CSS-Classes
     * 
     * @param svgp SVGPlot
     */
    private void addCSSClasses(SVGPlot svgp) {
      // Class for the rectangle to add eventListeners
      if(!svgp.getCSSClassManager().contains(CSS_ARROW)) {
        final CSSClass acls = new CSSClass(this, CSS_ARROW);
        acls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, "red");
        acls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
        acls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        svgp.addCSSClassOrLogError(acls);
      }
    }

    @Override
    public boolean startDrag(@SuppressWarnings("unused") SVGPoint startPoint, @SuppressWarnings("unused") Event evt) {
      return true;
    }

    @Override
    public boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, @SuppressWarnings("unused") Event evt, @SuppressWarnings("unused") boolean inside) {
      deleteChildren(rtag);
      rtag.appendChild(svgp.svgLine(startPoint.getX(), startPoint.getY(), dragPoint.getX(), dragPoint.getY()));
      return true;
    }

    @Override
    public boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, @SuppressWarnings("unused") Event evt, @SuppressWarnings("unused") boolean inside) {
      Vector movingVector = new Vector(2);
      movingVector.set(0, dragPoint.getX() - startPoint.getX());
      movingVector.set(1, dragPoint.getY() - startPoint.getY());
      for(DBID dbid : context.getSelection().getSelectedIds()) {
        updateDB(dbid, movingVector);
      }
      return true;
    }
  }
}