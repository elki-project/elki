package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D;
import de.lmu.ifi.dbs.elki.math.geometry.SweepHullDelaunay2D.Triangle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.VoronoiDraw;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;

/**
 * Visualizer drawing Voronoi cells for k-means clusterings.
 * 
 * See also: {@link de.lmu.ifi.dbs.elki.algorithm.clustering.KMeans KMeans
 * clustering}
 * 
 * @author Robert Rödler
 * @author Erich Schubert
 * 
 * @apiviz.has MeanModel oneway - - visualizes
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class VoronoiVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "k-means Voronoi cells";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  private static final String KMEANSBORDER = "kmeans-border";

  /**
   * Visualization mode.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static enum Mode {
    VORONOI, DELAUNAY, V_AND_D
  }

  /**
   * The result we work on
   */
  Clustering<MeanModel<NV>> clustering;

  /**
   * The Voronoi diagram
   */
  Element voronoi;

  /**
   * Active drawing mode.
   */
  private Mode mode;

  /**
   * Constructor
   * 
   * @param task VisualizationTask
   * @param mode Drawing mode
   */
  public VoronoiVisualization(VisualizationTask task, Mode mode) {
    super(task);
    this.clustering = task.getResult();
    this.mode = mode;
    context.addContextChangeListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    final List<Cluster<MeanModel<NV>>> clusters = clustering.getAllClusters();

    if(clusters.size() < 2) {
      return;
    }

    // Collect cluster means
    ArrayList<Vector> means = new ArrayList<Vector>(clusters.size());
    {
      for(Cluster<MeanModel<NV>> clus : clusters) {
        means.add(clus.getModel().getMean().getColumnVector());
      }
    }
    if(clusters.size() == 2) {
      if(mode == Mode.VORONOI || mode == Mode.V_AND_D) {
        Element path = VoronoiDraw.drawFakeVoronoi(proj, means).makeElement(svgp);
        SVGUtil.addCSSClass(path, KMEANSBORDER);
        layer.appendChild(path);
      }
      if(mode == Mode.DELAUNAY || mode == Mode.V_AND_D) {
        Element path = new SVGPath(proj.fastProjectDataToRenderSpace(means.get(0))).drawTo(proj.fastProjectDataToRenderSpace(means.get(1))).makeElement(svgp);
        SVGUtil.addCSSClass(path, KMEANSBORDER);
        layer.appendChild(path);
      }
    }
    else {
      // Compute Delaunay Triangulation
      ArrayList<Triangle> delaunay = new SweepHullDelaunay2D(means).getDelaunay();
      if(mode == Mode.VORONOI || mode == Mode.V_AND_D) {
        Element path = VoronoiDraw.drawVoronoi(proj, delaunay, means).makeElement(svgp);
        SVGUtil.addCSSClass(path, KMEANSBORDER);
        layer.appendChild(path);
      }
      if(mode == Mode.DELAUNAY || mode == Mode.V_AND_D) {
        Element path = VoronoiDraw.drawDelaunay(proj, delaunay, means).makeElement(svgp);
        SVGUtil.addCSSClass(path, KMEANSBORDER);
        layer.appendChild(path);
      }
    }
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
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses VoronoiVisualisation oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Mode for drawing: Voronoi, Delaunay, both
     * 
     * <p>
     * Key: {@code -voronoi.mode}
     * </p>
     */
    public static final OptionID MODE_ID = OptionID.getOrCreateOptionID("voronoi.mode", "Mode for drawing the voronoi cells (and/or delaunay triangulation)");

    /**
     * Drawing mode
     */
    private Mode mode;

    /**
     * Constructor
     * 
     * @param mode Drawing mode
     */
    public Factory(Mode mode) {
      super();
      this.mode = mode;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new VoronoiVisualization<NV>(task, mode);
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

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<NV extends NumberVector<NV, ?>> extends AbstractParameterizer {
      protected Mode mode;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        EnumParameter<Mode> modeP = new EnumParameter<Mode>(MODE_ID, Mode.class, Mode.VORONOI);
        if(config.grab(modeP)) {
          mode = modeP.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<NV>(mode);
      }
    }
  }
}