package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

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
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve.ROCResult;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.AbstractFilteredIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
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
 * @apiviz.has IterableResult oneway - - visualizes
 */
public class CurveVisFactory extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Curve";

  /**
   * SVG class name for plot line
   */
  private static final String SERIESID = "series";

  /**
   * Constructor, Parameterizable style - does nothing.
   */
  public CurveVisFactory() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    VisualizerContext context = task.getContext();
    SVGPlot svgp = task.getPlot();
    IterableResult<DoubleDoublePair> curve = task.getResult();

    setupCSS(context, svgp);
    double scale = StyleLibrary.SCALE;
    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    // determine scaling
    DoubleMinMax minmaxx = new DoubleMinMax();
    DoubleMinMax minmaxy = new DoubleMinMax();
    for(DoubleDoublePair pair : curve) {
      minmaxx.put(pair.first);
      minmaxy.put(pair.second);
    }
    LinearScale scalex = new LinearScale(minmaxx.getMin(), minmaxx.getMax());
    LinearScale scaley = new LinearScale(minmaxy.getMin(), minmaxy.getMax());
    // plot the line
    SVGPath path = new SVGPath();
    for(DoubleDoublePair pair : curve) {
      final double x = scalex.getScaled(pair.first);
      final double y = 1 - scaley.getScaled(pair.second);
      path.drawTo(sizex * x, sizey * y);
    }
    Element line = path.makeElement(svgp);
    line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, SERIESID);

    // add axes
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scalex, 0, sizey, sizex, sizey, true, true, context.getStyleLibrary());
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scaley, 0, sizey, 0, 0, true, false, context.getStyleLibrary());
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception(e);
    }

    // Add AUC value when found
    if(curve instanceof ROCResult) {
      Collection<String> header = ((ROCResult) curve).getHeader();
      for(String str : header) {
        String[] parts = str.split(":\\s*");
        if(parts[0].equals(ComputeROCCurve.ROCAUC_LABEL) && parts.length == 2) {
          double rocauc = Double.parseDouble(parts[1]);
          StyleLibrary style = context.getStyleLibrary();
          CSSClass cls = new CSSClass(svgp, "unmanaged");
          String lt = "ROC AUC: " + FormatUtil.NF8.format(rocauc);
          double fontsize = style.getTextSize("curve.labels");
          cls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(fontsize));
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor("curve.labels"));
          cls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily("curve.labels"));
          if(rocauc <= 0.5) {
            Element auclbl = svgp.svgText(sizex * 0.95, sizey * 0.95, lt);
            SVGUtil.setAtt(auclbl, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
            // SVGUtil.setAtt(auclbl, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE,
            // SVGConstants.SVG_START_VALUE);
            layer.appendChild(auclbl);
          }
          else {
            Element auclbl = svgp.svgText(sizex * 0.95, sizey * 0.95, lt);
            SVGUtil.setAtt(auclbl, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
            SVGUtil.setAtt(auclbl, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_END_VALUE);
            layer.appendChild(auclbl);
          }
        }
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
    CSSClass csscls = new CSSClass(this, SERIESID);
    // csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.2%");
    csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
    context.getStyleLibrary().lines().formatCSSClass(csscls, 0, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
    svgp.addCSSClassOrLogError(csscls);
  }

  /**
   * Filter to only retrieve double-double-pair results.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  class CurveFilter extends AbstractFilteredIterator<IterableResult<?>, IterableResult<DoubleDoublePair>> implements IterableIterator<IterableResult<DoubleDoublePair>> {
    /**
     * Parent iterator to use
     */
    Iterator<IterableResult<?>> parent;

    /**
     * Constructor.
     * 
     * @param parent Parent iterator to decorate.
     */
    public CurveFilter(Iterator<IterableResult<?>> parent) {
      super();
      this.parent = parent;
    }

    @Override
    protected Iterator<IterableResult<?>> getParentIterator() {
      return parent;
    }

    @Override
    protected IterableResult<DoubleDoublePair> testFilter(IterableResult<?> nextobj) {
      Iterator<?> iterator = nextobj.iterator();
      if(iterator.hasNext()) {
        Object o = iterator.next();
        if(o instanceof DoubleDoublePair) {
          @SuppressWarnings("unchecked")
          final IterableResult<DoubleDoublePair> ret = (IterableResult<DoubleDoublePair>) nextobj;
          return ret;
        }
      }
      return null;
    }

    @Override
    public Iterator<IterableResult<DoubleDoublePair>> iterator() {
      return this;
    }
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    final IterableIterator<IterableResult<?>> iterableResults = ResultUtil.filteredResults(result, IterableResult.class);
    final IterableIterator<IterableResult<DoubleDoublePair>> curves = new CurveFilter(iterableResults);
    for (IterableResult<DoubleDoublePair> curve : curves) {
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