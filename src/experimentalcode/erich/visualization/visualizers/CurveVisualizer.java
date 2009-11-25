package experimentalcode.erich.visualization.visualizers;

import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.batikutil.NodeReplaceChild;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

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
  public void init(VisualizerContext context) {
    super.init(NAME, context);
  }

  @SuppressWarnings("unchecked")
  public static IterableResult<Pair<Double,Double>> findCurveResult(Result result) {
    List<IterableResult<?>> iterables = ResultUtil.getIterableResults(result);
    for(IterableResult<?> iterable : iterables) {
      Iterator<?> iterator = iterable.iterator();
      if(iterator.hasNext()) {
        Object o = iterator.next();
        if(o instanceof Pair) {
          Pair<?, ?> p = (Pair<?, ?>) o;
          if(p.getFirst() != null && p.getFirst() instanceof Double && p.getSecond() != null && p.getSecond() instanceof Double) {
            return (IterableResult<Pair<Double,Double>>) iterable;
          }
        }
      }
    }
    return null;
  }
  
  protected void updateCurve(SVGPlot plot, Element parent, Iterable<Pair<Double, Double>> curve, double ratio) {
    // prepare replacement tag.
    Element newe = plot.svgElement(SVGConstants.SVG_G_TAG);
    Element rect = plot.svgRect(0, 0, ratio, 1);
    SVGUtil.setAtt(rect, SVGConstants.SVG_CLASS_ATTRIBUTE, FRAMEID);
    newe.appendChild(rect);

    SVGPath path = new SVGPath();
    for(Pair<Double, Double> pair : curve) {
      path.drawTo(ratio * pair.getFirst(), 1.0 - pair.getSecond());
    }
    Element line = path.makeElement(plot);
    line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, SERIESID);
    newe.appendChild(line);
    plot.scheduleUpdate(new NodeReplaceChild(parent, newe));
  }
  
  @Override
  public Element visualize(SVGPlot svgp) {
    IterableResult<Pair<Double, Double>> curve = findCurveResult(context.getResult());
    
    // setup CSS
    try {
      CSSClass csscls = new CSSClass(this, SERIESID);
      csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.2%");
      csscls.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.CSS_RED_VALUE);
      csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      svgp.getCSSClassManager().addClass(csscls);
      CSSClass frmcls = new CSSClass(this, FRAMEID);
      frmcls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.2%");
      frmcls.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.CSS_SILVER_VALUE);
      frmcls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      svgp.getCSSClassManager().addClass(frmcls);
    }
    catch(CSSNamingConflict e) {
      logger.exception(e);
    }
    
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_SVG_TAG);
    // Some default for the view box.
    SVGUtil.setAtt(layer, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "-0.1 -0.1 1.2 1.2");
    
    updateCurve(svgp, layer, curve, 1.0);
    
    return layer;
  }
}
