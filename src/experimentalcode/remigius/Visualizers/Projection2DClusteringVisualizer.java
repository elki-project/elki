package experimentalcode.remigius.Visualizers;

import java.util.Iterator;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class Projection2DClusteringVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Clusterdots";

  /**
   * Initializes this Visualizer.
   * 
   * @param database contains all objects to be processed.
   */
  public void init(VisualizerContext context) {
    super.init(NAME, context);
    super.setLevel(Visualizer.LEVEL_BACKGROUND + 1);
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj) {
    MarkerLibrary ml = context.getMarkerLibrary();
    Clustering<Model> c = context.getOrCreateDefaultClustering();
    Element layer = super.setupCanvas(svgp, proj);
    // get the Database
    Database<NV> database = context.getDatabase();
    // draw data
    Iterator<Cluster<Model>> ci = c.getAllClusters().iterator();
    for(int cnum = 0; cnum < c.getAllClusters().size(); cnum++) {
      Cluster<?> clus = ci.next();
      for(Integer objId : clus.getIDs()) {
        Vector v = proj.projectDataToRenderSpace(database.get(objId));
        Element dot = ml.useMarker(svgp, layer, v.get(0), v.get(1), cnum, 0.01);
        layer.appendChild(dot);
      }
    }
    return layer;
  }
}
