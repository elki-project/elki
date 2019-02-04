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
package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.*;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Algorithm to compute dynamic-window outlier factors in a database based on a
 * specified parameter k, which specifies the number of the neighbors to be
 * considered during the calculation of the DWOF score.
 * <p>
 * Reference:
 * <p>
 * Rana Momtaz, Nesma Mohssen and Mohammad A. Gowayyed:<br>
 * DWOF: A Robust Density-Based OutlierDetection Approach.<br>
 * Proc. 6th Iberian Conf. Pattern Recognition and Image Analysis (IbPRIA 2013)
 *
 * @author Omar Yousry
 * @since 0.6.0
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("DWOF: Dynamic Window Outlier Factor")
@Description("Algorithm to compute dynamic-window outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "R. Momtaz, N. Mohssen, M. A. Gowayyed", //
    title = "DWOF: A Robust Density-Based Outlier Detection Approach", //
    booktitle = "Proc. 6th Iberian Conf. Pattern Recognition and Image Analysis (IbPRIA 2013)", //
    url = "https://doi.org/10.1007/978-3-642-38628-2_61", //
    bibkey = "DBLP:conf/ibpria/MomtazMG13")
public class DWOF<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DWOF.class);

  /**
   * Holds the value of {@link Parameterizer#K_ID} i.e. Number of neighbors to
   * consider during the calculation of DWOF scores.
   */
  protected int k;

  /**
   * The radii changing ratio
   */
  private double delta = 1.1;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use in queries
   * @param k the value of k
   * @param delta Radius increase factor
   */
  public DWOF(DistanceFunction<? super O> distanceFunction, int k, double delta) {
    super(distanceFunction);
    this.k = k + 1; // + query point
    this.delta = delta;
  }

  /**
   * Performs the Generalized DWOF_SCORE algorithm on the given database by
   * calling all the other methods in the proper order.
   *
   * @param database Database to query
   * @param relation Data to process
   * @return new OutlierResult instance
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    final DBIDs ids = relation.getDBIDs();
    DistanceQuery<O> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    // Get k nearest neighbor and range query on the relation.
    KNNQuery<O> knnq = database.getKNNQuery(distFunc, k, DatabaseQuery.HINT_HEAVY_USE);
    RangeQuery<O> rnnQuery = database.getRangeQuery(distFunc, DatabaseQuery.HINT_HEAVY_USE);

    StepProgress stepProg = LOG.isVerbose() ? new StepProgress("DWOF", 2) : null;
    // DWOF output score storage.
    WritableDoubleDataStore dwofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_HOT, 0.);
    if(stepProg != null) {
      stepProg.beginStep(1, "Initializing objects' Radii", LOG);
    }
    WritableDoubleDataStore radii = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
    // Find an initial radius for each object:
    initializeRadii(ids, knnq, distFunc, radii);
    WritableIntegerDataStore oldSizes = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT, 1);
    WritableIntegerDataStore newSizes = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT, 1);
    int countUnmerged = relation.size();
    if(stepProg != null) {
      stepProg.beginStep(2, "Clustering-Evaluating Cycles.", LOG);
    }
    IndefiniteProgress clusEvalProgress = LOG.isVerbose() ? new IndefiniteProgress("Evaluating DWOFs", LOG) : null;
    while(countUnmerged > 0) {
      LOG.incrementProcessed(clusEvalProgress);
      // Increase radii
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        radii.putDouble(iter, radii.doubleValue(iter) * delta);
      }
      // stores the clustering label for each object
      WritableDataStore<ModifiableDBIDs> labels = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP, ModifiableDBIDs.class);
      // Cluster objects based on the current radius
      clusterData(ids, rnnQuery, radii, labels);
      // simple reference swap
      WritableIntegerDataStore temp = newSizes;
      newSizes = oldSizes;
      oldSizes = temp;

      // Update the cluster size count for each object.
      countUnmerged = updateSizes(ids, labels, newSizes);
      labels.destroy();
      // Update DWOF scores.
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double newScore = (newSizes.intValue(iter) > 0) ? ((double) (oldSizes.intValue(iter) - 1) / (double) newSizes.intValue(iter)) : 0.0;
        dwofs.putDouble(iter, dwofs.doubleValue(iter) + newScore);
      }
    }
    LOG.setCompleted(clusEvalProgress);
    LOG.setCompleted(stepProg);
    // Build result representation.
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      minmax.put(dwofs.doubleValue(iter));
    }
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    DoubleRelation rel = new MaterializedDoubleRelation("Dynamic-Window Outlier Factors", "dwof-outlier", dwofs, ids);
    return new OutlierResult(meta, rel);
  }

  /**
   * This method prepares a container for the radii of the objects and
   * initializes radii according to the equation:
   *
   * initialRadii of a certain object = (absoluteMinDist of all objects) *
   * (avgDist of the object) / (minAvgDist of all objects)
   *
   * @param ids Database IDs to process
   * @param distFunc Distance function
   * @param knnq kNN search function
   * @param radii WritableDoubleDataStore to store radii
   */
  private void initializeRadii(DBIDs ids, KNNQuery<O> knnq, DistanceQuery<O> distFunc, WritableDoubleDataStore radii) {
    FiniteProgress avgDistProgress = LOG.isVerbose() ? new FiniteProgress("Calculating average kNN distances-", ids.size(), LOG) : null;
    double absoluteMinDist = Double.POSITIVE_INFINITY;
    double minAvgDist = Double.POSITIVE_INFINITY;
    // to get the mean for each object
    Mean mean = new Mean();
    // Iterate over all objects
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      KNNList iterNeighbors = knnq.getKNNForDBID(iter, k);
      // skip the point itself
      mean.reset();
      for(DBIDIter neighbor1 = iterNeighbors.iter(); neighbor1.valid(); neighbor1.advance()) {
        if(DBIDUtil.equal(neighbor1, iter)) {
          continue;
        }
        for(DBIDIter neighbor2 = iterNeighbors.iter(); neighbor2.valid(); neighbor2.advance()) {
          if(DBIDUtil.equal(neighbor1, neighbor2) || DBIDUtil.equal(neighbor2, iter)) {
            continue;
          }
          double distance = distFunc.distance(neighbor1, neighbor2);
          mean.put(distance);
          if(distance > 0. && distance < absoluteMinDist) {
            absoluteMinDist = distance;
          }
        }
      }
      double currentMean = mean.getMean();
      radii.putDouble(iter, currentMean);
      if(currentMean < minAvgDist) {
        minAvgDist = currentMean;
      }
      LOG.incrementProcessed(avgDistProgress);
    }
    LOG.ensureCompleted(avgDistProgress);

    // Initializing the radii of all objects.
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      radii.putDouble(iter, (minAvgDist > 0) ? (absoluteMinDist * radii.doubleValue(iter) / minAvgDist) : Double.POSITIVE_INFINITY);
    }
  }

  /**
   * This method applies a density based clustering algorithm.
   *
   * It looks for an unclustered object and builds a new cluster for it, then
   * adds all the points within its radius to that cluster.
   *
   * nChain represents the points to be added to the cluster and its
   * radius-neighbors
   *
   * @param ids Database IDs to process
   * @param rnnQuery Data to process
   * @param radii Radii to cluster accordingly
   * @param labels Label storage.
   */
  private void clusterData(DBIDs ids, RangeQuery<O> rnnQuery, WritableDoubleDataStore radii, WritableDataStore<ModifiableDBIDs> labels) {
    FiniteProgress clustProg = LOG.isVerbose() ? new FiniteProgress("Density-Based Clustering", ids.size(), LOG) : null;
    // Iterate over all objects
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if(labels.get(iter) != null) {
        continue;
      }
      ModifiableDBIDs newCluster = DBIDUtil.newArray();
      newCluster.add(iter);
      labels.put(iter, newCluster);
      LOG.incrementProcessed(clustProg);
      // container of the points to be added and their radii neighbors to the
      // cluster
      ModifiableDBIDs nChain = DBIDUtil.newArray();
      nChain.add(iter);
      // iterate over nChain
      for(DBIDIter toGetNeighbors = nChain.iter(); toGetNeighbors.valid(); toGetNeighbors.advance()) {
        double range = radii.doubleValue(toGetNeighbors);
        DoubleDBIDList nNeighbors = rnnQuery.getRangeForDBID(toGetNeighbors, range);
        for(DoubleDBIDListIter iter2 = nNeighbors.iter(); iter2.valid(); iter2.advance()) {
          if(DBIDUtil.equal(toGetNeighbors, iter2)) {
            continue;
          }
          if(labels.get(iter2) == null) {
            newCluster.add(iter2);
            labels.put(iter2, newCluster);
            nChain.add(iter2);
            LOG.incrementProcessed(clustProg);
          }
          else if(labels.get(iter2) != newCluster) {
            ModifiableDBIDs toBeDeleted = labels.get(iter2);
            newCluster.addDBIDs(toBeDeleted);
            for(DBIDIter iter3 = toBeDeleted.iter(); iter3.valid(); iter3.advance()) {
              labels.put(iter3, newCluster);
            }
            toBeDeleted.clear();
          }
        }
      }
    }
    LOG.ensureCompleted(clustProg);
  }

  /**
   * This method updates each object's cluster size after the clustering step.
   *
   * @param ids Object IDs to process
   * @param labels references for each object's cluster
   * @param newSizes the sizes container to be updated
   * @return the number of unclustered objects
   */
  private int updateSizes(DBIDs ids, WritableDataStore<ModifiableDBIDs> labels, WritableIntegerDataStore newSizes) {
    // to count the unclustered all over
    int countUnmerged = 0;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      // checking the point's new cluster size after the clustering step
      int newClusterSize = labels.get(iter).size();
      newSizes.putInt(iter, newClusterSize);
      // the point is alone in the cluster --> not merged with other points
      if(newClusterSize == 1) {
        countUnmerged++;
      }
    }
    return countUnmerged;
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
   * @author Omar Yousry
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Option ID for the number of neighbors.
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("dwof.k", "Number of neighbors to get for DWOF score outlier detection.");

    /**
     * Option ID for radius increases
     */
    public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("dwof.delta", "Radius increase factor.");

    /**
     * Number of neighbors to get
     */
    protected int k;

    /**
     * Radius increase factor.
     */
    protected double delta = 1.1;

    @Override
    protected void makeOptions(Parameterization config) {
      // The super class has the distance function parameter!
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
      DoubleParameter deltaP = new DoubleParameter(DELTA_ID) //
          .setDefaultValue(1.1) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_DOUBLE);
      if(config.grab(deltaP)) {
        delta = deltaP.getValue();
      }
    }

    @Override
    protected DWOF<O> makeInstance() {
      return new DWOF<>(distanceFunction, k, delta);
    }
  }
}
