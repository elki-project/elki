package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Factory for visualizers to generate an SVG-Element containing a marker for
 * the mean in a KMeans-Clustering
 * 
 * @author Heidi Kolb
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class ClusterMeanVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Means";

  /**
   * Clustering to visualize.
   */
  protected Clustering<MeanModel<NV>> clustering = null;

  /**
   * Constructor
   */
  public ClusterMeanVisualizer() {
    super(NAME);
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_CLUSTERING);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new ClusterMeanVisualization<NV>(context, clustering, svgp, proj, width, height);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param clustering Clustering to visualize
   */
  public void init(VisualizerContext<? extends NV> context, Clustering<MeanModel<NV>> clustering) {
    super.init(context);
    super.setLevel(Visualizer.LEVEL_DATA + 1);
    this.clustering = clustering;
  }

  @Override
  public Visualization makeThumbnail(SVGPlot svgp, Projection2D proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV, Projection2D>(this, context, svgp, proj, width, height, tresolution, ThumbnailVisualization.ON_DATA);
  }

  /**
   * Visualize the mean of a KMeans-Clustering
   * 
   * @author Heidi Kolb
   * 
   * @param <NV> Type of the DatabaseObject being visualized.
   */
  public static class ClusterMeanVisualization<NV extends NumberVector<NV, ?>> extends Projection2DVisualization<NV> {
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

    /**
     * The actual visualization instance, for a single projection
     * 
     * @param context The context
     * @param svgp The SVGPlot
     * @param proj The Projection
     * @param width The width
     * @param height The height
     */
    public ClusterMeanVisualization(VisualizerContext<? extends NV> context, Clustering<MeanModel<NV>> clustering, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_DATA + 1);
      context.addContextChangeListener(this);
      this.clustering = clustering;
      incrementalRedraw();
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);

      MarkerLibrary ml = context.getMarkerLibrary();
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
  }
}