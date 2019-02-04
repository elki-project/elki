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
package de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain;

import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.GeneralizedDBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.NeighborPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.DiscreteUncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Density-based Clustering of Applications with Noise and Fuzzy objects
 * (FDBSCAN) is an Algorithm to find sets in a fuzzy database that are
 * density-connected with minimum probability.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Martin Pfeifle<br>
 * Density-based clustering of uncertain data<br>
 * Proc. 11th ACM Int. Conf. on Knowledge Discovery and Data Mining (SIGKDD)
 * <p>
 * This class is a NeighborPredicate presenting this Algorithm in use with
 * <code>{@link GeneralizedDBSCAN}</code>.
 * <p>
 * Only Euclidean distance is supported, because of the pruning strategy
 * described in the original article which needs minimum and maximum distances
 * of bounding rectangles. Index support is not yet available.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - Instance
 */
@Reference(authors = "Hans-Peter Kriegel, Martin Pfeifle", //
    title = "Density-based clustering of uncertain data", //
    booktitle = "Proc. 11th ACM Int. Conf. on Knowledge Discovery and Data Mining (SIGKDD)", //
    url = "https://doi.org/10.1145/1081870.1081955", //
    bibkey = "DBLP:conf/kdd/KriegelP05")
public class FDBSCANNeighborPredicate implements NeighborPredicate<DBIDs> {
  /**
   * Epsilon radius
   */
  protected double epsilon;

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
   * The random generator to draw the samples with.
   */
  protected RandomFactory rand;

  /**
   * Constructor.
   *
   * @param epsilon Maximum distance
   * @param sampleSize Sampling size
   * @param threshold Threshold on how many samples are within the radius
   * @param seed Random generator for sampling
   */
  public FDBSCANNeighborPredicate(double epsilon, int sampleSize, double threshold, RandomFactory seed) {
    super();
    this.epsilon = epsilon;
    this.sampleSize = sampleSize;
    this.threshold = threshold;
    this.rand = seed;
  }

  @Override
  public Instance instantiate(Database database) {
    Relation<? extends UncertainObject> relation = database.getRelation(UncertainObject.UNCERTAIN_OBJECT_FIELD);
    return new Instance(epsilon, sampleSize, threshold, relation, rand);
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return UncertainObject.UNCERTAIN_OBJECT_FIELD;
  }

  @Override
  public SimpleTypeInformation<DBIDs> getOutputType() {
    return TypeUtil.DBIDS;
  }

  /**
   * Instance of the neighbor predicate.
   *
   * @author Alexander Koos
   * @author Erich Schubert
   */
  public static class Instance implements NeighborPredicate.Instance<DBIDs> {
    /**
     * The epsilon distance a neighbor may have at most.
     */
    private double epsilon, epsilonsq;

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
     * The random generator to draw the samples with.
     */
    private Random rand;

    /**
     *
     * Constructor.
     *
     * @param epsilon Maximum distance
     * @param sampleSize Sampling size
     * @param threshold Threshold on how many samples are within the radius
     * @param relation Data relation
     * @param rand Random generator for sampling
     */
    public Instance(double epsilon, int sampleSize, double threshold, Relation<? extends UncertainObject> relation, RandomFactory rand) {
      super();
      this.epsilon = epsilon;
      this.epsilonsq = epsilon * epsilon;
      this.sampleSize = sampleSize;
      this.threshold = threshold;
      this.relation = relation;
      this.rand = rand.getRandom();
    }

    @Override
    public DBIDs getNeighbors(DBIDRef reference) {
      UncertainObject referenceObject = relation.get(reference);
      ModifiableDBIDs resultList = DBIDUtil.newArray();
      candidates: for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(reference, iter)) {
          resultList.add(iter); // Always include the query point.
          continue;
        }

