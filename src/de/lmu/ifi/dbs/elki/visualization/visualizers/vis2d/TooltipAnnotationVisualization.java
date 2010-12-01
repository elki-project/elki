package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.AnnotationBuiltins;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.IDResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Generates a SVG-Element containing Tooltips. Tooltips remain invisible until
 * their corresponding Marker is touched by the cursor and stay visible as long
 * as the cursor lingers on the marker.
 * 
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.result.AnnotationResult oneway - - visualizes
 * 
 * @param <NV> Data type visualized.
 */
public class TooltipAnnotationVisualization<NV extends NumberVector<NV, ?>> extends TooltipVisualization<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Tooltips";

  /**
   * Number value to visualize
   */
  private AnnotationResult<?> result;

  /**
   * Font size to use.
   */
  private double fontsize;

  public TooltipAnnotationVisualization(VisualizationTask task) {
    super(task);
    this.result = task.getResult();
    this.fontsize = 3 * context.getStyleLibrary().getTextSize(StyleLibrary.PLOT);
    synchronizedRedraw();
  }

  @Override
  protected Element makeTooltip(DBID id, double x, double y, double dotsize) {
    final Object data = result.getValueFor(id);
    String label;
    if(data == null) {
      label = "null";
    }
    else {
      label = data.toString();
    }
    if(label == "" || label == null) {
      label = "null";
    }
    return svgp.svgText(x + dotsize, y + fontsize * 0.07, label);
  }

  /**
   * Registers the Tooltip-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
   */
  @Override
  protected void setupCSS(SVGPlot svgp) {
    double fontsize = context.getStyleLibrary().getTextSize(StyleLibrary.PLOT);

    CSSClass tooltiphidden = new CSSClass(svgp, TOOLTIP_HIDDEN);
    tooltiphidden.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
    tooltiphidden.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.PLOT));
    tooltiphidden.setStatement(SVGConstants.CSS_DISPLAY_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    svgp.addCSSClassOrLogError(tooltiphidden);

    CSSClass tooltipvisible = new CSSClass(svgp, TOOLTIP_VISIBLE);
    tooltipvisible.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
    tooltipvisible.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.PLOT));
    svgp.addCSSClassOrLogError(tooltipvisible);

    CSSClass tooltipsticky = new CSSClass(svgp, TOOLTIP_STICKY);
    tooltipsticky.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
    tooltipsticky.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.PLOT));
    svgp.addCSSClassOrLogError(tooltipsticky);

    // invisible but sensitive area for the tooltip activator
    CSSClass tooltiparea = new CSSClass(svgp, TOOLTIP_AREA);
    tooltiparea.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
    tooltiparea.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    tooltiparea.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0");
    tooltiparea.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);
    svgp.addCSSClassOrLogError(tooltiparea);
  }

  /**
   * Factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has TooltipAnnotationVisualization oneway - - produces
   * 
   * @param <NV>
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends TooltipVisualization.Factory<NV, AnnotationResult<?>> {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super(NAME);
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new TooltipAnnotationVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, AnyResult result) {
      if(!VisualizerUtil.isNumberVectorDatabase(context.getDatabase())) {
        return;
      }
      // Label results.
      ArrayList<IDResult> idlabels = ResultUtil.filterResults(result, IDResult.class);
      for(IDResult ir : idlabels) {
        // ivis.init(context, ir, "Object ID");
        context.addVisualizer(ir, this);
      }
      ArrayList<AnnotationBuiltins.ExternalIDAnnotation> eidlabels = ResultUtil.filterResults(result, AnnotationBuiltins.ExternalIDAnnotation.class);
      for(AnnotationBuiltins.ExternalIDAnnotation eir : eidlabels) {
        // evis.init(context, eir, "External ID");
        context.addVisualizer(eir, this);
      }
      ArrayList<AnnotationBuiltins.ClassLabelAnnotation> classlabels = ResultUtil.filterResults(result, AnnotationBuiltins.ClassLabelAnnotation.class);
      for(AnnotationBuiltins.ClassLabelAnnotation clr : classlabels) {
        // cvis.init(context, clr, "Class Label");
        context.addVisualizer(clr, this);
      }
      ArrayList<AnnotationBuiltins.ObjectLabelAnnotation> objlabels = ResultUtil.filterResults(result, AnnotationBuiltins.ObjectLabelAnnotation.class);
      for(AnnotationBuiltins.ObjectLabelAnnotation olr : objlabels) {
        // ovis.init(context, olr, "Object Label");
        context.addVisualizer(olr, this);
      }
    }
  }
}