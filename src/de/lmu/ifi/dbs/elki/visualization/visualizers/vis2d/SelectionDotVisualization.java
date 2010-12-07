package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;

/**
 * Visualizer for generating an SVG-Element containing dots as markers
 * representing the selected Database's objects.
 * 
 * @author Heidi Kolb
 *
 * @apiviz.has SelectionResult oneway - - visualizes
 * @apiviz.has DBIDSelection oneway - - visualizes
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionDotVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements ContextChangeListener, DataStoreListener<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String MARKER = "selectionDotMarker";

  /**
   * The selection result we work on
   */
  private SelectionResult result;

  public SelectionDotVisualization(VisualizationTask task) {
    super(task, VisFactory.LEVEL_DATA - 1);
    this.result = task.getResult();
    context.addContextChangeListener(this);
    context.addDataStoreListener(this);
    incrementalRedraw();
  }

  @Override
  protected boolean testRedraw(ContextChangedEvent e) {
    return super.testRedraw(e) || (e instanceof SelectionChangedEvent);
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    final double size = context.getStyleLibrary().getSize(StyleLibrary.SELECTION);
    DBIDSelection selContext = context.getSelection();
    if(selContext != null) {
      Database<? extends NV> database = context.getDatabase();
      DBIDs selection = selContext.getSelectedIds();
      for(DBID i : selection) {
        try {
          double[] v = proj.fastProjectDataToRenderSpace(database.get(i));
          Element dot = svgp.svgCircle(v[0], v[1], size);
          SVGUtil.addCSSClass(dot, MARKER);
          layer.appendChild(dot);
        }
        catch(ObjectNotFoundException e) {
          // ignore
        }
      }
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    // Class for the dot markers
    if(!svgp.getCSSClassManager().contains(MARKER)) {
      CSSClass cls = new CSSClass(this, MARKER);
      final StyleLibrary style = context.getStyleLibrary();
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
      svgp.addCSSClassOrLogError(cls);
    }
  }

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<NV> e) {
    synchronizedRedraw();
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing dots as
   * markers representing the selected Database's objects.
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SelectionDotVisualization oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
    /**
     * Constructor
     */
    public Factory() {
      super(NAME);
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionDotVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      final ArrayList<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
      for(SelectionResult selres : selectionResults) {
        context.addVisualizer(selres, this);
      }
    }

  }
}