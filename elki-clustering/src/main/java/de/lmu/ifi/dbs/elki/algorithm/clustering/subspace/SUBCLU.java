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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.*;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionSelectingSubspaceDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Implementation of the SUBCLU algorithm, an algorithm to detect arbitrarily
 * shaped and positioned clusters in subspaces. SUBCLU delivers for each
 * subspace the same clusters DBSCAN would have found, when applied to this
 * subspace separately.
 * <p>
 * Note: this implementation does <em>not</em> yet implemented the suggested
 * indexing based on inverted files, and that we also only query subsets of
 * them, this makes the implementation as a reusable index challenging.
 * <p>
 * The original paper is not very clear on which clusters to return, as any
 * subspace cluster must be part of a lower-dimensional projected cluster,
 * so these results would be highly redundant. In this implementation, we
 * only include points in clusters that are not already part of sub-clusters
 * (note that this does not remove overlap of independent subspaces).
 * <p>
 * Reference:
 * <p>
 * Karin Kailing, Hans-Peter Kriegel, Peer Kröger<br>
 * Density Connected Subspace Clustering for High Dimensional Data<br>
 * Proc. SIAM Int. Conf. on Data Mining (SDM'04)
 * 
 * @author Elke Achtert
 * @since 0.3
 * 
 * @assoc - - - DBSCAN
 * @assoc - - - DimensionSelectingSubspaceDistanceFunction
 * @has - - - SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this algorithm
 */
@Title("SUBCLU: Density connected Subspace Clustering")
@Description("Algorithm to detect arbitrarily shaped and positioned clusters in subspaces. "//
    + "SUBCLU delivers for each subspace the same clusters DBSCAN would have found, "//
    + "when applied to this subspace seperately.")
@Reference(authors = "Karin Kailing, Hans-Peter Kriegel, Peer Kröger", //
    title = "Density Connected Subspace Clustering for High Dimensional Data", //
    booktitle = "Proc. SIAM Int. Conf. on Data Mining (SDM'04)", //
    url = "https://doi.org/10.1137/1.9781611972740.23", //
    bibkey = "DBLP:conf/sdm/KroegerKK04")
public class SUBCLU<V extends NumberVector> extends AbstractAlgorithm<Clustering<SubspaceModel>> implements SubspaceClusteringAlgorithm<SubspaceModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SUBCLU.class);

  /**
   * The distance function to determine the distance between objects.
   */
  protected DimensionSelectingSubspaceDistanceFunction<V> distanceFunction;

  /**
   * Maximum radius of the neighborhood to be considered.
   */
  protected double epsilon;

  /**
   * Minimum number of points.
   */
  protected int minpts;

  /**
   * Minimum dimensionality.
   */
  protected int mindim;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   * @param mindim Minimum dimensionality
   */
  public SUBCLU(DimensionSelectingSubspaceDistanceFunction<V> distanceFunction, double epsilon, int minpts, int mindim) {
    super();
    this.distanceFunction = distanceFunction;
    this.epsilon = epsilon;
    this.minpts = minpts;
    this.mindim = mindim;
  }

  /**
   * Performs the SUBCLU algorithm on the given database.
   * 
   * @param relation Relation to process
   * @return Clustering result
   */
  public Clustering<SubspaceModel> run(Relation<V> relation) {
    final int dimensionality = RelationUtil.dimensionality(relation);
    if(dimensionality <= 1) {
      throw new IllegalStateException("SUBCLU needs multivariate data.");
    }

    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(dimensionality) : null;

    // mapping of subspaces to list of clusters
    TreeMap<Subspace, List<Cluster<Model>>> clusterMap = new TreeMap<>(Subspace.DIMENSION_COMPARATOR);

    // Generate all 1-dimensional clusters
    LOG.beginStep(stepprog, 1, "Generate all 1-dimensional clusters.");
    List<Subspace> subspaces = new ArrayList<>();
    for(int d = 0; d < dimensionality; d++) {
      Subspace currentSubspace = new Subspace(d);
      List<Cluster<Model>> clusters = runDBSCAN(relation, null, currentSubspace);

      if(LOG.isDebuggingFiner()) {
        StringBuilder msg = new StringBuilder(1000) //
            .append(clusters.size()).append(" clusters in subspace ")//
            .append(currentSubspace.dimensionsToString()).append(':');
        for(Cluster<Model> cluster : clusters) {
          msg.append("\n      ").append(cluster.getIDs());
        }
        LOG.debugFiner(msg.toString());
      }

      if(!clusters.isEmpty()) {
        subspaces.add(currentSubspace);
        clusterMap.put(currentSubspace, clusters);
      }
    }

    // Generate (d+1)-dimensional clusters from d-dimensional clusters
    for(int d = 2; d <= dimensionality; d++) {
      if(stepprog != null) {
        stepprog.beginStep(d, "Generate " + d + "-dimensional clusters from " + (d - 1) + "-dimensional clusters.", LOG);
      }

      final List<Subspace> candidates = generateSubspaceCandidates(subspaces);
      List<Subspace> s_d = new ArrayList<>();
      FiniteProgress substepprog = LOG.isVerbose() ? new FiniteProgress("Candidates of dimensionality " + d, candidates.size(), LOG) : null;
      for(Subspace candidate : candidates) {
        Subspace bestSubspace = bestSubspace(subspaces, candidate, clusterMap);
        if(LOG.isDebuggingFine()) {
          LOG.debugFine("best subspace of " + candidate.dimensionsToString() + ": " + bestSubspace.dimensionsToString());
        }

        List<Cluster<Model>> clusters = new ArrayList<>();
        Iterator<Cluster<Model>> iter = clusterMap.get(bestSubspace).iterator();
        while(iter.hasNext()) {
          Cluster<Model> cluster = iter.next();
          if(cluster.size() < minpts) {
            continue;
          }
          List<Cluster<Model>> candidateClusters = runDBSCAN(relation, cluster.getIDs(), candidate);
          if(!candidateClusters.isEmpty()) {
            clusters.addAll(candidateClusters);
          }
        }

        if(LOG.isDebuggingFine() && !clusters.isEmpty()) {
          StringBuilder msg = new StringBuilder(1000).append(clusters.size()) //
              .append(" cluster(s) in subspace ").append(candidate).append(':');
          for(Cluster<Model> c : clusters) {
            msg.append("\n      ").append(c.getIDs());
          }
          LOG.debugFine(msg.toString());
        }

        if(!clusters.isEmpty()) {
          s_d.add(candidate);
          clusterMap.put(candidate, clusters);
        }
        LOG.incrementProcessed(substepprog);
      }
      LOG.ensureCompleted(substepprog);

      if(s_d.isEmpty()) {
        if(stepprog != null) {
          for(int dim = d + 1; dim <= dimensionality; dim++) {
            stepprog.beginStep(dim, "Generation of " + dim + "-dimensional clusters not applicable, because no " + d + "-dimensional subspaces were found.", LOG);
          }
        }
        break;
      }
      subspaces = s_d;
    }
    LOG.setCompleted(stepprog);

    // build result
    int numClusters = 0;
    ModifiableDBIDs noise = DBIDUtil.newHashSet(relation.getDBIDs());
    TreeMap<Subspace, ModifiableDBIDs> filtered = new TreeMap<>(Subspace.DIMENSION_COMPARATOR);
    Clustering<SubspaceModel> result = new Clustering<>("SUBCLU clustering", "subclu-clustering");
    for(Subspace subspace : clusterMap.descendingKeySet()) {
      if(subspace.dimensionality() < mindim) {
        continue;
      }
      // Objects already in subclusters:
      DBIDs blacklisted = filtered.get(subspace);
      blacklisted = blacklisted != null ? blacklisted : DBIDUtil.EMPTYDBIDS;
      ModifiableDBIDs blacklist = DBIDUtil.newHashSet(blacklisted);

      List<Cluster<Model>> clusters = clusterMap.get(subspace);
      for(Cluster<Model> cluster : clusters) {
        final DBIDs ids = cluster.getIDs();
        final ModifiableDBIDs newids = DBIDUtil.difference(ids, blacklisted);
        Cluster<SubspaceModel> newCluster = new Cluster<>(newids);
        newCluster.setModel(new SubspaceModel(subspace, Centroid.make(relation, ids).getArrayRef()));
        newCluster.setName("cluster_" + numClusters++);
        result.addToplevelCluster(newCluster);
        blacklist.addDBIDs(newids);
        noise.removeDBIDs(newids);
      }
      if(subspace.dimensionality() > mindim) {
        // Blacklist
        long[] tmp = BitsUtil.copy(subspace.getDimensions());
        for(int pos = BitsUtil.nextSetBit(tmp, 0); pos >= 0; pos = BitsUtil.nextSetBit(tmp, pos + 1)) {
          BitsUtil.clearI(tmp, pos);
          Subspace sub = new Subspace(BitsUtil.copy(tmp));
          BitsUtil.setI(tmp, pos);
          ModifiableDBIDs bl = filtered.get(sub);
          if(bl != null) {
            bl.addDBIDs(blacklist);
          }
          else {
            filtered.put(sub, DBIDUtil.newHashSet(blacklist));
          }
        }
      }
    }
    // Make a noise cluster
    if(!noise.isEmpty()) {
      Cluster<SubspaceModel> newCluster = new Cluster<>(noise);
      newCluster.setModel(new SubspaceModel(new Subspace(BitsUtil.zero(dimensionality)), Centroid.make(relation, noise).getArrayRef()));
      newCluster.setName("noise");
      newCluster.setNoise(true);
      result.addToplevelCluster(newCluster);
    }
    return result;
  }

  /**
   * Runs the DBSCAN algorithm on the specified partition of the database in the
   * given subspace. If parameter {@code ids} is null DBSCAN will be applied to
   * the whole database.
   * 
   * @param relation the database holding the objects to run DBSCAN on
   * @param ids the IDs of the database defining the partition to run DBSCAN on
   *        - if this parameter is null DBSCAN will be applied to the whole
   *        database
   * @param subspace the subspace to run DBSCAN on
   * @return the clustering result of the DBSCAN run
   */
  private List<Cluster<Model>> runDBSCAN(Relation<V> relation, DBIDs ids, Subspace subspace) {
    // distance function
    distanceFunction.setSelectedDimensions(subspace.getDimensions());
    // run DBSCAN
    if(LOG.isVerbose()) {
      LOG.verbose("Run DBSCAN on subspace " + subspace.dimensionsToString() + //
          (ids == null ? "" : " on cluster of " + ids.size() + " objects."));
    }
    // subset filter:
    relation = ids == null ? relation : new ProxyView<>(ids, relation);

    DBSCAN<V> dbscan = new DBSCAN<>(distanceFunction, epsilon, minpts);
    Clustering<Model> dbsres = dbscan.run(relation);

    // separate cluster and noise
    List<Cluster<Model>> clusters = new ArrayList<>();
    for(Cluster<Model> c : dbsres.getAllClusters()) {
      if(!c.isNoise()) {
        clusters.add(c);
      }
    }
    return clusters;
  }

  /**
   * Generates {@code d+1}-dimensional subspace candidates from the specified
   * {@code d}-dimensional subspaces.
   * 
   * @param subspaces the {@code d}-dimensional subspaces
   * @return the {@code d+1}-dimensional subspace candidates
   */
  private List<Subspace> generateSubspaceCandidates(List<Subspace> subspaces) {
    if(subspaces.isEmpty()) {
      return Collections.emptyList();
    }
    StringBuilder msgFinest = LOG.isDebuggingFinest() ? new StringBuilder(1000) : null;
    if(msgFinest != null) {
      msgFinest.append("subspaces ").append(subspaces).append('\n');
    }

    List<Subspace> candidates = new ArrayList<>();
    // Generate d-dimensional candidate subspaces
    final int d = subspaces.get(0).dimensionality() + 1;

    for(int i = 0; i < subspaces.size(); i++) {
      Subspace s1 = subspaces.get(i);
      for(int j = i + 1; j < subspaces.size(); j++) {
        Subspace candidate = s1.join(subspaces.get(j));

        if(candidate == null) {
          continue; // Filtered by prefix rule
        }
        // prune irrelevant candidate subspaces
        if(d == 2 || checkLower(candidate, subspaces)) {
          if(msgFinest != null) {
            msgFinest.append("candidate: ").append(candidate.dimensionsToString()).append('\n');
          }
          candidates.add(candidate);
        }
      }
    }

    if(msgFinest != null) {
      LOG.debugFinest(msgFinest.toString());
    }
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder(1000).append(d).append("-dimensional candidate subspaces: ");
      for(Subspace candidate : candidates) {
        msg.append(candidate.dimensionsToString()).append(' ');
      }
      LOG.debug(msg.toString());
    }

    return candidates;
  }

  /**
   * Perform Apriori-style pruning.
   *
   * @param candidate Current candidate
   * @param subspaces Subspaces
   * @return {@code true} if all lower-dimensional subspaces exist
   */
  private boolean checkLower(Subspace candidate, List<Subspace> subspaces) {
    long[] dimensions = BitsUtil.copy(candidate.getDimensions());
    // TODO: we could skip the generating two immediately.
    for(int dim = BitsUtil.nextSetBit(dimensions, 0); dim >= 0; dim = BitsUtil.nextSetBit(dimensions, dim + 1)) {
      BitsUtil.clearI(dimensions, dim);
      if(!subspaces.contains(new Subspace(dimensions))) {
        return false;
      }
      BitsUtil.setI(dimensions, dim);
    }
    return true;
  }

  /**
   * Determines the {@code d}-dimensional subspace of the {@code (d+1)}
   * -dimensional candidate with minimal number of objects in the cluster.
   * 
   * @param subspaces the list of {@code d}-dimensional subspaces containing
   *        clusters
   * @param candidate the {@code (d+1)}-dimensional candidate subspace
   * @param clusterMap the mapping of subspaces to clusters
   * @return the {@code d}-dimensional subspace of the {@code (d+1)}
   *         -dimensional candidate with minimal number of objects in the
   *         cluster
   */
  private Subspace bestSubspace(List<Subspace> subspaces, Subspace candidate, TreeMap<Subspace, List<Cluster<Model>>> clusterMap) {
    Subspace bestSubspace = null;
    int min = Integer.MAX_VALUE;

    for(Subspace subspace : subspaces) {
      if(subspace.isSubspace(candidate)) {
        int sum = 0;
        for(Cluster<Model> cluster : clusterMap.get(subspace)) {
          sum += cluster.size();
        }
        if(sum < min) {
          min = sum;
          bestSubspace = subspace;
        }
      }
    }

    return bestSubspace;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * The distance function to determine the distance between objects.
     */
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("subclu.distancefunction", "Distance function to determine the distance between database objects.");

    /**
     * Maximum radius of the neighborhood to be considered.
     */
    public static final OptionID EPSILON_ID = new OptionID("subclu.epsilon", "The maximum radius of the neighborhood to be considered.");

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-neighborhood of a point, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("subclu.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

    /**
     * Minimum dimensionality to generate clusters.
     */
    public static final OptionID MINDIM_ID = new OptionID("subclu.mindim", "Minimum dimensionality to generate clusters for.");

    /**
     * The distance function to determine the distance between objects.
     */
    protected DimensionSelectingSubspaceDistanceFunction<V> distance;

    /**
     * Maximum radius of the neighborhood to be considered.
     */
    protected double epsilon;

    /**
     * Minimum number of points.
     */
    protected int minpts;

    /**
     * Minimum dimensionality.
     */
    protected int mindim = 1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DimensionSelectingSubspaceDistanceFunction<V>> param = new ObjectParameter<>(DISTANCE_FUNCTION_ID, DimensionSelectingSubspaceDistanceFunction.class, SubspaceEuclideanDistanceFunction.class);
      if(config.grab(param)) {
        distance = param.instantiateClass(config);
      }

      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }

      IntParameter mindimP = new IntParameter(MINDIM_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .setOptional(true);
      if(config.grab(mindimP)) {
        mindim = mindimP.getValue();
      }
    }

    @Override
    protected SUBCLU<V> makeInstance() {
      return new SUBCLU<>(distance, epsilon, minpts, mindim);
    }
  }
}
