package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.outlier;

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

import de.lmu.ifi.dbs.elki.algorithm.outlier.COP;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize error vectors as produced by COP.
 *
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 * @apiviz.has OutlierResult oneway - - visualizes
 */
@Title("COP: Correlation Outlier Probability")
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", title = "Outlier Detection in Arbitrarily Oriented Subspaces", booktitle = "Proc. IEEE International Conference on Data Mining (ICDM 2012)")
public class COPVectorVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Error Vectors";

  /**
   * Constructor.
   */
  public COPVectorVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewResultVis(context, start, OutlierResult.class, ScatterPlotProjector.class, new VisualizationTree.Handler2<OutlierResult, ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, OutlierResult o, ScatterPlotProjector<?> p) {
        Hierarchy.Iter<Relation<?>> it1 = VisualizationTree.filterResults(context, o, Relation.class);
        for(; it1.valid(); it1.advance()) {
          Relation<?> rel = it1.get();
          if(!rel.getShortName().equals(COP.COP_ERRORVEC)) {
            continue;
          }
          final VisualizationTask task = new VisualizationTask(NAME, context, rel, p.getRelation(), COPVectorVisualization.this);
          task.level = VisualizationTask.LEVEL_DATA;
          context.addVis(o, task);
          context.addVis(p, task);
        }
      }
    });
  }

  /**
   * Visualize error vectors as produced by COP.
   *
   * @author Erich Schubert
   */
  public class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String VEC = "copvec";

    /**
     * The outlier result to visualize
     */
    protected Relation<Vector> result;

    /**
     * Constructor.
     *
     * @param task Visualization task
     */
    public Instance(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      this.result = task.getResult();
      context.addDataStoreListener(this);
      incrementalRedraw();
    }

    @Override
    public void destroy() {
      super.destroy();
      context.removeDataStoreListener(this);
    }

    @Override
    public void redraw() {
      setupCSS(svgp);
      for(DBIDIter objId = sample.getSample().iter(); objId.valid(); objId.advance()) {
        Vector evec = result.get(objId);
        if(evec == null) {
          continue;
        }
        double[] ev = proj.fastProjectRelativeDataToRenderSpace(evec);
        // TODO: avoid hard-coded plot threshold
        if(VMath.euclideanLength(ev) < 0.01) {
          continue;
        }
        final NumberVector vec = rel.get(objId);
        if(vec == null) {
          continue;
        }
        double[] v = proj.fastProjectDataToRenderSpace(vec);
        if(v[0] != v[0] || v[1] != v[1]) {
          continue; // NaN!
        }
        Element arrow = svgp.svgLine(v[0], v[1], v[0] + ev[0], v[1] + ev[1]);
        SVGUtil.addCSSClass(arrow, VEC);
        layer.appendChild(arrow);
      }
    }

    @Override
    public void resultChanged(Result current) {
      if(sample == current) {
        synchronizedRedraw();
      }
    }

    /**
     * Registers the COP error vector-CSS-Class at a SVGPlot.
     *
     * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
     */
    private void setupCSS(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleResult().getStyleLibrary();
      CSSClass bubble = new CSSClass(svgp, VEC);
      bubble.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) / 2);

      // ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);
      String color = "red"; // TODO: use style library
      bubble.setStatement(SVGConstants.CSS_STROKE_VALUE, color);
      bubble.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(bubble);
    }
  }
}
