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
package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

import org.apache.batik.util.SVG12Constants;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.RenderFlag;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSCut;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualizes a cut in an OPTICS Plot to select an Epsilon value and generate a
 * new clustering result.
 *
 * @author Heidi Kolb
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class OPTICSPlotCutVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Cut";

  /**
   * Constructor.
   */
  public OPTICSPlotCutVisualization() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findVis(context, result).filter(OPTICSProjector.class).forEach(p -> {
      context.addVis(p, new VisualizationTask(this, NAME, p.getResult(), null) //
          .level(VisualizationTask.LEVEL_INTERACTIVE) //
          .with(RenderFlag.NO_THUMBNAIL).with(RenderFlag.NO_EXPORT));
    });
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance.
   *
   * @author Heidi Kolb
   * @author Erich Schubert
   */
  public class Instance extends AbstractOPTICSVisualization implements DragableArea.DragListener {
    /**
     * CSS-Styles
     */
    protected static final String CSS_LINE = "opticsPlotLine";

    /**
     * CSS-Styles
     */
    protected static final String CSS_EPSILON = "opticsPlotEpsilonValue";

    /**
     * The current epsilon value.
     */
    private double epsilon = 0.0;

    /**
     * Sensitive (clickable) area
     */
    private DragableArea eventarea = null;

    /**
     * The label element
     */
    private Element elemText = null;

    /**
     * The line element
     */
    private Element elementLine = null;

    /**
     * The drag handle element
     */
    private Element elementPoint = null;

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
    }

    @Override
    public void fullRedraw() {
      incrementalRedraw();
    }

    @Override
    public void incrementalRedraw() {
      if(layer == null) {
        makeLayerElement();
        addCSSClasses();
      }

      // TODO make the number of digits configurable
      final String label = (epsilon > 0.0) ? FormatUtil.NF4.format(epsilon) : "";
      // compute absolute y-value of bar
      final double yAct = getYFromEpsilon(epsilon);

      if(elemText == null) {
        elemText = svgp.svgText(StyleLibrary.SCALE * 1.05, yAct, label);
        SVGUtil.setAtt(elemText, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_EPSILON);
        layer.appendChild(elemText);
      }
      else {
        elemText.setTextContent(label);
        SVGUtil.setAtt(elemText, SVGConstants.SVG_Y_ATTRIBUTE, yAct);
      }

      // line and handle
      if(elementLine == null) {
        elementLine = svgp.svgLine(0, yAct, StyleLibrary.SCALE * 1.04, yAct);
        SVGUtil.addCSSClass(elementLine, CSS_LINE);
        layer.appendChild(elementLine);
      }
      else {
        SVGUtil.setAtt(elementLine, SVG12Constants.SVG_Y1_ATTRIBUTE, yAct);
        SVGUtil.setAtt(elementLine, SVG12Constants.SVG_Y2_ATTRIBUTE, yAct);
      }
      if(elementPoint == null) {
        elementPoint = svgp.svgCircle(StyleLibrary.SCALE * 1.04, yAct, StyleLibrary.SCALE * 0.004);
        SVGUtil.addCSSClass(elementPoint, CSS_LINE);
        layer.appendChild(elementPoint);
      }
      else {
        SVGUtil.setAtt(elementPoint, SVG12Constants.SVG_CY_ATTRIBUTE, yAct);
      }

      if(eventarea == null) {
        eventarea = new DragableArea(svgp, StyleLibrary.SCALE, -StyleLibrary.SCALE * 0.01, //
            StyleLibrary.SCALE * 0.1, plotheight + StyleLibrary.SCALE * 0.02, this);
        layer.appendChild(eventarea.getElement());
      }
    }

    @Override
    public void destroy() {
      super.destroy();
      eventarea.destroy();
    }

    /**
     * Get epsilon from y-value
     *
     * @param y y-Value
     * @return epsilon
     */
    protected double getEpsilonFromY(double y) {
      OPTICSPlot opticsplot = optics.getOPTICSPlot(context);
      y = (y < 0) ? 0 : (y > plotheight) ? 1. : y / plotheight;
      return optics.getOPTICSPlot(context).scaleFromPixel(y * opticsplot.getHeight());
    }

    /**
     * Get y-value from epsilon
     *
     * @param epsilon epsilon
     * @return y-Value
     */
    protected double getYFromEpsilon(double epsilon) {
      OPTICSPlot opticsplot = optics.getOPTICSPlot(context);
      int h = opticsplot.getHeight();
      double y = opticsplot.getScale().getScaled(epsilon, h - .5, .5) / (double) h * plotheight;
      return (y < 0.) ? 0. : (y > plotheight) ? plotheight : y;
    }

    @Override
    public boolean startDrag(SVGPoint start, Event evt) {
      epsilon = getEpsilonFromY(plotheight - start.getY());
      // opvis.unsetEpsilonExcept(this);
      svgp.requestRedraw(this.task, this);
      return true;
    }

    @Override
    public boolean duringDrag(SVGPoint start, SVGPoint end, Event evt, boolean inside) {
      if(inside) {
        epsilon = getEpsilonFromY(plotheight - end.getY());
      }
      // opvis.unsetEpsilonExcept(this);
      svgp.requestRedraw(this.task, this);
      return true;
    }

    @Override
    public boolean endDrag(SVGPoint start, SVGPoint end, Event evt, boolean inside) {
      if(inside) {
        epsilon = getEpsilonFromY(plotheight - end.getY());
        // opvis.unsetEpsilonExcept(this);

        // FIXME: replace an existing optics cut result!
        final ClusterOrder order = optics.getResult();
        Clustering<Model> cl = OPTICSCut.makeOPTICSCut(order, epsilon);
        order.addChildResult(cl);
      }
      svgp.requestRedraw(this.task, this);
      return true;
    }

    /**
     * Reset the epsilon value.
     */
    public void unsetEpsilon() {
      epsilon = 0.0;
    }

    /**
     * Adds the required CSS-Classes
     */
    private void addCSSClasses() {
      // Class for the epsilon-value
      final StyleLibrary style = context.getStyleLibrary();
      if(!svgp.getCSSClassManager().contains(CSS_EPSILON)) {
        final CSSClass label = new CSSClass(svgp, CSS_EPSILON);
        label.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor(StyleLibrary.AXIS_LABEL));
        label.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.AXIS_LABEL));
        label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, style.getTextSize(StyleLibrary.AXIS_LABEL));
        svgp.addCSSClassOrLogError(label);
      }
      // Class for the epsilon cut line
      if(!svgp.getCSSClassManager().contains(CSS_LINE)) {
        final CSSClass lcls = new CSSClass(svgp, CSS_LINE);
        lcls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, style.getColor(StyleLibrary.PLOT));
        lcls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.5 * style.getLineWidth(StyleLibrary.PLOT));
        svgp.addCSSClassOrLogError(lcls);
      }
    }
  }
}
