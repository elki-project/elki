package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;

/**
 * Compute a partitioning from an OPTICS plot by doing a horizontal cut.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 * 
 * @apiviz.uses ClusterOrderResult
 * @apiviz.uses OPTICSDistanceAdapter
 */
// TODO: add non-flat clusterings
public class OPTICSCut {
  /**
   * Compute an OPTICS cut clustering
   * 
   * @param <D> Distance type
   * @param co Cluster order result
   * @param adapter Distance adapter
   * @param epsilon Epsilon value for cut
   * @return New partitioning clustering
   */
  public static <D extends Distance<D>> Clustering<Model> makeOPTICSCut(ClusterOrderResult<D> co, OPTICSDistanceAdapter<D> adapter, double epsilon) {
    List<ClusterOrderEntry<D>> order = co.getClusterOrder();
    // Clustering model we are building
    Clustering<Model> clustering = new Clustering<Model>("OPTICS Cut Clustering", "optics-cut");
    // Collects noise elements
    ModifiableDBIDs noise = DBIDUtil.newHashSet();

    double lastDist = Double.MAX_VALUE;
    double actDist = Double.MAX_VALUE;

    // Current working set
    ModifiableDBIDs current = DBIDUtil.newHashSet();

    // TODO: can we implement this more nicely with a 1-lookahead?
    for(int j = 0; j < order.size(); j++) {
      lastDist = actDist;
      actDist = adapter.getDoubleForEntry(order.get(j));

      if(actDist <= epsilon) {
        // the last element before the plot drops belongs to the cluster
        if(lastDist > epsilon && j > 0) {
          // So un-noise it
          noise.remove(order.get(j - 1).getID());
          // Add it to the cluster
          current.add(order.get(j - 1).getID());
        }
        current.add(order.get(j).getID());
      }
      else {
        // 'Finish' the previous cluster
        if(!current.isEmpty()) {
          // TODO: do we want a minpts restriction?
          // But we get have only core points guaranteed anyway.
          clustering.addCluster(new Cluster<Model>(current, ClusterModel.CLUSTER));
          current = DBIDUtil.newHashSet();
        }
        // Add to noise
        noise.add(order.get(j).getID());
      }
    }
    // Any unfinished cluster will also be added
    if(!current.isEmpty()) {
      clustering.addCluster(new Cluster<Model>(current, ClusterModel.CLUSTER));
    }
    // Add noise
    clustering.addCluster(new Cluster<Model>(noise, true, ClusterModel.CLUSTER));
    return clustering;
  }
}