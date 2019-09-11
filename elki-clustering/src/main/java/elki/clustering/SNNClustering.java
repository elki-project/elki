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
package elki.clustering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import elki.AbstractAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.Relation;
import elki.similarity.SharedNearestNeighborSimilarity;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.IndefiniteProgress;
import elki.result.Metadata;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Shared nearest neighbor clustering.
 * <p>
 * Reference:
 * <p>
 * L. Ertöz, M. Steinbach, V. Kumar<br>
 * Finding Clusters of Different Sizes, Shapes, and Densities in Noisy, High
 * Dimensional Data<br>
 * Proc. of SIAM Data Mining (SDM'03)
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @assoc - - - SharedNearestNeighborSimilarity
 *
 * @param <O> the type of Object the algorithm is applied on
 */
@Title("SNN: Shared Nearest Neighbor Clustering")
@Description("Algorithm to find shared-nearest-neighbors-density-connected sets in a database based on the " //
    + "parameters 'minPts' and 'epsilon' (specifying a volume). " //
    + "These two parameters determine a density threshold for clustering.")
@Reference(authors = "L. Ertöz, M. Steinbach, V. Kumar", //
    title = "Finding Clusters of Different Sizes, Shapes, and Densities in Noisy, High Dimensional Data", //
    booktitle = "Proc. of SIAM Data Mining (SDM'03)", //
    url = "https://doi.org/10.1137/1.9781611972733.5", //
    bibkey = "DBLP:conf/sdm/ErtozSK03")
public class SNNClustering<O> extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SNNClustering.class);

  /**
   * Epsilon radius threshold.
   */
  private int epsilon;

  /**
   * Minimum number of clusters for connectedness.
   */
  private int minpts;

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
   * The similarity function for the shared nearest neighbor similarity.
   */
  private SharedNearestNeighborSimilarity<O> similarityFunction;

  /**
   * Constructor.
   *
   * @param similarityFunction Similarity function
   * @param epsilon Epsilon
   * @param minpts Minpts
   */
  public SNNClustering(SharedNearestNeighborSimilarity<O> similarityFunction, int epsilon, int minpts) {
    super();
    this.similarityFunction = similarityFunction;
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Perform SNN clustering
   *
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public Clustering<Model> run(Database database, Relation<O> relation) {
    SimilarityQuery<O> snnInstance = similarityFunction.instantiate(relation);

    FiniteProgress objprog = LOG.isVerbose() ? new FiniteProgress("SNNClustering", relation.size(), LOG) : null;
    IndefiniteProgress clusprog = LOG.isVerbose() ? new IndefiniteProgress("Number of clusters", LOG) : null;
    resultList = new ArrayList<>();
    noise = DBIDUtil.newHashSet();
    processedIDs = DBIDUtil.newHashSet(relation.size());
    if(relation.size() >= minpts) {
      for(DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
        if(!processedIDs.contains(id)) {
          expandCluster(snnInstance, id, objprog, clusprog);
          if(processedIDs.size() == relation.size() && noise.size() == 0) {
            break;
          }
        }
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), LOG);
          clusprog.setProcessed(resultList.size(), LOG);
        }
      }
    }
    else {
      for(DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
        noise.add(id);
        if(objprog != null && clusprog != null) {
          objprog.setProcessed(noise.size(), LOG);
          clusprog.setProcessed(resultList.size(), LOG);
        }
      }
    }
    // Finish progress logging
    LOG.ensureCompleted(objprog);
    LOG.setCompleted(clusprog);

    Clustering<Model> result = new Clustering<>();
    Metadata.of(result).setLongName("Shared-Nearest-Neighbor Clustering");
    for(Iterator<ModifiableDBIDs> resultListIter = resultList.iterator(); resultListIter.hasNext();) {
      result.addToplevelCluster(new Cluster<Model>(resultListIter.next(), ClusterModel.CLUSTER));
    }
    result.addToplevelCluster(new Cluster<Model>(noise, true, ClusterModel.CLUSTER));

    return result;
  }

  /**
   * Returns the shared nearest neighbors of the specified query object in the
   * given database.
   *
   * @param snnInstance shared nearest neighbors
   * @param queryObject the query object
   * @return the shared nearest neighbors of the specified query object in the
   *         given database
   */
  protected ArrayModifiableDBIDs findSNNNeighbors(SimilarityQuery<O> snnInstance, DBIDRef queryObject) {
    ArrayModifiableDBIDs neighbors = DBIDUtil.newArray();
    for(DBIDIter iditer = snnInstance.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      if(snnInstance.similarity(queryObject, iditer) >= epsilon) {
        neighbors.add(iditer);
      }
    }
    return neighbors;
  }

  /**
   * DBSCAN-function expandCluster adapted to SNN criterion.
   * <p>
   * Border-Objects become members of the first possible cluster.
   *
   * @param snnInstance shared nearest neighbors
   * @param startObjectID potential seed of a new potential cluster
   * @param objprog the progress object to report about the progress of
   *        clustering
   */
  protected void expandCluster(SimilarityQuery<O> snnInstance, DBIDRef startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    ArrayModifiableDBIDs seeds = findSNNNeighbors(snnInstance, startObjectID);

    // startObject is no core-object
    if(seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), LOG);
        clusprog.setProcessed(resultList.size(), LOG);
      }
      return;
    }

    // try to expand the cluster
    ModifiableDBIDs currentCluster = DBIDUtil.newArray();
    for(DBIDIter seed = seeds.iter(); seed.valid(); seed.advance()) {
      if(!processedIDs.contains(seed)) {
        currentCluster.add(seed);
        processedIDs.add(seed);
      }
      else if(noise.contains(seed)) {
        currentCluster.add(seed);
        noise.remove(seed);
      }
    }

    DBIDVar o = DBIDUtil.newVar();
    while(seeds.size() > 0) {
      seeds.pop(o);
      ArrayModifiableDBIDs neighborhood = findSNNNeighbors(snnInstance, o);

      if(neighborhood.size() >= minpts) {
        for(DBIDIter iter = neighborhood.iter(); iter.valid(); iter.advance()) {
          boolean inNoise = noise.contains(iter);
          boolean unclassified = !processedIDs.contains(iter);
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(iter);
            }
            currentCluster.add(iter);
            processedIDs.add(iter);
            if(inNoise) {
              noise.remove(iter);
            }
          }
        }
      }

      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), LOG);
        int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
        clusprog.setProcessed(numClusters, LOG);
      }

      if(processedIDs.size() == snnInstance.getRelation().size() && noise.size() == 0) {
        break;
      }
    }
    if(currentCluster.size() >= minpts) {
      resultList.add(currentCluster);
    }
    else {
      noise.addDBIDs(currentCluster);
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(similarityFunction.getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the minimum SNN density, must be an integer greater
     * than 0.
     */
    public static final OptionID EPSILON_ID = new OptionID("snn.epsilon", "The minimum SNN density.");

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-SNN-neighborhood of a point, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("snn.minpts", "Threshold for minimum number of points in " + "the epsilon-SNN-neighborhood of a point.");

    protected int epsilon;

    protected int minpts;

    private SharedNearestNeighborSimilarity<O> similarityFunction;

    @Override
    public void configure(Parameterization config) {
      Class<SharedNearestNeighborSimilarity<O>> cls = ClassGenericsUtil.uglyCastIntoSubclass(SharedNearestNeighborSimilarity.class);
      similarityFunction = config.tryInstantiate(cls);

      new IntParameter(EPSILON_ID) //
          .grab(config, x -> epsilon = x);
      new IntParameter(MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minpts = x);
    }

    @Override
    public SNNClustering<O> make() {
      return new SNNClustering<>(similarityFunction, epsilon, minpts);
    }
  }
}
