package experimentalcode.students.roedler;

import java.util.ArrayList;
import java.util.Arrays;
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
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;
import experimentalcode.students.roedler.utils.Voronoi;

/**
 * Visualizer for generating an SVG-Element containing lines that separates the
 * means
 * 
 * @author Robert Rödler
 * 
 * @apiviz.has MeanModel oneway - - visualizes
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class kMeansBorderVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "k-means Voronoi cells";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  private static final String KMEANSBORDER = "kmeans-border";

  /**
   * The result we work on
   */
  Clustering<MeanModel<NV>> clustering;

  /**
   * The voronoi diagram
   */
  Element voronoi;

  /**
   * Draw this factor beyond the viewport.
   */
  double linesLonger = 1.2;

  /**
   * Constructor
   * 
   * @param task VisualizationTask
   */
  public kMeansBorderVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
    context.addContextChangeListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);

    // Viewing area size
    double[] graphSize = new double[4];
    Pair<DoubleMinMax, DoubleMinMax> vp = proj.estimateViewport();
    graphSize[0] = vp.first.getMax() * linesLonger;
    graphSize[1] = vp.second.getMax() * linesLonger;
    graphSize[2] = vp.first.getMin() * linesLonger;
    graphSize[3] = vp.second.getMin() * linesLonger;

    // Project the means
    Vector[] means = new Vector[clustering.getAllClusters().size()];
    {
      int cnum = 0;
      for(Cluster<MeanModel<NV>> clus : clustering.getAllClusters()) {
        double[] mean = proj.fastProjectDataToRenderSpace(clus.getModel().getMean());
        means[cnum] = new Vector(mean);
        cnum++;
      }
    }
    final Element path;
    if (means.length == 2) {
      path = Voronoi.drawFakeVoronoi(graphSize, means).makeElement(svgp);
    } else if (means.length > 2) {
      ArrayList<Triangle> delaunay = new SweepHullDelaunay2D(Arrays.asList(means)).getDelaunay();
      path = Voronoi.drawVoronoi(graphSize, delaunay, means).makeElement(svgp);
    } else {
      return;
    }
    SVGUtil.addCSSClass(path, KMEANSBORDER);
    layer.appendChild(path);
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    // Class for the distance markers
    if(!svgp.getCSSClassManager().contains(KMEANSBORDER)) {
      CSSClass cls = new CSSClass(this, KMEANSBORDER);
      cls = new CSSClass(this, KMEANSBORDER);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) * .5);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing the lines
   * between kMeans clusters
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses kMeansBorderVisualisation oneway - - «create»
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
      return new kMeansBorderVisualization<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        if(c.getAllClusters().size() > 0) {
          // Does the cluster have a model with cluster means?
          Clustering<MeanModel<NV>> mcls = findMeanModel(c);
          if(mcls != null) {
            Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
            for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
              if(DatabaseUtil.dimensionality(p.getRelation()) == 2) {
                final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
                task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 3);
                baseResult.getHierarchy().add(p, task);
                baseResult.getHierarchy().add(c, task);
              }
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
      if(firstModel instanceof MeanModel<?> && !(firstModel instanceof EMModel<?>)) {
        return (Clustering<MeanModel<NV>>) c;
      }
      return null;
    }
  }
}