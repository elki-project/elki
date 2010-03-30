package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Iterator;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
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
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   */
  public void init(VisualizerContext context) {
    super.init(NAME, context);
    super.setLevel(Visualizer.LEVEL_BACKGROUND + 1);
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    MarkerLibrary ml = context.getMarkerLibrary();
    Clustering<Model> c = context.getOrCreateDefaultClustering();
    double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = super.setupCanvas(svgp, proj, margin, width, height);
    
    double marker_size = context.getStyleLibrary().getSize(StyleLibrary.MARKERPLOT);
    // get the Database
    Database<NV> database = context.getDatabase();
    // draw data
    Iterator<Cluster<Model>> ci = c.getAllClusters().iterator();
    for(int cnum = 0; cnum < c.getAllClusters().size(); cnum++) {
      Cluster<?> clus = ci.next();
      for(Integer objId : clus.getIDs()) {
        Vector v = proj.projectDataToRenderSpace(database.get(objId));
        Element dot = ml.useMarker(svgp, layer, v.get(0), v.get(1), cnum, marker_size);
        layer.appendChild(dot);
      }
    }
    return layer;
  }
}