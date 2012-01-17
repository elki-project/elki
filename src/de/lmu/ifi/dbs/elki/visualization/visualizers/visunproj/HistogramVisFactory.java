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

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
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
 * Visualizer to draw histograms.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 * @apiviz.has HistogramResult oneway - - visualizes
 */
public class HistogramVisFactory extends AbstractVisFactory {
  /**
   * Histogram visualizer name
   */
  private static final String NAME = "Histogram";

  /**
   * CSS class name for the series.
   */
  private static final String SERIESID = "series";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public HistogramVisFactory() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    VisualizerContext context = task.getContext();
    SVGPlot svgp = task.getPlot();
    HistogramResult<? extends NumberVector<?, ?>> curve = task.getResult();
    
    double scale = StyleLibrary.SCALE;
    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    
    // find maximum, determine step size
    Integer dim = null;
    DoubleMinMax xminmax = new DoubleMinMax();
    DoubleMinMax yminmax = new DoubleMinMax();
    for(NumberVector<?, ?> vec : curve) {
      xminmax.put(vec.doubleValue(1));
      if(dim == null) {
        dim = vec.getDimensionality();
      }
      else {
        // TODO: test and throw always
        assert (dim == vec.getDimensionality());
      }
      for(int i = 1; i < dim; i++) {
        yminmax.put(vec.doubleValue(i + 1));
      }
    }
    // Minimum should always start at 0 for histograms
    yminmax.put(0.0);
    // remove one dimension which are the x values.
    dim = dim - 1;

    int size = curve.size();
    double range = xminmax.getMax() - xminmax.getMin();
    double binwidth = range / (size - 1);

    LinearScale xscale = new LinearScale(xminmax.getMin() - binwidth / 2, xminmax.getMax() + binwidth / 2);
    LinearScale yscale = new LinearScale(yminmax.getMin(), yminmax.getMax());

    SVGPath[] path = new SVGPath[dim];
    for(int i = 0; i < dim; i++) {
      path[i] = new SVGPath(sizex * xscale.getScaled(xminmax.getMin() - binwidth / 2), sizey);
    }

    // draw curves.
    for(NumberVector<?, ?> vec : curve) {
      for(int d = 0; d < dim; d++) {
        path[d].lineTo(sizex * (xscale.getScaled(vec.doubleValue(1) - binwidth / 2)), sizey * (1 - yscale.getScaled(vec.doubleValue(d + 2))));
        path[d].lineTo(sizex * (xscale.getScaled(vec.doubleValue(1) + binwidth / 2)), sizey * (1 - yscale.getScaled(vec.doubleValue(d + 2))));
      }
    }

    // close all histograms
    for(int i = 0; i < dim; i++) {
      path[i].lineTo(sizex * xscale.getScaled(xminmax.getMax() + binwidth / 2), sizey);
    }

    // add axes
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, xscale, 0, sizey, sizex, sizey, true, true, context.getStyleLibrary());
      SVGSimpleLinearAxis.drawAxis(svgp, layer, yscale, 0, sizey, 0, 0, true, false, context.getStyleLibrary());
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception(e);
    }
    // Setup line styles and insert lines.
    ColorLibrary cl = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
    for(int d = 0; d < dim; d++) {
      CSSClass csscls = new CSSClass(this, SERIESID + "_" + d);
      csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      csscls.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, cl.getColor(d));
      csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(csscls);

      Element line = path[d].makeElement(svgp);
      line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, csscls.getName());
      layer.appendChild(line);
    }

    return new StaticVisualization(task, layer);
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    List<HistogramResult<? extends NumberVector<?, ?>>> histograms = ResultUtil.filterResults(newResult, HistogramResult.class);
    for(HistogramResult<? extends NumberVector<?, ?>> histogram : histograms) {
      final VisualizationTask task = new VisualizationTask(NAME, histogram, null, this);
      task.width = 2.0;
      task.height = 1.0;
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      baseResult.getHierarchy().add(histogram, task);
    }
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // TODO: depending on the histogram complexity?
    return false;
  }
}