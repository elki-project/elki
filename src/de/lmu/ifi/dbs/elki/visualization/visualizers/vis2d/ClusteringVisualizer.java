package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Iterator;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize a clustering using different markers for different clusters.
 * 
 * @author Erich Schubert
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
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param clustering Clustering to visualize
   */
  @SuppressWarnings("unchecked")
  public void init(VisualizerContext context, Clustering<?> clustering) {
    super.init(NAME, context);
    super.setLevel(Visualizer.LEVEL_DATA);
    this.clustering = (Clustering<Model>) clustering;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    return new ClusteringVisualization(context, svgp, proj, width, height);
  }

  protected class ClusteringVisualization extends Projection2DVisualization<NV> implements DatabaseListener<NV> {
    Element container;

    public ClusteringVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height) {
      super(context, svgp, proj, width, height);
      double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
      this.container = super.setupCanvas(svgp, proj, margin, width, height);
      this.layer = new VisualizationLayer(Visualizer.LEVEL_DATA, this.container);

      context.addDatabaseListener(this);
      redraw();
    }

    @Override
    public void destroy() {
      super.destroy();
      context.removeDatabaseListener(this);
    }

    @Override
    public void redraw() {
      // Implementation note: replacing the container element is faster than
      // removing all markers and adding new ones - i.e. a "bluk" operation
      // instead of incremental changes
      Element oldcontainer = null;
      if(container.hasChildNodes()) {
        oldcontainer = container;
        container = (Element) container.cloneNode(false);
      }

      MarkerLibrary ml = context.getMarkerLibrary();
      double marker_size = context.getStyleLibrary().getSize(StyleLibrary.MARKERPLOT);
      // get the Database
      Database<? extends NV> database = context.getDatabase();
      // draw data
      Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
      for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
        Cluster<?> clus = ci.next();
        for(DBID objId : clus.getIDs()) {
          final NV vec = database.get(objId);
          if(vec != null) {
            Vector v = proj.projectDataToRenderSpace(vec);
            ml.useMarker(svgp, container, v.get(0), v.get(1), cnum, marker_size);
          }
        }
      }
      if(oldcontainer != null && oldcontainer.getParentNode() != null) {
        oldcontainer.getParentNode().replaceChild(container, oldcontainer);
      }
      //logger.warning("Redraw completed, " + this);
    }

    @Override
    public void objectsChanged(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      //logger.warning("change fired");
      synchronizedRedraw();
    }

    @Override
    public void objectsInserted(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      //logger.warning("insert fired");
      synchronizedRedraw();
    }

    @Override
    public void objectsRemoved(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      //logger.warning("remove fired");
      synchronizedRedraw();
    }
  }
}