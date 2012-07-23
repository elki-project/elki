package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import org.apache.batik.util.SVG12Constants;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSCut;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualizes a cut in an OPTICS Plot to select an Epsilon value and generate a
 * new clustering result.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses OPTICSPlotCutVisualization oneway - - «create»
 */
public class OPTICSPlotCutVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Cut";

  public OPTICSPlotCutVisualization() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Collection<OPTICSProjector<?>> ops = ResultUtil.filterResults(result, OPTICSProjector.class);
    for(OPTICSProjector<?> p : ops) {
      final VisualizationTask task = new VisualizationTask(NAME, p, null, this);
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
      baseResult.getHierarchy().add(p, task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance<DoubleDistance>(task);
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
   * 
   * @param <D> distance type
   */
  public class Instance<D extends Distance<D>> extends AbstractOPTICSVisualization<D> implements DragableArea.DragListener {
    /**
     * CSS-Styles
     */
    protected static final String CSS_LINE = "opticsPlotLine";

    /**
     * CSS-Styles
     */
    protected final static String CSS_EPSILON = "opticsPlotEpsilonValue";

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
     * @param task Task
     */
    public Instance(VisualizationTask task) {
      super(task);
    }

    @Override
    protected void redraw() {
      incrementalRedraw();
    }

    @Override
    protected void incrementalRedraw() {
      if(layer == null) {
        makeLayerElement();
        addCSSClasses();
      }

      // TODO make the number of digits configurable
      final String label = (epsilon != 0.0) ? FormatUtil.format(epsilon, 4) : "";
      // compute absolute y-value of bar
      final double yAct = plotheight - getYFromEpsilon(epsilon);

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
        eventarea = new DragableArea(svgp, StyleLibrary.SCALE, 0, StyleLibrary.SCALE * 0.1, plotheight, this);
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
      if(y < 0) {
        y = 0;
      }
      if(y > plotheight) {
        y = plotheight;
      }
      return optics.getOPTICSPlot(context).getScale().getUnscaled(y / plotheight);
    }

    /**
     * Get y-value from epsilon
     * 
     * @param epsilon epsilon
     * @return y-Value
     */
    protected double getYFromEpsilon(double epsilon) {
      double y = optics.getOPTICSPlot(context).getScale().getScaled(epsilon) * plotheight;
      if(y < 0) {
        y = 0;
      }
      if(y > plotheight) {
        y = plotheight;
      }
      return y;
    }

    @Override
    public boolean startDrag(SVGPoint start, Event evt) {
      epsilon = getEpsilonFromY(plotheight - start.getY());
      // opvis.unsetEpsilonExcept(this);
      synchronizedRedraw();
      return true;
    }

    @Override
    public boolean duringDrag(SVGPoint start, SVGPoint end, Event evt, boolean inside) {
      if(inside) {
        epsilon = getEpsilonFromY(plotheight - end.getY());
      }
      // opvis.unsetEpsilonExcept(this);
      synchronizedRedraw();
      return true;
    }

    @Override
    public boolean endDrag(SVGPoint start, SVGPoint end, Event evt, boolean inside) {
      if(inside) {
        epsilon = getEpsilonFromY(plotheight - end.getY());
        // opvis.unsetEpsilonExcept(this);

        // FIXME: replace an existing optics cut result!
        final ClusterOrderResult<D> order = optics.getResult();
        Clustering<Model> cl = OPTICSCut.makeOPTICSCut(order, optics.getOPTICSPlot(context).getDistanceAdapter(), epsilon);
        order.addChildResult(cl);
      }
      context.getHierarchy().resultChanged(this.task);
      // synchronizedRedraw();
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
      if(!svgp.getCSSClassManager().contains(CSS_EPSILON)) {
        final CSSClass label = new CSSClass(svgp, CSS_EPSILON);
        label.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.AXIS_LABEL));
        label.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.AXIS_LABEL));
        label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, context.getStyleLibrary().getTextSize(StyleLibrary.AXIS_LABEL));
        svgp.addCSSClassOrLogError(label);
      }
      // Class for the epsilon cut line
      if(!svgp.getCSSClassManager().contains(CSS_LINE)) {
        final CSSClass lcls = new CSSClass(svgp, CSS_LINE);
        lcls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getStyleLibrary().getColor(StyleLibrary.PLOT));
        lcls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.5 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
        svgp.addCSSClassOrLogError(lcls);
      }
    }
  }
}