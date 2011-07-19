package experimentalcode.students.roedler;

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
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
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
  private static final String NAME = "kMeans Border Visualization";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String KMEANSBORDER = "kMeansSeparator";

  /**
   * The result we work on
   */
  Clustering<MeanModel<NV>> clustering;

  /**
   * The voronoi diagram
   */
  Element voronoi;

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

    Vector[] means = new Vector[clustering.getAllClusters().size()];
    Vector[] meansproj = new Vector[clustering.getAllClusters().size()];

    Iterator<Cluster<MeanModel<NV>>> ci = clustering.getAllClusters().iterator();

    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<MeanModel<NV>> clus = ci.next();
      double[] mean = proj.fastProjectDataToRenderSpace(clus.getModel().getMean());
      meansproj[cnum] = new Vector(mean);
      means[cnum] = clus.getModel().getMean().getColumnVector();
    }
    Voronoi vrni = new Voronoi(svgp, proj);
    voronoi = vrni.createVoronoi(means, meansproj);
    SVGUtil.addCSSClass(voronoi, KMEANSBORDER);
    layer.appendChild(voronoi);
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
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
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
      return new kMeansBorderVisualization<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      final VisualizerContext context = VisualizerUtil.getContext(baseResult);
      Iterator<Relation<? extends NumberVector<?, ?>>> reps = VisualizerUtil.iterateVectorFieldRepresentations(context);
      for(Relation<? extends NumberVector<?, ?>> rep : IterableUtil.fromIterator(reps)) {
        if(DatabaseUtil.dimensionality(rep) > 2) {
          return;
        }
        // Find clusterings we can visualize:
        Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
        for(Clustering<?> c : clusterings) {
          if(c.getAllClusters().size() > 0) {
            // Does the cluster have a model with cluster means?
            Clustering<MeanModel<NV>> mcls = findMeanModel(c);
            if(mcls != null) {
              final VisualizationTask task = new VisualizationTask(NAME, c, rep, this, P2DVisualization.class);
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
      final Model firstModel = c.getAllClusters().get(0).getModel();
      if(firstModel instanceof MeanModel<?> && !(firstModel instanceof EMModel<?>)) {
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
