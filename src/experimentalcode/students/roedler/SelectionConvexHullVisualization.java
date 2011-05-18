package experimentalcode.students.roedler;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;
import experimentalcode.students.roedler.utils.convexhull.ConvexHull2D;

/**
 * Visualizer for generating an SVG-Element containing the convex hull of the
 * selected points
 * 
 * @author Robert Rödler
 * 
 * @apiviz.has SelectionResult oneway - - visualizes
 * @apiviz.has DBIDSelection oneway - - visualizes
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionConvexHullVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements ContextChangeListener, DataStoreListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Convex Hull of Selection";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String SELECTEDHULL = "selectionConvexHull";

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public SelectionConvexHullVisualization(VisualizationTask task) {
    super(task);
    context.addContextChangeListener(this);
    context.addResultListener(this);
    context.addDataStoreListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    DBIDSelection selContext = context.getSelection();
    if(selContext != null) {
      DBIDs selection = selContext.getSelectedIds();
      ConvexHull2D hull = new ConvexHull2D();
      int j = 0;
      for(DBID i : selection) {
        try {
          double[] v = proj.fastProjectDataToRenderSpace(rep.get(i));
          hull.add(new Vector(v));
          j++;
        }
        catch(ObjectNotFoundException e) {
          // ignore
        }
      }
      Polygon chres = hull.getHull();
      if (chres != null && chres.size() >= 3) {
        SVGPath path = new SVGPath();
        for(Vector vec : chres) {
          path.drawTo(vec.get(0), vec.get(1));
        }
        path.close();

        Element selHull = path.makeElement(svgp);

        SVGUtil.addCSSClass(selHull, SELECTEDHULL);
        layer.appendChild(selHull);
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
    if(!svgp.getCSSClassManager().contains(SELECTEDHULL)) {
      CSSClass cls = new CSSClass(this, SELECTEDHULL);
      // cls = new CSSClass(this, CONVEXHULL);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
  }

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing the convex
   * hull of the selected points
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
      return new SelectionConvexHullVisualization<NV>(task);
    }

    @Override
    public Visualization makeVisualizationOrThumbnail(VisualizationTask task) {
      return new ThumbnailVisualization(this, task, ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION);
    }

    @Override
    public void addVisualizers(VisualizerContext context, Result result) {
      Iterator<Relation<? extends NumberVector<?, ?>>> reps = VisualizerUtil.iterateVectorFieldRepresentations(context.getDatabase());
      for(Relation<? extends NumberVector<?, ?>> rep : IterableUtil.fromIterator(reps)) {
        final ArrayList<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
        for(SelectionResult selres : selectionResults) {
          final VisualizationTask task = new VisualizationTask(NAME, context, selres, rep, this, P2DVisualization.class);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 2);
          context.addVisualizer(selres, task);
        }
      }
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return Projection2D.class;
    }
  }
}