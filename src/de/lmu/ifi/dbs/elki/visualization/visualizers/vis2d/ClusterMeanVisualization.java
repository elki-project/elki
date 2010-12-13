package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Collection;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;

/**
 * Visualize the mean of a KMeans-Clustering
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has MeanModel oneway - - visualizes
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class ClusterMeanVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Means";

  /**
   * CSS class name for center of the means
   */
  private final static String CSS_MEAN_CENTER = "mean-center";

  /**
   * CSS class name for center of the means
   */
  private final static String CSS_MEAN = "mean-marker";

  /**
   * Clustering to visualize.
   */
  Clustering<MeanModel<NV>> clustering;

  public ClusterMeanVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
    context.addContextChangeListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);

    MarkerLibrary ml = context.getStyleLibrary().markers();
    double marker_size = context.getStyleLibrary().getSize(StyleLibrary.MARKERPLOT);

    Iterator<Cluster<MeanModel<NV>>> ci = clustering.getAllClusters().iterator();
    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<MeanModel<NV>> clus = ci.next();
      double[] mean = proj.fastProjectDataToRenderSpace(clus.getModel().getMean());

      // add a greater Marker for the mean
      Element meanMarker = ml.useMarker(svgp, layer, mean[0], mean[1], cnum, marker_size * 3);
      SVGUtil.setAtt(meanMarker, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_MEAN);

      // Add a fine cross to mark the exact location of the mean.
      Element meanMarkerCenter = svgp.svgLine(mean[0] - .7, mean[1], mean[0] + .7, mean[1]);
      SVGUtil.setAtt(meanMarkerCenter, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_MEAN_CENTER);
      Element meanMarkerCenter2 = svgp.svgLine(mean[0], mean[1] - .7, mean[0], mean[1] + .7);
      SVGUtil.setAtt(meanMarkerCenter2, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_MEAN_CENTER);

      layer.appendChild(meanMarkerCenter);
      layer.appendChild(meanMarkerCenter2);
    }
  }

  @Override
  protected boolean testRedraw(ContextChangedEvent e) {
    return super.testRedraw(e) || (e instanceof SelectionChangedEvent);
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    if(!svgp.getCSSClassManager().contains(CSS_MEAN_CENTER)) {
      CSSClass center = new CSSClass(svgp, CSS_MEAN_CENTER);
      center.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.DEFAULT));
      center.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.AXIS_TICK) / 2);
      svgp.addCSSClassOrLogError(center);
    }
    if(!svgp.getCSSClassManager().contains(CSS_MEAN)) {
      CSSClass center = new CSSClass(svgp, CSS_MEAN);
      center.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.7");
      svgp.addCSSClassOrLogError(center);
    }
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing a marker for
   * the mean in a KMeans-Clustering
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusterMeanVisualization oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }
    
    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusterMeanVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      if(!VisualizerUtil.isNumberVectorDatabase(context.getDatabase())) {
        return;
      }
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        if(c.getAllClusters().size() > 0) {
          // Does the cluster have a model with cluster means?
          Clustering<MeanModel<NV>> mcls = findMeanModel(c);
          if(mcls != null) {
            final VisualizationTask task = new VisualizationTask(NAME, context, c, this);
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 1);
            context.addVisualizer(c, task);
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
    public Object getVisualizationType() {
      return P2DVisualization.class;
    }
  }
}