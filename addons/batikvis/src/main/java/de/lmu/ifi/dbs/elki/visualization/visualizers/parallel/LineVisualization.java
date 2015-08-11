package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.lines.LineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Generates data lines.
 *
 * @author Robert Rödler
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class LineVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Data lines";

  /**
   * Constructor.
   */
  public LineVisualization() {
    super();
    thumbmask |= ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_STYLE;
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNew(context, start, ParallelPlotProjector.class, new VisualizationTree.Handler1<ParallelPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, ParallelPlotProjector<?> p) {
        final VisualizationTask task = new VisualizationTask(NAME, context, p.getRelation(), p.getRelation(), LineVisualization.this);
        task.level = VisualizationTask.LEVEL_DATA;
        context.addVis(p, task);
      }
    });
  }

  /**
   * Instance for a particular data set.
   *
   * @author Robert Rödler
   */
  public class Instance extends AbstractParallelVisualization<NumberVector>implements DataStoreListener {
    /**
     * Generic tags to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String DATALINE = "Dataline";

    /**
     * Sample we visualize.
     */
    private SamplingResult sample;

    /**
     * Constructor.
     *
     * @param task VisualizationTask
     */
    public Instance(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      this.sample = ResultUtil.getSamplingResult(relation);
      context.addResultListener(this);
      context.addDataStoreListener(this);
      incrementalRedraw();
    }

    @Override
    public void destroy() {
      context.removeDataStoreListener(this);
      context.removeResultListener(this);
      super.destroy();
    }

    @Override
    public void resultChanged(Result current) {
      super.resultChanged(current);
      if(current == sample || current == context.getStyleResult()) {
        synchronizedRedraw();
      }
    }

    @Override
    protected void redraw() {
      StylingPolicy sp = context.getStyleResult().getStylingPolicy();
      addCSSClasses(svgp, sp);

      if(sp instanceof ClassStylingPolicy) {
        final DBIDs sam = DBIDUtil.ensureSet(sample.getSample());
        ClassStylingPolicy csp = (ClassStylingPolicy) sp;
        for(int c = csp.getMinStyle(); c < csp.getMaxStyle(); c++) {
          String key = DATALINE + "_" + c;
          for(DBIDIter iter = csp.iterateClass(c); iter.valid(); iter.advance()) {
            if(!sam.contains(iter)) {
              continue; // TODO: can we test more efficiently than this?
            }
            Element line = drawLine(iter);
            if(line == null) {
              continue;
            }
            SVGUtil.addCSSClass(line, key);
            layer.appendChild(line);
          }
        }
      }
      else {
        DBIDIter ids = sample.getSample().iter();
        if(ids == null || !ids.valid()) {
          ids = relation.iterDBIDs();
        }
        for(; ids.valid(); ids.advance()) {
          Element line = drawLine(ids);
          if(line == null) {
            continue;
          }
          SVGUtil.addCSSClass(line, DATALINE);
          // assign color
          line.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_STROKE_PROPERTY + ":" + SVGUtil.colorToString(sp.getColorForDBID(ids)));
          layer.appendChild(line);
        }
      }
    }

    /**
     * Draw a single line.
     *
     * @param iter Object reference
     * @return Line element
     */
    private Element drawLine(DBIDRef iter) {
      SVGPath path = new SVGPath();
      double[] yPos = proj.fastProjectDataToRenderSpace(relation.get(iter));
      boolean drawn = false;
      int valid = 0; /* run length of valid values */
      for(int i = 0; i < yPos.length; i++) {
        // NaN handling:
        if(yPos[i] != yPos[i]) {
          valid = 0;
          continue;
        }
        ++valid;
        if(valid > 1) {
          if(valid == 2) {
            path.moveTo(getVisibleAxisX(i - 1), yPos[i - 1]);
          }
          path.lineTo(getVisibleAxisX(i), yPos[i]);
          drawn = true;
        }
      }
      if(!drawn) {
        return null; // Not enough data.
      }
      return path.makeElement(svgp);
    }

    /**
     * Adds the required CSS-Classes
     *
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp, StylingPolicy sp) {
      final StyleLibrary style = context.getStyleResult().getStyleLibrary();
      final LineStyleLibrary lines = style.lines();
      final double width = .5 * style.getLineWidth(StyleLibrary.PLOT);
      if(sp instanceof ClassStylingPolicy) {
        ClassStylingPolicy csp = (ClassStylingPolicy) sp;
        for(int i = csp.getMinStyle(); i < csp.getMaxStyle(); i++) {
          String key = DATALINE + "_" + i;
          if(!svgp.getCSSClassManager().contains(key)) {
            CSSClass cls = new CSSClass(this, key);
            cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
            cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
            cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
            lines.formatCSSClass(cls, i, width);
            svgp.addCSSClassOrLogError(cls);
          }
        }
      }
      else {
        // Class for the distance function
        if(!svgp.getCSSClassManager().contains(DATALINE)) {
          CSSClass cls = new CSSClass(this, DATALINE);
          cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
          cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
          lines.formatCSSClass(cls, -1, width);
          svgp.addCSSClassOrLogError(cls);
        }
      }
      svgp.updateStyleElement();
    }
  }
}