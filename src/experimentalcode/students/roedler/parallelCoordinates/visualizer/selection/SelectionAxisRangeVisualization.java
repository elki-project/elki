package experimentalcode.students.roedler.parallelCoordinates.visualizer.selection;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.RangeSelection;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;
import experimentalcode.students.roedler.parallelCoordinates.projector.ParallelPlotProjector;
import experimentalcode.students.roedler.parallelCoordinates.visualizer.ParallelVisualization;

/**
 * Visualizer for generating an SVG-Element representing the selected range for
 * each dimension
 * 
 * @author Robert Rödler
 * 
 * @apiviz.has SelectionResult oneway - - visualizes
 * @apiviz.has RangeSelection oneway - - visualizes
 */
public class SelectionAxisRangeVisualization extends ParallelVisualization<NumberVector<?, ?>> implements DataStoreListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection Axis Range";

  /**
   * CSS Class for the range marker
   */
  public static final String MARKER = "selectionAxisRange";

  public SelectionAxisRangeVisualization(VisualizationTask task) {
    super(task);
    addCSSClasses(svgp);
    context.addDataStoreListener(this);
    context.addResultListener(this);
    incrementalRedraw();
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    final StyleLibrary style = context.getStyleLibrary();
    // Class for the cube
    if(!svgp.getCSSClassManager().contains(MARKER)) {
      CSSClass cls = new CSSClass(this, MARKER);
      cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);

      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));

      svgp.addCSSClassOrLogError(cls);
    }
  }

  @Override
  protected void redraw() {
    DBIDSelection selContext = context.getSelection();
    if(selContext == null || !(selContext instanceof RangeSelection)) {
      return;
    }
    DoubleDoublePair[] ranges = ((RangeSelection) selContext).getRanges();
    if(ranges == null) {
      return;
    }

    // Project:
    double[] min = new double[ranges.length];
    double[] max = new double[ranges.length];
    for(int d = 0; d < ranges.length; d++) {
      if(ranges[d] != null) {
        min[d] = ranges[d].first;
        max[d] = ranges[d].second;
      }
    }
    min = proj.fastProjectDataToRenderSpace(new Vector(min));
    max = proj.fastProjectDataToRenderSpace(new Vector(max));

    int dim = proj.getVisibleDimensions();
    for(int d = 0; d < dim; d++) {
      if(ranges[proj.getDimensionNumber(d)] != null) {
        double amin, amax;
        if(min[d] < max[d]) {
          amin = min[d] * getAxisHeight();
          amax = max[d] * getAxisHeight();
        }
        else {
          amax = min[d] * getAxisHeight();
          amin = max[d] * getAxisHeight();
        }
        Element rect = svgp.svgRect(getAxisX(d) - 0.005 * StyleLibrary.SCALE, amin, 0.01 * StyleLibrary.SCALE, amax - amin);
        SVGUtil.addCSSClass(rect, MARKER);
        layer.appendChild(rect);
      }
    }
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing a cube as
   * marker representing the selected range for each dimension
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SelectionCubeVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor.
     */
    public Factory() {
      super();
      thumbmask |= ThumbnailVisualization.ON_SELECTION;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionAxisRangeVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      final ArrayList<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
      for(SelectionResult selres : selectionResults) {
        IterableIterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ParallelPlotProjector.class);
        for(ParallelPlotProjector<?> p : ps) {
          final VisualizationTask task = new VisualizationTask(NAME, selres, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
          baseResult.getHierarchy().add(selres, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }
  }
}