package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

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

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualizationInstance;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualizer to draw histograms.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualizationInstance oneway - - «create»
 * @apiviz.has HistogramResult oneway - - visualizes
 */
public class HistogramVisualization extends AbstractVisFactory {
  /**
   * Histogram visualizer name
   */
  private static final String NAME = "Histogram";

  /**
   * CSS class name for the series.
   */
  private static final String SERIESID = "series";

  /**
   * Constructor.
   */
  public HistogramVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    VisualizerContext context = task.getContext();
    HistogramResult<double[]> curve = task.getResult();

    final StyleLibrary style = context.getStyleLibrary();
    final double sizex = StyleLibrary.SCALE;
    final double sizey = StyleLibrary.SCALE * height / width;
    final double margin = style.getSize(StyleLibrary.MARGIN);
    Element layer = SVGUtil.svgElement(plot.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(width, height, sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    // find maximum, determine step size
    int dim = -1;
    DoubleMinMax xminmax = new DoubleMinMax();
    DoubleMinMax yminmax = new DoubleMinMax();
    for(double[] point : curve) {
      xminmax.put(point[0]);
      if(dim < 0) {
        dim = point.length;
      }
      else {
        // TODO: test and throw always
        assert(dim == point.length);
      }
      for(int i = 1; i < dim; i++) {
        yminmax.put(point[i]);
      }
    }
    // Minimum should always start at 0 for histograms
    yminmax.put(0.0);
    // remove one dimension which are the x values.
    dim = dim - 1;

    int size = curve.size();
    double range = xminmax.getMax() - xminmax.getMin();
    double binwidth = range / (size - 1);

    LinearScale xscale = new LinearScale(xminmax.getMin() - binwidth * .49999, xminmax.getMax() + binwidth * .49999);
    LinearScale yscale = new LinearScale(yminmax.getMin(), yminmax.getMax());

    SVGPath[] path = new SVGPath[dim];
    for(int i = 0; i < dim; i++) {
      path[i] = new SVGPath(sizex * xscale.getScaled(xminmax.getMin() - binwidth * .5), sizey);
    }

    // draw curves.
    for(double[] point : curve) {
      for(int d = 0; d < dim; d++) {
        path[d].lineTo(sizex * (xscale.getScaled(point[0] - binwidth * .5)), sizey * (1 - yscale.getScaled(point[d + 1])));
        path[d].lineTo(sizex * (xscale.getScaled(point[0] + binwidth * .5)), sizey * (1 - yscale.getScaled(point[d + 1])));
      }
    }

    // close all histograms
    for(int i = 0; i < dim; i++) {
      path[i].lineTo(sizex * xscale.getScaled(xminmax.getMax() + binwidth * .5), sizey);
    }

    // add axes
    try {
      SVGSimpleLinearAxis.drawAxis(plot, layer, yscale, 0, sizey, 0, 0, SVGSimpleLinearAxis.LabelStyle.LEFTHAND, style);
      SVGSimpleLinearAxis.drawAxis(plot, layer, xscale, 0, sizey, sizex, sizey, SVGSimpleLinearAxis.LabelStyle.RIGHTHAND, style);
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception(e);
    }
    // Setup line styles and insert lines.
    ColorLibrary cl = style.getColorSet(StyleLibrary.PLOT);
    for(int d = 0; d < dim; d++) {
      CSSClass csscls = new CSSClass(this, SERIESID + "_" + d);
      csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      csscls.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, cl.getColor(d));
      csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, style.getLineWidth(StyleLibrary.PLOT));
      plot.addCSSClassOrLogError(csscls);

      Element line = path[d].makeElement(plot);
      line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, csscls.getName());
      layer.appendChild(line);
    }

    return new StaticVisualizationInstance(task, plot, width, height, layer);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<HistogramResult<?>> it = VisualizationTree.filterResults(context, start, HistogramResult.class);
    for(; it.valid(); it.advance()) {
      HistogramResult<?> histogram = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, histogram, null, HistogramVisualization.this);
      task.reqwidth = 2.0;
      task.reqheight = 1.0;
      task.level = VisualizationTask.LEVEL_STATIC;
      context.addVis(histogram, task);
    }
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // TODO: depending on the histogram complexity?
    return false;
  }
}
