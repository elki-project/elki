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
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.ReferencePointsResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * The actual visualization instance, for a single projection
 * 
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class ReferencePointsVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Reference Points";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ReferencePointsVisualization() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Collection<ReferencePointsResult<?>> rps = ResultUtil.filterResults(result, ReferencePointsResult.class);
    for(ReferencePointsResult<?> rp : rps) {
      Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
      for(ScatterPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, rp, p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
        baseResult.getHierarchy().add(rp, task);
        baseResult.getHierarchy().add(p, task);
      }
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  /**
   * Instance.
   * 
   * @author Remigius Wojdanowski
   * @author Erich Schubert
   * 
   * @apiviz.has ReferencePointsResult oneway - - visualizes
   */
  // TODO: add a result listener for the reference points.
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String REFPOINT = "refpoint";

    /**
     * Serves reference points.
     */
    protected ReferencePointsResult<? extends NumberVector<?>> result;

    /**
     * Constructor.
     * 
     * @param task Visualization task
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.result = task.getResult();
      incrementalRedraw();
    }

    @Override
    public void redraw() {
      setupCSS(svgp);
      Iterator<? extends NumberVector<?>> iter = result.iterator();

      final double dotsize = context.getStyleLibrary().getSize(StyleLibrary.REFERENCE_POINTS);
      while(iter.hasNext()) {
        NumberVector<?> v = iter.next();
        double[] projected = proj.fastProjectDataToRenderSpace(v);
        Element dot = svgp.svgCircle(projected[0], projected[1], dotsize);
        SVGUtil.addCSSClass(dot, REFPOINT);
        layer.appendChild(dot);
      }
    }

    /**
     * Registers the Reference-Point-CSS-Class at a SVGPlot.
     * 
     * @param svgp the SVGPlot to register the -CSS-Class.
     */
    private void setupCSS(SVGPlot svgp) {
      CSSClass refpoint = new CSSClass(svgp, REFPOINT);
      refpoint.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getColor(StyleLibrary.REFERENCE_POINTS));
      svgp.addCSSClassOrLogError(refpoint);
    }
  }
}