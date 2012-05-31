package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel;

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
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
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
 */
public class LineVisualization extends AbstractParallelVisualization<NumberVector<?, ?>> implements DataStoreListener {
  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
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
  public LineVisualization(VisualizationTask task) {
    super(task);
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

    Iterator<DBID> ids = sample.getSample().iterator();
    if(ids == null || !ids.hasNext()) {
      ids = relation.iterDBIDs();
    }
    if(sp instanceof ClassStylingPolicy) {
      ClassStylingPolicy csp = (ClassStylingPolicy) sp;
      for(int c = csp.getMinStyle(); c < csp.getMaxStyle(); c++) {
        String key = DATALINE + "_" + c;
        for(Iterator<DBID> iter = csp.iterateClass(c); iter.hasNext();) {
          DBID id = iter.next();
          if(!sample.getSample().contains(id)) {
            continue; // TODO: can we test more efficiently than this?
          }
          SVGPath path = new SVGPath();
          double[] yPos = proj.fastProjectDataToRenderSpace(relation.get(id));
          for(int i = 0; i < yPos.length; i++) {
            path.drawTo(getVisibleAxisX(i), yPos[i]);
          }
          Element line = path.makeElement(svgp);
          SVGUtil.addCSSClass(line, key);
          layer.appendChild(line);
        }
      }
    }
    else {
      while(ids.hasNext()) {
        DBID id = ids.next();
        SVGPath path = new SVGPath();
        double[] yPos = proj.fastProjectDataToRenderSpace(relation.get(id));
        for(int i = 0; i < yPos.length; i++) {
          path.drawTo(getVisibleAxisX(i), yPos[i]);
        }
        Element line = path.makeElement(svgp);
        SVGUtil.addCSSClass(line, DATALINE);
        // assign color
        line.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_STROKE_PROPERTY + ":" + SVGUtil.colorToString(sp.getColorForDBID(id)));
        layer.appendChild(line);
      }
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp, StylingPolicy sp) {
    final StyleLibrary style = context.getStyleLibrary();
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

  /**
   * Factory for axis visualizations
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses LineVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Data lines";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
      thumbmask |= ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_STYLE;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new LineVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      IterableIterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(result, ParallelPlotProjector.class);
      for(ParallelPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, p.getRelation(), p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
        baseResult.getHierarchy().add(p, task);
      }
    }
  }
}