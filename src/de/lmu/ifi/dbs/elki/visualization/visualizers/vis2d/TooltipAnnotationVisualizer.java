package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
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
 * @author Erich Schubert
 * 
 * @apiviz.has TooltipAnnotationVisualization oneway - - produces
 * 
 * @param <NV> Data type visualized.
 */
public class TooltipAnnotationVisualizer<NV extends NumberVector<NV, ?>> extends TooltipVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Tooltips";

  /**
   * Class label annotation to visualize
   */
  private AnnotationResult<?> result;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public TooltipAnnotationVisualizer() {
    super(NAME);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param result the outlier score visualized
   */
  public void init(VisualizerContext<? extends NV> context, AnnotationResult<?> result, String name) {
    super.init(context);
    super.setName(name);
    this.result = result;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new TooltipAnnotationVisualization<NV>(context, svgp, proj, width, height, result);
  }

  /**
   * The actual visualization instance, for a single projection
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has de.lmu.ifi.dbs.elki.result.AnnotationResult oneway - - visualizes
   */
  protected static class TooltipAnnotationVisualization<NV extends NumberVector<NV, ?>> extends TooltipVisualization<NV> {
    /**
     * Number value to visualize
     */
    private AnnotationResult<?> result;

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
     */
    public TooltipAnnotationVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height, AnnotationResult<?> result) {
      super(context, svgp, proj, width, height);
      this.result = result;
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
  }
}