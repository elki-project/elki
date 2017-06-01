/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.IntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationMenuAction;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AxisVisualization;

import net.jafama.FastMath;

/**
 * Dendrogram visualizer.
 *
 * @author Erich Schubert
 * @since 0.7.1
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class DendrogramVisualization implements VisFactory {
  /**
   * Visualizer name.
   */
  private static final String NAME = "Dendrogram";

  /**
   * Styles for dendrograms.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static enum Style {
    RECTANGULAR, //
    TRIANGULAR, //
  }

  /**
   * Drawing style.
   */
  private Style style = Style.RECTANGULAR;

  /**
   * Constructor.
   *
   * @param style Visualization style.
   */
  public DendrogramVisualization(Style style) {
    super();
    this.style = style;
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    // Ensure there is a clustering result:
    VisualizationTree.findNewResults(context, start).filter(PointerHierarchyRepresentationResult.class).forEach(pi -> {
      final VisualizationTask task = new VisualizationTask(this, NAME, pi, null) //
          .level(VisualizationTask.LEVEL_STATIC) //
          .with(UpdateFlag.ON_STYLEPOLICY);
      context.addVis(context.getStylingPolicy(), task);
      context.addVis(pi, new SwitchStyleAction(task, context));
    });
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height);
  }

  /**
   * Menu item to change visualization styles.
   *
   * @author Erich Schubert
   */
  public class SwitchStyleAction implements VisualizationMenuAction {
    /**
     * Task we represent.
     */
    private VisualizationTask task;

    /**
     * Visualizer context.
     */
    private VisualizerContext context;

    /**
     * Constructor.
     *
     * @param task Task
     * @param context Visualizer context
     */
    public SwitchStyleAction(VisualizationTask task, VisualizerContext context) {
      super();
      this.task = task;
      this.context = context;
    }

    @Override
    public String getMenuName() {
      return "Switch Dendrogram Style";
    }

    @Override
    public void activate() {
      switch(style){
      case RECTANGULAR:
        style = Style.TRIANGULAR;
        break;
      case TRIANGULAR:
        style = Style.RECTANGULAR;
        break;
      }
      context.visChanged(task);
    }

    @Override
    public boolean enabled() {
      return true;
    }
  }

  /**
   * Visualization instance.
   *
   * @author Erich Schubert
   */
  public class Instance extends AbstractVisualization {
    /**
     * CSS class for key captions.
     */
    private static final String KEY_CAPTION = "key-caption";

    /**
     * CSS class for hierarchy plot lines
     */
    private static final String KEY_HIERLINE = "key-hierarchy";

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height) {
      super(context, task, plot, width, height);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      layer = svgp.svgElement(SVGConstants.SVG_G_TAG);

      StyleLibrary style = context.getStyleLibrary();
      StylingPolicy spol = context.getStylingPolicy();

      PointerHierarchyRepresentationResult p = task.getResult();
      final boolean squared = p.isSquared();

      DBIDs ids = p.getDBIDs();
      DBIDDataStore par = p.getParentStore();
      DoubleDataStore pdi = p.getParentDistanceStore();
      IntegerDataStore pos = p.getPositions();
      DBIDVar pa = DBIDUtil.newVar();

      final int size = ids.size();
      double linew = StyleLibrary.SCALE * .1 / FastMath.log1p(size);
      double width = StyleLibrary.SCALE,
          height = width / getWidth() * getHeight();
      double xscale = width / size, xoff = xscale * .5;
      double maxh = Double.MIN_NORMAL;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        if(DBIDUtil.equal(it, par.assignVar(it, pa))) {
          continue; // Root
        }
        double v = pdi.doubleValue(it);
        if(v == Double.POSITIVE_INFINITY) {
          continue;
        }
        maxh = v > maxh ? v : maxh;
      }
      LinearScale yscale = new LinearScale(0, squared ? FastMath.sqrt(maxh) : maxh);
      // add axes
      try {
        SVGSimpleLinearAxis.drawAxis(svgp, layer, yscale, 0, height, 0, 0, SVGSimpleLinearAxis.LabelStyle.LEFTHAND, style);
        final double lxoff = style.getTextSize(StyleLibrary.AXIS_LABEL) * -3.5;
        Element label = svgp.svgText(lxoff, .5 * height, squared ? "sqrt(distance)" : "distance");
        CSSClass alcls = new CSSClass(AxisVisualization.class, "unmanaged");
        alcls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(1.1 * style.getTextSize(StyleLibrary.AXIS_LABEL)));
        alcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor(StyleLibrary.AXIS_LABEL));
        alcls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.AXIS_LABEL));

        SVGUtil.setAtt(label, SVGConstants.SVG_STYLE_ATTRIBUTE, alcls.inlineCSS());
        SVGUtil.setAtt(label, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
        SVGUtil.setAtt(label, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(-90," + lxoff + "," + .5 * height + ")");
        layer.appendChild(label);
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception(e);
      }
      // FIXME: add axis label.

      // Initial positions:
      double[] xy = new double[size << 1];
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        final int off = pos.intValue(it);
        xy[off] = off * xscale + xoff;
        xy[off + size] = height;
      }
      // Draw ascending by distance
      ArrayModifiableDBIDs order = DBIDUtil.newArray(ids);
      order.sort(new DataStoreUtil.AscendingByDoubleDataStoreAndId(pdi));

      if(spol instanceof ClassStylingPolicy) {
        ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
        setupCSS(svgp, cspol, linew);
        int mins = cspol.getMinStyle() - 1, maxs = cspol.getMaxStyle();
        SVGPath[] paths = new SVGPath[maxs - mins + 1];
        for(int i = 0; i < paths.length; i++) {
          paths[i] = new SVGPath();
        }
        for(DBIDIter it = order.iter(); it.valid(); it.advance()) {
          par.assignVar(it, pa); // Get parent.
          double h = pdi.doubleValue(it);
          final int o1 = pos.intValue(it);
          final int p1 = cspol.getStyleForDBID(it);
          double x1 = xy[o1], y1 = xy[o1 + size];
          if(DBIDUtil.equal(it, pa)) {
            paths[p1 - mins + 1].moveTo(x1, y1).verticalLineTo(height * (1 - yscale.getScaled(squared ? FastMath.sqrt(h) : h)));
            continue; // Root
          }
          final int o2 = pos.intValue(pa);
          final int p2 = cspol.getStyleForDBID(pa);
          double x2 = xy[o2], y2 = xy[o2 + size];
          double x3 = (x1 + x2) * .5,
              y3 = height * (1 - yscale.getScaled(squared ? FastMath.sqrt(h) : h));
          switch(DendrogramVisualization.this.style){
          case RECTANGULAR:
            if(p1 == p2) {
              paths[p1 - mins + 1].moveTo(x1, y1).verticalLineTo(y3).horizontalLineTo(x2).verticalLineTo(y2);
            }
            else {
              paths[y1 == height ? p1 - mins + 1 : 0].moveTo(x1, y1).verticalLineTo(y3);
              paths[y2 == height ? p2 - mins + 1 : 0].moveTo(x2, y2).verticalLineTo(y3);
              paths[0].moveTo(x1, y3).horizontalLineTo(x2);
            }
            break;
          case TRIANGULAR:
            if(p1 == p2) {
              paths[p1 - mins + 1].moveTo(x1, y1).drawTo(x3, y3).drawTo(x2, y2);
            }
            else {
              paths[y1 == height ? p1 - mins + 1 : 0].moveTo(x1, y1).drawTo(x3, y3);
              paths[y2 == height ? p2 - mins + 1 : 0].moveTo(x2, y2).drawTo(x3, y3);
            }
            break;
          }
          xy[o2] = x3;
          xy[o2 + size] = y3;
        }
        for(int i = 0; i < paths.length; i++) {
          SVGPath path = paths[i];
          if(!path.isStarted()) {
            continue;
          }
          Element elem = path.makeElement(svgp);
          SVGUtil.setCSSClass(elem, (i > 0 ? KEY_HIERLINE + "_" + (i + mins - 1) : KEY_HIERLINE));
          layer.appendChild(elem);
        }
      }
      else {
        setupCSS(svgp, linew);
        SVGPath dendrogram = new SVGPath();

        for(DBIDIter it = order.iter(); it.valid(); it.advance()) {
          double h = pdi.doubleValue(it);
          final int o1 = pos.intValue(it);
          double x1 = xy[o1], y1 = xy[o1 + size];
          if(DBIDUtil.equal(it, par.assignVar(it, pa))) {
            dendrogram.moveTo(x1, y1).verticalLineTo(height * (1 - yscale.getScaled(squared ? FastMath.sqrt(h) : h)));
            continue; // Root
          }
          final int o2 = pos.intValue(pa);
          double x2 = xy[o2], y2 = xy[o2 + size];
          double x3 = (x1 + x2) * .5,
              y3 = height * (1 - yscale.getScaled(squared ? FastMath.sqrt(h) : h));
          switch(DendrogramVisualization.this.style){
          case RECTANGULAR:
            dendrogram.moveTo(x1, y1).verticalLineTo(y3).horizontalLineTo(x2).verticalLineTo(y2);
            break;
          case TRIANGULAR:
            dendrogram.moveTo(x1, y1).drawTo(x3, y3).drawTo(x2, y2);
            break;
          }
          xy[o2] = x3;
          xy[o2 + size] = y3;
        }
        Element elem = dendrogram.makeElement(svgp);
        SVGUtil.setCSSClass(elem, KEY_HIERLINE);
        layer.appendChild(elem);
      }

      final double margin = style.getSize(StyleLibrary.MARGIN);
      final String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), width, height, 2. * margin, margin, margin, margin);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    }

    /**
     * Register the CSS classes.
     *
     * @param svgp the SVGPlot to register the CSS classes.
     * @param linew Line width adjustment
     */
    protected void setupCSS(SVGPlot svgp, double linew) {
      final StyleLibrary style = context.getStyleLibrary();
      final double fontsize = style.getTextSize(StyleLibrary.KEY);
      final String fontfamily = style.getFontFamily(StyleLibrary.KEY);
      final String color = style.getColor(StyleLibrary.KEY);

      CSSClass keycaption = new CSSClass(svgp, KEY_CAPTION);
      keycaption.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
      keycaption.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, fontfamily);
      keycaption.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
      keycaption.setStatement(SVGConstants.CSS_FONT_WEIGHT_PROPERTY, SVGConstants.CSS_BOLD_VALUE);
      svgp.addCSSClassOrLogError(keycaption);

      CSSClass hierline = new CSSClass(svgp, KEY_HIERLINE);
      hierline.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
      hierline.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, linew * style.getLineWidth("key.hierarchy") / StyleLibrary.SCALE);
      hierline.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      hierline.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(hierline);

      svgp.updateStyleElement();
    }

    /**
     * Register the CSS classes.
     *
     * @param svgp the SVGPlot to register the CSS classes.
     * @param cspol Class styling policy
     * @param linew Line width adjustment
     */
    protected void setupCSS(SVGPlot svgp, ClassStylingPolicy cspol, double linew) {
      setupCSS(svgp, linew);
      StyleLibrary style = context.getStyleLibrary();
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      for(int i = cspol.getMinStyle(); i <= cspol.getMaxStyle(); i++) {
        CSSClass hierline = new CSSClass(svgp, KEY_HIERLINE + "_" + i);
        hierline.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
        hierline.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, linew * style.getLineWidth("key.hierarchy") / StyleLibrary.SCALE);
        hierline.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        hierline.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        svgp.addCSSClassOrLogError(hierline);
      }

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
     * Dendrogram drawing style.
     */
    public static final OptionID STYLE_ID = new OptionID("dendrogram.style", "Drawing style for dendrograms.");

    /**
     * Drawing style.
     */
    private Style style = Style.RECTANGULAR;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      EnumParameter<Style> styleP = new EnumParameter<>(STYLE_ID, Style.class, Style.RECTANGULAR);
      if(config.grab(styleP)) {
        style = styleP.getValue();
      }
    }

    @Override
    protected DendrogramVisualization makeInstance() {
      return new DendrogramVisualization(style);
    }
  }
}
