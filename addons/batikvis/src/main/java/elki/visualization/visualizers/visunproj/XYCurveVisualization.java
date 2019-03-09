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
package elki.visualization.visualizers.visunproj;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.evaluation.outlier.OutlierPrecisionRecallCurve;
import elki.evaluation.outlier.OutlierPrecisionRecallCurve.PRCurve;
import elki.evaluation.outlier.OutlierROCCurve;
import elki.evaluation.outlier.OutlierROCCurve.ROCResult;
import elki.logging.LoggingUtil;
import elki.math.geometry.XYCurve;
import elki.math.scales.LinearScale;
import elki.utilities.io.FormatUtil;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.css.CSSClass;
import elki.visualization.css.CSSClassManager.CSSNamingConflict;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGPath;
import elki.visualization.svg.SVGPlot;
import elki.visualization.svg.SVGSimpleLinearAxis;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.StaticVisualizationInstance;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;

/**
 * Visualizer to render a simple 2D curve such as a ROC curve.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @stereotype factory
 * @navassoc - create - StaticVisualizationInstance
 * @navhas - visualizes - XYCurve
 */
public class XYCurveVisualization implements VisFactory {
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
  public XYCurveVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    XYCurve curve = task.getResult();

    setupCSS(context, plot);
    final StyleLibrary style = context.getStyleLibrary();
    final double sizex = StyleLibrary.SCALE;
    final double sizey = StyleLibrary.SCALE * height / width;
    final double margin = style.getSize(StyleLibrary.MARGIN);
    Element layer = SVGUtil.svgElement(plot.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(width, height, sizex, sizey, margin);
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
    Element line = path.makeElement(plot, SERIESID);

    // add axes
    try {
      SVGSimpleLinearAxis.drawAxis(plot, layer, scaley, 0, sizey, 0, 0, SVGSimpleLinearAxis.LabelStyle.LEFTHAND, style);
      SVGSimpleLinearAxis.drawAxis(plot, layer, scalex, 0, sizey, sizex, sizey, SVGSimpleLinearAxis.LabelStyle.RIGHTHAND, style);
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception(e);
    }
    // Add axis labels
    {
      Element labelx = plot.svgText(sizex * .5, sizey + margin * .9, curve.getLabelx());
      SVGUtil.setCSSClass(labelx, CSS_AXIS_LABEL);
      layer.appendChild(labelx);
      Element labely = plot.svgText(margin * -.8, sizey * .5, curve.getLabely());
      SVGUtil.setCSSClass(labely, CSS_AXIS_LABEL);
      SVGUtil.setAtt(labely, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(-90," + FormatUtil.NF6.format(margin * -.8) + "," + FormatUtil.NF6.format(sizey * .5) + ")");
      layer.appendChild(labely);
    }

    // Add AUC value when found
    if(curve instanceof ROCResult) {
      double rocauc = ((ROCResult) curve).getAUC();
      String lt = OutlierROCCurve.ROCAUC_LABEL + ": " + FormatUtil.NF.format(rocauc);
      if(rocauc <= 0.5) {
        Element auclbl = plot.svgText(sizex * 0.5, sizey * 0.10, lt);
        SVGUtil.setCSSClass(auclbl, CSS_AXIS_LABEL);
        layer.appendChild(auclbl);
      }
      else {
        Element auclbl = plot.svgText(sizex * 0.5, sizey * 0.95, lt);
        SVGUtil.setCSSClass(auclbl, CSS_AXIS_LABEL);
        layer.appendChild(auclbl);
      }
    }
    if(curve instanceof PRCurve) {
      double prauc = ((PRCurve) curve).getAUC();
      String lt = OutlierPrecisionRecallCurve.PRAUC_LABEL + ": " + FormatUtil.NF.format(prauc);
      if(prauc <= 0.5) {
        Element auclbl = plot.svgText(sizex * 0.5, sizey * 0.10, lt);
        SVGUtil.setCSSClass(auclbl, CSS_AXIS_LABEL);
        layer.appendChild(auclbl);
      }
      else {
        Element auclbl = plot.svgText(sizex * 0.5, sizey * 0.95, lt);
        SVGUtil.setCSSClass(auclbl, CSS_AXIS_LABEL);
        layer.appendChild(auclbl);
      }
    }

    layer.appendChild(line);
    return new StaticVisualizationInstance(context, task, plot, width, height, layer);
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
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewResults(context, start).filter(XYCurve.class).forEach(curve -> {
      context.addVis(curve, new VisualizationTask(this, NAME, curve, null) //
          .level(VisualizationTask.LEVEL_STATIC));
    });
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // TODO: depending on the curve complexity?
    return false;
  }
}
