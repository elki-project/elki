package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;

import java.util.Random;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/*
This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
 * Density-based Clustering of Applications with Noise and Fuzzy objects
 * (FDBSCAN) is an Algorithm to find sets in a fuzzy database that are
 * density-connected with minimum probability.
 *
 * <p>
 * Reference:<br>
 * H.-P. Kriegel and M. Pfeifle:<br />
 * Density-based clustering of uncertain data<br />
 * In Proc. 11th ACM Int. Conf. on Knowledge Discovery and Data Mining (SIGKDD),
 * Chicago, IL, 2005.
 * </p>
 *
 * This class is a NeighborPredicate presenting this Algorithm in use with <code>{@link GeneralizedDBSCAN}</code>
 *
 * @author Alexander Koos
 * @param <O> Object type
 */
@Title("FDBSCAN: Density-based Clustering of Applications with Noise on fuzzy objects")
@Description("Algorithm to find density-connected sets in a database consisting of uncertain/fuzzy objects based on the"
    + " parameters 'minpts', 'epsilon', 'samplesize', and (if used) 'threshold'")
@Reference(authors = "H.-P. Kriegel and M. Pfeifle", //
title = "Density-based clustering of uncertain data", //
booktitle = "KDD05", //
url = "http://dx.doi.org/10.1145/1081870.1081955")
public class FDBSCANNeighborPredicate<U extends UncertainObject> extends EpsilonNeighborPredicate<DoubleVector> {

  /**
   * The size of samplesets that should be drawn for neighborcheck.
   */
  private int sampleSize;

  /**
   * The relative amount of epsilon-close pairings determined by the neighborcheck.
   */
  private double threshold;

  /**
   * The <code>Random</code> object to draw the samples with.
   */
  private Random rand;

  /**
   *
   * Constructor.
   *
   * @param epsilon
   * @param distFunc
   * @param sampleSize
   * @param threshold
   * @param seed
   */
  public FDBSCANNeighborPredicate(double epsilon, DistanceFunction<DoubleVector> distFunc, int sampleSize, double threshold, long seed) {
    super(epsilon, distFunc);
    this.sampleSize = sampleSize;
    this.threshold = threshold;
    this.rand = new Random(seed == 0l ? null : seed);
  }

  /**
   *
   * @author Alexander Koos
   *
   */
  /*
   * TODO: Why does it have to be DoubleDBIDs?
   * Took the implementation from EpsilonNeighborPredicate.Instance as inspiration...
   */
  public static class Instance<U extends UncertainObject> implements NeighborPredicate.Instance<DoubleDBIDList> {

    /**
     * The DBIDs to iterate over for neighborcheck.
     */
    private DoubleDBIDList ids;

    /**
     * The epsilon distance a neighbor may have at most.
     */
    private double epsilon;

    /**
     * The size of samplesets that should be drawn for neighborcheck.
     */
    private int sampleSize;

    /**
     * The relative amount of epsilon-close pairings determined by the neighborcheck.
     */
    private double threshold;

    /**
     * The relation holding the uncertain objects.
     */
    private Relation<U> relation;

    /**
     * The distancequery to determine the distance of two samples.
     */
    private DistanceQuery<DoubleVector> distQuery;

    /**
     * The <code>Random</code> object to draw the samples with.
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
     * @param distQuery
     * @param rand
     */
    public Instance(double epsilon, DoubleDBIDList ids, int sampleSize, double threshold, Relation<U> relation, DistanceQuery<DoubleVector> distQuery, Random rand) {
      super();
      this.ids = ids;
      this.epsilon = epsilon;
      this.sampleSize = sampleSize;
      this.threshold = threshold;
      this.relation = relation;
      this.distQuery = distQuery;
      this.rand = rand;
      // TODO: integrate distancefunction correctly
    }

    @Override
    // FIXME: You can't accelerate me by using indices as I am!
    // Particularly useful would be spatial index-structures like
    // R-Trees.
    // That would give the opportunity to use a rangequery on the
    // database that only returns objects that aren't completely excluded.
    //
    // Even more powerful would be a spatial index-structure that knows about
    // complete inclusion of objects in other objects boundaries and returns
    // with its rangequery a tupel that holds as FIRST a list of all completely
    // included and as SECOND a list of all other possible neighbors...
    //
    // But I believe this to be too special to consider it in detail right now.
    public DoubleDBIDList getNeighbors(DBIDRef reference) {
      U referenceObject = relation.get(reference);
      ModifiableDoubleDBIDList resultList = DBIDUtil.newDistanceDBIDList();
      for(DBIDIter iter = iterDBIDs(ids); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(reference,iter)) {
          // No need to test the object itself
          continue;
        }

        boolean included = true;
        U comparisonObject = relation.get(iter);
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
          resultList.add(epsilon, iter);
          continue;
        }

