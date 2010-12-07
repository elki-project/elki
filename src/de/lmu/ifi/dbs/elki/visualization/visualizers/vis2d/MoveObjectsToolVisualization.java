package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea.DragListener;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;

/**
 * Tool to move the currently selected objects.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 * 
 * @apiviz.has NumberVector oneway - - edits
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class MoveObjectsToolVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DragListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Move Objects";

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
   * Our database
   */
  private Database<NV> database;

  public MoveObjectsToolVisualization(VisualizationTask task) {
    super(task, VisFactory.LEVEL_INTERACTIVE);
    this.database = task.getResult();
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
   * Updates the objects with the given DBIDs It will be moved depending on the
   * given Vector
   * 
   * @param dbids - DBIDs of the objects to move
   * @param movingVector - Vector for moving object
   */
  // TODO: move to DatabaseUtil?
  private void updateDB(DBIDs dbids, Vector movingVector) {
    database.accumulateDataStoreEvents();
    for(DBID dbid : dbids) {
      NV obj = database.get(dbid);
      String objectLabel = database.getObjectLabel(dbid);
      ClassLabel classLabel = database.getClassLabel(dbid);

      Vector v = proj.projectDataToRenderSpace(obj);
      v.set(0, v.get(0) + movingVector.get(0));
      v.set(1, v.get(1) + movingVector.get(1));
      NV nv = proj.projectRenderToDataSpace(v, obj);
      nv.setID(obj.getID());

      try {
        database.delete(dbid);
        database.insert(new Pair<NV, DatabaseObjectMetadata>(nv, null));
        // restore class and object labels
        database.setClassLabel(dbid, classLabel);
        database.setObjectLabel(dbid, objectLabel);
      }
      catch(UnableToComplyException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
      }
    }
    database.flushDataStoreEvents();
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
      final StyleLibrary style = context.getStyleLibrary();
      acls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, style.getColor(StyleLibrary.SELECTION_ACTIVE));
      acls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.SELECTION_ACTIVE));
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
    if(context.getSelection() != null) {
      updateDB(context.getSelection().getSelectedIds(), movingVector);
    }
    deleteChildren(rtag);
    return true;
  }

  /**
   * Factory for tool visualizations for changing objects in the database
   * 
   * @author Heidi Kolb
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses MoveObjectsToolVisualization oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
    /**
     * Constructor
     */
    public Factory() {
      super(NAME);
      super.metadata.put(VisFactory.META_TOOL, true);
      super.metadata.put(VisFactory.META_NOTHUMB, true);
      super.metadata.put(VisFactory.META_NOEXPORT, true);
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new MoveObjectsToolVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      ArrayList<Database<?>> databases = ResultUtil.filterResults(result, Database.class);
      for(Database<?> database : databases) {
        if(!VisualizerUtil.isNumberVectorDatabase(database)) {
          return;
        }
        context.addVisualizer(database, this);
      }
    }
  }
}