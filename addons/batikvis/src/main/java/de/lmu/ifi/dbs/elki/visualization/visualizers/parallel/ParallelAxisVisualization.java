package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel;

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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
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
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ParallelAxisVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Parallel Axes";

  /**
   * Constructor.
   */
  public ParallelAxisVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<ParallelPlotProjector<?>> it = VisualizationTree.filter(context, start, ParallelPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ParallelPlotProjector<?> p = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, p, p.getRelation(), this);
      task.level = VisualizationTask.LEVEL_BACKGROUND;
      context.addVis(p, task);
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
  public class Instance extends AbstractParallelVisualization<NumberVector> {
    /**
     * Axis label class.
     */
    public static final String AXIS_LABEL = "paxis-label";

    /**
     * Clickable area for the axis.
     */
    public static final String INVERTEDAXIS = "paxis-button";

    /**
     * Constructor.
     *
     * @param task VisualizationTask
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      super.fullRedraw();
      final StyleLibrary style = context.getStyleLibrary();
      addCSSClasses(svgp);
      final int dim = proj.getInputDimensionality();
      for(int i = 0, vdim = 0; i < dim; i++) {
        if(!proj.isAxisVisible(i)) {
          continue;
        }
        final int truedim = proj.getDimForAxis(i);
        final double axisX = getVisibleAxisX(vdim);
        try {
          if(!proj.isAxisInverted(vdim)) {
            SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getAxisScale(i), axisX, getSizeY(), axisX, 0, SVGSimpleLinearAxis.LabelStyle.ENDLABEL, style);
          }
          else {
            SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getAxisScale(i), axisX, 0, axisX, getSizeY(), SVGSimpleLinearAxis.LabelStyle.ENDLABEL, style);
          }
        }
        catch(CSSNamingConflict e) {
          LOG.warning("Conflict in CSS naming for axes.", e);
          continue;
        }
        // Get axis label
        final String label = RelationUtil.getColumnLabel(relation, truedim);
        // Add axis label
        Element text = svgp.svgText(axisX, -.7 * getMarginTop(), label);
        SVGUtil.setCSSClass(text, AXIS_LABEL);
        // TODO: find a reliable way for sizing axis labels.
        if(dim > 10) {
          SVGUtil.setAtt(text, SVGConstants.SVG_TEXT_LENGTH_ATTRIBUTE, getAxisSep() * 0.95);
          SVGUtil.setAtt(text, SVGConstants.SVG_LENGTH_ADJUST_ATTRIBUTE, SVGConstants.SVG_SPACING_AND_GLYPHS_VALUE);
        }
        layer.appendChild(text);
        // TODO: Split into background + clickable layer.
        Element button = svgp.svgRect(axisX - getAxisSep() * .475, -getMarginTop(), .95 * getAxisSep(), .5 * getMarginTop());
        SVGUtil.setCSSClass(button, INVERTEDAXIS);
        addEventListener(button, truedim);
        layer.appendChild(button);
        vdim++;
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
     * Add an event listener to the Element.
     *
     * @param tag Element to add the listener
     * @param truedim Tool number for the Element
     */
    private void addEventListener(final Element tag, final int truedim) {
      EventTarget targ = (EventTarget) tag;
      targ.addEventListener(SVGConstants.SVG_EVENT_CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          proj.toggleDimInverted(truedim);
          context.visChanged(proj);
        }
      }, false);
    }
  }
}