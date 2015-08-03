package de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain;

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
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
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

/**
 * Density-based Clustering of Applications with Noise and Fuzzy objects
 * (FDBSCAN), an Algorithm to find sets in a fuzzy database that are
 * density-connected with minimum probability
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
 * @param <O> Object type
 */
// FIXME add @Reference
// FIXME: we CANNOT interherit from DBSCAN, because we operate on
// UncertainObject, but need a distance function defined on Samples.
// We can, however, implement the predicates of GeneralizedDBSCAN maybe?
// OR: maybe we can just define an uncertain distance function to use?
public class FDBSCAN<O extends UncertainObject> extends DBSCAN<O> {
  /**
   * The logger for this class.
   */
  private final static Logging LOG = Logging.getLogger(FDBSCAN.class);

  /**
   * The minimum probability that is needed for two fuzzy objects to be
   * considered connected.
   */
  protected double threshold;

  /**
   * The amount of samples that are drawn from a fuzzy object and used to
   * determine if they're density-connected or not.
   */
  protected int sampleSize;

  /**
   * The amount of density-connected samples that suffice for two fuzzy objects
   * to be density-connected, stored to minimize necessary computation.
   */
  protected int constraint;

  /**
   *
   * Constructor.
   *
   * @param distanceFunction To determine a distance measure for pairs of
   *        objects.
   * @param epsilon Analogous to the epsilon parameter in {@link DBSCAN}.
   * @param threshold The probability necessary for two objects to be
   *        density-connected - the Default is 50%.
   * @param minpts Analogous to the minpoints parameter in {@link DBSCAN}.
   * @param sampleSize The amount of samples that have to be drawn per fuzzy
   *        object.
   */
  protected FDBSCAN(DistanceFunction<? super O> distanceFunction, double epsilon, double threshold, int minpts, int sampleSize) {
    super(distanceFunction, epsilon, minpts);
    this.threshold = threshold;
    this.sampleSize = sampleSize;
    // to reduce computational overhead, pre-compute the absolute necessary
    // amount of found connections
    //
    // TODO: Maybe instead of getting the default floored value we should use
    // Math.ceil to prevent us from using slightly lower boundaries for our
    // probabilistic epsilon neighborhood?
    // E.g. if we have a threshold of 0.5 and a sampleSize of 5, it would be
    // sufficient to have 12 matches.
    // 12 / 25 = 0.48, which is lower than the given 0.5.
    this.constraint = (int) (sampleSize * sampleSize * threshold);
  }

