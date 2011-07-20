package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.Collection;
import java.util.List;

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Handle the marker in an OPTICS plot.
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.uses ClusterOrderResult oneway - 1
 * @apiviz.uses OPTICSPlot oneway - 1
 * @apiviz.uses DBIDSelection oneway - 1 visualizes
 * 
 * @param <D> distance type
 */
public class OPTICSPlotSelectionVisualization<D extends Distance<D>> extends AbstractVisualization implements DragableArea.DragListener {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OPTICSPlotSelectionVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Selection";

  /**
   * CSS class for markers
   */
  protected static final String CSS_MARKER = "opticsPlotMarker";

  /**
   * CSS class for markers
   */
  protected static final String CSS_RANGEMARKER = "opticsPlotRangeMarker";

  /**
   * Input modes
   * 
   * @apiviz.exclude
   */
  // TODO: Refactor all Mode copies into a shared class?
  private enum Mode {
    REPLACE, ADD, INVERT
  }

  /**
   * Our concerned curve
   */
  ClusterOrderResult<D> co;

  /**
   * The plot
   */
  private OPTICSPlot<D> opticsplot;

  /**
   * Element for the events
   */
  private Element etag;

  /**
   * Element for the marker
   */
  private Element mtag;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public OPTICSPlotSelectionVisualization(VisualizationTask task) {
    super(task);
    this.co = task.getResult();
    this.opticsplot = OPTICSPlot.plotForClusterOrder(this.co, context);
    context.addResultListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    double scale = StyleLibrary.SCALE;
    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    addCSSClasses();

    this.etag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.mtag = svgp.svgElement(SVGConstants.SVG_G_TAG);

    double heightPlot = scale / opticsplot.getRatio();

    DragableArea drag = new DragableArea(svgp, 0 - scale * 0.1, 0, scale * 1.1, heightPlot, this);
    etag = drag.getElement();

    addMarker();
    // mtag first, etag must be the top Element
    layer.appendChild(mtag);
    layer.appendChild(etag);
  }

  /**
   * Add marker for the selected IDs to mtag
   */
  public void addMarker() {
    List<ClusterOrderEntry<D>> order = co.getClusterOrder();
    // TODO: replace mtag!
    DBIDSelection selContext = context.getSelection();
    if(selContext != null) {
      DBIDs selection = DBIDUtil.ensureSet(selContext.getSelectedIds());

      final double width = StyleLibrary.SCALE / order.size();
      int begin = -1;
      for(int j = 0; j < order.size(); j++) {
        DBID id = order.get(j).getID();
        if(selection.contains(id)) {
          if(begin == -1) {
            begin = j;
          }
        }
        else {
          if(begin != -1) {
            Element marker = addMarkerRect(begin * width, (j - begin) * width);
            SVGUtil.addCSSClass(marker, CSS_MARKER);
            mtag.appendChild(marker);
            begin = -1;
          }
        }
      }
      // tail
      if(begin != -1) {
        Element marker = addMarkerRect(begin * width, (order.size() - begin) * width);
        SVGUtil.addCSSClass(marker, CSS_MARKER);
        mtag.appendChild(marker);
      }
    }
  }

  /**
   * Create a rectangle as marker (Marker higher than plot!)
   * 
   * @param x1 X-Value for the marker
   * @param width Width of an entry
   * @return SVG-Element svg-rectangle
   */
  public Element addMarkerRect(double x1, double width) {
    double heightPlot = StyleLibrary.SCALE / opticsplot.getRatio();
    return svgp.svgRect(x1, 0, width, heightPlot);
  }

  @Override
  public boolean startDrag(SVGPoint startPoint, @SuppressWarnings("unused") Event evt) {
    List<ClusterOrderEntry<D>> order = co.getClusterOrder();
    int mouseActIndex = getSelectedIndex(order, startPoint);
    if(mouseActIndex >= 0 && mouseActIndex < order.size()) {
      double width = StyleLibrary.SCALE / order.size();
      double x1 = mouseActIndex * width;
      Element marker = addMarkerRect(x1, width);
      SVGUtil.setCSSClass(marker, CSS_RANGEMARKER);
      mtag.appendChild(marker);
      return true;
    }
    return false;
  }

