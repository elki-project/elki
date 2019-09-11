/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.visualization.visualizers.scatterplot;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.data.type.TypeUtil;
import elki.database.ids.DBIDRef;
import elki.database.relation.Relation;
import elki.result.Metadata;
import elki.result.outlier.OutlierResult;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.css.CSSClass;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projector.ScatterPlotProjector;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGPlot;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;

/**
 * Generates a SVG-Element containing Tooltips. Tooltips remain invisible until
 * their corresponding Marker is touched by the cursor and stay visible as long
 * as the cursor lingers on the marker.
 *
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class TooltipScoreVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Outlier Score Tooltips";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME_GEN = " Tooltips";

  /**
   * Settings
   */
  protected Par settings;

  /**
   * Constructor.
   *
   * @param settings Settings
   */
  public TooltipScoreVisualization(Par settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    // TODO: we can also visualize other scores!
    VisualizationTree.findNewSiblings(context, result, OutlierResult.class, ScatterPlotProjector.class, (o, p) -> {
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      addTooltips(Metadata.of(o).getLongName() + NAME_GEN, o.getScores(), context, p, rel);
    });
    VisualizationTree.findNewSiblings(context, result, Relation.class, ScatterPlotProjector.class, (r, p) -> {
      if(Metadata.hierarchyOf(r).iterParents().filter(OutlierResult.class).valid()) {
        return; // Handled by above case already.
      }
      if(!TypeUtil.DOUBLE.isAssignableFromType(r.getDataTypeInformation()) && !TypeUtil.INTEGER.isAssignableFromType(r.getDataTypeInformation())) {
        return;
      }
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      addTooltips(r.getLongName() + NAME_GEN, r, context, p, rel);
    });
  }

  /**
   * Add tooltips.
   * 
   * @param nam Name
   * @param val Value relation
   * @param context Visualization context
   * @param p Projector
   * @param rel Data projection relation
   */
  private void addTooltips(String nam, Relation<?> val, VisualizerContext context, ScatterPlotProjector<?> p, Relation<?> rel) {
    final VisualizationTask task = new VisualizationTask(this, nam, val, rel) //
        .tool(true).visibility(false) //
        .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SAMPLE);
    context.addVis(val, task);
    context.addVis(p, task);
  }

  /**
   * Instance
   *
   * @author Remigius Wojdanowski
   * @author Erich Schubert
   */
  public class Instance extends AbstractTooltipVisualization {
    /**
     * Number value to visualize
     */
    private Relation<? extends Number> result;

    /**
     * Font size to use.
     */
    private double fontsize;

    /**
     * Constructor
     *
     * @param context Visualizer context
     * @param task Task
     * @param plot Plot
     * @param width Width
     * @param height Height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      this.result = task.getResult();
      final StyleLibrary style = context.getStyleLibrary();
      this.fontsize = 3 * style.getTextSize(StyleLibrary.PLOT);
      addListeners();
    }

    @Override
    protected Element makeTooltip(DBIDRef id, double x, double y, double dotsize) {
      return svgp.svgText(x + dotsize, y + fontsize * 0.07, settings.nf.format(result.get(id).doubleValue()));
    }

    /**
     * Registers the Tooltip-CSS-Class at a SVGPlot.
     *
     * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
     */
    @Override
    protected void setupCSS(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
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

      svgp.updateStyleElement();
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Number formatter used for visualization
     */
    NumberFormat nf = null;

    /**
     * Parameter for the gamma-correction.
     */
    public static final OptionID DIGITS_ID = new OptionID("tooltip.digits", "Number of digits to show (e.g. when visualizing outlier scores)");

    @Override
    public void configure(Parameterization config) {
      new IntParameter(DIGITS_ID, 4) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> {
            nf = NumberFormat.getInstance(Locale.ROOT);
            nf.setGroupingUsed(false);
            nf.setMaximumFractionDigits(x);
          });
    }

    @Override
    public TooltipScoreVisualization make() {
      return new TooltipScoreVisualization(this);
    }
  }
}
