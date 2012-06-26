package de.lmu.ifi.dbs.elki.data;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.BasicResult;

/**
 * Result class for clusterings. Can be used for both hierarchical and
 * non-hierarchical clusterings.
 * 
 * The class does not enforce or rely on clusterings to be a tree or DAG,
 * instead they can be an arbitrary forest of directed graphs that COULD contain
 * cycles.
 * 
 * This class is NOT iterable for a simple reason: there is more than one method to do so.
 * You need to specify whether you want to use getToplevelClusters() or getAllClusters().
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Cluster oneway - n
 * 
 * @param <M> Model type
 */
public class Clustering<M extends Model> extends BasicResult {
  /**
   * Keep a list of top level clusters.
   */
  private List<Cluster<M>> toplevelclusters;

  /**
   * Constructor with a list of top level clusters
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param toplevelclusters Top level clusters
   */
  public Clustering(String name, String shortname, List<Cluster<M>> toplevelclusters) {
    super(name, shortname);
    this.toplevelclusters = toplevelclusters;
  }

  /**
   * Constructor for an empty clustering
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public Clustering(String name, String shortname) {
    this(name, shortname, new ArrayList<Cluster<M>>());
  }

  /**
   * Add a cluster to the clustering.
   * 
   * @param n new cluster
   */
  public void addCluster(Cluster<M> n) {
    toplevelclusters.add(n);
  }

  /**
   * Return top level clusters
   * 
   * @return top level clusters
   */
  public List<Cluster<M>> getToplevelClusters() {
    return toplevelclusters;
  }

  /**
   * Collect all clusters (recursively) into a List.
   * 
   * @return List of all clusters.
   */
  public List<Cluster<M>> getAllClusters() {
    Set<Cluster<M>> clu = new HashSet<Cluster<M>>();
    for(Cluster<M> rc : toplevelclusters) {
      if(!clu.contains(rc)) {
        clu.add(rc);
        for (Iterator<Cluster<M>> iter = rc.iterDescendants(); iter.hasNext(); ) {
          clu.add(iter.next());
        }
      }
    }
    // Note: we canNOT use TreeSet above, because this comparator is only
    // partial!
    ArrayList<Cluster<M>> res = new ArrayList<Cluster<M>>(clu);
    Collections.sort(res, new Cluster.PartialComparator());
    return res;
  }
}
