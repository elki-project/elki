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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.cluster;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minus;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusTimes;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.normalize;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.plus;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.plusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.plusTimes;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.times;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transpose;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.geometry.FilteredConvexHull2D;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;
import net.jafama.FastMath;

/**
 * Visualizer for generating SVG-Elements containing ellipses for first, second
 * and third standard deviation. In more than 2-dimensional data, the class
 * tries to approximate the cluster extends.
 *
 * @author Robert Rödler
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class EMClusterVisualization implements VisFactory {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(EMClusterVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "EM Cluster Models";

  /**
   * Constants for quantiles of standard deviation
   */
  final static double[] SIGMA = new double[] { 0.41, 0.223, 0.047 };

  /**
   * Constructor
   */
  public EMClusterVisualization() {
    super();
  }

  @Override
  public Instance makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      context.addVis(p, new VisualizationTask(this, NAME, p, p.getRelation()) //
          .level(VisualizationTask.LEVEL_DATA + 3) //
          .with(UpdateFlag.ON_STYLEPOLICY));
    });
  }

  /**
   * Instance.
   *
   * @author Robert Rödler
   *
   * @navhas - visualizes - EMModel
   * @assoc - - - GrahamScanConvexHull2D
   */
  // TODO: nicer stacking of n-fold hulls
  // TODO: can we find a proper sphere for 3+ dimensions?
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * Generic tags to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String EMBORDER = "EMClusterBorder";

    /**
     * Kappa constant.
     */
    private static final double KAPPA = SVGHyperSphere.EUCLIDEAN_KAPPA;

    /**
     * Minus Kappa constant.
     */
    private static final double MKAPPA = -KAPPA;

    /**
     * Opacity
     */
    private int opacStyle = 1;

    /**
     * Soft or hard border
     */
    private int softBorder = 1;

    /**
     * Round or boxed.
     */
    private int drawStyle = 0;

    /**
     * Constructor
     *
     * @param context Visualizer context
     * @param task VisualizationTask
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StylingPolicy spol = context.getStylingPolicy();
      if(!(spol instanceof ClusterStylingPolicy)) {
        return;
      }
      @SuppressWarnings("unchecked")
      Clustering<Model> clustering = (Clustering<Model>) ((ClusterStylingPolicy) spol).getClustering();
      List<Cluster<Model>> clusters = clustering.getAllClusters();
      if(clusters.size() <= 1) {
        return;
      }

      StyleLibrary style = context.getStyleLibrary();
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      Iterator<Cluster<Model>> ci = clusters.iterator();
      for(int cnum = 0; cnum < clusters.size(); cnum++) {
        Cluster<Model> clus = ci.next();
        DBIDs ids = clus.getIDs();
        if(ids.size() <= 0) {
          continue;
        }
        if(!(clus.getModel() instanceof EMModel)) {
          continue;
        }
        EMModel model = (EMModel) clus.getModel();

        // Add cluster style
        final String sname = EMBORDER + "_" + cnum;
        if(!svgp.getCSSClassManager().contains(sname)) {
          CSSClass cls = new CSSClass(this, sname);
          cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) * .5);

          String color = colors.getColor(cnum);
          if(softBorder == 0) {
            cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
          }
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.15);

          svgp.addCSSClassOrLogError(cls);
        }

        double[][] covmat = model.getCovarianceMatrix();
        double[] centroid = model.getMean();
        double[] cent = proj.fastProjectDataToRenderSpace(centroid);

        // Compute the eigenvectors
        EigenvalueDecomposition evd = new EigenvalueDecomposition(covmat);
        double[] eigenvalues = evd.getRealEigenvalues();
        double[][] eigenvectors = transpose(evd.getV());

        // Projected eigenvectors:
        double[][] pc = new double[eigenvalues.length][];
        for(int i = 0; i < eigenvalues.length; i++) {
          pc[i] = proj.fastProjectRelativeDataToRenderSpace(times(eigenvectors[i], FastMath.sqrt(eigenvalues[i])));
        }
        if(drawStyle != 0 || eigenvalues.length == 2) {
          drawSphere2D(sname, cent, pc);
        }
        else {
          drawHullLines(sname, cent, makeHullComplex(pc));
        }
      }
    }

    /**
     * Draw by approximating a sphere via cubic splines
     *
     * @param sname CSS class name
     * @param cent center
     * @param pc Principal components
     */
    protected void drawSphere2D(String sname, double[] cent, double[][] pc) {
      CSSClass cls = opacStyle == 1 ? new CSSClass(null, "temp") : null;
      double[] p1 = new double[2], p2 = new double[2], p3 = new double[2],
          p4 = new double[2], tmp1 = new double[2], tmp2 = new double[2];
      for(int dim1 = 0; dim1 < pc.length - 1; dim1++) {
        for(int dim2 = dim1 + 1; dim2 < pc.length; dim2++) {
          for(int i = 1; i <= SIGMA.length; i++) {
            equalsPlusTimes(p1, cent, pc[dim1], i);
            equalsPlusTimes(p2, cent, pc[dim2], i);
            equalsPlusTimes(p3, cent, pc[dim1], -i);
            equalsPlusTimes(p4, cent, pc[dim2], -i);

            SVGPath path = new SVGPath();
            path.moveTo(p1);
            path.cubicTo(//
                equalsPlusTimes(tmp1, p1, pc[dim2], KAPPA * i), //
                equalsPlusTimes(tmp2, p2, pc[dim1], KAPPA * i), //
                p2);
            path.cubicTo(//
                equalsPlusTimes(tmp1, p2, pc[dim1], MKAPPA * i), //
                equalsPlusTimes(tmp2, p3, pc[dim2], KAPPA * i), //
                p3);
            path.cubicTo(//
                equalsPlusTimes(tmp1, p3, pc[dim2], MKAPPA * i), //
                equalsPlusTimes(tmp2, p4, pc[dim1], MKAPPA * i), //
                p4);
            path.cubicTo(//
                equalsPlusTimes(tmp1, p4, pc[dim1], KAPPA * i), //
                equalsPlusTimes(tmp2, p1, pc[dim2], MKAPPA * i), //
                p1);
            path.close();

            Element ellipse = path.makeElement(svgp, sname);
            if(cls != null) {
              cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, SIGMA[i - 1]);
              SVGUtil.setAtt(ellipse, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
            }
            layer.appendChild(ellipse);
          }
        }
      }
    }

    /**
     * Compute out = x + y * a, for 2d.
     * 
     * @param out Output buffer
     * @param x X
     * @param y Y
     * @param a Scaling
     * @return out
     */
    private double[] equalsPlusTimes(double[] out, double[] x, double[] y, double a) {
      out[0] = y[0] * a + x[0];
      out[1] = y[1] * a + x[1];
      return out;
    }

    /**
     * Approximate by convex hull.
     *
     * @param sname CSS name
     * @param cent center
     * @param chres Polygon around center
     */
    protected void drawHullLines(String sname, double[] cent, Polygon chres) {
      if(chres.size() <= 1) {
        return;
      }
      CSSClass cls = opacStyle == 1 ? new CSSClass(null, "temp") : null;
      for(int i = 1; i <= SIGMA.length; i++) {
        SVGPath path = new SVGPath();
        for(int p = 0; p < chres.size(); p++) {
          path.drawTo(plusTimes(cent, chres.get(p), i));
        }
        path.close();
        Element ellipse = path.makeElement(svgp, sname);
        if(cls != null) {
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, SIGMA[i - 1]);
          SVGUtil.setAtt(ellipse, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
        }
        layer.appendChild(ellipse);
      }
    }

    /**
     * Build a convex hull to approximate the sphere.
     *
     * @param pc Principal components
     * @return Polygon
     */
    protected Polygon makeHull(double[][] pc) {
      FilteredConvexHull2D hull = new FilteredConvexHull2D();

      double[] diag = new double[] { 0, 0 };
      for(int j = 0; j < pc.length; j++) {
        hull.add(pc[j]);
        hull.add(times(pc[j], -1));
        for(int k = j + 1; k < pc.length; k++) {
          double[] q = pc[k];
          double[] ppq = timesEquals(plus(pc[j], q), MathUtil.SQRTHALF);
          double[] pmq = timesEquals(minus(pc[j], q), MathUtil.SQRTHALF);
          hull.add(ppq);
          hull.add(times(ppq, -1));
          hull.add(pmq);
          hull.add(times(pmq, -1));
        }
        plusEquals(diag, pc[j]);
      }
      timesEquals(diag, 1.0 / FastMath.sqrt(pc.length));
      hull.add(diag);
      hull.add(times(diag, -1));

      return hull.getHull();
    }

    /**
     * Build a convex hull to approximate the sphere.
     *
     * @param pc Principal components
     * @return Polygon
     */
    protected Polygon makeHullComplex(double[][] pc) {
      FilteredConvexHull2D hull = new FilteredConvexHull2D();

      double[] diag = new double[] { 0, 0 };
      for(int j = 0; j < pc.length; j++) {
        hull.add(pc[j]);
        hull.add(times(pc[j], -1));
        for(int k = j + 1; k < pc.length; k++) {
          double[] q = pc[k];
          double[] ppq = timesEquals(plus(pc[j], q), MathUtil.SQRTHALF);
          double[] pmq = timesEquals(minus(pc[j], q), MathUtil.SQRTHALF);
          hull.add(ppq);
          hull.add(times(ppq, -1));
          hull.add(pmq);
          hull.add(times(pmq, -1));
          for(int l = k + 1; l < pc.length; l++) {
            double[] r = pc[k];
            double[] ppqpr = timesEquals(plus(ppq, r), MathUtil.SQRTTHIRD);
            double[] pmqpr = timesEquals(plus(pmq, r), MathUtil.SQRTTHIRD);
            double[] ppqmr = timesEquals(minus(ppq, r), MathUtil.SQRTTHIRD);
            double[] pmqmr = timesEquals(minus(pmq, r), MathUtil.SQRTTHIRD);
            hull.add(ppqpr);
            hull.add(times(ppqpr, -1));
            hull.add(pmqpr);
            hull.add(times(pmqpr, -1));
            hull.add(ppqmr);
            hull.add(times(ppqmr, -1));
            hull.add(pmqmr);
            hull.add(times(pmqmr, -1));
          }
        }
        plusEquals(diag, pc[j]);
      }
      timesEquals(diag, 1.0 / FastMath.sqrt(pc.length));
      hull.add(diag);
      hull.add(times(diag, -1));
      return hull.getHull();
    }

    /**
     * Approximate the hull using arcs.
     *
     * @param sname CSS name
     * @param cent Center
     * @param chres Polygon
     */
    protected void drawHullArc(String sname, double[] cent, Polygon chres) {
      if(chres.size() <= 1) {
        return;
      }
      CSSClass cls = opacStyle == 1 ? new CSSClass(null, "temp") : null;
      for(int i = 1; i <= SIGMA.length; i++) {
        SVGPath path = new SVGPath();

        ArrayList<double[]> delta = new ArrayList<>(chres.size());
        for(int p = 0; p < chres.size(); p++) {
          double[] prev = chres.get((p - 1 + chres.size()) % chres.size());
          double[] curr = chres.get(p);
          double[] next = chres.get((p + 1) % chres.size());
          double[] d1 = normalize(minus(next, curr));
          double[] d2 = normalize(minus(curr, prev));
          delta.add(plus(d1, d2));
          // delta.add(next.minus(prev));
        }

        for(int p = 0; p < chres.size(); p++) {
          double[] cur = plus(cent, chres.get(p));
          double[] nex = plus(cent, chres.get((p + 1) % chres.size()));
          double[] dcur = delta.get(p);
          double[] dnex = delta.get((p + 1) % chres.size());
          drawArc(path, cent, cur, nex, dcur, dnex, i);
        }
        path.close();

        Element ellipse = path.makeElement(svgp, sname);
        if(cls != null) {
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, SIGMA[i - 1]);
          SVGUtil.setAtt(ellipse, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
        }
        layer.appendChild(ellipse);
      }
    }

    /**
     * Draw an arc to simulate the hyper ellipse.
     *
     * @param path Path to draw to
     * @param cent Center
     * @param pre Previous point
     * @param nex Next point
     * @param scale Scaling factor
     */
    private void drawArc(SVGPath path, double[] cent, double[] pre, double[] nex, double[] oPrev, double[] oNext, double scale) {
      // Delta vectors
      final double[] rPrev = minus(pre, cent);
      final double[] rNext = minus(nex, cent);
      final double[] rPrNe = minus(pre, nex);
      // Scaled fix points
      final double[] sPrev = plusTimes(cent, rPrev, scale);
      final double[] sNext = plusTimes(cent, rNext, scale);
      // Orthogonal double[]s to the relative double[]s
      // final double[] oPrev = new double[](rPrev.get(1), -rPrev.get(0));
      // final double[] oNext = new double[](-rNext.get(1), rNext.get(0));

      // Compute the intersection of rPrev+tp*oPrev and rNext+tn*oNext
      // rPrNe == rPrev - rNext
      final double zp = rPrNe[0] * oNext[1] - rPrNe[1] * oNext[0];
      final double zn = rPrNe[0] * oPrev[1] - rPrNe[1] * oPrev[0];
      final double n = oPrev[1] * oNext[0] - oPrev[0] * oNext[1];
      if(n == 0) {
        LOG.warning("Parallel?!?");
        path.drawTo(sNext[0], sNext[1]);
        return;
      }
      final double tp = Math.abs(zp / n), tn = Math.abs(zn / n);

      // Guide points
      final double[] gPrev = plusTimes(sPrev, oPrev, KAPPA * scale * tp);
      final double[] gNext = minusTimes(sNext, oNext, KAPPA * scale * tn);

      if(!path.isStarted()) {
        path.moveTo(sPrev);
      }
      // path.drawTo(sPrev);
      // path.drawTo(gPrev);
      // path.drawTo(gNext);
      // path.drawTo(sNext));
      // path.moveTo(sPrev);
      // if(tp < 0 || tn < 0) {
      // path.drawTo(sNext);
      // }
      // else {
      path.cubicTo(gPrev, gNext, sNext);
      // }
    }
  }
}
