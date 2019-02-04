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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Density-Based Clustering of Applications with Noise (DBSCAN), an algorithm to
 * find density-connected sets in a database.
 * <p>
 * Reference:
 * <p>
 * Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu<br>
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases
 * with Noise<br>
 * Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)
 * <p>
 * Further discussion:
 * <p>
 * Erich Schubert, Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu<br>
 * DBSCAN Revisited, Revisited: Why and How You Should (Still) Use DBSCAN<br>
 * ACM Trans. Database Systems (TODS)
 *
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.1
 * @param <O> the type of Object the algorithm is applied to
 */
@Title("DBSCAN: Density-Based Clustering of Applications with Noise")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minpts' and 'epsilon' (specifying a volume). " + "These two parameters determine a density threshold for clustering.")
@Reference(authors = "Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu", //
    title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", //
    booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)", //
    url = "http://www.aaai.org/Library/KDD/1996/kdd96-037.php", //
    bibkey = "DBLP:conf/kdd/EsterKSX96")
@Reference(authors = "Erich Schubert, Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu", //
    title = "DBSCAN Revisited, Revisited: Why and How You Should (Still) Use DBSCAN", //
    booktitle = "ACM Trans. Database Systems (TODS)", //
    url = "https://doi.org/10.1145/3068335", //
    bibkey = "DBLP:journals/tods/SchubertSEKX17")
