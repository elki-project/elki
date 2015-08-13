package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationItem;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.CanvasSize;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Renders a tool box on the left of the 2D visualization
 *
 * @author Heidi Kolb
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class ToolBox2DVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Tool Box";

  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ToolBox2DVisualization.class);

  /**
   * Constructor
   */
  public ToolBox2DVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<ScatterPlotProjector<?>> it = VisualizationTree.filter(context, start, ScatterPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ScatterPlotProjector<?> p = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, p, p.getRelation(), ToolBox2DVisualization.this);
      task.level = VisualizationTask.LEVEL_INTERACTIVE;
      task.addFlags(VisualizationTask.FLAG_NO_THUMBNAIL | VisualizationTask.FLAG_NO_EXPORT | VisualizationTask.FLAG_NO_EMBED);
      context.addVis(p, task);
    }
  }

  /**
   * Instance.
   *
   * @author Heidi Kolb
   *
   * @apiviz.has VisualizationTask oneway - - visualizes
   */
  public class Instance extends AbstractScatterplotVisualization {
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

    /**
     * Constructor.
     *
     * @param task Task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      context.addVisualizationListener(this);
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
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
     * Build the toolbox
     */
    private void buildToolBox() {
      double scale = StyleLibrary.SCALE;
      deleteChildren(container);

      ArrayList<VisualizationTask> vis = new ArrayList<>();
      Hierarchy.Iter<VisualizationTask> it = VisualizationTree.filter(context, task.getResult(), VisualizationTask.class);
      for(; it.valid(); it.advance()) {
        VisualizationTask task = it.get();
        if(task.tool && !vis.contains(task)) {
          vis.add(task);
        }
      }

      // calculate the position of the first tool
      CanvasSize viewport = proj.estimateViewport();
      double x = viewport.getMinX() - 0.17 * scale;
      double width = 0.07 * scale;
      double height = 0.06 * scale;
      double miny = viewport.getMinY();
      double maxy = viewport.getMaxY();
      double y = (miny + maxy - vis.size() * height * 1.4) * .5;
      if(y < miny) {
        LOG.warning("Too many Tools");
      }

      // add tools
      Element[] toolTags = new Element[vis.size()];
      for(int i = 0; i < vis.size(); i++) {
        VisualizationTask v = vis.get(i);
        toolTags[i] = svgp.svgRect(x, y, width, height);
        String name = v.getMenuName();
        // Split
        List<String> lines = FormatUtil.splitAtLastBlank(name, 8);
        // Generate label objects.
        for(int l = 0; l < lines.size(); l++) {
          Element selectRangeText = svgp.svgText(x + 0.01 * scale, y + (0.02 + 0.05 * l / lines.size()) * scale, lines.get(l));
          SVGUtil.setAtt(selectRangeText, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_TOOL_CAPTION);
          container.appendChild(selectRangeText);
        }

        if(v.visible) {
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
        final StyleLibrary style = context.getStyleLibrary();
        final CSSClass label = new CSSClass(svgp, CSS_TOOL_CAPTION);
        label.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor(StyleLibrary.AXIS_LABEL));
        label.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.AXIS_LABEL));
        label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, style.getTextSize(StyleLibrary.AXIS_LABEL) * .8);

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
        public void handleEvent(Event evt) {
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
      if(tool.visible) {
        context.setSelection(null);
      }
      VisualizationTree.setVisible(context, tool, true);
    }

    @Override
    public void visualizationChanged(VisualizationItem item) {
      super.visualizationChanged(item);
      if(item instanceof VisualizationTask) {
        VisualizationTask task = (VisualizationTask) item;
        if(task.tool) {
          svgp.requestRedraw(this.task, this);
        }
      }
    }
  }
}