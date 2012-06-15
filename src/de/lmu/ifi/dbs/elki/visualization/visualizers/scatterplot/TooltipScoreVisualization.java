package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.List;
import java.util.Locale;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Generates a SVG-Element containing Tooltips. Tooltips remain invisible until
 * their corresponding Marker is touched by the cursor and stay visible as long
 * as the cursor lingers on the marker.
 * 
 * @author Remigius Wojdanowski
 */
public class TooltipScoreVisualization extends AbstractTooltipVisualization {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Outlier Score Tooltips";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME_GEN = " Tooltips";

  /**
   * Number format.
   */
  NumberFormat nf;

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
   * @param nf Number Format
   */
  public TooltipScoreVisualization(VisualizationTask task, NumberFormat nf) {
    super(task);
    this.result = task.getResult();
    this.nf = nf;
    this.fontsize = 3 * context.getStyleLibrary().getTextSize(StyleLibrary.PLOT);
    synchronizedRedraw();
  }

  @Override
  protected Element makeTooltip(DBID id, double x, double y, double dotsize) {
    return svgp.svgText(x + dotsize, y + fontsize * 0.07, nf.format(result.get(id).doubleValue()));
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

  /**
   * Factory for tooltip visualizers
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses TooltipScoreVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
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
    public static final OptionID DIGITS_ID = OptionID.getOrCreateOptionID("tooltip.digits", "Number of digits to show (e.g. when visualizing outlier scores)");

    /**
     * Number formatter used for visualization
     */
    NumberFormat nf = null;

    /**
     * Constructor.
     * 
     * @param digits number of digits
     */
    public Factory(int digits) {
      super();
      nf = NumberFormat.getInstance(Locale.ROOT);
      nf.setGroupingUsed(false);
      nf.setMaximumFractionDigits(digits);
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new TooltipScoreVisualization(task, nf);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // TODO: we can also visualize other scores!
      List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
      for(OutlierResult o : ors) {
        IterableIterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
        for(ScatterPlotProjector<?> p : ps) {
          final VisualizationTask task = new VisualizationTask(NAME, o.getScores(), p.getRelation(), this);
          task.put(VisualizationTask.META_TOOL, true);
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
          baseResult.getHierarchy().add(o.getScores(), task);
          baseResult.getHierarchy().add(p, task);
        }
      }
      List<Relation<?>> rrs = ResultUtil.filterResults(result, Relation.class);
      for(Relation<?> r : rrs) {
        if(!TypeUtil.DOUBLE.isAssignableFromType(r.getDataTypeInformation()) && !TypeUtil.INTEGER.isAssignableFromType(r.getDataTypeInformation())) {
          continue;
        }
        // Skip if we already considered it above
        boolean add = true;
        for(Result p : baseResult.getHierarchy().getChildren(r)) {
          if(p instanceof VisualizationTask && ((VisualizationTask) p).getFactory() instanceof Factory) {
            add = false;
            break;
          }
        }
        if(add) {
          IterableIterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
          for(ScatterPlotProjector<?> p : ps) {
            final VisualizationTask task = new VisualizationTask(r.getLongName() + NAME_GEN, r, p.getRelation(), this);
            task.put(VisualizationTask.META_TOOL, true);
            task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
            baseResult.getHierarchy().add(r, task);
            baseResult.getHierarchy().add(p, task);
          }
        }
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
      protected int digits = 4;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        IntParameter DIGITS_PARAM = new IntParameter(DIGITS_ID, new GreaterEqualConstraint(0), 4);

        if(config.grab(DIGITS_PARAM)) {
          digits = DIGITS_PARAM.getValue();
        }
      }

      @Override
      protected Factory makeInstance() {
        return new Factory(digits);
      }
    }
  }
}