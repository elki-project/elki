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
package elki.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import elki.data.model.Model;
import elki.data.type.SimpleTypeInformation;
import elki.result.Metadata;
import elki.utilities.datastructures.hierarchy.HashMapHierarchy;
import elki.utilities.datastructures.hierarchy.Hierarchy;
import elki.utilities.datastructures.hierarchy.ModifiableHierarchy;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.datastructures.iterator.IterableIt;

/**
 * Result class for clusterings. Can be used for both hierarchical and
 * non-hierarchical clusterings.
 *
 * The class does not enforce or rely on clusterings to be a tree or DAG,
 * instead they can be an arbitrary forest of directed graphs that COULD contain
 * cycles.
 *
 * This class is NOT iterable for a simple reason: there is more than one method
 * to do so. You need to specify whether you want to use getToplevelClusters()
 * or getAllClusters().
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @composed - - * Cluster
 *
 * @param <M> Model type
 */
public class Clustering<M extends Model> {
  /**
   * Type information, for relation matching.
   */
  public static final SimpleTypeInformation<Clustering<?>> TYPE = new SimpleTypeInformation<>(Clustering.class);

  /**
   * Keep a list of top level clusters.
   */
  private List<Cluster<M>> toplevelclusters;

  /**
   * Cluster hierarchy.
   */
  private ModifiableHierarchy<Cluster<M>> hierarchy;

  /**
   * Constructor with a list of top level clusters
   *
   * @param toplevelclusters Top level clusters
   */
  public Clustering(List<Cluster<M>> toplevelclusters) {
    super();
    this.toplevelclusters = toplevelclusters;
    this.hierarchy = new HashMapHierarchy<>();
    for(Cluster<M> clus : toplevelclusters) {
      hierarchy.add(clus);
    }
  }

  /**
   * Constructor for an empty clustering
   */
  public Clustering() {
    this(new ArrayList<Cluster<M>>());
  }

  /**
   * Add a cluster to the clustering.
   *
   * @param clus new cluster
   */
  public void addToplevelCluster(Cluster<M> clus) {
    toplevelclusters.add(clus);
    hierarchy.add(clus);
  }

  /**
   * Add a cluster to the clustering.
   *
   * @param parent Parent cluster
   * @param child Child cluster.
   */
  public void addChildCluster(Cluster<M> parent, Cluster<M> child) {
    hierarchy.add(parent, child);
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
   * Get the cluster hierarchy.
   *
   * @return Cluster hierarchy.
   */
  public Hierarchy<Cluster<M>> getClusterHierarchy() {
    return hierarchy;
  }

  /**
   * Collect all clusters (recursively) into a List.
   *
   * @return List of all clusters.
   */
  public List<Cluster<M>> getAllClusters() {
    ArrayList<Cluster<M>> res = new ArrayList<>(hierarchy.size());
    for(It<Cluster<M>> iter = hierarchy.iterAll(); iter.valid(); iter.advance()) {
      res.add(iter.get());
    }
    Collections.sort(res, Cluster.BY_NAME_SORTER);
    return res;
  }

  /**
   * Iterate over the top level clusters.
   *
   * @return Iterator
   */
  public It<Cluster<M>> iterToplevelClusters() {
    return new IterableIt<>(toplevelclusters);
  }

  /**
   * Collect all clustering results from a Result
   *
   * @param r Result
   * @return List of clustering results
   */
  public static List<Clustering<? extends Model>> getClusteringResults(Object r) {
    return Metadata.hierarchyOf(r).iterDescendantsSelf()//
        .<Clustering<? extends Model>> filter(Clustering.class)//
        .collect(new ArrayList<Clustering<? extends Model>>());
  }
}
