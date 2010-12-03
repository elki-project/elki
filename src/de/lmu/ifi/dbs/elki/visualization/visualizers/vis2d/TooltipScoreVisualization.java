package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
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
 * 
 * @apiviz.has AnnotationResult oneway - - visualizes
 */
public class TooltipScoreVisualization<NV extends NumberVector<NV, ?>> extends TooltipVisualization<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Outlier Score Tooltips";

  /**
   * Number format.
   */
  NumberFormat nf;

  /**
   * Number value to visualize
   */
  private AnnotationResult<? extends Number> result;

  /**
   * Font size to use.
   */
  private double fontsize;

  public TooltipScoreVisualization(VisualizationTask task, NumberFormat nf) {
    super(task);
    this.result = task.getResult();
    this.nf = nf;
    this.fontsize = 3 * context.getStyleLibrary().getTextSize(StyleLibrary.PLOT);
    synchronizedRedraw();
  }

  @Override
  protected Element makeTooltip(DBID id, double x, double y, double dotsize) {
    return svgp.svgText(x + dotsize, y + fontsize * 0.07, nf.format(result.getValueFor(id).doubleValue()));
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
   * Factory for tooltip visualizers
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses TooltipScoreVisualization oneway - - «create»
   * 
   * @param <NV> Data type visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends TooltipVisualization.Factory<NV, AnnotationResult<? extends Number>> {
    /**
     * OptionID for {@link #DIGITS_PARAM}.
     */
    public static final OptionID DIGITS_ID = OptionID.getOrCreateOptionID("tooltip.digits", "Number of digits to show (e.g. when visualizing outlier scores)");

    /**
     * Parameter for the gamma-correction.
     * 
     * <p>
     * Key: {@code -tooltip.digits}
     * </p>
     * 
     * <p>
     * Default value: 4
     * </p>
     */
    private final IntParameter DIGITS_PARAM = new IntParameter(DIGITS_ID, new GreaterEqualConstraint(0), 4);

    /**
     * Number formatter used for visualization
     */
    NumberFormat nf = NumberFormat.getInstance(Locale.ROOT);

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super(NAME);
      config = config.descend(this);
      if(config.grab(DIGITS_PARAM)) {
        int digits = DIGITS_PARAM.getValue();
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(digits);
      }
    }
    
    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new TooltipScoreVisualization<NV>(task, nf);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, AnyResult result) {
      if(!VisualizerUtil.isNumberVectorDatabase(context.getDatabase())) {
        return;
      }
      // TODO: we can also visualize other scores!
      List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
      for(OutlierResult o : ors) {
        context.addVisualizer(o.getScores(), this);
      }
    }
  }
}