package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;

/**
 * Renders a tool box on the left of the 2D visualization
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has VisFactory oneway - - visualizes
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class ToolBox2DVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Tool Box";

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(Factory.class);

  /**
   * CSS class for a tool button
   */
  public static final String CSS_TOOL_BUTTON = "toolButton";

  /**
   * CSS class for the tool button caption
   */
  public static final String CSS_TOOL_CAPTION = "toolCaption";

  /**
   * CSS class for a tool button
   */
  public static final String CSS_TOOL_BUTTON_SELECTED = "toolButtonSelected";

  /**
   * The container
   */
  private Element container;

  public ToolBox2DVisualization(VisualizationTask task) {
    super(task, VisualizationTask.LEVEL_INTERACTIVE);
    // TODO: which result do we attach to?
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
    container = svgp.svgElement(SVGConstants.SVG_G_TAG);
    buildToolBox();
    layer.appendChild(container);
  }

  /**
   * Deletes the children of the container
   * 
   * @param container Element to delete children
   */
  private void deleteChildren(Element container) {
    while(container.hasChildNodes()) {
      container.removeChild(container.getLastChild());
    }
  }

  /**
   * Sets the toolbox
   */
  private void buildToolBox() {
    double scale = StyleLibrary.SCALE;
    deleteChildren(container);

    ArrayList<VisualizationTask> vis = new ArrayList<VisualizationTask>();
    for(VisualizationTask task : context.iterVisualizers()) {
      if(VisualizerUtil.isTool(task)) {
        vis.add(task);
      }
    }

    // calculate the position of the first tool
    Pair<DoubleMinMax, DoubleMinMax> pt = proj.estimateViewport();
    double x = pt.getFirst().getMin() - 0.17 * scale;
    double width = 0.07 * scale;
    double height = 0.06 * scale;
    double miny = pt.getSecond().getMin();
    double maxy = pt.getSecond().getMax();
    double y = (miny + maxy) / 2 - (vis.size() * height * 1.4) / 2;
    if(y < miny) {
      logger.warning("Too many Tools");
    }

    // add tools
    Element[] toolTags = new Element[vis.size()];
    for(int i = 0; i < vis.size(); i++) {
      VisualizationTask v = vis.get(i);
      toolTags[i] = svgp.svgRect(x, y, width, height);
      String name = v.getLongName();
      // Split
      List<String> lines = FormatUtil.splitAtLastBlank(name, 8);
      // Generate label objects.
      for(int l = 0; l < lines.size(); l++) {
        Element selectRangeText = svgp.svgText(x + 0.01 * scale, y + (0.02 + 0.05 * l / lines.size()) * scale, lines.get(l));
        SVGUtil.setAtt(selectRangeText, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_TOOL_CAPTION);
        container.appendChild(selectRangeText);
      }

      if(VisualizerUtil.isVisible(v)) {
        SVGUtil.addCSSClass(toolTags[i], CSS_TOOL_BUTTON_SELECTED);
      }
      else {
        SVGUtil.addCSSClass(toolTags[i], CSS_TOOL_BUTTON);
      }
      addEventListener(toolTags[i], v);

      container.appendChild(toolTags[i]);
      y = y + 0.1 * scale;
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    // Class for the not selected tool
    if(!svgp.getCSSClassManager().contains(CSS_TOOL_BUTTON)) {
      final CSSClass modeCls = new CSSClass(this, CSS_TOOL_BUTTON);
      modeCls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.4);
      modeCls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_GREEN_VALUE);
      modeCls.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);

      svgp.addCSSClassOrLogError(modeCls);
    }
    // Class for the selected tool
    if(!svgp.getCSSClassManager().contains(CSS_TOOL_BUTTON_SELECTED)) {
      final CSSClass modeCls = new CSSClass(this, CSS_TOOL_BUTTON_SELECTED);
      modeCls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.4);
      modeCls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLUE_VALUE);
      modeCls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, 0.4);
      modeCls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLUE_VALUE);
      modeCls.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);

      svgp.addCSSClassOrLogError(modeCls);
    }
    // Class for the text of the tools
    if(!svgp.getCSSClassManager().contains(CSS_TOOL_CAPTION)) {
      final CSSClass label = new CSSClass(svgp, CSS_TOOL_CAPTION);
      label.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.AXIS_LABEL));
      label.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.AXIS_LABEL));
      label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, context.getStyleLibrary().getTextSize(StyleLibrary.AXIS_LABEL) * .8);

      svgp.addCSSClassOrLogError(label);
    }
  }

  /**
   * Add an event listener to the Element
   * 
   * @param tag Element to add the listener
   * @param tool Tool represented by the Element
   */
  private void addEventListener(final Element tag, final VisualizationTask tool) {
    EventTarget targ = (EventTarget) tag;
    targ.addEventListener(SVGConstants.SVG_EVENT_CLICK, new EventListener() {
      @Override
      public void handleEvent(@SuppressWarnings("unused") Event evt) {
        handleMouseClick(tool);
      }
    }, false);
  }

  /**
   * Handle the mouseClick - change the selected tool in the context
   * 
   * @param tool Selected Tool
   */
  protected void handleMouseClick(VisualizationTask tool) {
    // TODO: Move this to the selected tool instead?
    if(VisualizerUtil.isVisible(tool)) {
      context.setSelection(null);
    }
    context.setVisualizerVisibility(tool, true);
  }

  /**
   * Factory for visualizers for a toolbox
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ToolBox2DVisualization oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ToolBox2DVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      ArrayList<Database<?>> databases = ResultUtil.filterResults(result, Database.class);
      for(Database<?> database : databases) {
        if(!VisualizerUtil.isNumberVectorDatabase(database)) {
          return;
        }
        final VisualizationTask task = new VisualizationTask(NAME, context, database, this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        task.put(VisualizationTask.META_NOTHUMB, true);
        task.put(VisualizationTask.META_NOEXPORT, true);
        context.addVisualizer(database, task);
      }
    }
  }
}