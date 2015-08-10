package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Generates a SVG-Element containing Tooltips. Tooltips remain invisible until
 * their corresponding Marker is touched by the cursor and stay visible as long
 * as the cursor lingers on the marker.
 *
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class TooltipStringVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME_ID = "ID Tooltips";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME_LABEL = "Object Label Tooltips";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME_CLASS = "Class Label Tooltips";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME_EID = "External ID Tooltips";

  /**
   * Constructor.
   */
  public TooltipStringVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizerUtil.findNewResultVis(context, result, Relation.class, ScatterPlotProjector.class, new VisualizerUtil.Handler2<Relation<?>, ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, Relation<?> rep, ScatterPlotProjector<?> p) {
        if(DBID.class.isAssignableFrom(rep.getDataTypeInformation().getRestrictionClass())) {
          final VisualizationTask task = new VisualizationTask(NAME_ID, rep, p.getRelation(), TooltipStringVisualization.this);
          task.tool = true;
          task.initDefaultVisibility(false);
          context.addVis(rep, task);
          context.addVis(p, task);
        }
        if(ClassLabel.class.isAssignableFrom(rep.getDataTypeInformation().getRestrictionClass())) {
          final VisualizationTask task = new VisualizationTask(NAME_CLASS, rep, p.getRelation(), TooltipStringVisualization.this);
          task.tool = true;
          task.initDefaultVisibility(false);
          context.addVis(rep, task);
          context.addVis(p, task);
        }
        if(LabelList.class.isAssignableFrom(rep.getDataTypeInformation().getRestrictionClass())) {
          final VisualizationTask task = new VisualizationTask(NAME_LABEL, rep, p.getRelation(), TooltipStringVisualization.this);
          task.tool = true;
          task.initDefaultVisibility(false);
          context.addVis(rep, task);
          context.addVis(p, task);
        }
        if(ExternalID.class.isAssignableFrom(rep.getDataTypeInformation().getRestrictionClass())) {
          final VisualizationTask task = new VisualizationTask(NAME_EID, rep, p.getRelation(), TooltipStringVisualization.this);
          task.tool = true;
          task.initDefaultVisibility(false);
          context.addVis(rep, task);
          context.addVis(p, task);
        }
      }
    });
  }

  /**
   * Instance
   *
   * @author Remigius Wojdanowski
   * @author Erich Schubert
   *
   * @apiviz.has Relation oneway - - visualizes
   */
  public class Instance extends AbstractTooltipVisualization {
    /**
     * Number value to visualize
     */
    private Relation<?> result;

    /**
     * Font size to use.
     */
    private double fontsize;

    /**
     * Constructor.
     *
     * @param task Task
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.result = task.getResult();
      final StyleLibrary style = context.getStyleResult().getStyleLibrary();
      this.fontsize = 3 * style.getTextSize(StyleLibrary.PLOT);
      synchronizedRedraw();
    }

    @Override
    protected Element makeTooltip(DBIDRef id, double x, double y, double dotsize) {
      final Object data = result.get(id);
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
      final StyleLibrary style = context.getStyleResult().getStyleLibrary();
      final double fontsize = style.getTextSize(StyleLibrary.PLOT);
      final String fontfamily = style.getFontFamily(StyleLibrary.PLOT);

      CSSClass tooltiphidden = new CSSClass(svgp, TOOLTIP_HIDDEN);
      tooltiphidden.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
      tooltiphidden.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, fontfamily);
      tooltiphidden.setStatement(SVGConstants.CSS_DISPLAY_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(tooltiphidden);

      CSSClass tooltipvisible = new CSSClass(svgp, TOOLTIP_VISIBLE);
      tooltipvisible.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
      tooltipvisible.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, fontfamily);
      svgp.addCSSClassOrLogError(tooltipvisible);

      CSSClass tooltipsticky = new CSSClass(svgp, TOOLTIP_STICKY);
      tooltipsticky.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
      tooltipsticky.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, fontfamily);
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