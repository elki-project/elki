package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Generates a SVG-Element containing Tooltips. Tooltips remain invisible until
 * their corresponding Marker is touched by the cursor and stay visible as long
 * as the cursor lingers on the marker.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Data type visualized.
 */
public class TooltipScoreVisualizer<NV extends NumberVector<NV, ?>> extends TooltipVisualizer<NV> {
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
   * < /p>
   */
  private final IntParameter DIGITS_PARAM = new IntParameter(DIGITS_ID, new GreaterEqualConstraint(0), 4);

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Outlier Score Tooltips";

  /**
   * Contains the "outlierness-scores" to be displayed as Tooltips. If this
   * result does not contain <b>all</b> IDs the database contains, behavior is
   * undefined.
   */
  protected AnnotationResult<? extends Number> result;

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
  public TooltipScoreVisualizer(Parameterization config) {
    super(NAME);
    config = config.descend(this);
    if(config.grab(DIGITS_PARAM)) {
      int digits = DIGITS_PARAM.getValue();
      nf.setGroupingUsed(false);
      nf.setMaximumFractionDigits(digits);
    }
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param result the outlier score visualized
   */
  public void init(VisualizerContext<? extends NV> context, OutlierResult result) {
    super.init(context);
    this.result = result.getScores();
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new TooltipScoreVisualization<NV>(context, svgp, proj, width, height, result, nf);
  }

  /**
   * The actual visualization instance, for a single projection
   * 
   * @author Erich Schubert
   */
  protected static class TooltipScoreVisualization<NV extends NumberVector<NV, ?>> extends TooltipVisualization<NV> {
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
    
    /**
     * Constructor.
     * 
     * @param context Context
     * @param svgp Plot
     * @param proj Projection
     * @param width Width
     * @param height Height
     * @param result Result to visualizer
     * @param nf Number format
     */
    public TooltipScoreVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height, AnnotationResult<? extends Number> result, NumberFormat nf) {
      super(context, svgp, proj, width, height);
      this.nf = nf;
      this.result = result;
      this.fontsize = 3 * context.getStyleLibrary().getTextSize(StyleLibrary.PLOT);
      synchronizedRedraw();
    }

    @Override
    protected Element makeTooltip(DBID id, double x, double y, double dotsize) {
      return svgp.svgText(x + dotsize, y + fontsize * 0.07, nf.format(getValue(id).doubleValue()));
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
     * Returns the outlierness-score for a given ID. If there is no
     * corresponding score, behavior is undefined.
     * 
     * @param id an ID which has to exist in both the database and the result.
     * @return the outlierness-score for a given ID.
     */
    private Number getValue(DBID id) {
      return result.getValueFor(id);
    }

    @Override
    public void objectsChanged(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }

    @Override
    public void objectsInserted(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }

    @Override
    public void objectsRemoved(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }
  }
}