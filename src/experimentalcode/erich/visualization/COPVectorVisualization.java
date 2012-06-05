package experimentalcode.erich.visualization;

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

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
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
 * @apiviz.has OutlierResult oneway - - visualizes
 */
public class COPVectorVisualization extends AbstractScatterplotVisualization implements DataStoreListener {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String VEC = "copvec";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Error Vectors";

  /**
   * The outlier result to visualize
   */
  protected Relation<Vector> result;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public COPVectorVisualization(VisualizationTask task) {
    super(task);
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
    for(DBID objId : sample.getSample()) {
      Vector evec = result.get(objId);
      if(evec == null) {
        continue;
      }
      double[] ev = proj.fastProjectRelativeDataToRenderSpace(evec);
      // TODO: avoid hard-coded plot threshold
      if(VMath.euclideanLength(ev) < 0.01) {
        continue;
      }
      final NumberVector<?, ?> vec = rel.get(objId);
      if(vec == null) {
        continue;
      }
      double[] v = proj.fastProjectDataToRenderSpace(vec);
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
   * Registers the Bubble-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
   */
  private void setupCSS(SVGPlot svgp) {
    CSSClass bubble = new CSSClass(svgp, VEC);
    bubble.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) / 2);

    // ColorLibrary colors =
    // context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
    String color = "red"; // TODO: use style library
    bubble.setStatement(SVGConstants.CSS_STROKE_VALUE, color);
    bubble.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    svgp.addCSSClassOrLogError(bubble);
  }

  /**
   * Factory for producing cop error vector visualizations
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses COPVectorVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor.
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new COPVectorVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
      for(OutlierResult o : ors) {
        List<Relation<?>> rels = ResultUtil.filterResults(o, Relation.class);
        for(Relation<?> rel : rels) {
          // TODO: use a constant.
          if(!rel.getShortName().equals("cop-errorvec")) {
            continue;
          }
          List<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
          boolean vis = true;
          for(ScatterPlotProjector<?> p : ps) {
            final VisualizationTask task = new VisualizationTask(NAME, rel, p.getRelation(), this);
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
            if(!vis) {
              task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
            }
            baseResult.getHierarchy().add(o, task);
            baseResult.getHierarchy().add(p, task);
          }
        }
      }
    }
  }
}