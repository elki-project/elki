package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
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
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Visualize e.g. a clustering using different markers for different clusters.
 * This visualizer is not constraint to clusters. It can in fact visualize any
 * kind of result we have a style source for.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class MarkerVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Markers";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public MarkerVisualization() {
    super();
    thumbmask |= ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_STYLE;
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    // Find a style result to visualize:
    Collection<StyleResult> styleres = ResultUtil.filterResults(result, StyleResult.class);
    for(StyleResult c : styleres) {
      Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
      for(ScatterPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
        baseResult.getHierarchy().add(c, task);
        baseResult.getHierarchy().add(p, task);
      }
    }
  }

  /**
   * Instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.uses StyleResult
   */
  public class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
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
    public Instance(VisualizationTask task) {
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
          for(DBIDIter iter = cspol.iterateClass(cnum); iter.valid(); iter.advance()) {
            if(!sample.getSample().contains(iter)) {
              continue; // TODO: can we test more efficiently than this?
            }
            try {
              final NumberVector<?> vec = rel.get(iter);
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
        for(DBIDIter iter = sample.getSample().iter(); iter.valid(); iter.advance()) {
          try {
            double[] v = proj.fastProjectDataToRenderSpace(rel.get(iter));
            Element dot = svgp.svgCircle(v[0], v[1], marker_size);
            SVGUtil.addCSSClass(dot, DOTMARKER);
            int col = spol.getColorForDBID(iter);
            SVGUtil.setAtt(dot, SVGConstants.SVG_STYLE_ATTRIBUTE, FILL + SVGUtil.colorToString(col));
            layer.appendChild(dot);
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
    }
  }
}