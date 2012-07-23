package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Generates a SVG-Element containing axes, including labeling.
 * 
 * @author Robert Rödler
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class ParallelAxisVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Parallel Axes";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ParallelAxisVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Collection<ParallelPlotProjector<?>> ps = ResultUtil.filterResults(result, ParallelPlotProjector.class);
    for(ParallelPlotProjector<?> p : ps) {
      final VisualizationTask task = new VisualizationTask(NAME, p, p.getRelation(), this);
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_BACKGROUND);
      baseResult.getHierarchy().add(p, task);
    }
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return true;
  }

  /**
   * Instance.
   * 
   * @author Robert Rödler
   * 
   * @apiviz.uses SVGSimpleLinearAxis
   */
  // TODO: split into interactive / non-interactive parts?
  public class Instance extends AbstractParallelVisualization<NumberVector<?, ?>> {
    /**
     * Axis label class
     */
    public final static String AXIS_LABEL = "paxis-label";

    /**
     * Clickable area for the axis.
     */
    public final static String INVERTEDAXIS = "paxis-button";

    /**
     * Constructor.
     * 
     * @param task VisualizationTask
     */
    public Instance(VisualizationTask task) {
      super(task);
      incrementalRedraw();
      context.addResultListener(this);
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);
      final int dim = proj.getVisibleDimensions();
      try {
        for(int i = 0; i < dim; i++) {
          final int truedim = proj.getDimForVisibleAxis(i);
          final double axisX = getVisibleAxisX(i);
          if(!proj.isAxisInverted(i)) {
            SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getAxisScale(i), axisX, getSizeY(), axisX, 0, SVGSimpleLinearAxis.LabelStyle.ENDLABEL, context.getStyleLibrary());
          }
          else {
            SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getAxisScale(i), axisX, 0, axisX, getSizeY(), SVGSimpleLinearAxis.LabelStyle.ENDLABEL, context.getStyleLibrary());
          }
          // Get axis label
          final String label = DatabaseUtil.getColumnLabel(relation, truedim + 1);
          // Add axis label
          Element text = svgp.svgText(axisX, -.7 * getMarginTop(), label);
          SVGUtil.setCSSClass(text, AXIS_LABEL);
          layer.appendChild(text);
          // TODO: Split into background + clickable layer.
          Element button = svgp.svgRect(axisX - getAxisSep() * .475, -getMarginTop(), .95 * getAxisSep(), .5 * getMarginTop());
          SVGUtil.setCSSClass(button, INVERTEDAXIS);
          addEventListener(button, truedim);
          layer.appendChild(button);
        }
      }
      catch(CSSNamingConflict e) {
        throw new RuntimeException("Conflict in CSS naming for axes.", e);
      }
    }

    /**
     * Add the main CSS classes.
     * 
     * @param svgp Plot to draw to
     */
    private void addCSSClasses(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
      if(!svgp.getCSSClassManager().contains(AXIS_LABEL)) {
        CSSClass cls = new CSSClass(this, AXIS_LABEL);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor(StyleLibrary.AXIS_LABEL));
        cls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.AXIS_LABEL));
        cls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, style.getTextSize(StyleLibrary.AXIS_LABEL));
        cls.setStatement(SVGConstants.CSS_TEXT_ANCHOR_PROPERTY, SVGConstants.SVG_MIDDLE_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
      if(!svgp.getCSSClassManager().contains(INVERTEDAXIS)) {
        CSSClass cls = new CSSClass(this, INVERTEDAXIS);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.1);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_GREY_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
    }

    /**
     * Add an event listener to the Element
     * 
     * @param tag Element to add the listener
     * @param i Tool number for the Element
     */
    private void addEventListener(final Element tag, final int i) {
      EventTarget targ = (EventTarget) tag;
      targ.addEventListener(SVGConstants.SVG_EVENT_CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          proj.toggleDimInverted(i);
          context.getHierarchy().resultChanged(proj);
        }
      }, false);
    }
  }
}