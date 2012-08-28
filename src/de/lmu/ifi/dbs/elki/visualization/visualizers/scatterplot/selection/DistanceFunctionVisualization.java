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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ArcCosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.CanvasSize;
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
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Factory for visualizers to generate an SVG-Element containing dots as markers
 * representing the kNN of the selected Database objects.
 * 
 * To use this, add a kNN preprocessor index to your database!
 * 
 * @author Erich Schubert
 * @author Robert Rödler
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
    thumbmask |= ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION;
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance<DoubleDistance>(task);
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Collection<AbstractMaterializeKNNPreprocessor<?, ?, ?>> kNNIndex = ResultUtil.filterResults(result, AbstractMaterializeKNNPreprocessor.class);
    for(AbstractMaterializeKNNPreprocessor<?, ?, ?> kNN : kNNIndex) {
      Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
      for(ScatterPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, kNN, p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
        baseResult.getHierarchy().add(kNN, task);
        baseResult.getHierarchy().add(p, task);
      }
    }
  }

  /**
   * Get the "p" value of an Lp norm.
   * 
   * @param kNN kNN preprocessor
   * @return p of LP norm, or NaN
   */
  public static double getLPNormP(AbstractMaterializeKNNPreprocessor<?, ?, ?> kNN) {
    DistanceFunction<?, ?> distanceFunction = kNN.getDistanceQuery().getDistanceFunction();
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
  public static boolean isAngularDistance(AbstractMaterializeKNNPreprocessor<?, ?, ?> kNN) {
    DistanceFunction<?, ?> distanceFunction = kNN.getDistanceQuery().getDistanceFunction();
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
  public static Element drawCosine(SVGPlot svgp, Projection2D proj, NumberVector<?, ?> mid, double angle) {
    // Project origin
    double[] pointOfOrigin = proj.fastProjectDataToRenderSpace(new double[proj.getInputDimensionality()]);
  
    // direction of the selected Point
    double[] selPoint = proj.fastProjectDataToRenderSpace(mid);
  
    double[] range1, range2;
    {
      // Rotation plane:
      double[] p1 = proj.fastProjectRenderToDataSpace(new double[] { selPoint[0] + 10, selPoint[1] });
      double[] p2 = proj.fastProjectRenderToDataSpace(new double[] { selPoint[0], selPoint[1] + 10 });
      double[] pm = mid.getColumnVector().getArrayRef();
      // Compute relative vectors
      VMath.minusEquals(p1, pm);
      VMath.minusEquals(p2, pm);
      // Scale p1 and p2 to unit length:
      VMath.timesEquals(p1, 1. / VMath.euclideanLength(p1));
      VMath.timesEquals(p2, 1. / VMath.euclideanLength(p2));
      {
        double test = VMath.scalarProduct(p1, p2);
        if(Math.abs(test) > 1E-10) {
          LoggingUtil.warning("Projection does not seem to be orthogonal?");
        }
      }
      // Project onto p1, p2:
      double l1 = VMath.scalarProduct(pm, p1), l2 = VMath.scalarProduct(pm, p2);
      // Rotate projection by + and - angle
      // Using sin(-x) = -sin(x) and cos(-x)=cos(x)
      final double cangle = Math.cos(angle), sangle = Math.sin(angle);
      double r11 = +cangle * l1 - sangle * l2, r12 = +sangle * l1 + cangle * l2;
      double r21 = +cangle * l1 + sangle * l2, r22 = -sangle * l1 + cangle * l2;
      // Build rotated vectors - remove projected component, add rotated
      // component:
      double[] r1 = VMath.copy(pm), r2 = VMath.copy(pm);
      VMath.plusTimesEquals(r1, p1, -l1 + r11);
      VMath.plusTimesEquals(r1, p2, -l2 + r12);
      VMath.plusTimesEquals(r2, p1, -l1 + r21);
      VMath.plusTimesEquals(r2, p2, -l2 + r22);
      // Project to render space:
      range1 = proj.fastProjectDataToRenderSpace(r1);
      range2 = proj.fastProjectDataToRenderSpace(r2);
    }
  
    // Continue lines to viewport.
    {
      CanvasSize viewport = proj.estimateViewport();
      VMath.minusEquals(range1, pointOfOrigin);
      VMath.minusEquals(range2, pointOfOrigin);
      VMath.timesEquals(range1, viewport.continueToMargin(pointOfOrigin, range1));
      VMath.timesEquals(range2, viewport.continueToMargin(pointOfOrigin, range2));
      VMath.plusEquals(range1, pointOfOrigin);
      VMath.plusEquals(range2, pointOfOrigin);
      // Go backwards into the other direction - the origin might not be in the
      // viewport!
      double[] start1 = VMath.minus(pointOfOrigin, range1);
      double[] start2 = VMath.minus(pointOfOrigin, range2);
      VMath.timesEquals(start1, viewport.continueToMargin(range1, start1));
      VMath.timesEquals(start2, viewport.continueToMargin(range2, start2));
      VMath.plusEquals(start1, range1);
      VMath.plusEquals(start2, range2);
  
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
   * @apiviz.has SelectionResult oneway - - visualizes
   * @apiviz.has DBIDSelection oneway - - visualizes
   * 
   * @param <D> Distance type
   */
  public class Instance<D extends NumberDistance<D, ?>> extends AbstractScatterplotVisualization implements DataStoreListener {
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
    private AbstractMaterializeKNNPreprocessor<? extends NumberVector<?, ?>, D, ?> result;

    /**
     * Constructor
     * 
     * @param task VisualizationTask
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.result = task.getResult();
      context.addDataStoreListener(this);
      context.addResultListener(this);
      incrementalRedraw();
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);
      final double p = getLPNormP(result);
      final boolean angular = isAngularDistance(result);

      final double size = context.getStyleLibrary().getSize(StyleLibrary.SELECTION);
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        DBIDs selection = selContext.getSelectedIds();

        for(DBIDIter i = selection.iter(); i.valid(); i.advance()) {
          final KNNResult<D> knn = result.get(i);
          for(DistanceDBIDResultIter<D> iter = knn.iter(); iter.valid(); iter.advance()) {
            try {
              double[] v = proj.fastProjectDataToRenderSpace(rel.get(iter));
              Element dot = svgp.svgCircle(v[0], v[1], size);
              SVGUtil.addCSSClass(dot, KNNMARKER);
              layer.appendChild(dot);

              Element lbl = svgp.svgText(v[0] + size, v[1] + size, iter.getDistance().toString());
              SVGUtil.addCSSClass(lbl, KNNDIST);
              layer.appendChild(lbl);
            }
            catch(ObjectNotFoundException e) {
              // ignore
            }
          }
          // Last element
          DistanceDBIDPair<D> last = knn.get(knn.size() - 1);
          // Draw hypersphere if possible
          {
            final Element dist;
            if(p == 1.0) {
              dist = SVGHyperSphere.drawManhattan(svgp, proj, rel.get(i), last.getDistance());
            }
            else if(p == 2.0) {
              dist = SVGHyperSphere.drawEuclidean(svgp, proj, rel.get(i), last.getDistance());
            }
            else if(!Double.isNaN(p)) {
              dist = SVGHyperSphere.drawLp(svgp, proj, rel.get(i), last.getDistance(), p);
            }
            else if(angular) {
              final NumberVector<?, ?> refvec = rel.get(i);
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

    @Override
    public void resultChanged(Result current) {
      if(current instanceof SelectionResult) {
        synchronizedRedraw();
        return;
      }
      super.resultChanged(current);
    }
  }
}