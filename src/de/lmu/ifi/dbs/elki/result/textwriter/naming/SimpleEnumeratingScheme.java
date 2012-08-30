package de.lmu.ifi.dbs.elki.result.textwriter.naming;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;

/**
 * Simple enumerating naming scheme. Cluster names are generated as follows: -
 * if the cluster has a name assigned, use it - otherwise use getNameAutomatic()
 * as name, and add an enumeration postfix
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Clustering
 */
public class SimpleEnumeratingScheme implements NamingScheme {
  /**
   * Clustering this scheme is applied to.
   */
  private Clustering<?> clustering;

  /**
   * count how often each name occurred so far.
   */
  private Map<String, Integer> namecount = new HashMap<String, Integer>();

  /**
   * Assigned cluster names.
   */
  private Map<Cluster<?>, String> names = new HashMap<Cluster<?>, String>();

  /**
   * This is the postfix added to the first cluster, which will be removed when
   * there is only one cluster of this name.
   */
  private static final String nullpostfix = " " + Integer.toString(0);

  /**
   * Constructor.
   * 
   * @param clustering Clustering result to name.
   */
  public SimpleEnumeratingScheme(Clustering<?> clustering) {
    super();
    this.clustering = clustering;
    updateNames();
  }

  /**
   * Assign names to each cluster (which doesn't have a name yet)
   */
  private void updateNames() {
    for(Cluster<?> cluster : clustering.getAllClusters()) {
      if(names.get(cluster) == null) {
        String sugname = cluster.getNameAutomatic();
        Integer count = namecount.get(sugname);
        if(count == null) {
          count = new Integer(0);
        }
        names.put(cluster, sugname + " " + count.toString());
        count++;
        namecount.put(sugname, count);
      }
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
    if(nam.endsWith(nullpostfix)) {
      if(namecount.get(nam.substring(0, nam.length() - nullpostfix.length())) == 1) {
        nam = nam.substring(0, nam.length() - nullpostfix.length());
      }
    }
    return nam;
  }
}