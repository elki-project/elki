package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve.ROCResult;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
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
 * Visualizer to render a simple 2D curve such as a ROC curve.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has StaticVisualization oneway - - produces
 * @apiviz.has IterableResult oneway - - visualizes
 */
public class CurveVisFactory extends AbstractUnprojectedVisFactory<DatabaseObject> {
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
    super(NAME);
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    VisualizerContext<?> context = task.getContext();
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
    MinMax<Double> minmaxx = new MinMax<Double>();
    MinMax<Double> minmaxy = new MinMax<Double>();
    for(DoubleDoublePair pair : curve) {
      minmaxx.put(pair.getFirst());
      minmaxy.put(pair.getSecond());
    }
    LinearScale scalex = new LinearScale(minmaxx.getMin(), minmaxx.getMax());
    LinearScale scaley = new LinearScale(minmaxy.getMin(), minmaxy.getMax());
    // plot the line
    SVGPath path = new SVGPath();
    for(DoubleDoublePair pair : curve) {
      final double x = scalex.getScaled(pair.getFirst());
      final double y = 1 - scaley.getScaled(pair.getSecond());
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
        if(parts[0].equals(ComputeROCCurve.ROC_AUC.getLabel()) && parts.length == 2) {
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
    return new StaticVisualization(task, layer, this.getLevel());
  }

  /**
   * Setup the CSS classes for the plot.
   * 
   * @param svgp Plot
   */
  private void setupCSS(VisualizerContext<?> context, SVGPlot svgp) {
    CSSClass csscls = new CSSClass(this, SERIESID);
    // csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.2%");
    csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
    context.getLineStyleLibrary().formatCSSClass(csscls, 0, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
    svgp.addCSSClassOrLogError(csscls);
  }

  /**
   * Find a 2D Double curve in the result object.
   * 
   * @param result Result object to inspect
   * @return Collection of curves
   */
  @SuppressWarnings("unchecked")
  public static Collection<IterableResult<DoubleDoublePair>> findCurveResults(AnyResult result) {
    List<IterableResult<?>> iterables = ResultUtil.getIterableResults(result);
    java.util.Vector<IterableResult<DoubleDoublePair>> matching = new java.util.Vector<IterableResult<DoubleDoublePair>>();
    for(IterableResult<?> iterable : iterables) {
      Iterator<?> iterator = iterable.iterator();
      if(iterator.hasNext()) {
        Object o = iterator.next();
        if(o instanceof DoubleDoublePair) {
          matching.add((IterableResult<DoubleDoublePair>) iterable);
        }
      }
    }
    return matching;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, AnyResult result) {
    Collection<IterableResult<DoubleDoublePair>> curves = findCurveResults(result);
    for (IterableResult<DoubleDoublePair> curve : curves) {
      context.addVisualizer(curve, this);
    }
  }

  @Override
  public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
    // TODO: depending on the curve complexity?
    return false;
  }
}