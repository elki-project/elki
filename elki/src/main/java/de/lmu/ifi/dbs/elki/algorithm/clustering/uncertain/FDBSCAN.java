package de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.UOModel;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UniformDiscreteUO;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
 * Density-based Clustering of Applications with Noise and Fuzzy objects (FDBSCAN), an
 * Algorithm to find sets in a fuzzy database that are density-connected with minimum
 * probability
 *
 * <p>
 * Reference:<br>
 * H.-P. Kriegel and M. Pfeifle:<br />
 * Density-based clustering of uncertain data<br />
 * In Proc. 11th ACM Int. Conf. on Knowledge Discovery and Data Mining (SIGKDD),
 * Chicago, IL, 2005.
 * </p>
 *
 * @author Alexander Koos
 * @param <O>
 */
public class FDBSCAN<O extends UncertainObject<UOModel>> extends DBSCAN<O> {
  /**
   * The logger for this class.
   */
  private final static Logging LOG = Logging.getLogger(FDBSCAN.class);

  /**
   * The minimum probability that is needed for two fuzzy objects to be considered connected.
   */
  protected double threshold;

  /**
   * The amount of samples that are drawn from a fuzzy object and used to determine if they're density-connected or not.
   */
  protected int sampleSize;

  /**
   * The amount of density-connected samples that suffice for two fuzzy objects to be density-connected, stored to minimize necessary computation.
   */
  protected int constraint;

  /**
   *
   * Constructor.
   *
   * @param distanceFunction To determine a distance measure for pairs of objects.
   * @param epsilon Analogous to the epsilon parameter in {@link DBSCAN}.
   * @param threshold The probability necessary for two objects to be density-connected - the Default is 50%.
   * @param minpts Analogous to the minpoints parameter in {@link DBSCAN}.
   * @param sampleSize The amount of samples that have to be drawn per fuzzy object.
   */
  protected FDBSCAN(final DistanceFunction<? super O> distanceFunction, final double epsilon, final double threshold, final int minpts, final int sampleSize) {
    super(distanceFunction, epsilon, minpts);
    this.threshold = threshold;
    this.sampleSize = sampleSize;
    // to reduce computational overhead, pre-compute the absolute necessary amount of found connections
    //
    // TODO: Maybe instead of getting the default floored value we should use Math.ceil
    // to prevent us from using slightly lower boundaries for our probabilistic epsilon
    // neighborhood?
    // E.g. if we have a threshold of 0.5 and a sampleSize of 5, it would be sufficient to have 12 matches.
    // 12 / 25 = 0.48, which is lower than the given 0.5.
    this.constraint = (int) (sampleSize * sampleSize * threshold);
  }

  /**
   *
   * Check for each fuzzy object if it is a core-object. If yes, create a Cluster, add the object, and check if
   * the surrounding fuzzy objects core-objects as well.
   * Add all objects, that are core-objects or within the epsilon-environment of a core-object to the corresponding
   * Cluster.
   * Add all objects that aren't core-objects or within the epsilon-environment of a core-object to the Noise.
   *
   * @param relation
   * @param rangeQuery
   * @param startObjectID
   * @param objprog
   * @param clusprog
   */
  protected void expandCluster(final Relation<O> relation, final RangeQuery<O> rangeQuery, final DBIDRef startObjectID, final FiniteProgress objprog, final IndefiniteProgress clusprog) {
    final DBIDs epsilonNeighbours = this.getEpsilonNeighbours(startObjectID, relation, this.epsilon);


    // startObject is no core-object
    if(epsilonNeighbours.size() < this.minpts) {
      this.noise.add(startObjectID);
      this.processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(this.processedIDs.size(), FDBSCAN.LOG);
        clusprog.setProcessed(this.resultList.size(), FDBSCAN.LOG);
      }
      return;
    }

    // try to expand the cluster
    final ModifiableDBIDs seeds = DBIDUtil.newHashSet();
    final ModifiableDBIDs currentCluster = DBIDUtil.newArray();
    for(final DBIDIter seed = epsilonNeighbours.iter(); seed.valid(); seed.advance()) {
      if(!this.processedIDs.contains(seed)) {
        currentCluster.add(seed);
        this.processedIDs.add(seed);
        seeds.add(seed);
      }
      else if(this.noise.contains(seed)) {
        currentCluster.add(seed);
        this.noise.remove(seed);
      }
    }
    seeds.remove(startObjectID);

