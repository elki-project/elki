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
package de.lmu.ifi.dbs.elki.result.textwriter.naming;

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Simple enumerating naming scheme. Cluster names are generated as follows:
 * <ol>
 * <li>if the cluster has a name assigned, use it
 * <li>otherwise use getNameAutomatic() as name, and add an enumeration postfix
 * </ol>
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @assoc - - - Clustering
 */
public class SimpleEnumeratingScheme implements NamingScheme {
  /**
   * Clustering this scheme is applied to.
   */
  private Clustering<?> clustering;

  /**
   * Count how often each name occurred so far.
   */
  private Object2IntOpenHashMap<String> namefreq;

  /**
   * Assigned cluster names.
   */
  private Map<Cluster<?>, String> names;

  /**
   * This is the postfix added to the first cluster, which will be removed when
   * there is only one cluster of this name.
   */
  private static final String NULLPOSTFIX = " 0";

  /**
   * Constructor.
   * 
   * @param clustering Clustering result to name.
   */
  public SimpleEnumeratingScheme(Clustering<?> clustering) {
    super();
    this.namefreq = new Object2IntOpenHashMap<>();
    this.names = new HashMap<>();
    this.clustering = clustering;
    updateNames();
  }

  /**
   * Assign names to each cluster (which doesn't have a name yet)
   */
  private void updateNames() {
    for(Cluster<?> cluster : clustering.getAllClusters()) {
      if(names.get(cluster) != null) {
        continue;
      }
      String sugname = cluster.getNameAutomatic();
      int count = namefreq.addTo(sugname, 1);
      names.put(cluster, sugname + " " + count);
    }
  }

  /**
   * Retrieve the cluster name. When a name has not yet been assigned, call
   * {@link #updateNames}
   */
  @Override
  public String getNameFor(Cluster<?> cluster) {
    String nam = names.get(cluster);
    if(nam == null) {
      updateNames();
      nam = names.get(cluster);
    }
    if(nam.endsWith(NULLPOSTFIX)) {
      String basename = nam.substring(0, nam.length() - NULLPOSTFIX.length());
      if(namefreq.getInt(basename) == 1) {
        nam = basename;
      }
    }
    return nam;
  }
}
