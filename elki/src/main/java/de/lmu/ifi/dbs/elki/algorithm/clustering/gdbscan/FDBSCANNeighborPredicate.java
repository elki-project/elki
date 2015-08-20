package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Density-based Clustering of Applications with Noise and Fuzzy objects
 * (FDBSCAN) is an Algorithm to find sets in a fuzzy database that are
 * density-connected with minimum probability.
 *
 * Reference:
 * <p>
 * H.-P. Kriegel and M. Pfeifle:<br />
 * Density-based clustering of uncertain data<br />
 * In Proc. 11th ACM Int. Conf. on Knowledge Discovery and Data Mining (SIGKDD),
 * Chicago, IL, 2005.
 * </p>
 *
 * This class is a NeighborPredicate presenting this Algorithm in use with
 * <code>{@link GeneralizedDBSCAN}</code>
 *
 * @author Alexander Koos
 */
@Title("FDBSCAN: Density-based Clustering of Applications with Noise on fuzzy objects")
@Description("Algorithm to find density-connected sets in a database consisting of uncertain/fuzzy objects based on the" //
+ " parameters 'minpts', 'epsilon', 'samplesize', and (if used) 'threshold'")
@Reference(authors = "H.-P. Kriegel and M. Pfeifle", //
title = "Density-based clustering of uncertain data", //
booktitle = "KDD05", //
url = "http://dx.doi.org/10.1145/1081870.1081955")
public class FDBSCANNeighborPredicate implements NeighborPredicate {
  /**
   * Epsilon radius
   */
  protected double epsilon;

  /**
   * Distance function
   */
  protected NumberVectorDistanceFunction<?> distance;

  /**
   * The size of samplesets that should be drawn for neighborcheck.
   */
  protected int sampleSize;

  /**
   * The relative amount of epsilon-close pairings determined by the
   * neighborcheck.
   */
  protected double threshold;

  /**
   * The <code>Random</code> object to draw the samples with.
   */
  protected RandomFactory rand;

  /**
   *
   * Constructor.
   *
   * @param epsilon
   * @param distance
   * @param sampleSize
   * @param threshold
   * @param seed
   */
  public FDBSCANNeighborPredicate(double epsilon, NumberVectorDistanceFunction<?> distance, int sampleSize, double threshold, RandomFactory seed) {
    super();
    this.epsilon = epsilon;
    this.distance = distance;
    this.sampleSize = sampleSize;
    this.threshold = threshold;
    this.rand = seed;
  }

  /**
   * Instance of the neighbor predicate.
   *
   * @author Alexander Koos
   */
  public static class Instance implements NeighborPredicate.Instance<DBIDs> {
    /**
     * The DBIDs to iterate over for neighborcheck.
     */
    private DBIDs ids;

    /**
     * The epsilon distance a neighbor may have at most.
     */
    private double epsilon;

    /**
     * The size of samplesets that should be drawn for neighborcheck.
     */
    private int sampleSize;

    /**
     * The relative amount of epsilon-close pairings determined by the
     * neighborcheck.
     */
    private double threshold;

    /**
     * The relation holding the uncertain objects.
     */
    private Relation<? extends UncertainObject> relation;

    /**
     * The distance query to determine the distance of two samples.
     */
    private NumberVectorDistanceFunction<?> distance;

    /**
     * The random generator to draw the samples with.
     */
    private Random rand;

    /**
     *
     * Constructor.
     *
     * @param epsilon
     * @param ids
     * @param sampleSize
     * @param threshold
     * @param relation
     * @param distance
     * @param rand
     */
    public Instance(double epsilon, DoubleDBIDList ids, int sampleSize, double threshold, Relation<? extends UncertainObject> relation, NumberVectorDistanceFunction<?> distance, RandomFactory rand) {
      super();
      this.ids = ids;
      this.epsilon = epsilon;
      this.sampleSize = sampleSize;
      this.threshold = threshold;
      this.relation = relation;
      this.distance = distance;
      this.rand = rand.getRandom();
      // TODO: integrate distancefunction correctly
    }

