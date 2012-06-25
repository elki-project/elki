package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

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

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve.ROCResult;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualizer to render a simple 2D curve such as a ROC curve.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 * @apiviz.has XYCurve oneway - - visualizes
 */
public class XYCurveVisFactory extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "XYCurve";

  /**
   * SVG class name for plot line
   */
  private static final String SERIESID = "series";

  /**
   * Axis labels
   */
  private static final String CSS_AXIS_LABEL = "xy-axis-label";

  /**
   * Constructor, Parameterizable style - does nothing.
   */
  public XYCurveVisFactory() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    VisualizerContext context = task.getContext();
    SVGPlot svgp = task.getPlot();
    XYCurve curve = task.getResult();

    setupCSS(context, svgp);
    double scale = StyleLibrary.SCALE;
    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    // determine scaling
    LinearScale scalex = new LinearScale(curve.getMinx(), curve.getMaxx());
    LinearScale scaley = new LinearScale(curve.getMiny(), curve.getMaxy());
    // plot the line
    SVGPath path = new SVGPath();
    for(XYCurve.Itr iterator = curve.iterator(); iterator.valid(); iterator.advance()) {
      final double x = scalex.getScaled(iterator.getX());
      final double y = 1 - scaley.getScaled(iterator.getY());
      path.drawTo(sizex * x, sizey * y);
    }
    Element line = path.makeElement(svgp);
    line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, SERIESID);

    // add axes
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scaley, 0, sizey, 0, 0, SVGSimpleLinearAxis.LabelStyle.LEFTHAND, context.getStyleLibrary());
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scalex, 0, sizey, sizex, sizey, SVGSimpleLinearAxis.LabelStyle.RIGHTHAND, context.getStyleLibrary());
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception(e);
    }
    // Add axis labels
    {
      Element labelx = svgp.svgText(sizex / 2, sizey + margin * .9, curve.getLabelx());
      SVGUtil.setCSSClass(labelx, CSS_AXIS_LABEL);
      layer.appendChild(labelx);
      Element labely = svgp.svgText(margin * -.8, sizey * .5, curve.getLabely());
      SVGUtil.setCSSClass(labely, CSS_AXIS_LABEL);
      SVGUtil.setAtt(labely, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(-90,"+FormatUtil.NF6.format(margin * -.8)+","+FormatUtil.NF6.format(sizey * .5)+")");
      layer.appendChild(labely);
    }

    // Add AUC value when found
    if(curve instanceof ROCResult) {
      double rocauc = ((ROCResult) curve).getAUC();
      String lt = ComputeROCCurve.ROCAUC_LABEL + ": " + FormatUtil.NF8.format(rocauc);
      if(rocauc <= 0.5) {
        Element auclbl = svgp.svgText(sizex * 0.5, sizey * 0.5, lt);
        SVGUtil.setCSSClass(auclbl, CSS_AXIS_LABEL);
        layer.appendChild(auclbl);
      }
      else {
        Element auclbl = svgp.svgText(sizex * 0.5, sizey * 0.95, lt);
        SVGUtil.setCSSClass(auclbl, CSS_AXIS_LABEL);
        layer.appendChild(auclbl);
      }
    }

    layer.appendChild(line);
    return new StaticVisualization(task, layer);
  }

  /**
   * Setup the CSS classes for the plot.
   * 
   * @param svgp Plot
   */
  private void setupCSS(VisualizerContext context, SVGPlot svgp) {
    StyleLibrary style = context.getStyleLibrary();
    CSSClass csscls = new CSSClass(this, SERIESID);
    // csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.2%");
    csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
    style.lines().formatCSSClass(csscls, 0, style.getLineWidth(StyleLibrary.XYCURVE));
    svgp.addCSSClassOrLogError(csscls);
    // Axis label
    CSSClass label = new CSSClass(this, CSS_AXIS_LABEL);
    label.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor(StyleLibrary.XYCURVE));
    label.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.XYCURVE));
    label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, style.getTextSize(StyleLibrary.XYCURVE));
    label.setStatement(SVGConstants.CSS_TEXT_ANCHOR_PROPERTY, SVGConstants.CSS_MIDDLE_VALUE);
    svgp.addCSSClassOrLogError(label);
    svgp.updateStyleElement();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    final ArrayList<XYCurve> curves = ResultUtil.filterResults(result, XYCurve.class);
    for(XYCurve curve : curves) {
      final VisualizationTask task = new VisualizationTask(NAME, curve, null, this);
      task.width = 1.0;
      task.height = 1.0;
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      baseResult.getHierarchy().add(curve, task);
    }
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // TODO: depending on the curve complexity?
    return false;
  }
}