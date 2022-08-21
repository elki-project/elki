/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.visualization.visualizers.visunproj;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.logging.LoggingUtil;
import elki.math.scales.LinearScale;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.visualization.VisualizationMenuAction;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.colors.ColorLibrary;
import elki.visualization.css.CSSClass;
import elki.visualization.css.CSSClassManager.CSSNamingConflict;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.style.ClassStylingPolicy;
import elki.visualization.style.StyleLibrary;
import elki.visualization.style.StylingPolicy;
import elki.visualization.svg.SVGPath;
import elki.visualization.svg.SVGPlot;
import elki.visualization.svg.SVGSimpleLinearAxis;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.AbstractVisualization;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;
import elki.visualization.visualizers.scatterplot.AxisVisualization;

import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import net.jafama.FastMath;

/**
 * Dendrogram visualizer.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @stereotype factory
 * @navassoc - create - Instance
 * @composed - - - SwitchStyleAction
 * @composed - - - DrawingStyle
 * @composed - - - PositionStyle
 * @has - - - Positions
 */
public class DendrogramVisualization implements VisFactory {
  /**
   * Visualizer name.
   */
  private static final String NAME = "Dendrogram";

  /**
   * Drawing styles for dendrograms.
   *
   * @author Erich Schubert
   */
  public enum DrawingStyle {
    RECTANGULAR, //
    TRIANGULAR_MAX, //
    TRIANGULAR, //
  }

  /**
   * Positioning style
   *
   * @author Erich Schubert
   */
  public enum PositionStyle {
    HALF_POS, //
    HALF_WIDTH, //
  }

  /**
   * Drawing style.
   */
  private DrawingStyle style = DrawingStyle.RECTANGULAR;

  /**
   * Position style.
   */
  private PositionStyle style2 = PositionStyle.HALF_POS;

