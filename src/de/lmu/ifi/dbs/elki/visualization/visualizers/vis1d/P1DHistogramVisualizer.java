package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

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

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.histograms.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.HistogramProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleResult;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Generates a SVG-Element containing a histogram representing the distribution
 * of the database's objects.
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.has NumberVector oneway - - visualizes
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
// FIXME: make non-static, react to database changes!
// FIXME: cache histogram instead of recomputing it.
public class P1DHistogramVisualizer<NV extends NumberVector<NV, ?>> extends P1DVisualization {
  /**
   * Name for this visualizer.
   */
  private static final String CNAME = "Histograms";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String BIN = "bin";

  /**
   * Internal storage of the curves flag.
   */
  private boolean curves;

  /**
   * Number of bins to use in the histogram.
   */
  private int bins;

  /**
   * The database we visualize
   */
  private Relation<NV> relation;

  /**
   * The style policy
   */
  private StyleResult style;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   * @param curves Curves flag
   * @param bins Number of bins
   */
  public P1DHistogramVisualizer(VisualizationTask task, boolean curves, int bins) {
    super(task);
    this.curves = curves;
    this.bins = bins;
    this.relation = task.getRelation();
    this.style = task.getResult();
    context.addResultListener(this);
  }

  @Override
  public void destroy() {
    context.removeResultListener(this);
    super.destroy();
  }

  @Override
  protected void redraw() {
    double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    double xsize = Projection.SCALE * task.getWidth() / task.getHeight();
    double ysize = Projection.SCALE;

    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), xsize, ysize, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    // Styling policy
    final StylingPolicy spol = style.getStylingPolicy();
    final ClassStylingPolicy cspol;
    if(spol instanceof ClassStylingPolicy) {
      cspol = (ClassStylingPolicy) spol;
    }
    else {
      cspol = null;
    }
    // TODO also use min style?
    setupCSS(svgp, (cspol != null) ? cspol.getMaxStyle() : 0);

    // Create histograms
    final int off = (cspol != null) ? cspol.getMinStyle() : 0;
    final int numc = (cspol != null) ? (cspol.getMaxStyle() - cspol.getMinStyle()) : 0;
    DoubleMinMax minmax = new DoubleMinMax();
    final double frac = 1. / relation.size(); // TODO: sampling?
    final int cols = numc + 1;
    AggregatingHistogram<double[], double[]> histogram = new AggregatingHistogram<double[], double[]>(bins, -.5, .5, new AggregatingHistogram.Adapter<double[], double[]>() {
      @Override
      public double[] aggregate(double[] existing, double[] data) {
        for(int i = 0; i < existing.length; i++) {
          existing[i] += data[i];
        }
        return existing;
      }

      @Override
      public double[] make() {
        return new double[cols];
      }
    });

    if(cspol != null) {
      for(int snum = 0; snum < numc; snum++) {
        double[] inc = new double[cols];
        inc[0] = frac;
        inc[snum + 1] = frac;
        for(Iterator<DBID> iter = cspol.iterateClass(snum + off); iter.hasNext();) {
          DBID id = iter.next();
          try {
            double pos = proj.fastProjectDataToRenderSpace(relation.get(id)) / Projection.SCALE;
            histogram.aggregate(pos, inc);
          }
          catch(ObjectNotFoundException e) {
            // Ignore. The object was probably deleted from the database
          }
        }
      }
    }
    else {
      // Actual data distribution.
      double[] inc = new double[cols];
      inc[0] = frac;
      for(DBID id : relation.iterDBIDs()) {
        double pos = proj.fastProjectDataToRenderSpace(relation.get(id)) / Projection.SCALE;
        histogram.aggregate(pos, inc);
      }
    }
    // for scaling, get the maximum occurring value in the bins:
    for(DoubleObjPair<double[]> bin : histogram) {
      for(double val : bin.second) {
        minmax.put(val);
      }
    }

    LinearScale yscale = new LinearScale(0, minmax.getMax());
    LinearScale xscale = new LinearScale(histogram.getCoverMinimum(), histogram.getCoverMaximum());

