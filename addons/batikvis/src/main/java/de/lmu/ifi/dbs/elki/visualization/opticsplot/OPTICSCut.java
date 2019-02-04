/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;

/**
 * Compute a partitioning from an OPTICS plot by doing a horizontal cut.
 *
 * @author Heidi Kolb
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - ClusterOrder
 */
// TODO: add non-flat clusterings
public final class OPTICSCut {
  /**
   * Private constructor. Static methods only.
   */
  private OPTICSCut() {
    // Do not use.
  }

  /**
   * Compute an OPTICS cut clustering
   *
   * @param co Cluster order result
   * @param epsilon Epsilon value for cut
   * @return New partitioning clustering
   */
  public static <E extends ClusterOrder> Clustering<Model> makeOPTICSCut(E co, double epsilon) {
    // Clustering model we are building
    Clustering<Model> clustering = new Clustering<>("OPTICS Cut Clustering", "optics-cut");
    // Collects noise elements
    ModifiableDBIDs noise = DBIDUtil.newHashSet();

    double lastDist = Double.MAX_VALUE;
    double actDist = Double.MAX_VALUE;

    // Current working set
    ModifiableDBIDs current = DBIDUtil.newHashSet();

    // TODO: can we implement this more nicely with a 1-lookahead?
    DBIDVar prev = DBIDUtil.newVar();
    for(DBIDIter it = co.iter(); it.valid(); prev.set(it), it.advance()) {
      lastDist = actDist;
      actDist = co.getReachability(it);

      if(actDist <= epsilon) {
        // the last element before the plot drops belongs to the cluster
        if(lastDist > epsilon && prev.isSet()) {
          // So un-noise it
          noise.remove(prev);
          // Add it to the cluster
          current.add(prev);
        }
        current.add(it);
      }
      else {
        // 'Finish' the previous cluster
        if(!current.isEmpty()) {
          // TODO: do we want a minpts restriction?
          // But we get have only core points guaranteed anyway.
          clustering.addToplevelCluster(new Cluster<Model>(current, ClusterModel.CLUSTER));
          current = DBIDUtil.newHashSet();
        }
        // Add to noise
        noise.add(it);
      }
    }
    // Any unfinished cluster will also be added
    if(!current.isEmpty()) {
      clustering.addToplevelCluster(new Cluster<Model>(current, ClusterModel.CLUSTER));
    }
    // Add noise
    clustering.addToplevelCluster(new Cluster<Model>(noise, true, ClusterModel.CLUSTER));
    return clustering;
  }
}