    // Check for each object in the last looked at objects epsilon-environment if it
    // is a core-object itself and if yes add its epsilon-neighbors to the cluster and the seedlist.
    while(!seeds.isEmpty()) {
      final DBIDMIter o = seeds.iter();
      final DBIDs neighborhood = this.getEpsilonNeighbours(o, relation, this.epsilon);
      o.remove();

      if(neighborhood.size() >= this.minpts) {
        for(final DBIDIter neighbor = neighborhood.iter(); neighbor.valid(); neighbor.advance()) {
          final boolean inNoise = this.noise.contains(neighbor);
          final boolean unclassified = !this.processedIDs.contains(neighbor);
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(neighbor);
            }
            currentCluster.add(neighbor);
            this.processedIDs.add(neighbor);
            if(inNoise) {
              this.noise.remove(neighbor);
            }
          }
        }
      }

      if(this.processedIDs.size() == relation.size() && this.noise.size() == 0) {
        break;
      }

      if(objprog != null && clusprog != null) {
        objprog.setProcessed(this.processedIDs.size(), FDBSCAN.LOG);
        final int numClusters = currentCluster.size() > this.minpts ? this.resultList.size() + 1 : this.resultList.size();
        clusprog.setProcessed(numClusters, FDBSCAN.LOG);
      }
    }
    // when the cluster has at least minpts elements it is a cluster,...
    if(currentCluster.size() >= this.minpts) {
      this.resultList.add(currentCluster);
    }
    // ... otherwise it would be a microcluster and therefore is considered to be noise instead
    else {
      this.noise.addDBIDs(currentCluster);
      this.noise.add(startObjectID);
      this.processedIDs.add(startObjectID);
    }
  }

  /**
   * Determine which objects are in the epsilon-environment of the
   * object identified by startObjectID.
   *
   * @param startObjectID The <code>DBID</code> of the object that is evaluated
   * @param relation The <code>Relation</code> containing the dataset
   * @param epsilon The distance a potential neighbor may have at most
   * @return The DBIDs of all epsilon neighbors of the evaluated object
   */
  @SuppressWarnings("unchecked")
  private DBIDs getEpsilonNeighbours(final DBIDRef startObjectID, final Relation<O> relation, final double epsilon) {
    final ModifiableDBIDs epsilonNeighbours = DBIDUtil.newArray();
    final UncertainObject<UOModel> uo = relation.get(startObjectID);

    // look at each object once
    for(final DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      // ignore the object itself
      if(iter.internalGetIndex() == startObjectID.internalGetIndex()) {
        continue;
      }
      final UncertainObject<UOModel> possibleNeighbour = relation.get(iter);

      boolean inside = true, possible = true;
      for(int i = 0; i < uo.getDimensionality(); i++) {
        final double minBound = uo.getMin(i) - epsilon;
        final double maxBound = uo.getMax(i) + epsilon;
        // If those checks evaluate to false, the current object is completely included in the maximum
        // epsilon environment of the evaluated startObject.
        // Otherwise it has to be checked further.
        if(possibleNeighbour.getMin(i) < minBound || possibleNeighbour.getMax(i) > maxBound) {
          inside = false;
          // if those checks evaluate to true, the current object is completely outside
          // of the maximum epsilon environment of the evaluated startObject.
          // In this case further checks are obsolete.
          if(possibleNeighbour.getMin(i) > maxBound || possibleNeighbour.getMax(i) < minBound) {
            possible = false;
            break;
          }
        }
      }
      if(inside) {
        // Completely included as discussed above, therefore it is an epsilon neighbor and
        // we can proceed.
        epsilonNeighbours.add(iter);
        continue;
      } else if (!possible) {
        // Completely excluded as discussed above, therefore it can't be an epsilon neighbor
        // and we can proceed.
        continue;
      }

      if(uo.getModel() instanceof UniformDiscreteUO) {
        int matches = 0;
        // Look at each pairing, respectively the cartesian product of both samplesets.
        // Hence we iterate over both lists.
        // To prevent us from holding the lists of samples in the memory, we retrieve them from
        // the uncertain objects directly.
        for(final DoubleVector uoSample : ((UniformDiscreteUO) uo.getModel()).getObservationsCopy()) {
          for(final DoubleVector possibleNeighbourSample : ((UniformDiscreteUO) possibleNeighbour.getModel()).getObservationsCopy()) {
            if(((AbstractPrimitiveDistanceFunction<NumberVector>) this.getDistanceFunction()).distance(uoSample, possibleNeighbourSample) <= epsilon) {
              matches++;
            }
          }
        }
        // samplesUO.size() * samplesPossibleNeighbour.size() is the size of the cartesian
        // product we iterated above, hence the amount of pairwise distances we checked for being
        // less or equal to epsilon.
        // By multiplying this value with the threshold and converting it back into an integer
        // it is rounded down and we can check if there have been enough matches to consider it
        // an epsilon neighbor.
        //
        // TODO: Maybe instead of getting the default floored value we should use Math.ceil
        // to prevent us from using slightly lower boundaries for our probabilistic epsilon
        // neighborhood?
        // E.g. if we have a threshold of 0.5 and a sampleSize of 5, it would be sufficient to have 12 matches.
        // 12 / 25 = 0.48, which is lower than the given 0.5.
        if(matches >= (((UniformDiscreteUO) uo.getModel()).getObservationsCopy().size() *
            ((UniformDiscreteUO) possibleNeighbour.getModel()).getObservationsCopy().size() * this.threshold)) {
          epsilonNeighbours.add(iter);
        }
      } else {
        // If the datasets contains DistributedDiscreteUOs or even continuous uncertain objects,
        // we draw lists of samples as large as given by sampleSize.
        // The reason for continuous uncertain objects to be treated that way should be obvious, since
        // we have no observation lists in the first place.
        // To handle the DistributedDiscreteUOs this way is meaningful, since that way we take into
        // account that some of their observations have a higher probability to be true.
        int matches = 0;
        for(int i = 0; i < this.sampleSize; i++) {
          for(int j = 0; j < this.sampleSize; j++) {
            // We just use UncertainObject.drawSample inline to prevent us from the need to store the
            // lists of samples.
            if(((AbstractPrimitiveDistanceFunction<NumberVector>) this.getDistanceFunction()).distance(uo.drawSample(), possibleNeighbour.drawSample()) <= epsilon) {
              matches++;
            }
          }
        }
        if(matches >= this.constraint) {
          epsilonNeighbours.add(iter);
        }
      }
    }
    return epsilonNeighbours;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new SimpleTypeInformation<>(UncertainObject.class));
  }

  @Override
  protected Logging getLogger() {
    return FDBSCAN.LOG;
  }

  /**
   * Parameterization Class.
   *
   * @author Alexander Koos
   */
  public static class Parameterizer<O extends UncertainObject<UOModel>> extends DBSCAN.Parameterizer<O> {

    protected double threshold;

    protected int sampleSize;

    /**
     * The OptionID to retrieve the threshold as discussed in {@link FDBSCAN}
     * from the user input. Must be a decimal value between 0 and 1, the default
     * is 0.5 (50%) as discussed in the corresponding paper.
     */
    public static final OptionID THRESHOLD_ID = new OptionID("fdbscan.threshold", "The minimum part of the sampled distances claiming epsilon-neighborhood.");

    /**
     * The OptionID to retrieve the threshold as discussed in {@link FDBSCAN}
     * from the user input. Must be an integer value greater than 0.
     */
    public static final OptionID SAMPLE_SIZE_ID = new OptionID("fdbscan.sampleSize", "How many samples should be drawn per uncertain object. Choose in regard to your uncertainty model.");

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter thresholdP = new DoubleParameter(Parameterizer.THRESHOLD_ID, 0.5);
      if(config.grab(thresholdP)) {
        this.threshold = thresholdP.getValue();
      }
      final IntParameter sampleP = new IntParameter(Parameterizer.SAMPLE_SIZE_ID, 5);
      if(config.grab(sampleP)) {
        this.sampleSize = sampleP.getValue();
      }
    }

    @Override
    protected FDBSCAN<O> makeInstance() {
      return new FDBSCAN<>(this.distanceFunction, this.epsilon, this.threshold, this.minpts, this.sampleSize);
    }

  }
}