@Priority(Priority.RECOMMENDED)
public class DBSCAN<O> extends AbstractDistanceBasedAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DBSCAN.class);

  /**
   * Holds the epsilon radius threshold.
   */
  protected double epsilon;

  /**
   * Holds the minimum cluster size.
   */
  protected int minpts;

  /**
   * Holds a list of clusters found.
   */
  protected List<ModifiableDBIDs> resultList;

  /**
   * Holds a set of noise.
   */
  protected ModifiableDBIDs noise;

  /**
   * Holds a set of processed ids.
   */
  protected ModifiableDBIDs processedIDs;

  /**
   * Number of neighbors.
   */
  protected long ncounter;

  /**
   * Constructor with parameters.
   *
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts parameter
   */
  public DBSCAN(DistanceFunction<? super O> distanceFunction, double epsilon, int minpts) {
    super(distanceFunction);
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Performs the DBSCAN algorithm on the given database.
   */
  public Clustering<Model> run(Relation<O> relation) {
    final int size = relation.size();
    if(size < minpts) {
      Clustering<Model> result = new Clustering<>("DBSCAN Clustering", "dbscan-clustering");
      result.addToplevelCluster(new Cluster<Model>(relation.getDBIDs(), true, ClusterModel.CLUSTER));
      return result;
    }

    RangeQuery<O> rangeQuery = QueryUtil.getRangeQuery(relation, getDistanceFunction());
    resultList = new ArrayList<>();
    noise = DBIDUtil.newHashSet();
    runDBSCAN(relation, rangeQuery);

    double averagen = ncounter / (double) relation.size();
    LOG.statistics(new DoubleStatistic(DBSCAN.class.getName() + ".average-neighbors", averagen));
    if(averagen < 1 + 0.1 * (minpts - 1)) {
      LOG.warning("There are very few neighbors found. Epsilon may be too small.");
    }
    if(averagen > 100 * minpts) {
      LOG.warning("There are very many neighbors found. Epsilon may be too large.");
    }

    Clustering<Model> result = new Clustering<>("DBSCAN Clustering", "dbscan-clustering");
    for(ModifiableDBIDs res : resultList) {
      result.addToplevelCluster(new Cluster<Model>(res, ClusterModel.CLUSTER));
    }
    result.addToplevelCluster(new Cluster<Model>(noise, true, ClusterModel.CLUSTER));
    return result;
  }

  /**
   * Run the DBSCAN algorithm
   *
   * @param relation Data relation
   * @param rangeQuery Range query class
   */
  protected void runDBSCAN(Relation<O> relation, RangeQuery<O> rangeQuery) {
    final int size = relation.size();
    FiniteProgress objprog = LOG.isVerbose() ? new FiniteProgress("Processing objects", size, LOG) : null;
    IndefiniteProgress clusprog = LOG.isVerbose() ? new IndefiniteProgress("Number of clusters", LOG) : null;

    processedIDs = DBIDUtil.newHashSet(size);
    ArrayModifiableDBIDs seeds = DBIDUtil.newArray();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      if(!processedIDs.contains(iditer)) {
        expandCluster(relation, rangeQuery, iditer, seeds, objprog, clusprog);
      }
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), LOG);
        clusprog.setProcessed(resultList.size(), LOG);
      }
      if(processedIDs.size() == size) {
        break;
      }
    }
    // Finish progress logging
    LOG.ensureCompleted(objprog);
    LOG.setCompleted(clusprog);
  }

  /**
   * DBSCAN-function expandCluster.
   *
   * Border-Objects become members of the first possible cluster.
   *
   * @param relation Database relation to run on
   * @param rangeQuery Range query to use
   * @param startObjectID potential seed of a new potential cluster
   * @param seeds Array to store the current seeds
   * @param objprog Number of objects processed (may be {@code null})
   * @param clusprog Number of clusters found (may be {@code null})
   */
  protected void expandCluster(Relation<O> relation, RangeQuery<O> rangeQuery, DBIDRef startObjectID, ArrayModifiableDBIDs seeds, FiniteProgress objprog, IndefiniteProgress clusprog) {
    DoubleDBIDList neighbors = rangeQuery.getRangeForDBID(startObjectID, epsilon);
    ncounter += neighbors.size();

    // startObject is no core-object
    if(neighbors.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(objprog != null) {
        objprog.incrementProcessed(LOG);
      }
      return;
    }

    ModifiableDBIDs currentCluster = DBIDUtil.newArray();
    currentCluster.add(startObjectID);
    processedIDs.add(startObjectID);

    // try to expand the cluster
    assert (seeds.size() == 0);
    seeds.clear();
    processNeighbors(neighbors.iter(), currentCluster, seeds);

    DBIDVar o = DBIDUtil.newVar();
    while(!seeds.isEmpty()) {
      neighbors = rangeQuery.getRangeForDBID(seeds.pop(o), epsilon);
      ncounter += neighbors.size();

      if(neighbors.size() >= minpts) {
        processNeighbors(neighbors.iter(), currentCluster, seeds);
      }

      if(objprog != null) {
        objprog.incrementProcessed(LOG);
      }
    }
    resultList.add(currentCluster);
    if(clusprog != null) {
      clusprog.setProcessed(resultList.size(), LOG);
    }
  }

  /**
   * Process a single core point.
   *
   * @param neighbor Iterator over neighbors
   * @param currentCluster Current cluster
   * @param seeds Seed set
   */
  private void processNeighbors(DoubleDBIDListIter neighbor, ModifiableDBIDs currentCluster, ArrayModifiableDBIDs seeds) {
    final boolean ismetric = getDistanceFunction().isMetric();
    for(; neighbor.valid(); neighbor.advance()) {
      if(processedIDs.add(neighbor)) {
        if(!ismetric || neighbor.doubleValue() > 0.) {
          seeds.add(neighbor);
        }
      }
      else if(!noise.remove(neighbor)) {
        continue;
      }
      currentCluster.add(neighbor);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered, must be suitable to the distance function specified.
     */
    public static final OptionID EPSILON_ID = new OptionID("dbscan.epsilon", "The maximum radius of the neighborhood to be considered.");

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-neighborhood of a point, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("dbscan.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point. The suggested value is '2 * dim - 1'.");

    /**
     * Holds the epsilon radius threshold.
     */
    protected double epsilon;

    /**
     * Holds the minimum cluster size.
     */
    protected int minpts;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
        if(minpts <= 2) {
          LOG.warning("DBSCAN with minPts <= 2 is equivalent to single-link clustering at a single height. Consider using larger values of minPts.");
        }
      }
    }

    @Override
    protected DBSCAN<O> makeInstance() {
      return new DBSCAN<>(distanceFunction, epsilon, minpts);
    }
  }
}
