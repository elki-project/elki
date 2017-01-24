/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.selection;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.copy;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.euclideanLength;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minus;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.plusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.plusTimesEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.scalarProduct;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesEquals;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.datastore.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ArcCosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.CanvasSize;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;
import net.jafama.FastMath;

/**
 * Factory for visualizers to generate an SVG-Element containing dots as markers
 * representing the kNN of the selected Database objects.
 *
 * To use this, add a kNN preprocessor index to your database!
 *
 * @author Erich Schubert
 * @author Robert Rödler
 * @since 0.5.5
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
// FIXME: for >2 dimensions, cosine doesn't seem to be correct yet.
public class DistanceFunctionVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "k Nearest Neighbor Visualization";

  /**
   * Constructor
   */
  public DistanceFunctionVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewSiblings(context, start, AbstractMaterializeKNNPreprocessor.class, ScatterPlotProjector.class, //
        new VisualizationTree.Handler2<AbstractMaterializeKNNPreprocessor<?>, ScatterPlotProjector<?>>() {
          @Override
          public void process(VisualizerContext context, AbstractMaterializeKNNPreprocessor<?> kNN, ScatterPlotProjector<?> p) {
            final Relation<?> rel = p.getRelation();
            if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
              return;
            }
            final VisualizationTask task = new VisualizationTask(NAME, context, kNN, rel, DistanceFunctionVisualization.this);
            task.level = VisualizationTask.LEVEL_DATA - 1;
            task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_SAMPLE | VisualizationTask.ON_SELECTION);
            context.addVis(kNN, task);
            context.addVis(p, task);
          }
        });
  }

  /**
   * Get the "p" value of an Lp norm.
   *
   * @param kNN kNN preprocessor
   * @return p of LP norm, or NaN
   */
  public static double getLPNormP(AbstractMaterializeKNNPreprocessor<?> kNN) {
    DistanceFunction<?> distanceFunction = kNN.getDistanceQuery().getDistanceFunction();
    if(LPNormDistanceFunction.class.isInstance(distanceFunction)) {
      return ((LPNormDistanceFunction) distanceFunction).getP();
    }
    return Double.NaN;
  }

  /**
   * Test whether the given preprocessor used an angular distance function
   *
   * @param kNN kNN preprocessor
   * @return true when angular
   */
  public static boolean isAngularDistance(AbstractMaterializeKNNPreprocessor<?> kNN) {
    DistanceFunction<?> distanceFunction = kNN.getDistanceQuery().getDistanceFunction();
    if(CosineDistanceFunction.class.isInstance(distanceFunction)) {
      return true;
    }
    if(ArcCosineDistanceFunction.class.isInstance(distanceFunction)) {
      return true;
    }
    return false;
  }

  /**
   * Visualizes Cosine and ArcCosine distance functions
   *
   * @param svgp SVG Plot
   * @param proj Visualization projection
   * @param mid mean vector
   * @param angle Opening angle in radians
   * @return path element
   */
  public static Element drawCosine(SVGPlot svgp, Projection2D proj, NumberVector mid, double angle) {
    // Project origin
    double[] pointOfOrigin = proj.fastProjectDataToRenderSpace(new double[proj.getInputDimensionality()]);

    // direction of the selected Point
    double[] selPoint = proj.fastProjectDataToRenderSpace(mid);

    double[] range1, range2;
    {
      // Rotation plane:
      double[] p1 = proj.fastProjectRenderToDataSpace(selPoint[0] + 10, selPoint[1]);
      double[] p2 = proj.fastProjectRenderToDataSpace(selPoint[0], selPoint[1] + 10);
      double[] pm = mid.toArray();
      // Compute relative vectors
      minusEquals(p1, pm);
      minusEquals(p2, pm);
      // Scale p1 and p2 to unit length:
      timesEquals(p1, 1. / euclideanLength(p1));
      timesEquals(p2, 1. / euclideanLength(p2));
      {
        double test = scalarProduct(p1, p2);
        if(Math.abs(test) > 1E-10) {
          LoggingUtil.warning("Projection does not seem to be orthogonal?");
        }
      }
      // Project onto p1, p2:
      double l1 = scalarProduct(pm, p1), l2 = scalarProduct(pm, p2);
      // Rotate projection by + and - angle
      // Using sin(-x) = -sin(x) and cos(-x)=cos(x)
      final double cangle = FastMath.cos(angle),
          sangle = MathUtil.cosToSin(angle, cangle);
      double r11 = +cangle * l1 - sangle * l2, r12 = +sangle * l1 + cangle * l2;
      double r21 = +cangle * l1 + sangle * l2, r22 = -sangle * l1 + cangle * l2;
      // Build rotated vectors - remove projected component, add rotated
      // component:
      double[] r1 = copy(pm), r2 = copy(pm);
      plusTimesEquals(r1, p1, -l1 + r11);
      plusTimesEquals(r1, p2, -l2 + r12);
      plusTimesEquals(r2, p1, -l1 + r21);
      plusTimesEquals(r2, p2, -l2 + r22);
      // Project to render space:
      range1 = proj.fastProjectDataToRenderSpace(r1);
      range2 = proj.fastProjectDataToRenderSpace(r2);
    }

    // Continue lines to viewport.
    {
      CanvasSize viewport = proj.estimateViewport();
      minusEquals(range1, pointOfOrigin);
      minusEquals(range2, pointOfOrigin);
      timesEquals(range1, viewport.continueToMargin(pointOfOrigin, range1));
      timesEquals(range2, viewport.continueToMargin(pointOfOrigin, range2));
      plusEquals(range1, pointOfOrigin);
      plusEquals(range2, pointOfOrigin);
      // Go backwards into the other direction - the origin might not be in the
      // viewport!
      double[] start1 = minus(pointOfOrigin, range1);
      double[] start2 = minus(pointOfOrigin, range2);
      timesEquals(start1, viewport.continueToMargin(range1, start1));
      timesEquals(start2, viewport.continueToMargin(range2, start2));
      plusEquals(start1, range1);
      plusEquals(start2, range2);

      // TODO: add filled variant?
      SVGPath path = new SVGPath();
      path.moveTo(start1);
      path.lineTo(range1);
      path.moveTo(start2);
      path.lineTo(range2);
      return path.makeElement(svgp);
    }
  }

  /**
   * Instance, visualizing a particular set of kNNs
   *
   * @author Robert Rödler
   * @author Erich Schubert
   *
   * @apiviz.has DBIDSelection oneway - - visualizes
   */
  public class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * Generic tags to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String KNNMARKER = "kNNMarker";

    public static final String KNNDIST = "kNNDist";

    public static final String DISTANCEFUNCTION = "distancefunction";

    /**
     * The selection result we work on
     */
    private AbstractMaterializeKNNPreprocessor<? extends NumberVector> result;

    /**
     * Constructor
     *
     * @param task VisualizationTask
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      this.result = task.getResult();
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
      addCSSClasses(svgp);
      final double p = getLPNormP(result);
      final boolean angular = isAngularDistance(result);

      final double size = style.getSize(StyleLibrary.SELECTION);
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        DBIDs selection = selContext.getSelectedIds();

        for(DBIDIter i = selection.iter(); i.valid(); i.advance()) {
          final KNNList knn = result.get(i);
          for(DoubleDBIDListIter iter = knn.iter(); iter.valid(); iter.advance()) {
            try {
              double[] v = proj.fastProjectDataToRenderSpace(rel.get(iter));
              if(v[0] != v[0] || v[1] != v[1]) {
                continue; // NaN!
              }
              Element dot = svgp.svgCircle(v[0], v[1], size);
              SVGUtil.addCSSClass(dot, KNNMARKER);
              layer.appendChild(dot);

              Element lbl = svgp.svgText(v[0] + size, v[1] + size, Double.toString(iter.doubleValue()));
              SVGUtil.addCSSClass(lbl, KNNDIST);
              layer.appendChild(lbl);
            }
            catch(ObjectNotFoundException e) {
              // ignore
            }
          }
          // Last element
          DoubleDBIDPair last = knn.get(knn.size() - 1);
          // Draw hypersphere if possible
          {
            final Element dist;
            if(p == 1.0) {
              dist = SVGHyperSphere.drawManhattan(svgp, proj, rel.get(i), last.doubleValue());
            }
            else if(p == 2.0) {
              dist = SVGHyperSphere.drawEuclidean(svgp, proj, rel.get(i), last.doubleValue());
            }
            else if(!Double.isNaN(p)) {
              dist = SVGHyperSphere.drawLp(svgp, proj, rel.get(i), last.doubleValue(), p);
            }
            else if(angular) {
              final NumberVector refvec = rel.get(i);
              // Recompute the angle - it could be cosine or arccosine distance
              double maxangle = Math.acos(VectorUtil.cosAngle(refvec, rel.get(last)));
              dist = drawCosine(svgp, proj, refvec, maxangle);
            }
            else {
              dist = null;
            }
            if(dist != null) {
              SVGUtil.addCSSClass(dist, DISTANCEFUNCTION);
              layer.appendChild(dist);
            }
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
      final StyleLibrary style = context.getStyleLibrary();
      // Class for the distance markers
      if(!svgp.getCSSClassManager().contains(KNNMARKER)) {
        CSSClass cls = new CSSClass(this, KNNMARKER);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_DARKGREEN_VALUE);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        svgp.addCSSClassOrLogError(cls);
      }
      // Class for the distance function
      if(!svgp.getCSSClassManager().contains(DISTANCEFUNCTION)) {
        CSSClass cls = new CSSClass(this, DISTANCEFUNCTION);
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_RED_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
      // Class for the distance label
      if(!svgp.getCSSClassManager().contains(KNNDIST)) {
        CSSClass cls = new CSSClass(this, KNNDIST);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
        cls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, style.getTextSize(StyleLibrary.PLOT));
        cls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.PLOT));
        svgp.addCSSClassOrLogError(cls);
      }
    }
  }
}