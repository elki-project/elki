package de.lmu.ifi.dbs.elki.visualization.visualizers.histogram;

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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.DoubleArrayStaticHistogram;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.HistogramProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
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
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class ColoredHistogramVisualizer extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String CNAME = "Histograms";

  /**
   * Settings
   */
  protected Parameterizer settings;

  /**
   * Number of bins to use in histogram.
   */
  private static final int DEFAULT_BINS = 80;

  /**
   * Constructor.
   *
   * @param settings Settings
   */
  public ColoredHistogramVisualizer(Parameterizer settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance<DoubleVector>(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNew(context, start, HistogramProjector.class, //
    new VisualizationTree.Handler1<HistogramProjector<?>>() {
      @Override
      public void process(VisualizerContext context, HistogramProjector<?> p) {
        // register self
        final VisualizationTask task = new VisualizationTask(CNAME, context, p, p.getRelation(), ColoredHistogramVisualizer.this);
        task.level = VisualizationTask.LEVEL_DATA;
        task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_STYLEPOLICY);
        context.addVis(p, task);
      }
    });
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance
   *
   * @author Remigius Wojdanowski
   *
   * @apiviz.has NumberVector oneway - - visualizes
   *
   * @param <NV> Type of the DatabaseObject being visualized.
   */
  // FIXME: cache histogram instead of recomputing it?
  public class Instance<NV extends NumberVector> extends AbstractHistogramVisualization {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String BIN = "bin";

    /**
     * The database we visualize
     */
    private Relation<NV> relation;

    /**
     * Sampling result
     */
    private SamplingResult sample;

    /**
     * Constructor.
     *
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      this.relation = task.getRelation();
      this.sample = ResultUtil.getSamplingResult(relation);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      StyleLibrary style = context.getStyleLibrary();
      double margin = style.getSize(StyleLibrary.MARGIN);
      layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
      double xsize = Projection.SCALE * getWidth() / getHeight();
      double ysize = Projection.SCALE;

      final String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), xsize, ysize, margin);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

      // Styling policy
      final StylingPolicy spol = context.getStylingPolicy();
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
      DoubleArrayStaticHistogram histogram = new DoubleArrayStaticHistogram(settings.bins, -.5, .5, cols);

      if(cspol != null) {
        for(int snum = 0; snum < numc; snum++) {
          double[] inc = new double[cols];
          inc[0] = frac;
          inc[snum + 1] = frac;
          for(DBIDIter iter = cspol.iterateClass(snum + off); iter.valid(); iter.advance()) {
            if(!sample.getSample().contains(iter)) {
              continue; // TODO: can we test more efficiently than this?
            }
            try {
              double pos = proj.fastProjectDataToRenderSpace(relation.get(iter)) / Projection.SCALE;
              histogram.increment(pos, inc);
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
        for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
          double pos = proj.fastProjectDataToRenderSpace(relation.get(iditer)) / Projection.SCALE;
          histogram.increment(pos, inc);
        }
      }
      // for scaling, get the maximum occurring value in the bins:
      for(DoubleArrayStaticHistogram.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
        for(double val : iter.getValue()) {
          minmax.put(val);
        }
      }

      LinearScale yscale = new LinearScale(0, minmax.getMax());
      LinearScale xscale = new LinearScale(histogram.getCoverMinimum(), histogram.getCoverMaximum());

      // Axis. TODO: Add an AxisVisualizer for this?
      try {
        SVGSimpleLinearAxis.drawAxis(svgp, layer, yscale, 0, ysize, 0, 0, SVGSimpleLinearAxis.LabelStyle.LEFTHAND, style);

        // draw axes that are non-trivial
        final int dimensionality = RelationUtil.dimensionality(relation);
        double orig = proj.fastProjectScaledToRender(new Vector(dimensionality));
        for(int d = 0; d < dimensionality; d++) {
          Vector v = new Vector(dimensionality);
          v.set(d, 1);
          // projected endpoint of axis
          double ax = proj.fastProjectScaledToRender(v);
          if(ax < orig || ax > orig) {
            final double left = (orig / Projection.SCALE + 0.5) * xsize;
            final double right = (ax / Projection.SCALE + 0.5) * xsize;
            SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getScale(d), left, ysize, right, ysize, SVGSimpleLinearAxis.LabelStyle.RIGHTHAND, style);
          }
        }
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("CSS class exception in axis class.", e);
      }

      // Visualizing
      if(!settings.curves) {
        for(DoubleArrayStaticHistogram.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
          double lpos = xscale.getScaled(iter.getLeft());
          double rpos = xscale.getScaled(iter.getRight());
          double stack = 0.0;
          final int start = numc > 0 ? 1 : 0;
          for(int key = start; key < cols; key++) {
            double val = yscale.getScaled(iter.getValue()[key]);
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
        for(DoubleArrayStaticHistogram.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
          left = xscale.getScaled(iter.getLeft());
          right = xscale.getScaled(iter.getRight());
          for(int i = 0; i < cols; i++) {
            double val = yscale.getScaled(iter.getValue()[i]);
            if(lasty[i] > val || lasty[i] < val) {
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
      svgp.updateStyleElement();
    }

    /**
     * Generate the needed CSS classes.
     *
     * @param svgp Plot context
     * @param numc Number of classes we need.
     */
    private void setupCSS(SVGPlot svgp, int numc) {
      final StyleLibrary style = context.getStyleLibrary();
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      CSSClass allInOne = new CSSClass(svgp, BIN + -1);
      if(!settings.curves) {
        allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
        allInOne.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 1.0);
      }
      else {
        allInOne.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
        allInOne.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
        allInOne.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      }
      svgp.addCSSClassOrLogError(allInOne);

      for(int clusterID = 0; clusterID < numc; clusterID++) {
        CSSClass bin = new CSSClass(svgp, BIN + clusterID);

        if(!settings.curves) {
          bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, colors.getColor(clusterID));
        }
        else {
          bin.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(clusterID));
          bin.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
          bin.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        }

        svgp.addCSSClassOrLogError(bin);
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
    /**
     * Flag to specify the "curves" rendering style.
     *
     * <p>
     * Key: {@code -histogram.curves}
     * </p>
     */
    public static final OptionID STYLE_CURVES_ID = new OptionID("projhistogram.curves", "Use curves instead of the stacked histogram style.");

    /**
     * Parameter to specify the number of bins to use in histogram.
     *
     * <p>
     * Key: {@code -projhistogram.bins} Default: 80
     * </p>
     */
    public static final OptionID HISTOGRAM_BINS_ID = new OptionID("projhistogram.bins", "Number of bins in the distribution histogram");

    /**
     * Internal storage of the curves flag.
     */
    protected boolean curves = false;

    /**
     * Number of bins to use in the histogram.
     */
    protected int bins = DEFAULT_BINS;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag curvesF = new Flag(STYLE_CURVES_ID);
      if(config.grab(curvesF)) {
        curves = curvesF.isTrue();
      }
      IntParameter binsP = new IntParameter(HISTOGRAM_BINS_ID, DEFAULT_BINS);
      binsP.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(binsP)) {
        bins = binsP.intValue();
      }
    }

    @Override
    protected ColoredHistogramVisualizer makeInstance() {
      return new ColoredHistogramVisualizer(this);
    }
  }
}
