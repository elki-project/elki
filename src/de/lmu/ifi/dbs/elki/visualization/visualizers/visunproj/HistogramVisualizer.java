package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualizer to draw histograms.
 * 
 * TODO: dashed lines aren't sensible on screen.
 * 
 * @author Erich Schubert
 */
public class HistogramVisualizer extends AbstractVisualizer implements UnprojectedVisualizer {
  /**
   * Histogram visualizer name
   */
  private static final String NAME = "Histogram";

  /**
   * CSS class name for the series.
   */
  private static final String SERIESID = "series";

  /**
   * The histogram result to visualize
   */
  private HistogramResult<? extends NumberVector<?, ?>> curve;

  // TODO: re-add "-histogram.ymax" option.

  /**
   * Constructor, Parameterizable style - does nothing.
   */
  public HistogramVisualizer() {
    super();
  }

  /**
   * Initialization.
   * 
   * @param context context.
   */
  public void init(VisualizerContext context, HistogramResult<? extends NumberVector<?, ?>> curve) {
    super.init(NAME, context);
    this.curve = curve;
  }

  @Override
  public Element visualize(SVGPlot svgp, double width, double height) {
    final double ratio = width / height;

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
      path[i] = new SVGPath(ratio * xscale.getScaled(xminmax.getMin() - binwidth / 2) * 2, 2);
    }

    // draw curves.
    for(NumberVector<?, ?> vec : curve) {
      for(int d = 0; d < dim; d++) {
        path[d].lineTo(ratio * (xscale.getScaled(vec.doubleValue(1) - binwidth / 2)) * 2, 2 - 2 * yscale.getScaled(vec.doubleValue(d + 2)));
        path[d].lineTo(ratio * (xscale.getScaled(vec.doubleValue(1) + binwidth / 2)) * 2, 2 - 2 * yscale.getScaled(vec.doubleValue(d + 2)));
      }
    }

    // close all histograms
    for(int i = 0; i < dim; i++) {
      path[i].lineTo(ratio * xscale.getScaled(xminmax.getMax() + binwidth / 2) * 2, 2);
    }

    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    // add axes
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, xscale, 0, 2, 2, 2, true, true, context.getStyleLibrary());
      SVGSimpleLinearAxis.drawAxis(svgp, layer, yscale, 0, 2, 0, 0, true, false, context.getStyleLibrary());
    }
    catch(CSSNamingConflict e) {
      logger.exception(e);
    }
    // Setup line styles and insert lines.
    ColorLibrary cl = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
    for(int d = 0; d < dim; d++) {
      CSSClass csscls = new CSSClass(this, SERIESID + "_" + d);
      csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      csscls.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, cl.getColor(d));
      csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, 0.01 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      try {
        svgp.getCSSClassManager().addClass(csscls);
      }
      catch(CSSNamingConflict e) {
        logger.exception(e);
      }

      Element line = path[d].makeElement(svgp);
      line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, csscls.getName());
      layer.appendChild(line);
    }

    // add a small margin for the axis labels
    // FIXME: use width, height
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale(0.45) translate(0.16 0.08)");

    return layer;
  }
}
