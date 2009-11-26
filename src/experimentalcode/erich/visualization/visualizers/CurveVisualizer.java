package experimentalcode.erich.visualization.visualizers;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualizer to render a simple 2D curve such as a ROC curve.
 * 
 * @author Erich Schubert
 */
public class CurveVisualizer extends AbstractVisualizer implements UnprojectedVisualizer {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Curve";

  /**
   * SVG class name for plot line
   */
  private static final String SERIESID = "series";

  /**
   * SVG class name for frame
   */
  private static final String FRAMEID = "frame";

  /**
   * Curve to visualize
   */
  IterableResult<Pair<Double, Double>> curve = null;

  /**
   * Constructor, Parameterizable style - does nothing.
   */
  public CurveVisualizer() {
    super();
  }

  /**
   * Initialization.
   * 
   * @param context context.
   */
  public void init(VisualizerContext context, IterableResult<Pair<Double, Double>> curve) {
    super.init(NAME, context);
    this.curve = curve;
  }

  /**
   * Find a 2D Double curve in the result object.
   * 
   * @param result Result object to inspect
   * @return
   */
  @SuppressWarnings("unchecked")
  public static Collection<IterableResult<Pair<Double, Double>>> findCurveResult(Result result) {
    List<IterableResult<?>> iterables = ResultUtil.getIterableResults(result);
    java.util.Vector<IterableResult<Pair<Double, Double>>> matching = new java.util.Vector<IterableResult<Pair<Double, Double>>>();
    for(IterableResult<?> iterable : iterables) {
      Iterator<?> iterator = iterable.iterator();
      if(iterator.hasNext()) {
        Object o = iterator.next();
        if(o instanceof Pair) {
          Pair<?, ?> p = (Pair<?, ?>) o;
          if(p.getFirst() != null && p.getFirst() instanceof Double && p.getSecond() != null && p.getSecond() instanceof Double) {
            matching.add((IterableResult<Pair<Double, Double>>) iterable);
          }
        }
      }
    }
    return matching;
  }

  @Override
  public Element visualize(SVGPlot svgp) {
    setupCSS(svgp);

    final double ratio = 1.0;

    Element rect = svgp.svgRect(0, 0, ratio, 1);
    SVGUtil.setAtt(rect, SVGConstants.SVG_CLASS_ATTRIBUTE, FRAMEID);

    // determine scaling
    MinMax<Double> minmaxx = new MinMax<Double>();
    MinMax<Double> minmaxy = new MinMax<Double>();
    for(Pair<Double, Double> pair : curve) {
      minmaxx.put(pair.getFirst());
      minmaxy.put(pair.getSecond());
    }
    LinearScale scalex = new LinearScale(minmaxx.getMin(), minmaxx.getMax());
    LinearScale scaley = new LinearScale(minmaxy.getMin(), minmaxy.getMax());
    // plot the line
    SVGPath path = new SVGPath();
    for(Pair<Double, Double> pair : curve) {
      final double x = scalex.getScaled(pair.getFirst());
      final double y = scaley.getScaled(pair.getSecond());
      path.drawTo(ratio * x, 1.0 - y);
    }
    Element line = path.makeElement(svgp);
    line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, SERIESID);

    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    // add axes
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scalex, 0, 1, 1, 1, true, true);
      SVGSimpleLinearAxis.drawAxis(svgp, layer, scaley, 0, 1, 0, 0, true, false);
    }
    catch(CSSNamingConflict e) {
      logger.exception(e);
    }

    layer.appendChild(rect);
    layer.appendChild(line);

    // add a slight border
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale(0.9) translate(0.08 0.02)");

    return layer;
  }

  /**
   * Setup the CSS classes for the plot.
   * 
   * @param svgp
   */
  private void setupCSS(SVGPlot svgp) {
    // setup CSS
    try {
      CSSClass csscls = new CSSClass(this, SERIESID);
      //csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.2%");
      csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      context.getLineStyleLibrary().formatCSSClass(csscls, 0, 0.005);
      svgp.getCSSClassManager().addClass(csscls);
      CSSClass frmcls = new CSSClass(this, FRAMEID);
      frmcls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.5%");
      frmcls.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.CSS_SILVER_VALUE);
      frmcls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      svgp.getCSSClassManager().addClass(frmcls);
    }
    catch(CSSNamingConflict e) {
      logger.exception(e);
    }
  }
}