        UncertainObject comparisonObject = relation.get(iter);
        double mindistsq = 0., maxdistsq = 0.;
        for(int d = 0; d < referenceObject.getDimensionality(); d++) {
          final double rmin = referenceObject.getMin(d);
          final double rmax = referenceObject.getMax(d);
          final double cmin = comparisonObject.getMin(d);
          final double cmax = comparisonObject.getMax(d);
          // Minimum distance in this dimension:
          double mindelta = (cmin > rmax) ? cmin - rmax : (rmin > cmax) ? rmin - cmax : 0.;
          // True negative in this single dimension:
          if(mindelta > epsilon) {
            continue candidates;
          }
          mindistsq += mindelta * mindelta;
          // True negative by aggregated distances:
          if(mindistsq > epsilonsq) {
            continue candidates;
          }
          // Maximum distance in this dimension:
          double m1 = Math.abs(rmin - cmax), m2 = Math.abs(cmin - rmax);
          double maxdelta = m1 > m2 ? m1 : m2;
          maxdistsq += maxdelta * maxdelta;
        }
        // True positive:
        if(maxdistsq <= epsilonsq) {
          resultList.add(iter);
          continue;
        }
        // Perform the expensive checks:
        if(checkSamples(referenceObject, comparisonObject)) {
          resultList.add(iter);
          continue;
        }
      }
      return resultList;
    }

    private boolean checkSamples(UncertainObject o1, UncertainObject o2) {
      final SquaredEuclideanDistanceFunction distance = SquaredEuclideanDistanceFunction.STATIC;
      // Optimization for discrete objects:
      if(o1 instanceof DiscreteUncertainObject && o2 instanceof DiscreteUncertainObject) {
        DiscreteUncertainObject d1 = (DiscreteUncertainObject) o1;
        DiscreteUncertainObject d2 = (DiscreteUncertainObject) o2;
        final int l1 = d1.getNumberSamples(), l2 = d2.getNumberSamples();
        final double limit = threshold * l1 * l2;
        int count = 0;
        for(int i = 0; i < l1; i++) {
          NumberVector s1 = d1.getSample(i);
          for(int j = 0; j < l2; j++) {
            NumberVector s2 = d2.getSample(j);
            if(distance.distance(s2, s1) <= epsilonsq) {
              // Keep track of how many are epsilon-close
              count++;
              // Stop early:
              if(count >= limit) {
                return true;
              }
            }
          }
        }
        return false;
      }
      final double limit = threshold * sampleSize * sampleSize;
      int count = 0;
      for(int j = 0; j < sampleSize; j++) {
        NumberVector s1 = o1.drawSample(rand);
        for(int i = 0; i < sampleSize; i++) {
          NumberVector s2 = o2.drawSample(rand);
          if(distance.distance(s2, s1) <= epsilonsq) {
            // Keep track of how many are epsilon-close
            count++;
            // Stop early:
            if(count >= limit) {
              return true;
            }
          }
        }
      }
      return false;
    }

    @Override
    public DBIDs getIDs() {
      return relation.getDBIDs();
    }

    @Override
    public DBIDIter iterDBIDs(DBIDs neighbors) {
      return neighbors.iter();
    }
  }

  /**
   * Parameterizer class.
   *
   * @author Alexander Koos
   * @author Erich Schubert
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
      IntParameter sampleSizep = new IntParameter(SAMPLE_SIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(sampleSizep)) {
        sampleSize = sampleSizep.intValue();
      }
      DoubleParameter thresholdp = new DoubleParameter(THRESHOLD_ID, 0.5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(thresholdp)) {
        threshold = thresholdp.doubleValue();
      }
      RandomParameter seedp = new RandomParameter(SEED_ID);
      if(config.grab(seedp)) {
        seed = seedp.getValue();
      }
    }

    @Override
    protected FDBSCANNeighborPredicate makeInstance() {
      return new FDBSCANNeighborPredicate(epsilon, sampleSize, threshold, seed);
    }
  }
}
