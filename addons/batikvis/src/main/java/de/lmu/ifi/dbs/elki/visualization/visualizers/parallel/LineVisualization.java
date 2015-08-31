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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.lines.LineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

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
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<ParallelPlotProjector<?>> it = VisualizationTree.filter(context, start, ParallelPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ParallelPlotProjector<?> p = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, p.getRelation(), p.getRelation(), LineVisualization.this);
      task.level = VisualizationTask.LEVEL_DATA;
      task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_STYLEPOLICY | VisualizationTask.ON_SAMPLE);
      context.addVis(p, task);
    }
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
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      this.sample = ResultUtil.getSamplingResult(relation);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      super.fullRedraw();
      final DBIDs sam = sample.getSample();
      StylingPolicy sp = context.getStylingPolicy();
      final StyleLibrary style = context.getStyleLibrary();
      final LineStyleLibrary lines = style.lines();
      final double width = style.getLineWidth(StyleLibrary.PLOT) * MathUtil.min(.5, 2. / MathUtil.log2(sam.size()));
      if(sp instanceof ClassStylingPolicy) {
        ClassStylingPolicy csp = (ClassStylingPolicy) sp;
        final int min = csp.getMinStyle();
        String[] keys = new String[csp.getMaxStyle() - min];
        for(int c = min; c < csp.getMaxStyle(); c++) {
          String key = keys[c - min] = DATALINE + "_" + c;
          if(!svgp.getCSSClassManager().contains(key)) {
            CSSClass cls = new CSSClass(this, key);
            cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
            cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
            cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
            lines.formatCSSClass(cls, c, width);
            svgp.addCSSClassOrLogError(cls);
          }
        }
        for(DBIDIter iter = sam.iter(); iter.valid(); iter.advance()) {
          Element line = drawLine(iter);
          if(line == null) {
            continue;
          }
          SVGUtil.addCSSClass(line, keys[csp.getStyleForDBID(iter) + min]);
          layer.appendChild(line);
        }
      }
      else {
        // No classes available, but individually colored
        if(!svgp.getCSSClassManager().contains(DATALINE)) {
          CSSClass cls = new CSSClass(this, DATALINE);
          cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
          cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
          lines.formatCSSClass(cls, -1, width);
          svgp.addCSSClassOrLogError(cls);
        }
        StringBuilder buf = new StringBuilder().append(SVGConstants.CSS_STROKE_PROPERTY).append(':');
        final int prefix = buf.length();
        for(DBIDIter iter = sam.iter(); iter.valid(); iter.advance()) {
          Element line = drawLine(iter);
          if(line == null) {
            continue;
          }
          SVGUtil.addCSSClass(line, DATALINE);
          // assign color
          buf.delete(prefix, buf.length());
          buf.append(SVGUtil.colorToString(sp.getColorForDBID(iter)));
          line.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, buf.toString());
          layer.appendChild(line);
        }
      }
      svgp.updateStyleElement();
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
  }
}