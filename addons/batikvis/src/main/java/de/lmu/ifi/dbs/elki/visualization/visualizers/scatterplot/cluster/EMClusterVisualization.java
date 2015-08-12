package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.cluster;

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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.geometry.GrahamScanConvexHull2D;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualizer for generating SVG-Elements containing ellipses for first, second
 * and third standard deviation. In more than 2-dimensional data, the class
 * tries to approximate the cluster extends.
 *
 * @author Robert Rödler
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class EMClusterVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "EM Cluster Models";

  /**
   * Constants for quantiles of standard deviation
   */
  final static double[] sigma = new double[] { 0.41, 0.223, 0.047 };

  /**
   * Constructor
   */
  public EMClusterVisualization() {
    super();
  }

  @Override
  public Instance makeVisualization(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNew(context, start, ScatterPlotProjector.class, new VisualizationTree.Handler1<ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, ScatterPlotProjector<?> p) {
        final VisualizationTask task = new VisualizationTask(NAME, context, p, p.getRelation(), EMClusterVisualization.this);
        task.level = VisualizationTask.LEVEL_DATA + 3;
        task.addUpdateFlags(VisualizationTask.ON_STYLEPOLICY);
        context.addVis(p, task);
      }
    });
  }

  /**
   * Instance.
   *
   * @author Robert Rödler
   *
   * @apiviz.has EMModel oneway - - visualizes
   * @apiviz.uses GrahamScanConvexHull2D
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
     * Kappa constant,
     */
    private static final double KAPPA = SVGHyperSphere.EUCLIDEAN_KAPPA;

    /**
     * StyleParameter:
     */
    private int times = 3;

    private int opacStyle = 1;

    private int softBorder = 1;

    private int drawStyle = 0;

    /**
     * Constructor
     *
     * @param task VisualizationTask
     */
    public Instance(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
    }

    @Override
    protected void redraw() {
      super.redraw();
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

      // PCARunner
      PCARunner pcarun = ClassGenericsUtil.parameterizeOrAbort(PCARunner.class, new EmptyParameterization());

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

        Matrix covmat = model.getCovarianceMatrix();
        Vector centroid = model.getMean();
        Vector cent = new Vector(proj.fastProjectDataToRenderSpace(centroid));

        // Compute the eigenvectors
        SortedEigenPairs eps = pcarun.processCovarMatrix(covmat).getEigenPairs();
        Vector[] pc = new Vector[eps.size()];
        for(int i = 0; i < eps.size(); i++) {
          EigenPair ep = eps.getEigenPair(i);
          Vector sev = ep.getEigenvector().times(Math.sqrt(ep.getEigenvalue()));
          pc[i] = new Vector(proj.fastProjectRelativeDataToRenderSpace(sev.getArrayRef()));
        }
        if(drawStyle != 0 || eps.size() == 2) {
          drawSphere2D(sname, cent, pc);
        }
        else {
          Polygon chres = makeHullComplex(pc);
          drawHullLines(sname, cent, chres);
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
    protected void drawSphere2D(String sname, Vector cent, Vector[] pc) {
      CSSClass cls = opacStyle == 1 ? new CSSClass(null, "temp") : null;
      for(int dim1 = 0; dim1 < pc.length - 1; dim1++) {
        for(int dim2 = dim1 + 1; dim2 < pc.length; dim2++) {
          for(int i = 1; i <= times; i++) {
            SVGPath path = new SVGPath();

            Vector p1 = cent.plusTimes(pc[dim1], i);
            Vector p2 = cent.plusTimes(pc[dim2], i);
            Vector p3 = cent.minusTimes(pc[dim1], i);
            Vector p4 = cent.minusTimes(pc[dim2], i);

            path.moveTo(p1);
            path.cubicTo(//
            p1.plusTimes(pc[dim2], KAPPA * i), //
            p2.plusTimes(pc[dim1], KAPPA * i), //
            p2);
            path.cubicTo(//
            p2.minusTimes(pc[dim1], KAPPA * i), //
            p3.plusTimes(pc[dim2], KAPPA * i), //
            p3);
            path.cubicTo(//
            p3.minusTimes(pc[dim2], KAPPA * i), //
            p4.minusTimes(pc[dim1], KAPPA * i), //
            p4);
            path.cubicTo(//
            p4.plusTimes(pc[dim1], KAPPA * i), //
            p1.minusTimes(pc[dim2], KAPPA * i), //
            p1);
            path.close();

            Element ellipse = path.makeElement(svgp);
            SVGUtil.addCSSClass(ellipse, sname);
            if(cls != null) {
              double s = (i >= 1 && i <= sigma.length) ? sigma[i - 1] : 0.0;
              cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, s);
              SVGUtil.setAtt(ellipse, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
            }
            layer.appendChild(ellipse);
          }
        }
      }
    }

    /**
     * Approximate by convex hull.
     *
     * @param sname CSS name
     * @param cent center
     * @param chres Polygon around center
     */
    protected void drawHullLines(String sname, Vector cent, Polygon chres) {
      if(chres.size() <= 1) {
        return;
      }
      CSSClass cls = opacStyle == 1 ? new CSSClass(null, "temp") : null;
      for(int i = 1; i <= times; i++) {
        SVGPath path = new SVGPath();
        for(int p = 0; p < chres.size(); p++) {
          path.drawTo(cent.plusTimes(chres.get(p), i));
        }
        path.close();
        Element ellipse = path.makeElement(svgp);
        SVGUtil.addCSSClass(ellipse, sname);
        if(cls != null) {
          double s = (i >= 1 && i <= sigma.length) ? sigma[i - 1] : 0.0;
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, s);
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
    protected Polygon makeHull(Vector[] pc) {
      GrahamScanConvexHull2D hull = new GrahamScanConvexHull2D();

      Vector diag = new Vector(0, 0);
      for(int j = 0; j < pc.length; j++) {
        hull.add(pc[j]);
        hull.add(pc[j].times(-1));
        for(int k = j + 1; k < pc.length; k++) {
          Vector q = pc[k];
          Vector ppq = pc[j].plus(q).timesEquals(MathUtil.SQRTHALF);
          Vector pmq = pc[j].minus(q).timesEquals(MathUtil.SQRTHALF);
          hull.add(ppq);
          hull.add(ppq.times(-1));
          hull.add(pmq);
          hull.add(pmq.times(-1));
        }
        diag.plusEquals(pc[j]);
      }
      diag.timesEquals(1.0 / Math.sqrt(pc.length));
      hull.add(diag);
      hull.add(diag.times(-1));

      return hull.getHull();
    }

    /**
     * Build a convex hull to approximate the sphere.
     *
     * @param pc Principal components
     * @return Polygon
     */
    protected Polygon makeHullComplex(Vector[] pc) {
      GrahamScanConvexHull2D hull = new GrahamScanConvexHull2D();

      Vector diag = new Vector(0, 0);
      for(int j = 0; j < pc.length; j++) {
        hull.add(pc[j]);
        hull.add(pc[j].times(-1));
        for(int k = j + 1; k < pc.length; k++) {
          Vector q = pc[k];
          Vector ppq = pc[j].plus(q).timesEquals(MathUtil.SQRTHALF);
          Vector pmq = pc[j].minus(q).timesEquals(MathUtil.SQRTHALF);
          hull.add(ppq);
          hull.add(ppq.times(-1));
          hull.add(pmq);
          hull.add(pmq.times(-1));
          for(int l = k + 1; l < pc.length; l++) {
            Vector r = pc[k];
            Vector ppqpr = ppq.plus(r).timesEquals(Math.sqrt(1 / 3.));
            Vector pmqpr = pmq.plus(r).timesEquals(Math.sqrt(1 / 3.));
            Vector ppqmr = ppq.minus(r).timesEquals(Math.sqrt(1 / 3.));
            Vector pmqmr = pmq.minus(r).timesEquals(Math.sqrt(1 / 3.));
            hull.add(ppqpr);
            hull.add(ppqpr.times(-1));
            hull.add(pmqpr);
            hull.add(pmqpr.times(-1));
            hull.add(ppqmr);
            hull.add(ppqmr.times(-1));
            hull.add(pmqmr);
            hull.add(pmqmr.times(-1));
          }
        }
        diag.plusEquals(pc[j]);
      }
      diag.timesEquals(1.0 / Math.sqrt(pc.length));
      hull.add(diag);
      hull.add(diag.times(-1));
      return hull.getHull();
    }

    /**
     * Approximate the hull using arcs.
     *
     * @param sname CSS name
     * @param cent Center
     * @param chres Polygon
     */
    protected void drawHullArc(String sname, Vector cent, Polygon chres) {
      if(chres.size() <= 1) {
        return;
      }
      CSSClass cls = opacStyle == 1 ? new CSSClass(null, "temp") : null;
      for(int i = 1; i <= times; i++) {
        SVGPath path = new SVGPath();

        ArrayList<Vector> delta = new ArrayList<>(chres.size());
        for(int p = 0; p < chres.size(); p++) {
          Vector prev = chres.get((p - 1 + chres.size()) % chres.size());
          Vector curr = chres.get(p);
          Vector next = chres.get((p + 1) % chres.size());
          Vector d1 = next.minus(curr).normalize();
          Vector d2 = curr.minus(prev).normalize();
          delta.add(d1.plus(d2));
          // delta.add(next.minus(prev));
        }

        for(int p = 0; p < chres.size(); p++) {
          Vector cur = cent.plus(chres.get(p));
          Vector nex = cent.plus(chres.get((p + 1) % chres.size()));
          Vector dcur = delta.get(p);
          Vector dnex = delta.get((p + 1) % chres.size());
          drawArc(path, cent, cur, nex, dcur, dnex, i);
        }
        path.close();

        Element ellipse = path.makeElement(svgp);

        SVGUtil.addCSSClass(ellipse, sname);
        if(cls != null) {
          double s = (i >= 1 && i <= sigma.length) ? sigma[i - 1] : 0.0;
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, s);
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
    private void drawArc(SVGPath path, Vector cent, Vector pre, Vector nex, Vector oPrev, Vector oNext, double scale) {
      // Delta vectors
      final Vector rPrev = pre.minus(cent);
      final Vector rNext = nex.minus(cent);
      final Vector rPrNe = pre.minus(nex);
      // Scaled fix points
      final Vector sPrev = cent.plusTimes(rPrev, scale);
      final Vector sNext = cent.plusTimes(rNext, scale);
      // Orthogonal vectors to the relative vectors
      // final Vector oPrev = new Vector(rPrev.get(1), -rPrev.get(0));
      // final Vector oNext = new Vector(-rNext.get(1), rNext.get(0));

      // Compute the intersection of rPrev+tp*oPrev and rNext+tn*oNext
      // rPrNe == rPrev - rNext
      final double zp = rPrNe.get(0) * oNext.get(1) - rPrNe.get(1) * oNext.get(0);
      final double zn = rPrNe.get(0) * oPrev.get(1) - rPrNe.get(1) * oPrev.get(0);
      final double n = oPrev.get(1) * oNext.get(0) - oPrev.get(0) * oNext.get(1);
      if(n == 0) {
        LoggingUtil.warning("Parallel?!?");
        path.drawTo(sNext.get(0), sNext.get(1));
        return;
      }
      final double tp = Math.abs(zp / n);
      final double tn = Math.abs(zn / n);
      // LoggingUtil.warning("tp: "+tp+" tn: "+tn);

      // Guide points
      final Vector gPrev = sPrev.plusTimes(oPrev, KAPPA * scale * tp);
      final Vector gNext = sNext.minusTimes(oNext, KAPPA * scale * tn);

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