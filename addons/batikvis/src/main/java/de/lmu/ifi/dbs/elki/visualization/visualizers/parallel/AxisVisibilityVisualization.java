/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.RenderFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Layer for controlling axis visbility in parallel coordinates.
 *
 * @author Robert Rödler
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class AxisVisibilityVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Axis Visibility";

  /**
   * Constructor, adhering to
   */
  public AxisVisibilityVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ParallelPlotProjector.class).forEach(p -> {
      context.addVis(p, new VisualizationTask(this, NAME, p.getRelation(), p.getRelation())//
          .level(VisualizationTask.LEVEL_INTERACTIVE) //
          .with(RenderFlag.NO_THUMBNAIL).with(RenderFlag.NO_EXPORT));
    });
  }

  /**
   * Instance for a particular data set.
   *
   * @author Robert Rödler
   * @author Erich Schubert
   */
  public class Instance extends AbstractParallelVisualization<NumberVector> {
    /**
     * Generic tags to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String SELECTAXISVISIBILITY = "SelectAxisVisibility";

    /**
     * CSS class for a tool button
     */
    public static final String SAV_BUTTON = "SAVbutton";

    /**
     * CSS class for a button border
     */
    public static final String SAV_BORDER = "SAVborder";

    /**
     * CSS class for a button cross
     */
    public static final String SAV_CROSS = "SAVbuttoncross";

    /**
     * Active area size
     */
    double controlsize;

    /**
     * Button size
     */
    double buttonsize;

    /**
     * Vertical position
     */
    double ypos;

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task VisualizationTask
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      super.fullRedraw();
      final int dim = proj.getInputDimensionality();
      addCSSClasses(svgp);
      controlsize = 0.025 * getSizeY();
      buttonsize = 0.75 * controlsize;
      ypos = getSizeY() + getMarginTop() * .5;

      // Background
      Element back = svgp.svgRect(-controlsize * .5, ypos - controlsize * .5 + buttonsize * .5, getSizeX() + controlsize, controlsize);
      SVGUtil.addCSSClass(back, SELECTAXISVISIBILITY);
      layer.appendChild(back);

      // Previous visible dimension.
      for(int i = 0, hidden = 0, vax = 0; i <= dim; i++) {
        if(i < dim && !proj.isAxisVisible(i)) {
          hidden += 1;
          continue;
        }
        // Add button for showing hidden dimensions:
        if(hidden > 0) {
          makeButtonsForHidden(vax, i - hidden, hidden, dim);
          hidden = 0;
        }
        // Add buttons for current dimension
        if(i < dim) {
          makeButtonForVisible(i, vax);
          vax++;
        }
      }
    }

    /**
     * Make a button for a visible axis
     *
     * @param anum Axis number
     * @param apos Axis position in plot
     */
    protected void makeButtonForVisible(int anum, int apos) {
      final double xpos = getVisibleAxisX(apos) - buttonsize * .5;

      Element border = svgp.svgRect(xpos, ypos, buttonsize, buttonsize);
      SVGUtil.addCSSClass(border, SAV_BORDER);
      layer.appendChild(border);

      SVGPath path = new SVGPath();
      final double qs = controlsize * .5;
      final double cs = controlsize * .125;
      path.moveTo(xpos + cs, ypos + cs);
      path.relativeLineTo(qs, qs);
      path.relativeMoveTo(0, -qs);
      path.relativeLineTo(-qs, qs);
      layer.appendChild(path.makeElement(svgp, SAV_CROSS));

      Element rect = svgp.svgRect(xpos, ypos, buttonsize, buttonsize);
      SVGUtil.addCSSClass(rect, SAV_BUTTON);
      addEventListener(rect, anum);
      layer.appendChild(rect);
    }

    /**
     * Insert buttons for hidden dimensions.
     *
     * @param vnum Column number (= next visible axis number)
     * @param first First invisible axis
     * @param count Number of invisible axes
     * @param dim Number of total dimensions
     */
    private void makeButtonsForHidden(final int vnum, final int first, final int count, final int dim) {
      final double lpos, rpos;
      if(vnum == 0) {
        lpos = -getMarginLeft();
      }
      else {
        lpos = getVisibleAxisX(vnum - 1);
      }
      if(first + count + 1 >= dim) {
        rpos = getWidth() + getMarginLeft();
      }
      else {
        rpos = getVisibleAxisX(vnum);
      }
      final double step = (rpos - lpos) / (count + 1.0);
      for(int j = 0; j < count; j++) {
        final double apos = lpos + (j + 1) * step - buttonsize * .5;
        Element border = svgp.svgRect(apos, ypos, buttonsize, buttonsize);
        SVGUtil.addCSSClass(border, SAV_BORDER);
        layer.appendChild(border);

        Element rect = svgp.svgRect(apos, ypos, buttonsize, buttonsize);
        SVGUtil.addCSSClass(rect, SAV_BUTTON);
        addEventListener(rect, first + j);
        layer.appendChild(rect);
      }
    }

    /**
     * Add an event listener to the Element
     *
     * @param tag Element to add the listener
     * @param axis Axis number (including hidden axes)
     */
    private void addEventListener(final Element tag, final int axis) {
      EventTarget targ = (EventTarget) tag;
      targ.addEventListener(SVGConstants.SVG_EVENT_CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if(proj.getVisibleDimensions() > 2) {
            proj.toggleAxisVisible(axis);
            context.visChanged(proj);
          }
        }
      }, false);
    }

    /**
     * Adds the required CSS-Classes
     *
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
      if(!svgp.getCSSClassManager().contains(SELECTAXISVISIBILITY)) {
        CSSClass cls = new CSSClass(this, SELECTAXISVISIBILITY);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.1);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLUE_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
      if(!svgp.getCSSClassManager().contains(SAV_BORDER)) {
        CSSClass cls = new CSSClass(this, SAV_BORDER);
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_GREY_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) * .5);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
      if(!svgp.getCSSClassManager().contains(SAV_BUTTON)) {
        CSSClass cls = new CSSClass(this, SAV_BUTTON);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.01);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_GREY_VALUE);
        cls.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
      if(!svgp.getCSSClassManager().contains(SAV_CROSS)) {
        CSSClass cls = new CSSClass(this, SAV_CROSS);
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) * .75);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
    }
  }
}
