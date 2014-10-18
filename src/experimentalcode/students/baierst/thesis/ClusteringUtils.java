package experimentalcode.students.baierst.thesis;

import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Utility class for clustering results.
 * 
 * @author Stephan Baier
 * 
 */
public class ClusteringUtils {

  static public LinkedList<Cluster<?>> convertNoiseToSingletons(Clustering<?> c) {

    LinkedList<Cluster<?>> returnClusterList = new LinkedList<>();

    List<? extends Cluster<?>> clusters = c.getAllClusters();

    for(Cluster<?> cluster : clusters) {

      if(cluster.isNoise()) {
        for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {

          HashSetModifiableDBIDs hs = DBIDUtil.newHashSet();
          hs.add(it);
          Cluster<?> singleton = new Cluster<>(hs);
          returnClusterList.add(singleton);
        }
      }
      else {
        returnClusterList.add(cluster);
      }
    }
    return returnClusterList;
  }
}
