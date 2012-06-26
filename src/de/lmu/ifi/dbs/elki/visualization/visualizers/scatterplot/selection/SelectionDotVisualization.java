package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.selection;

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

import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Visualizer for generating an SVG-Element containing dots as markers
 * representing the selected Database's objects.
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has SelectionResult oneway - - visualizes
 * @apiviz.has DBIDSelection oneway - - visualizes
 */
public class SelectionDotVisualization extends AbstractScatterplotVisualization implements DataStoreListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String MARKER = "selectionDotMarker";

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public SelectionDotVisualization(VisualizationTask task) {
    super(task);
    context.addResultListener(this);
    context.addDataStoreListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    context.removeResultListener(this);
    super.destroy();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    final double size = context.getStyleLibrary().getSize(StyleLibrary.SELECTION);
    DBIDSelection selContext = context.getSelection();
    if(selContext != null) {
      DBIDs selection = selContext.getSelectedIds();
      for(DBID i : selection) {
        try {
          double[] v = proj.fastProjectDataToRenderSpace(rel.get(i));
          Element dot = svgp.svgCircle(v[0], v[1], size);
          SVGUtil.addCSSClass(dot, MARKER);
          layer.appendChild(dot);
        }
        catch(ObjectNotFoundException e) {
          // ignore
        }
      }
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    // Class for the dot markers
    if(!svgp.getCSSClassManager().contains(MARKER)) {
      CSSClass cls = new CSSClass(this, MARKER);
      final StyleLibrary style = context.getStyleLibrary();
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
      svgp.addCSSClassOrLogError(cls);
    }
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing dots as
   * markers representing the selected Database's objects.
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SelectionDotVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor
     */
    public Factory() {
      super();
      thumbmask |= ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionDotVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Collection<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
      for(SelectionResult selres : selectionResults) {
        Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
        for(ScatterPlotProjector<?> p : ps) {
          final VisualizationTask task = new VisualizationTask(NAME, selres, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
          baseResult.getHierarchy().add(selres, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }
  }
}