  /**
   *
   * Check for each fuzzy object if it is a core-object. If yes, create a
   * Cluster, add the object, and check if the surrounding fuzzy objects
   * core-objects as well. Add all objects, that are core-objects or within the
   * epsilon-environment of a core-object to the corresponding Cluster. Add all
   * objects that aren't core-objects or within the epsilon-environment of a
   * core-object to the Noise.
   *
   * @param relation TODO
   * @param rangeQuery TODO
   * @param startObjectID TODO
   * @param objprog TODO
   * @param clusprog TODO
   */
  protected void expandCluster(Relation<O> relation, RangeQuery<O> rangeQuery, DBIDRef startObjectID, FiniteProgress objprog, IndefiniteProgress clusprog) {
    DBIDs epsilonNeighbours = getEpsilonNeighbours(startObjectID, relation, epsilon);

    // startObject is no core-object
    if(epsilonNeighbours.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), LOG);
        clusprog.setProcessed(resultList.size(), LOG);
      }
      return;
    }

    // try to expand the cluster
    ModifiableDBIDs seeds = DBIDUtil.newHashSet();
    ModifiableDBIDs currentCluster = DBIDUtil.newArray();
    for(DBIDIter seed = epsilonNeighbours.iter(); seed.valid(); seed.advance()) {
      if(!processedIDs.contains(seed)) {
        currentCluster.add(seed);
        processedIDs.add(seed);
        seeds.add(seed);
      }
      else if(noise.contains(seed)) {
        currentCluster.add(seed);
        noise.remove(seed);
      }
    }
    seeds.remove(startObjectID);

    // Check for each object in the last looked at objects epsilon-environment
    // if it is a core-object itself and if yes add its epsilon-neighbors to the
    // cluster and the seedlist.
    while(!seeds.isEmpty()) {
      DBIDMIter o = seeds.iter();
      DBIDs neighborhood = getEpsilonNeighbours(o, relation, epsilon);
      o.remove();

      if(neighborhood.size() >= minpts) {
        for(DBIDIter neighbor = neighborhood.iter(); neighbor.valid(); neighbor.advance()) {
          boolean inNoise = noise.contains(neighbor);
          boolean unclassified = !processedIDs.contains(neighbor);
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(neighbor);
            }
            currentCluster.add(neighbor);
            processedIDs.add(neighbor);
            if(inNoise) {
              noise.remove(neighbor);
            }
          }
        }
      }

      if(processedIDs.size() == relation.size() && noise.size() == 0) {
        break;
      }

      if(objprog != null && clusprog != null) {
        objprog.setProcessed(processedIDs.size(), LOG);
        int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
        clusprog.setProcessed(numClusters, LOG);
      }
    }
    // when the cluster has at least minpts elements it is a cluster,...
    if(currentCluster.size() >= this.minpts) {
      resultList.add(currentCluster);
    }
    // ... otherwise it would be a microcluster and therefore is considered to
    // be noise instead
    else {
      noise.addDBIDs(currentCluster);
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }
  }

  /**
   * Determine which objects are in the epsilon-environment of the object
   * identified by startObjectID.
   *
   * @param startObjectID The <code>DBID</code> of the object that is evaluated
   * @param relation The <code>Relation</code> containing the dataset
   * @param epsilon The distance a potential neighbor may have at most
   * @return The DBIDs of all epsilon neighbors of the evaluated object
   */
  private DBIDs getEpsilonNeighbours(DBIDRef startObjectID, Relation<O> relation, double epsilon) {
    final ModifiableDBIDs epsilonNeighbours = DBIDUtil.newArray();
    final O uo = relation.get(startObjectID);

    // look at each object once
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      // ignore the object itself
      if(iter.internalGetIndex() == startObjectID.internalGetIndex()) {
        continue;
      }
      O possibleNeighbour = relation.get(iter);

      boolean inside = true, possible = true;
      for(int i = 0; i < uo.getDimensionality(); i++) {
        final double minBound = uo.getMin(i) - epsilon;
        final double maxBound = uo.getMax(i) + epsilon;
        // If those checks evaluate to false, the current object is completely
        // included in the maximum epsilon environment of the evaluated
        // startObject. Otherwise it has to be checked further.
        if(possibleNeighbour.getMin(i) < minBound || possibleNeighbour.getMax(i) > maxBound) {
          inside = false;
          // if those checks evaluate to true, the current object is completely
          // outside of the maximum epsilon environment of the evaluated
          // startObject. In this case further checks are obsolete.
          if(possibleNeighbour.getMin(i) > maxBound || possibleNeighbour.getMax(i) < minBound) {
            possible = false;
            break;
          }
        }
      }
      if(inside) {
        // Completely included as discussed above, therefore it is an epsilon
        // neighbor and we can proceed.
        epsilonNeighbours.add(iter);
        continue;
      }
      else if(!possible) {
        // Completely excluded as discussed above, therefore it can't be an
        // epsilon neighbor and we can proceed.
        continue;
      }

      if(uo instanceof UniformDiscreteUO) {
        int matches = 0;
        // Look at each pairing, respectively the cartesian product of both
        // samplesets. Hence we iterate over both lists.
        // To prevent us from holding the lists of samples in the memory, we
        // retrieve them from the uncertain objects directly.
        // TODO: Avoid copies.
        final UniformDiscreteUO uuo = (UniformDiscreteUO) uo;
        final UniformDiscreteUO upn = (UniformDiscreteUO) possibleNeighbour;
        for(DoubleVector uoSample : uuo.getObservationsCopy()) {
          for(DoubleVector possibleNeighbourSample : upn.getObservationsCopy()) {
            // FIXME: ugly cast. This IS an error, so ignoring this is not OK.
            if(((AbstractPrimitiveDistanceFunction<NumberVector>) getDistanceFunction()).distance(uoSample, possibleNeighbourSample) <= epsilon) {
              matches++;
            }
          }
        }
        // samplesUO.size() * samplesPossibleNeighbour.size() is the size of the
        // cartesian product we iterated above, hence the amount of pairwise
        // distances we checked for being less or equal to epsilon.
        // By multiplying this value with the threshold and converting it back
        // into an integer it is rounded down and we can check if there have
        // been enough matches to consider it an epsilon neighbor.
        //
        // TODO: Maybe instead of getting the default floored value we should
        // use Math.ceil to prevent us from using slightly lower boundaries for
        // our probabilistic epsilon neighborhood?
        // E.g. if we have a threshold of 0.5 and a sampleSize of 5, it would be
        // sufficient to have 12 matches.
        // 12 / 25 = 0.48, which is lower than the given 0.5.
        if(matches >= (uuo.getObservationsCopy().size() * upn.getObservationsCopy().size() * threshold)) {
          epsilonNeighbours.add(iter);
        }
      }
      else {
        // If the data sets contains DistributedDiscreteUOs or even continuous
        // uncertain objects, we draw lists of samples as large as given by
        // sampleSize.
        // The reason for continuous uncertain objects to be treated that way
        // should be obvious, since we have no observation lists in the first
        // place. To handle the DistributedDiscreteUOs this way is meaningful,
        // since that way we take into account that some of their observations
        // have a higher probability to be true.
        int matches = 0;
        for(int i = 0; i < sampleSize; i++) {
          for(int j = 0; j < sampleSize; j++) {
            // We just use UOModel.drawSample inline to prevent us from
            // the need to store the lists of samples.
            // FIXME: ugly cast. This IS an error, so ignoring this is not OK.
            if(((AbstractPrimitiveDistanceFunction<NumberVector>) getDistanceFunction()).distance(uo.drawSample(), possibleNeighbour.drawSample()) <= epsilon) {
              matches++;
            }
          }
        }
        if(matches >= constraint) {
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
    return LOG;
  }

  /**
   * Parameterization Class.
   *
   * @author Alexander Koos
   *
   * @param <O> Object type
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends UncertainObject> extends DBSCAN.Parameterizer<O> {
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

    // TODO
    protected double threshold;

    // TODO
    protected int sampleSize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter thresholdP = new DoubleParameter(Parameterizer.THRESHOLD_ID, 0.5);
      if(config.grab(thresholdP)) {
        threshold = thresholdP.getValue();
      }
      IntParameter sampleP = new IntParameter(Parameterizer.SAMPLE_SIZE_ID, 5);
      if(config.grab(sampleP)) {
        sampleSize = sampleP.getValue();
      }
    }

    @Override
    protected FDBSCAN<O> makeInstance() {
      return new FDBSCAN<>(distanceFunction, epsilon, threshold, minpts, sampleSize);
    }
  }
}