  @Override
  public boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, @SuppressWarnings("unused") Event evt, @SuppressWarnings("unused") boolean inside) {
    List<ClusterOrderEntry<D>> order = co.getClusterOrder();
    int mouseDownIndex = getSelectedIndex(order, startPoint);
    int mouseActIndex = getSelectedIndex(order, dragPoint);
    final int begin = Math.max(Math.min(mouseDownIndex, mouseActIndex), 0);
    final int end = Math.min(Math.max(mouseDownIndex, mouseActIndex), order.size());
    double width = StyleLibrary.SCALE / order.size();
    double x1 = begin * width;
    double x2 = (end * width) + width;
    mtag.removeChild(mtag.getLastChild());
    Element marker = addMarkerRect(x1, x2 - x1);
    SVGUtil.setCSSClass(marker, CSS_RANGEMARKER);
    mtag.appendChild(marker);
    return true;
  }

  @Override
  public boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, @SuppressWarnings("unused") boolean inside) {
    List<ClusterOrderEntry<D>> order = co.getClusterOrder();
    int mouseDownIndex = getSelectedIndex(order, startPoint);
    int mouseActIndex = getSelectedIndex(order, dragPoint);
    Mode mode = getInputMode(evt);
    final int begin = Math.max(Math.min(mouseDownIndex, mouseActIndex), 0);
    final int end = Math.min(Math.max(mouseDownIndex, mouseActIndex), order.size());
    updateSelection(mode, begin, end);
    return true;
  }

  /**
   * Get the current input mode, on each mouse event.
   * 
   * @param evt Mouse event.
   * @return Input mode
   */
  private Mode getInputMode(Event evt) {
    if(evt instanceof DOMMouseEvent) {
      DOMMouseEvent domme = (DOMMouseEvent) evt;
      // TODO: visual indication of mode possible?
      if(domme.getShiftKey()) {
        return Mode.ADD;
      }
      else if(domme.getCtrlKey()) {
        return Mode.INVERT;
      }
      else {
        return Mode.REPLACE;
      }
    }
    // Default mode is replace.
    return Mode.REPLACE;
  }

  /**
   * Gets the Index of the ClusterOrderEntry where the event occurred
   * 
   * @param order List of ClusterOrderEntries
   * @param cPt clicked point
   * @return Index of the object
   */
  private int getSelectedIndex(List<ClusterOrderEntry<D>> order, SVGPoint cPt) {
    int mouseActIndex = (int) ((cPt.getX() / StyleLibrary.SCALE) * order.size());
    return mouseActIndex;
  }

  /**
   * Updates the selection for the given ClusterOrderEntry.
   * 
   * @param mode Input mode
   * @param begin first index to select
   * @param end last index to select
   */
  protected void updateSelection(Mode mode, int begin, int end) {
    List<ClusterOrderEntry<D>> order = co.getClusterOrder();
    if(begin < 0 || begin > end || end >= order.size()) {
      logger.warning("Invalid range in updateSelection: " + begin + " .. " + end);
      return;
    }

    DBIDSelection selContext = context.getSelection();
    HashSetModifiableDBIDs selection;
    if(selContext == null || mode == Mode.REPLACE) {
      selection = DBIDUtil.newHashSet();
    }
    else {
      selection = DBIDUtil.newHashSet(selContext.getSelectedIds());
    }

    for(int i = begin; i <= end; i++) {
      DBID id = order.get(i).getID();
      if(mode == Mode.INVERT) {
        if(!selection.contains(id)) {
          selection.add(id);
        }
        else {
          selection.remove(id);
        }
      }
      else {
        // In REPLACE and ADD, add objects.
        // The difference was done before by not re-using the selection.
        // Since we are using a set, we can just add in any case.
        selection.add(id);
      }
    }
    context.setSelection(new DBIDSelection(selection));
  }

  /**
   * Adds the required CSS-Classes
   */
  private void addCSSClasses() {
    // Class for the markers
    if(!svgp.getCSSClassManager().contains(CSS_MARKER)) {
      final CSSClass cls = new CSSClass(this, CSS_MARKER);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLUE_VALUE);
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.2");
      svgp.addCSSClassOrLogError(cls);
    }

    // Class for the range marking
    if(!svgp.getCSSClassManager().contains(CSS_RANGEMARKER)) {
      final CSSClass rcls = new CSSClass(this, CSS_RANGEMARKER);
      rcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
      rcls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.2");
      svgp.addCSSClassOrLogError(rcls);
    }
  }

  @Override
  public void resultChanged(Result current) {
    if(current instanceof SelectionResult || current == co || current == opticsplot) {
      synchronizedRedraw();
      return;
    }
    super.resultChanged(current);
  }

  /**
   * Factory class for OPTICS plot selections.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses OPTICSPlotSelectionVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Collection<OPTICSPlot<?>> plots = ResultUtil.filterResults(result, OPTICSPlot.class);
      for(OPTICSPlot<?> plot : plots) {
        ClusterOrderResult<?> co = plot.getClusterOrder();
        final VisualizationTask task = new VisualizationTask(NAME, co, null, this, plot);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        baseResult.getHierarchy().add(plot, task);
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new OPTICSPlotSelectionVisualization<DoubleDistance>(task);
    }

    @Override
    public Visualization makeVisualizationOrThumbnail(VisualizationTask task) {
      return new ThumbnailVisualization(this, task, ThumbnailVisualization.ON_SELECTION);
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return null;
    }
  }
}