        int count = 0;
        for(int i = 0; i < sampleSize; i++) {
          DoubleVector comparisonSample = comparisonObject.drawSample(rand);
          // nested loop because of cartesian product
          for(int j = 0; j < sampleSize; j++) {
            DoubleVector referenceSample = referenceObject.drawSample(rand);
            if(epsilon <= distQuery.distance(comparisonSample, referenceSample)) {
              // Keep track of how many are epsilon-close
              count++;
            }
          }
        }
        // check if enough sample-pairings were epsilon-close
        // If yes, add to neighborlist to return
        if(count >= threshold * sampleSize * sampleSize) {
          resultList.add(epsilon,iter);
        }
      }
      return resultList;
    }

    @Override
    public DBIDs getIDs() {
      return this.ids;
    }

    @Override
    public DBIDIter iterDBIDs(DoubleDBIDList neighbors) {
      return neighbors.iter();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> NeighborPredicate.Instance<T> instantiate(Database database, SimpleTypeInformation<?> type) {
    Relation<U> relation = database.getRelation(TypeUtil.UNCERTAIN_OBJECT_FIELD);
    return (NeighborPredicate.Instance<T>) new Instance<U>(epsilon,
        (DoubleDBIDList) relation.getDBIDs(), sampleSize, threshold,
        relation, QueryUtil.getDistanceQuery(database,distFunc), rand);
  }

  // The methods to determine input and output type restrictions behave similar to the
  // superclasses implementation of these methods, therefor I left them out

  /**
   * Parametizer class.
   *
   * @author Alexander Koos
   *
   * @param <U>
   */
  public static class Parameterizer<U extends UncertainObject> extends EpsilonNeighborPredicate.Parameterizer<DoubleVector> {

    /**
     * The size of samplesets that should be drawn for neighborcheck.
     */
    private int sampleSize;

    /**
     * The relative amount of epsilon-close pairings determined by the neighborcheck.
     */
    private double threshold;

    /**
     * The <code>DistanceFunction</code> to determine the distance between two samples.
     */
    private DistanceFunction<DoubleVector> distfun;

    /**
     * The seed for the <code>Random</code> object used to draw samples.
     */
    private long seed;

    /**
     * Number of samples per uncertain object.
     */
    public final static OptionID SAMPLE_SIZE_ID = new OptionID("fdbscan.samplesize","That many samples are drawn from each uncertain object to determine the epsilon-neighborhood.");

    /**
     * Threshold for epsilon-neighborhood, defaults to 0.5.
     */
    public final static OptionID THRESHOLD_ID = new OptionID("fdbscan.threshold","That many pairings of samples of two uncertain objects have to be epsilon-close for neighborhood.");

    /**
     * Seed for random sample draw.
     */
    public final static OptionID SEED_ID = new OptionID("fdbscan.seed","The seed for the random sample draws. 0 initializes the Random object with null.");

    @Override
    public void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter sampleSizep = new IntParameter(SAMPLE_SIZE_ID);
      if(config.grab(sampleSizep)) {
        sampleSize = sampleSizep.getValue();
      }
      DoubleParameter thresholdp = new DoubleParameter(THRESHOLD_ID, 0.5);
      if(config.grab(thresholdp)) {
        threshold = thresholdp.getValue();
      }
      LongParameter seedp = new LongParameter(SEED_ID, 0);
      if(config.grab(seedp)) {
        seed = seedp.getValue();
      }
      ObjectParameter<DistanceFunction<DoubleVector>> distanceFunctionP = makeParameterDistanceFunction(EuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distfun = distanceFunctionP.instantiateClass(config);
      }
    }

    @Override
    protected FDBSCANNeighborPredicate<U> makeInstance() {
      return new FDBSCANNeighborPredicate<>(epsilon, distfun, sampleSize, threshold, seed);
    }

    /**
     * TODO: took this from an abstract implementation of DistanceBasedAlgorithms.
     * Should this be kept this way or handled otherwise?
     *
     * @param defaultDistanceFunction
     * @param restriction
     * @return
     */
    public static <F extends DistanceFunction<?>> ObjectParameter<F> makeParameterDistanceFunction(Class<?> defaultDistanceFunction, Class<?> restriction) {
      return new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, restriction, defaultDistanceFunction);
    }
  }
}
