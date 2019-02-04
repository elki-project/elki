/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.outlier;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.outlier.COP;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize error vectors as produced by COP.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br>
 * Outlier Detection in Arbitrarily Oriented Subspaces<br>
 * Proc. IEEE Int. Conf. on Data Mining (ICDM 2012)
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @stereotype factory
 * @navassoc - create - Instance
 * @navhas - visualizes - OutlierResult
 */
@Title("COP: Correlation Outlier Probability")
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "Outlier Detection in Arbitrarily Oriented Subspaces", //
    booktitle = "Proc. IEEE Int. Conf. on Data Mining (ICDM 2012)", //
    url = "https://doi.org/10.1109/ICDM.2012.21", //
    bibkey = "DBLP:conf/icdm/KriegelKSZ12")
public class COPVectorVisualization implements VisFactory {
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
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewSiblings(context, start, OutlierResult.class, ScatterPlotProjector.class, (o, p) -> {
      final Relation<?> rel2 = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel2.getDataTypeInformation())) {
        return;
      }
      VisualizationTree.findNewResults(context, o).filter(Relation.class).forEach(rel -> {
        if(!rel.getShortName().equals(COP.COP_ERRORVEC)) {
          return;
        }
        final VisualizationTask task = new VisualizationTask(this, NAME, rel, rel2) //
            .level(VisualizationTask.LEVEL_DATA) //
            .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SAMPLE);
        context.addVis(o, task);
        context.addVis(p, task);
      });
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
    protected Relation<double[]> result;

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      this.result = task.getResult();
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      setupCSS(svgp);
      for(DBIDIter objId = sample.getSample().iter(); objId.valid(); objId.advance()) {
        double[] evec = result.get(objId);
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

    /**
     * Registers the COP error vector-CSS-Class at a SVGPlot.
     *
     * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
     */
    private void setupCSS(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
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
