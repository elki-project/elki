package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

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
public class SelectionDotVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements ContextChangeListener, DataStoreListener {
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

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public SelectionDotVisualization(VisualizationTask task) {
    super(task);
    this.result = task.getResult();
    context.addContextChangeListener(this);
    context.addResultListener(this);
    context.addDataStoreListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    final double size = context.getStyleLibrary().getSize(StyleLibrary.SELECTION);
    DBIDSelection selContext = context.getSelection();
    if(selContext != null) {
      DBIDs selection = selContext.getSelectedIds();
      for(DBID i : selection) {
        try {
          double[] v = proj.fastProjectDataToRenderSpace(rep.get(i));
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
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent e) {
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
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionDotVisualization<NV>(task);
    }

    @Override
    public Visualization makeVisualizationOrThumbnail(VisualizationTask task) {
      return new ThumbnailVisualization(this, task, ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Iterator<Relation<? extends NumberVector<?, ?>>> reps = VisualizerUtil.iterateVectorFieldRepresentations(baseResult);
      for(Relation<? extends NumberVector<?, ?>> rep : IterableUtil.fromIterator(reps)) {
        final ArrayList<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
        for(SelectionResult selres : selectionResults) {
          final VisualizationTask task = new VisualizationTask(NAME, selres, rep, this, P2DVisualization.class);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
          baseResult.getHierarchy().add(selres, task);
        }
      }
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return Projection2D.class;
    }
  }
}