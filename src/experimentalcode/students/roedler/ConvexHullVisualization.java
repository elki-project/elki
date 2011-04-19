package experimentalcode.students.roedler;

import java.util.Collection;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;
import experimentalcode.students.roedler.utils.convexhull.ConvexHull2D;

/**
 * Visualizer for generating an SVG-Element containing the convex hull of each
 * cluster
 * 
 * @author Robert Rödler
 * 
 * @apiviz.has MeanModel oneway - - visualizes
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class ConvexHullVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "convex Hull Visualization";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CONVEXHULL = "convexHull";

  /**
   * The result we work on
   */
  Clustering<MeanModel<NV>> clustering;

  /**
   * The hulls
   */
  Element hulls;

  /**
   * Constructor
   * 
   * @param task VisualizationTask
   */
  public ConvexHullVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
    context.addContextChangeListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {

    // addCSSClasses(svgp);
    // SVGPath path = new SVGPath();
    ConvexHull2D ch;

    double opacity = 0.25;
    Vector[] chres;
    Vector[] means = new Vector[clustering.getAllClusters().size()];
    Vector[] meansproj = new Vector[clustering.getAllClusters().size()];

    Iterator<Cluster<MeanModel<NV>>> ci = clustering.getAllClusters().iterator();
    int clusterID = 0;

    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      SVGPath path = new SVGPath();
      Cluster<MeanModel<NV>> clus = ci.next();
      double[] mean = proj.fastProjectDataToRenderSpace(clus.getModel().getMean());
      meansproj[cnum] = new Vector(mean);
      means[cnum] = clus.getModel().getMean().getColumnVector();

      Vector[] clsPoints = new Vector[clustering.getAllClusters().get(cnum).getIDs().size()];
      Iterator<DBID> clp = clustering.getAllClusters().get(cnum).getIDs().iterator();

      for(int i = 0; i < clsPoints.length; i++) {
        DBID clpnum = clp.next();
        double[] projP = proj.fastProjectDataToRenderSpace(rep.get(clpnum).getColumnVector());
        clsPoints[i] = new Vector(projP);
      }
      ch = new ConvexHull2D(clsPoints);
      chres = ch.start();

      double minX, maxX, minY, maxY;

      if(chres != null) {
        minX = chres[0].get(0);
        maxX = chres[0].get(0);
        minY = chres[1].get(1);
        maxY = chres[1].get(1);

        for(int i = 0; i < chres.length; i++) {
          if(chres[i].get(0) > maxX) {
            maxX = chres[i].get(0);
          }
          if(chres[i].get(0) < minX) {
            minX = chres[i].get(0);
          }
          if(chres[i].get(1) > maxY) {
            maxY = chres[i].get(1);
          }
          if(chres[i].get(1) < minY) {
            minY = chres[i].get(1);
          }

          path.drawTo(chres[i].get(0), chres[i].get(1));
        }
        double hullarea = (Math.abs(maxX - minX) * Math.abs(maxY - minY));
        double projarea = (proj.estimateViewport().getFirst().getMax() - proj.estimateViewport().getFirst().getMin()) * (proj.estimateViewport().getSecond().getMax() - proj.estimateViewport().getSecond().getMin());

        opacity = Math.sqrt(((double) clsPoints.length / rep.size()) * ((projarea - hullarea) / projarea));

        path.close();
      }

      hulls = path.makeElement(svgp);
      addCSSClasses(svgp, cnum, opacity);

      SVGUtil.addCSSClass(hulls, CONVEXHULL + cnum);
      layer.appendChild(hulls);
      clusterID++;
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp, int clusterID, double opac) {
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    CSSClass cls = new CSSClass(this, CONVEXHULL + clusterID);
    cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));

    String color;

    if(clustering.getAllClusters().size() == 1) {
      color = "black";
    }
    else {
      color = colors.getColor(clusterID);
    }
    cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
    cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, opac);

    svgp.addCSSClassOrLogError(cls);
  }

  /*
   * @Override public void resultChanged(Result current) { if (current
   * instanceof SelectionResult) { synchronizedRedraw(); return; }
   * super.resultChanged(current); }
   */

  /**
   * Factory for visualizers to generate an SVG-Element containing the lines
   * between kMeans clusters
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses kMeansVisualisation oneway - - «create»
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
      return new ConvexHullVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext context, Result result) {
      Iterator<Relation<? extends NumberVector<?, ?>>> reps = VisualizerUtil.iterateVectorFieldRepresentations(context.getDatabase());
      for(Relation<? extends NumberVector<?, ?>> rep : IterableUtil.fromIterator(reps)) {
        // Find clusterings we can visualize:
        Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
        for(Clustering<?> c : clusterings) {
          if(c.getAllClusters().size() > 0) {
            // Does the cluster have a model with cluster means?
            Clustering<MeanModel<NV>> mcls = findMeanModel(c);
            if(mcls != null) {
              final VisualizationTask task = new VisualizationTask(NAME, context, c, rep, this, P2DVisualization.class);
              task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 3);
              context.addVisualizer(c, task);
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
      if(c.getAllClusters().get(0).getModel() instanceof MeanModel<?>) {
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
