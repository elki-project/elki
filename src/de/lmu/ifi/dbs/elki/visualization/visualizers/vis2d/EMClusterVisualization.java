package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.ConvexHull2D;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Visualizer for generating SVG-Elements containing ellipses for first, second
 * and third standard deviation
 * 
 * @author Robert Rödler
 * 
 * @apiviz.has EMModel oneway - - visualizes
 * @apiviz.uses ConvexHull2D
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
// TODO: nicer stacking of n-fold hulls
// TODO: can we find a proper sphere for 3+ dimensions?
public class EMClusterVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "EM Cluster Visualization";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String EMBORDER = "EMClusterBorder";

  /**
   * The result we work on
   */
  Clustering<EMModel<NV>> clustering;

  private static final double KAPPA = SVGHyperSphere.EUCLIDEAN_KAPPA;

  /**
   * StyleParameter:
   */
  private int times = 3;

  private int opacStyle = 1;

  private int softBorder = 1;

  private int drawStyle = 0;

  final static double[] sigma = new double[] { 0.41, 0.223, 0.047 };

  /**
   * Constructor
   * 
   * @param task VisualizationTask
   */
  public EMClusterVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
    context.addContextChangeListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    // set styles
    addCSSClasses(svgp);

    // PCARunner
    PCARunner<NV> pcarun = ClassGenericsUtil.parameterizeOrAbort(PCARunner.class, new EmptyParameterization());

    Iterator<Cluster<EMModel<NV>>> ci = clustering.getAllClusters().iterator();
    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<EMModel<NV>> clus = ci.next();
      DBIDs ids = clus.getIDs();

      if(ids.size() > 0) {
        Matrix covmat = clus.getModel().getCovarianceMatrix();
        NV centroid = clus.getModel().getMean();
        Vector cent = new Vector(proj.fastProjectDataToRenderSpace(centroid));

        // Compute the eigenvectors
        SortedEigenPairs eps = pcarun.processCovarMatrix(covmat).getEigenPairs();

        Vector[] pc = new Vector[eps.size()];
        for(int i = 0; i < eps.size(); i++) {
          EigenPair ep = eps.getEigenPair(i);
          Vector sev = ep.getEigenvector().times(Math.sqrt(ep.getEigenvalue()));
          pc[i] = new Vector(proj.fastProjectRelativeDataToRenderSpace(sev));
        }
        if(drawStyle != 0 || eps.size() == 2) {
          drawSphere2D(cnum, cent, pc);
        }
        else {
          Polygon chres = makeHullComplex(pc);
          drawHullLines(cnum, cent, chres);
        }
      }
    }
  }

  protected void drawSphere2D(int cnum, Vector cent, Vector[] pc) {
    for(int dim1 = 0; dim1 < pc.length - 1; dim1++) {
      for(int dim2 = dim1 + 1; dim2 < pc.length; dim2++) {
        for(int i = 1; i <= times; i++) {
          SVGPath path = new SVGPath();

          Vector direction1 = pc[dim1].times(KAPPA * i);
          Vector direction2 = pc[dim2].times(KAPPA * i);

          Vector p1 = cent.plusTimes(pc[dim1], i);
          Vector p2 = cent.plusTimes(pc[dim2], i);
          Vector p3 = cent.minusTimes(pc[dim1], i);
          Vector p4 = cent.minusTimes(pc[dim2], i);

          path.moveTo(p1);
          path.cubicTo(p1.plus(direction2), p2.plus(direction1), p2);
          path.cubicTo(p2.minus(direction1), p3.plus(direction2), p3);
          path.cubicTo(p3.minus(direction2), p4.minus(direction1), p4);
          path.cubicTo(p4.plus(direction1), p1.minus(direction2), p1);
          path.close();

          Element ellipse = path.makeElement(svgp);
          SVGUtil.addCSSClass(ellipse, EMBORDER + cnum);
          if(opacStyle == 1) {
            CSSClass cls = new CSSClass(null, "temp");
            double s = (i > 1 && i <= sigma.length) ? sigma[i - 1] : 0.0;
            cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, s);
            SVGUtil.setAtt(ellipse, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
          }
          layer.appendChild(ellipse);
        }
      }
    }
  }

  protected void drawHullLines(int cnum, Vector cent, Polygon chres) {
    for(int i = 1; i <= times; i++) {
      SVGPath path = new SVGPath();
      for(int p = 0; p < chres.size(); p++) {
        Vector cur = cent.plusTimes(chres.get(p), i);
        path.drawTo(cur);
      }
      path.close();
      Element ellipse = path.makeElement(svgp);
      SVGUtil.addCSSClass(ellipse, EMBORDER + cnum);
      if(opacStyle == 1) {
        CSSClass cls = new CSSClass(null, "temp");
        double s = (i > 1 && i <= sigma.length) ? sigma[i - 1] : 0.0;
        cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, s);
        SVGUtil.setAtt(ellipse, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
      }
      layer.appendChild(ellipse);
    }
  }

  protected Polygon makeHull(Vector[] pc) {
    ConvexHull2D hull = new ConvexHull2D();

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

    Polygon chres = hull.getHull();
    return chres;
  }

  protected Polygon makeHullComplex(Vector[] pc) {
    ConvexHull2D hull = new ConvexHull2D();

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
    Polygon chres = hull.getHull();
    return chres;
  }

  protected void drawHullArc(int cnum, Vector cent, Polygon chres) {
    for(int i = 1; i <= times; i++) {
      SVGPath path = new SVGPath();

      ArrayList<Vector> delta = new ArrayList<Vector>(chres.size());
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

      SVGUtil.addCSSClass(ellipse, EMBORDER + cnum);
      if(opacStyle == 1) {
        CSSClass cls = new CSSClass(null, "temp");
        double s = (i > 1 && i <= sigma.length) ? sigma[i - 1] : 0.0;
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

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    if(!svgp.getCSSClassManager().contains(EMBORDER)) {
      ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
      String color;
      int clusterID = 0;

      for(@SuppressWarnings("unused")
      Cluster<?> cluster : clustering.getAllClusters()) {
        CSSClass cls = new CSSClass(this, EMBORDER + clusterID);
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) / 2);

        if(clustering.getAllClusters().size() == 1) {
          color = "black";
        }
        else {
          color = colors.getColor(clusterID);
        }
        if(softBorder == 0) {
          cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
        }
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
        cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.15);

        svgp.addCSSClassOrLogError(cls);
        if(opacStyle == 0) {
          break;
        }
        clusterID++;
      }
    }
  }

  /**
   * Visualizer for generating SVG-Elements containing ellipses for first,
   * second and third standard deviation
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses EMClusterVisualisation oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new EMClusterVisualization<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Iterator<Relation<? extends NumberVector<?, ?>>> reps = VisualizerUtil.iterateVectorFieldRepresentations(baseResult);
      for(Relation<? extends NumberVector<?, ?>> rep : IterableUtil.fromIterator(reps)) {
        // Find clusterings we can visualize:
        Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
        for(Clustering<?> c : clusterings) {
          if(c.getAllClusters().size() > 0) {
            // Does the cluster have a model with cluster means?
            Clustering<MeanModel<NV>> mcls = findMeanModel(c);
            if(mcls != null) {
              final VisualizationTask task = new VisualizationTask(NAME, c, rep, this, P2DVisualization.class);
              task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 3);
              baseResult.getHierarchy().add(c, task);
            }
          }
        }
      }
    }

    /**
     * Test if the given clustering has a mean model.
     * 
     * @param <NV> Vector type
     * @param c Clustering to inspect
     * @return the clustering cast to return a mean model, null otherwise.
     */
    @SuppressWarnings("unchecked")
    private static <NV extends NumberVector<NV, ?>> Clustering<MeanModel<NV>> findMeanModel(Clustering<?> c) {
      final Model firstModel = c.getAllClusters().get(0).getModel();
      if(c.getAllClusters().get(0).getModel() instanceof MeanModel<?> && firstModel instanceof EMModel<?>) {
        return (Clustering<MeanModel<NV>>) c;
      }
      return null;
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return Projection2D.class;
    }
  }
}
