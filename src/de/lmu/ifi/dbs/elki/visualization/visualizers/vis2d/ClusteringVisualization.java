package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Visualize a clustering using different markers for different clusters.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Clustering oneway - - visualizes
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
// TODO: ensure we have the right database for this
public class ClusteringVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DataStoreListener<NV> {
  /**
   * The result we visualize
   */
  private Clustering<Model> clustering;

  /**
   * Constructor.
   * 
   * @param clustering Clustering to visualize
   * @param context Context
   * @param svgp Plot
   * @param proj Projection
   * @param width Width
   * @param height Height
   */
  public ClusteringVisualization(VisualizationTask task) {
    super(task, VisFactory.LEVEL_DATA);
    this.clustering = task.getResult();
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

  /**
   * Visualization factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusteringVisualization oneway - - «create»
   * 
   * @param <NV> Type of the DatabaseObject being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Cluster Markers";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super(NAME, VisFactory.LEVEL_DATA);
    }
    
    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusteringVisualization<NV>(task);
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
          context.addVisualizer(c, this);
        }
      }
    }
  }
}