  /**
   * Constructor.
   *
   * @param style Drawing style.
   * @param style2 position style.
   */
  public DendrogramVisualization(DrawingStyle style, PositionStyle style2) {
    super();
    this.style = style;
    this.style2 = style2;
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    // Ensure there is a clustering result:
    VisualizationTree.findNewResults(context, start).filter(ClusterMergeHistory.class).forEach(pi -> {
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
        style = DrawingStyle.TRIANGULAR_MAX;
        break;
      case TRIANGULAR_MAX:
        style = DrawingStyle.TRIANGULAR;
        break;
      case TRIANGULAR:
        style = DrawingStyle.RECTANGULAR;
        // Switch position style
        style2 = style2 == PositionStyle.HALF_POS ? PositionStyle.HALF_WIDTH : PositionStyle.HALF_POS;
        break;
      }
      context.visChanged(task);
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

      ClusterMergeHistory p = task.getResult();
      final boolean squared = p.isSquared();
      int[] pos = p.getPositions();

      final int size = p.size(), m = p.numMerges();
      double linew = StyleLibrary.SCALE * .1 / FastMath.log1p(size);
      double width = StyleLibrary.SCALE;
      double height = width / getWidth() * getHeight();
      double xscale = width / size, xoff = xscale * .5;
      double maxh = Double.MIN_NORMAL;
      for(int i = 0, n = p.numMerges(); i < n; i++) {
        double v = p.getMergeHeight(i);
        maxh = (v < Double.POSITIVE_INFINITY && v > maxh) ? v : maxh;
      }
      // TODO: add a SqrtScale!
      LinearScale yscale = new LinearScale(0, squared ? Math.sqrt(maxh) : maxh);
      // Y projection function
      Double2DoubleFunction proy = squared ? //
          (h -> height * (1 - yscale.getScaled(Math.sqrt(h)))) : //
          (h -> height * (1 - yscale.getScaled(h)));
      // Draw axes
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

      // Initial positions:
      Positions coord = style2 == PositionStyle.HALF_POS ? //
          new HalfPosPositions(size + m) : new HalfWidthPositions(size + m);
      for(int i = 0; i < size; i++) {
        coord.set(i, pos[i] * xscale + xoff, height);
      }
      // Draw in merge order
      if(spol instanceof ClassStylingPolicy) {
        ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
        setupCSS(svgp, cspol, linew);
        int mins = cspol.getMinStyle() - 1, maxs = cspol.getMaxStyle();
        SVGPath[] paths = new SVGPath[maxs - mins + 1];
        for(int i = 0; i < paths.length; i++) {
          paths[i] = new SVGPath();
        }
        // Cluster color of each point/cluster:
        int[] pcol = new int[size + m];
        DBIDVar tmp = DBIDUtil.newVar();
        for(int i = 0; i < size; i++) {
          pcol[i] = cspol.getStyleForDBID(p.assignVar(i, tmp));
        }
        for(int i = 0; i < m; i++) {
          int a = p.getMergeA(i), b = p.getMergeB(i);
          double h = p.getMergeHeight(i);
          int pa = pcol[a], pb = pcol[b];
          double xa = coord.getX(a), ya = coord.getY(a);
          double xb = coord.getX(b), yb = coord.getY(b);
          double yc = proy.applyAsDouble(h);
          double xc = coord.combine(a, b, yc, i + size);
          pcol[i + size] = pa == pb ? pa : mins - 1;
          if(!Double.isFinite(xa) || !Double.isFinite(ya) || !Double.isFinite(xa) || !Double.isFinite(ya) || !Double.isFinite(xa) || !Double.isFinite(ya)) {
            LoggingUtil.warning("Infinite or NaN values in dendrogram.");
            continue;
          }
          switch(DendrogramVisualization.this.style){
          case RECTANGULAR:
            if(pa == pb) {
              paths[pa - mins + 1].moveTo(xa, ya).verticalLineTo(yc).horizontalLineTo(xb).verticalLineTo(yb);
            }
            else {
              paths[ya == height ? pa - mins + 1 : 0].moveTo(xa, ya).verticalLineTo(yc);
              paths[yb == height ? pb - mins + 1 : 0].moveTo(xb, yb).verticalLineTo(yc);
              paths[0].moveTo(xa, yc).horizontalLineTo(xb);
            }
            break;
          case TRIANGULAR_MAX:
            double miny = Math.min(ya, yb);
            if(pa == pb) {
              paths[pa - mins + 1].moveTo(xa, ya).verticalLineTo(miny).drawTo(xc, yc).drawTo(xb, miny).verticalLineTo(yb);
            }
            else {
              paths[ya == height ? pa - mins + 1 : 0].moveTo(xa, ya).verticalLineTo(miny).drawTo(xc, yc);
              paths[yb == height ? pb - mins + 1 : 0].moveTo(xb, yb).verticalLineTo(miny).drawTo(xc, yc);
            }
            break;
          case TRIANGULAR:
            if(pa == pb) {
              paths[pa - mins + 1].moveTo(xa, ya).drawTo(xc, yc).drawTo(xb, yb);
            }
            else {
              paths[ya == height ? pa - mins + 1 : 0].moveTo(xa, ya).drawTo(xc, yc);
              paths[yb == height ? pb - mins + 1 : 0].moveTo(xb, yb).drawTo(xc, yc);
            }
            break;
          }
        }
        for(int i = 0; i < paths.length; i++) {
          SVGPath path = paths[i];
          if(!path.isStarted()) {
            continue;
          }
          layer.appendChild(path.makeElement(svgp, (i > 0 ? KEY_HIERLINE + "_" + (i + mins - 1) : KEY_HIERLINE)));
        }
      }
      else {
        setupCSS(svgp, linew);
        SVGPath dendrogram = new SVGPath();

        for(int i = 0; i < m; i++) {
          int a = p.getMergeA(i), b = p.getMergeB(i);
          double h = p.getMergeHeight(i);
          double xa = coord.getX(a), ya = coord.getY(a);
          double xb = coord.getX(b), yb = coord.getY(b);
          double yc = proy.applyAsDouble(h);
          double xc = coord.combine(a, b, yc, i + size);
          switch(DendrogramVisualization.this.style){
          case RECTANGULAR:
            dendrogram.moveTo(xa, ya).verticalLineTo(yc).horizontalLineTo(xb).verticalLineTo(yb);
            break;
          case TRIANGULAR_MAX:
            double miny = Math.min(ya, yb);
            dendrogram.moveTo(xa, ya).verticalLineTo(miny).drawTo(xc, yc).drawTo(xb, miny).verticalLineTo(yb);
            break;
          case TRIANGULAR:
            dendrogram.moveTo(xa, ya).drawTo(xc, yc).drawTo(xb, yb);
            break;
          }
        }
        layer.appendChild(dendrogram.makeElement(svgp, KEY_HIERLINE));
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
   * Compact position storage.
   *
   * @author Erich Schubert
   */
  private interface Positions {
    /**
     * Set the initial position
     *
     * @param off Object offset
     * @param x X coordinate
     * @param height Y coordinate
     */
    void set(int off, double x, double height);

    /**
     * Get the X coordinate of an object.
     *
     * @param o Object
     * @return X coordinate
     */
    double getX(int o);

    /**
     * Get the Y coordinate of an object.
     *
     * @param o Object
     * @return Y coordinate
     */
    double getY(int o);

    /**
     * Combine two objects, and return the new X coordinate.
     * 
     * @param o1 First object
     * @param o2 Second object
     * @param y3 Merge Y coordinate
     * @param o3 Resulting cluster number
     * @return New X coordinate
     */
    double combine(int o1, int o2, double y3, int o3);
  }

  /**
   * Compact position storage.
   *
   * @author Erich Schubert
   */
  private static class HalfPosPositions implements Positions {
    /**
     * Compact storage of positions.
     */
    final double[] xy;

    /**
     * Constructor.
     *
     * @param size Size
     */
    private HalfPosPositions(int size) {
      this.xy = new double[size << 1];
    }

    @Override
    public void set(int off, double d, double height) {
      off <<= 1;
      xy[off] = d;
      xy[off + 1] = height;
    }

    @Override
    public double getX(int o) {
      return xy[o << 1];
    }

    @Override
    public double getY(int o) {
      return xy[(o << 1) + 1];
    }

    @Override
    public double combine(int o1, int o2, double y3, int o3) {
      o3 <<= 1;
      xy[o3 + 1] = y3;
      return xy[o3] = 0.5 * (xy[o1 << 1] + xy[o2 << 1]);
    }
  }

  /**
   * Compact position storage.
   *
   * @author Erich Schubert
   */
  private static class HalfWidthPositions implements Positions {
    /**
     * Compact storage of positions.
     */
    final double[] xxy;

    /**
     * Constructor.
     *
     * @param size Size
     */
    private HalfWidthPositions(int size) {
      this.xxy = new double[size * 3];
    }

    @Override
    public void set(int off, double d, double height) {
      off *= 3;
      xxy[off] = xxy[off + 1] = 0.5 * d;
      xxy[off + 2] = height;
    }

    @Override
    public double getX(int o) {
      o *= 3;
      return xxy[o] + xxy[o + 1];
    }

    @Override
    public double getY(int o) {
      return xxy[o * 3 + 2];
    }

    @Override
    public double combine(int o1, int o2, double y3, int o3) {
      o1 *= 3;
      o2 *= 3;
      o3 *= 3;
      xxy[o3 + 2] = y3;
      return (xxy[o3] = Math.min(xxy[o1], xxy[o2])) //
          + (xxy[o3 + 1] = Math.max(xxy[o1 + 1], xxy[o2 + 1]));
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Dendrogram drawing style.
     */
    public static final OptionID STYLE_ID = new OptionID("dendrogram.style", "Drawing style for dendrograms.");

    /**
     * Dendrogram positioning logic.
     */
    public static final OptionID LAYOUT_ID = new OptionID("dendrogram.layout", "Positioning logic for dendrograms.");

    /**
     * Drawing style.
     */
    private DrawingStyle style = DrawingStyle.RECTANGULAR;

    /**
     * Positioning style.
     */
    private PositionStyle style2 = PositionStyle.HALF_POS;

    @Override
    public void configure(Parameterization config) {
      new EnumParameter<DrawingStyle>(STYLE_ID, DrawingStyle.class, DrawingStyle.RECTANGULAR) //
          .grab(config, x -> style = x);
      new EnumParameter<PositionStyle>(LAYOUT_ID, PositionStyle.class, PositionStyle.HALF_POS) //
          .grab(config, x -> style2 = x);
    }

    @Override
    public DendrogramVisualization make() {
      return new DendrogramVisualization(style, style2);
    }
  }
}