    @Override
    public DBIDs getNeighbors(DBIDRef reference) {
      UncertainObject referenceObject = relation.get(reference);
      ModifiableDBIDs resultList = DBIDUtil.newArray();
      final double thresSampleSample = threshold * sampleSize * sampleSize;
      for(DBIDIter iter = iterDBIDs(ids); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(reference, iter)) {
          continue; // FIXME: do not ignore the query point.
        }

        boolean included = true;
        UncertainObject comparisonObject = relation.get(iter);
        for(int i = 0; i < referenceObject.getDimensionality(); i++) {
          if(included) {
            if(((referenceObject.getMax(i) - referenceObject.getMin(i)) < (epsilon * 2)) && referenceObject.getMin(i) <= comparisonObject.getMin(i) && referenceObject.getMax(i) >= comparisonObject.getMax(i)) {
              // leave as marked as completely included
              continue;
            }
            // at least in one dimension it is not completely included
            included = false;
          }
          if((referenceObject.getMin(i) - epsilon) > comparisonObject.getMax(i) || (referenceObject.getMax(i) + epsilon) < comparisonObject.getMin(i)) {
            // completely excluded
            continue;
          }
        }
        if(included) {
          resultList.add(iter);
          continue;
        }

        int count = 0;
        for(int i = 0; i < sampleSize; i++) {
          NumberVector comparisonSample = comparisonObject.drawSample(rand);
          // nested loop because of cartesian product
          for(int j = 0; j < sampleSize; j++) {
            NumberVector referenceSample = referenceObject.drawSample(rand);
            if(epsilon <= distance.distance(comparisonSample, referenceSample)) {
              // Keep track of how many are epsilon-close
              count++;
            }
          }
        }
        // check if enough sample-pairings were epsilon-close
        // If yes, add to neighborlist to return
        if(count >= thresSampleSample) {
          resultList.add(iter);
        }
      }
      return resultList;
    }

    @Override
    public DBIDs getIDs() {
      return this.ids;
    }

    @Override
    public DBIDIter iterDBIDs(DBIDs neighbors) {
      return neighbors.iter();
    }
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.UNCERTAIN_OBJECT_FIELD;
  }

  @Override
  public SimpleTypeInformation<?>[] getOutputType() {
    return new SimpleTypeInformation<?>[] { TypeUtil.DBIDS };
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> NeighborPredicate.Instance<T> instantiate(Database database, SimpleTypeInformation<?> type) {
    Relation<? extends UncertainObject> relation = database.getRelation(TypeUtil.UNCERTAIN_OBJECT_FIELD);
    return (NeighborPredicate.Instance<T>) new Instance(epsilon, (DoubleDBIDList) relation.getDBIDs(), sampleSize, threshold, relation, distance, rand);
  }

  /**
   * Parameterizer class.
   *
   * @author Alexander Koos
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Number of samples per uncertain object.
     */
    public final static OptionID SAMPLE_SIZE_ID = new OptionID("fdbscan.samplesize", //
    "The number of samples to draw from each uncertain object to determine the epsilon-neighborhood.");

    /**
     * Threshold for epsilon-neighborhood, defaults to 0.5.
     */
    public final static OptionID THRESHOLD_ID = new OptionID("fdbscan.threshold", //
    "The amount of samples that have to be epsilon-close for two objects to be neighbors.");

    /**
     * Seed for random sample draw.
     */
    public final static OptionID SEED_ID = new OptionID("fdbscan.seed", //
    "Random generator used to draw samples.");

    /**
     * Epsilon radius
     */
    protected double epsilon;

    /**
     * The <code>DistanceFunction</code> to determine the distance between two
     * samples.
     */
    protected NumberVectorDistanceFunction<?> distance;

    /**
     * The size of samplesets that should be drawn for neighborcheck.
     */
    protected int sampleSize;

    /**
     * The relative amount of epsilon-close pairings determined by the
     * neighborcheck.
     */
    protected double threshold;

    /**
     * Random generator.
     */
    protected RandomFactory seed;

    @Override
    public void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter epsilonP = new DoubleParameter(DBSCAN.Parameterizer.EPSILON_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.doubleValue();
      }
      ObjectParameter<NumberVectorDistanceFunction<? super NumberVector>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, NumberVectorDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }
      IntParameter sampleSizep = new IntParameter(SAMPLE_SIZE_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(sampleSizep)) {
        sampleSize = sampleSizep.getValue();
      }
      DoubleParameter thresholdp = new DoubleParameter(THRESHOLD_ID, 0.5) //
      .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
      .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(thresholdp)) {
        threshold = thresholdp.getValue();
      }
      RandomParameter seedp = new RandomParameter(SEED_ID);
      if(config.grab(seedp)) {
        seed = seedp.getValue();
      }
    }

    @Override
    protected FDBSCANNeighborPredicate makeInstance() {
      return new FDBSCANNeighborPredicate(epsilon, distance, sampleSize, threshold, seed);
    }
  }
}
