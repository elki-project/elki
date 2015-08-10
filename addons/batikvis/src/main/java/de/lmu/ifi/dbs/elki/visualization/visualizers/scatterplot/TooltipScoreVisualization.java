package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
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
public class TooltipScoreVisualization extends AbstractVisFactory {
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
  protected Parameterizer settings;

  /**
   * Constructor.
   *
   * @param settings Settings
   */
  public TooltipScoreVisualization(Parameterizer settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    final ResultHierarchy hier = context.getHierarchy();
    // TODO: we can also visualize other scores!
    VisualizerUtil.findNewResultVis(context, result, OutlierResult.class, ScatterPlotProjector.class, new VisualizerUtil.Handler2<OutlierResult, ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, OutlierResult o, ScatterPlotProjector<?> p) {
        final VisualizationTask task = new VisualizationTask(NAME, o.getScores(), p.getRelation(), TooltipScoreVisualization.this);
        task.tool = true;
        task.initDefaultVisibility(false);
        context.addVis(o.getScores(), task);
        context.addVis(p, task);
      }
    });
    VisualizerUtil.findNewResultVis(context, result, DoubleRelation.class, ScatterPlotProjector.class, new VisualizerUtil.Handler2<DoubleRelation, ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, DoubleRelation r, ScatterPlotProjector<?> p) {
        for(Iter<Result> it = hier.iterParents(r); it.valid(); it.advance()) {
          if(it instanceof OutlierResult) {
            return; // Handled by above case already.
          }
        }
        final VisualizationTask task = new VisualizationTask(NAME, r, p.getRelation(), TooltipScoreVisualization.this);
        task.tool = true;
        task.initDefaultVisibility(false);
        context.addVis(r, task);
        context.addVis(p, task);
      }
    });
    VisualizerUtil.findNewResultVis(context, result, Relation.class, ScatterPlotProjector.class, new VisualizerUtil.Handler2<Relation<?>, ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, Relation<?> r, ScatterPlotProjector<?> p) {
        if(r instanceof DoubleRelation) {
          return; // Handled above already.
        }
        if(!TypeUtil.DOUBLE.isAssignableFromType(r.getDataTypeInformation()) && !TypeUtil.INTEGER.isAssignableFromType(r.getDataTypeInformation())) {
          return;
        }
        final VisualizationTask task = new VisualizationTask(r.getLongName() + NAME_GEN, r, p.getRelation(), TooltipScoreVisualization.this);
        task.tool = true;
        task.initDefaultVisibility(false);
        context.addVis(r, task);
        context.addVis(p, task);
      }
    });
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
      return svgp.svgText(x + dotsize, y + fontsize * 0.07, settings.nf.format(result.get(id).doubleValue()));
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

      svgp.updateStyleElement();
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Number formatter used for visualization
     */
    NumberFormat nf = null;

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
    public static final OptionID DIGITS_ID = new OptionID("tooltip.digits", "Number of digits to show (e.g. when visualizing outlier scores)");

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter digitsP = new IntParameter(DIGITS_ID, 4);
      digitsP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);

      if(config.grab(digitsP)) {
        int digits = digitsP.intValue();
        nf = NumberFormat.getInstance(Locale.ROOT);
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(digits);
      }
    }

    @Override
    protected TooltipScoreVisualization makeInstance() {
      return new TooltipScoreVisualization(this);
    }
  }
}