    // Axis. TODO: Add an AxisVisualizer for this?
    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, yscale, 0, ysize, 0, 0, SVGSimpleLinearAxis.LabelStyle.LEFTHAND, context.getStyleLibrary());

      // draw axes that are non-trivial
      final int dimensionality = DatabaseUtil.dimensionality(relation);
      double orig = proj.fastProjectScaledToRender(new Vector(dimensionality));
      for(int d = 0; d < dimensionality; d++) {
        Vector v = new Vector(dimensionality);
        v.set(d, 1);
        // projected endpoint of axis
        double ax = proj.fastProjectScaledToRender(v);
        if(ax != orig) {
          final double left = (orig / Projection.SCALE + 0.5) * xsize;
          final double right = (ax / Projection.SCALE + 0.5) * xsize;
          SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getScale(d), left, ysize, right, ysize, SVGSimpleLinearAxis.LabelStyle.RIGHTHAND, context.getStyleLibrary());
        }
      }
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("CSS class exception in axis class.", e);
    }

    double binwidth = histogram.getBinsize();
    // Visualizing
    if(!curves) {
      for(DoubleObjPair<double[]> bin : histogram) {
        double lpos = xscale.getScaled(bin.first - binwidth / 2);
        double rpos = xscale.getScaled(bin.first + binwidth / 2);
        double stack = 0.0;
        final int start = numc > 0 ? 1 : 0;
        for(int key = start; key < cols; key++) {
          double val = yscale.getScaled(bin.getSecond()[key]);
          Element row = SVGUtil.svgRect(svgp.getDocument(), xsize * lpos, ysize * (1 - (val + stack)), xsize * (rpos - lpos), ysize * val);
          stack = stack + val;
          SVGUtil.addCSSClass(row, BIN + (off + key - 1));
          layer.appendChild(row);
        }
      }
    }
    else {
      double left = xscale.getScaled(histogram.getCoverMinimum());
      double right = left;

      SVGPath[] paths = new SVGPath[cols];
      double[] lasty = new double[cols];
      for(int i = 0; i < cols; i++) {
        paths[i] = new SVGPath(xsize * left, ysize * 1);
        lasty[i] = 0;
      }

      // draw histogram lines
      for(DoubleObjPair<double[]> bin : histogram) {
        left = xscale.getScaled(bin.first - binwidth / 2);
        right = xscale.getScaled(bin.first + binwidth / 2);
        for(int i = 0; i < cols; i++) {
          double val = yscale.getScaled(bin.getSecond()[i]);
          if(lasty[i] != val) {
            paths[i].lineTo(xsize * left, ysize * (1 - lasty[i]));
            paths[i].lineTo(xsize * left, ysize * (1 - val));
            paths[i].lineTo(xsize * right, ysize * (1 - val));
            lasty[i] = val;
          }
        }
      }
      // close and insert all lines.
      for(int i = 0; i < cols; i++) {
        if(lasty[i] != 0) {
          paths[i].lineTo(xsize * right, ysize * (1 - lasty[i]));
        }
        paths[i].lineTo(xsize * right, ysize * 1);
        Element elem = paths[i].makeElement(svgp);
        SVGUtil.addCSSClass(elem, BIN + (off + i - 1));
        layer.appendChild(elem);
      }
    }
  }

  /**
   * Generate the needed CSS classes.
   * 
   * @param svgp Plot context
   * @param numc Number of classes we need.
   */
  private void setupCSS(SVGPlot svgp, int numc) {
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    CSSClass allInOne = new CSSClass(svgp, BIN + -1);
    if(!curves) {
      allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      allInOne.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 1.0);
    }
    else {
      allInOne.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      allInOne.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    }
    svgp.addCSSClassOrLogError(allInOne);

    for(int clusterID = 0; clusterID < numc; clusterID++) {
      CSSClass bin = new CSSClass(svgp, BIN + clusterID);

      if(!curves) {
        bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(clusterID));
      }
      else {
        bin.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(clusterID));
        bin.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
        bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      }

      svgp.addCSSClassOrLogError(bin);
    }
  }

  /**
   * Visualizer factory for 1D histograms
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses P1DHistogramVisualizer oneway - - «create»
   * 
   * @param <NV> Number vector type
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Flag to specify the "curves" rendering style.
     * 
     * <p>
     * Key: {@code -histogram.curves}
     * </p>
     */
    public static final OptionID STYLE_CURVES_ID = OptionID.getOrCreateOptionID("projhistogram.curves", "Use curves instead of the stacked histogram style.");

    /**
     * Parameter to specify the number of bins to use in histogram.
     * 
     * <p>
     * Key: {@code -projhistogram.bins} Default: 20
     * </p>
     */
    public static final OptionID HISTOGRAM_BINS_ID = OptionID.getOrCreateOptionID("projhistogram.bins", "Number of bins in the distribution histogram");

    /**
     * Internal storage of the curves flag.
     */
    private boolean curves;

    /**
     * Number of bins to use in histogram.
     */
    private static final int DEFAULT_BINS = 50;

    /**
     * Number of bins to use in the histogram.
     */
    private int bins = DEFAULT_BINS;

    /**
     * Constructor.
     * 
     * @param curves
     * @param bins
     */
    public Factory(boolean curves, int bins) {
      super();
      this.curves = curves;
      this.bins = bins;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new P1DHistogramVisualizer<NV>(task, curves, bins);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find a style result to visualize:
      IterableIterator<StyleResult> clusterings = ResultUtil.filteredResults(result, StyleResult.class);
      for(StyleResult c : clusterings) {
        IterableIterator<HistogramProjector<?>> ps = ResultUtil.filteredResults(baseResult, HistogramProjector.class);
        for(HistogramProjector<?> p : ps) {
          // register self
          final VisualizationTask task = new VisualizationTask(CNAME, c, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
          baseResult.getHierarchy().add(c, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<NV extends NumberVector<NV, ?>> extends AbstractParameterizer {
      /**
       * Internal storage of the curves flag.
       */
      private boolean curves;

      /**
       * Number of bins to use in the histogram.
       */
      private int bins = DEFAULT_BINS;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        Flag STYLE_CURVES_FLAG = new Flag(STYLE_CURVES_ID);
        if(config.grab(STYLE_CURVES_FLAG)) {
          curves = STYLE_CURVES_FLAG.getValue();
        }
        IntParameter HISTOGRAM_BINS_PARAM = new IntParameter(HISTOGRAM_BINS_ID, new GreaterEqualConstraint(2), DEFAULT_BINS);
        if(config.grab(HISTOGRAM_BINS_PARAM)) {
          bins = HISTOGRAM_BINS_PARAM.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<NV>(curves, bins);
      }
    }
  }
}