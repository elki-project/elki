/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.clustering;

import java.util.ArrayList;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.SimplePrototypeModel;
import elki.data.type.TypeInformation;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.MutableProgress;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * GreedyKCenter algorithm as proposed by Teofilo F. Gonzalez.
 * It is a greedy Variant of a K-partitioning.
 * <p>
 * Reference:
 * <p>
 * Teofilo F. Gonzalez:<br>
 * >
 * Clustering to Minimize the Maximum Intercluster Distance.<br>
 * Theoretical Computer Science, 38, 1985, pp. 293-306
 * 
 * @author Robert Gehde
 *
 * @param <O> Object type to cluster
 */
@Reference(authors = "Teofilo F. Gonzalez", //
    title = "Clustering to Minimize the Maximum Intercluster Distance", //
    booktitle = "Theoretical Computer Science, 38", //
    url = "https://doi.org/10.1016/0304-3975(85)90224-5", //
    bibkey = "DBLP:journals/tcs/Gonzalez85")
public class GreedyKCenter<O> implements ClusteringAlgorithm<Clustering<SimplePrototypeModel<O>>> {

  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(GreedyKCenter.class);

  /**
   * Distance function
   */
  Distance<O> distance;

  /**
   * Distance query
   */
  DistanceQuery<O> distq;

  /**
   * number of clusters
   */
  int k;

  /**
   * 
   * Constructor.
   *
   * @param k number of clusters
   * @param distance distance function to use
   */
  public GreedyKCenter(int k, Distance<O> distance) {
    this.k = k;
    this.distance = distance;
  }

  /**
   * perform GreedyKCenter clustering on the relation
   * 
   * @param relation data to cluster
   * @return clustering
   */
  public Clustering<SimplePrototypeModel<O>> run(Relation<O> relation) {
    if(relation.size() == 0) {
      // we directly access the first element, so having at least should be nice
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    MutableProgress progCluster = LOG.isVerbose() ? new MutableProgress("GreedyKCenter number of clusters", k, LOG) : null;
    
    this.distq = distance.instantiate(relation);
    ArrayDBIDs aids = DBIDUtil.ensureArray(relation.getDBIDs());
    double[] distanceToHead = new double[aids.size()];
    int[] clusAssignments = new int[aids.size()];
    ArrayList<SimplePrototypeModel<O>> heads = new ArrayList<>(k);

    DBIDVar curHead = DBIDUtil.newVar();

    int headID = 0; // could be random as well
    curHead = aids.assignVar(headID, curHead);
    heads.add(0, new SimplePrototypeModel<O>(relation.get(curHead)));

    double maxd = 0;
    // init with all in cluster 0
    for(DBIDArrayIter it = aids.iter(); it.valid(); it.advance()) {
      // calc distance to Head
      final double d = distq.distance(it, curHead);
      distanceToHead[it.getOffset()] = d;

      // find furthest for next iteration
      if(maxd < d) {
        maxd = d;
        headID = it.getOffset();
      }
    }
    if(progCluster != null) {
      progCluster.setProcessed(1, LOG);
    }
    // calc other clusters
    for(int i = 1; i < k; i++) {
      maxd = 0;
      // assign new head
      curHead = aids.assignVar(headID, curHead);
      clusAssignments[headID] = i;
      heads.add(i, new SimplePrototypeModel<O>(relation.get(curHead)));

      for(DBIDArrayIter it = aids.iter(); it.valid(); it.advance()) {
        // calc distance to Head
        final int off = it.getOffset();
        final double d = distq.distance(it, curHead);
        // if close to new head, assign to new head
        if(d < distanceToHead[off]) {
          distanceToHead[off] = d;
          clusAssignments[off] = i;
        }
        // find furthest for next iteration
        if(maxd < d) {
          maxd = d;
          headID = off;
        }
      }
      if(progCluster != null) {
        progCluster.incrementProcessed(LOG);
      }
    }
    // final cluster assignment
    Clustering<SimplePrototypeModel<O>> result = new Clustering<SimplePrototypeModel<O>>();
    ArrayModifiableDBIDs[] clusters = new ArrayModifiableDBIDs[k];
    for(int i = 0; i < clusters.length; i++) {
      clusters[i] = DBIDUtil.newArray();
    }
    for(DBIDArrayIter it = aids.iter(); it.valid(); it.advance()) {
      clusters[clusAssignments[it.getOffset()]].add(it);
    }

    for(int i = 0; i < clusters.length; i++) {
      result.addToplevelCluster(new Cluster<SimplePrototypeModel<O>>(clusters[i], heads.get(i)));
    }
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return new TypeInformation[] { distance.getInputTypeRestriction() };
  }

  /**
   * Parameterization class
   * 
   * @author Robert Gehde
   *
   * @param <O> object type to cluster
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the number of clusters
     */
    public static final OptionID K_ID = new OptionID("greedyKCluster.k", "Number of centers to choose.");

    /**
     * Parameter to specify the distance function to use
     */
    public static final OptionID DISTANCE_ID = new OptionID("greedyKCluster.distance", "Distance function to use.");

    /**
     * Distance function to use
     */
    Distance<O> distance;

    /**
     * number of clusters
     */
    int k;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(K_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)//
          .grab(config, x -> this.k = x);
      new ObjectParameter<Distance<O>>(DISTANCE_ID, Distance.class).grab(config, x -> {
        this.distance = x;
        if(!distance.isMetric()) {
          LOG.warning("MVPTree requires a metric to be exact.");
        }
      });
    }

    @Override
    public GreedyKCenter<O> make() {
      return new GreedyKCenter<>(k, distance);
    }
  }
}
