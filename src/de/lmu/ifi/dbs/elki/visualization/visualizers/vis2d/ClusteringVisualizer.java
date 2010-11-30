package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Visualize a clustering using different markers for different clusters.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ClusteringVisualization oneway - - produces
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class ClusteringVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Markers";

  /**
   * Clustering to visualize.
   */
  protected Clustering<Model> clustering = null;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ClusteringVisualizer() {
    super(NAME, Visualizer.LEVEL_DATA);
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_CLUSTERING);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param clustering Clustering to visualize
   */
  @SuppressWarnings("unchecked")
  public void init(VisualizerContext<? extends NV> context, Clustering<?> clustering) {
    super.init(context);
    this.clustering = (Clustering<Model>) clustering;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new ClusteringVisualization(context, svgp, proj, width, height);
  }

  @Override
  public Visualization makeThumbnail(SVGPlot svgp, Projection2D proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV, Projection2D>(this, context, svgp, proj, width, height, tresolution, ThumbnailVisualization.ON_DATA);
  }

  /**
   * The actual visualization instance, for a single projection
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has de.lmu.ifi.dbs.elki.data.Clustering oneway - - visualizes
   */
  protected class ClusteringVisualization extends Projection2DVisualization<NV> implements DataStoreListener<NV> {
    /**
     * Constructor.
     * 
     * @param context Context
     * @param svgp Plot
     * @param proj Projection
     * @param width Width
     * @param height Height
     */
    public ClusteringVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_DATA);
      context.addDataStoreListener(this);
      incrementalRedraw();
    }

    @Override
    public void destroy() {
      super.destroy();
      context.removeDataStoreListener(this);
    }

    @Override
    public void redraw() {
      MarkerLibrary ml = context.getMarkerLibrary();
      double marker_size = context.getStyleLibrary().getSize(StyleLibrary.MARKERPLOT);
      // get the Database
      Database<? extends NV> database = context.getDatabase();
      // draw data
      Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
      for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
        Cluster<?> clus = ci.next();
        for(DBID objId : clus.getIDs()) {
          try {
            final NV vec = database.get(objId);
            double[] v = proj.fastProjectDataToRenderSpace(vec);
            ml.useMarker(svgp, layer, v[0], v[1], cnum, marker_size);
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
    }

    @Override
    public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<NV> e) {
      synchronizedRedraw();
    }
  }
}