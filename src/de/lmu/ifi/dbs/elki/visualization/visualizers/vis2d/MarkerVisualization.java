package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

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
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleResult;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize e.g. a clustering using different markers for different clusters.
 * This visualizer is not constraint to clusters. It can in fact visualize any
 * kind of result we have a style source for.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses StyleResult
 */
public class MarkerVisualization extends P2DVisualization implements DataStoreListener {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String DOTMARKER = "dot";

  /**
   * The result we visualize
   */
  private StyleResult style;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public MarkerVisualization(VisualizationTask task) {
    super(task);
    this.style = task.getResult();
    context.addDataStoreListener(this);
    context.addResultListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDataStoreListener(this);
    context.removeResultListener(this);
  }

  @Override
  public void redraw() {
    final MarkerLibrary ml = context.getStyleLibrary().markers();
    final double marker_size = context.getStyleLibrary().getSize(StyleLibrary.MARKERPLOT);
    final StylingPolicy spol = style.getStylingPolicy();

    if(spol instanceof ClassStylingPolicy) {
      ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
      for(int cnum = cspol.getMinStyle(); cnum < cspol.getMaxStyle(); cnum++) {
        for(Iterator<DBID> iter = cspol.iterateClass(cnum); iter.hasNext();) {
          DBID cur = iter.next();
          try {
            final NumberVector<?, ?> vec = rel.get(cur);
            double[] v = proj.fastProjectDataToRenderSpace(vec);
            ml.useMarker(svgp, layer, v[0], v[1], cnum, marker_size);
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
    }
    else {
      final String FILL = SVGConstants.CSS_FILL_PROPERTY + ":";
      // Color-based styling. Fall back to dots
      for(DBID id : sample.getSample()) {
        try {
          double[] v = proj.fastProjectDataToRenderSpace(rel.get(id));
          Element dot = svgp.svgCircle(v[0], v[1], marker_size);
          SVGUtil.addCSSClass(dot, DOTMARKER);
          int col = spol.getColorForDBID(id);
          SVGUtil.setAtt(dot, SVGConstants.SVG_STYLE_ATTRIBUTE, FILL + SVGUtil.colorToString(col));
          layer.appendChild(dot);
        }
        catch(ObjectNotFoundException e) {
          // ignore.
        }
      }
    }
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * Visualization factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses MarkerVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Markers";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new MarkerVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find a style result to visualize:
      IterableIterator<StyleResult> clusterings = ResultUtil.filteredResults(result, StyleResult.class);
      for(StyleResult c : clusterings) {
        IterableIterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
        for(ScatterPlotProjector<?> p : ps) {
          final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
          baseResult.getHierarchy().add(c, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }
  }
}