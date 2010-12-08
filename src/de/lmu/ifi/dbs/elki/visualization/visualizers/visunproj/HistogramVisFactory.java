package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualizer to draw histograms.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 * @apiviz.has HistogramResult oneway - - visualizes
 */
public class HistogramVisFactory extends UnpVisFactory<DatabaseObject> {
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
    VisualizerContext<?> context = task.getContext();
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
    MinMax<Double> xminmax = new MinMax<Double>();
    MinMax<Double> yminmax = new MinMax<Double>();
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
  public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, Result result) {
    List<HistogramResult<? extends NumberVector<?, ?>>> histograms = ResultUtil.filterResults(result, HistogramResult.class);
    for(HistogramResult<? extends NumberVector<?, ?>> histogram : histograms) {
      final VisualizationTask task = new VisualizationTask(NAME, context, histogram, this);
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      context.addVisualizer(histogram, task);
    }
  }

  @Override
  public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
    // TODO: depending on the histogram complexity?
    return false;
  }
}