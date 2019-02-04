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

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.GeneralizedDBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.MinPtsCorePredicate;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * FDBSCAN is an adaption of DBSCAN for fuzzy (uncertain) objects.
 * <p>
 * This implementation is based on GeneralizedDBSCAN. All implementation of
 * FDBSCAN functionality is located in the neighbor predicate
 * {@link FDBSCANNeighborPredicate}.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Martin Pfeifle<br>
 * Density-based clustering of uncertain data<br>
 * Proc. 11th ACM Int. Conf. on Knowledge Discovery and Data Mining (SIGKDD)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - FDBSCANNeighborPredicate
 */
@Title("FDBSCAN: Density-based Clustering of Applications with Noise on fuzzy objects")
@Description("Algorithm to find density-connected sets in a database consisting of uncertain/fuzzy objects based on the" //
    + " parameters 'minpts', 'epsilon', 'samplesize', and (if used) 'threshold'")
@Reference(authors = "Hans-Peter Kriegel, Martin Pfeifle", //
    title = "Density-based clustering of uncertain data", //
    booktitle = "Proc. 11th ACM Int. Conf. on Knowledge Discovery and Data Mining (SIGKDD)", //
    url = "https://doi.org/10.1145/1081870.1081955", //
    bibkey = "DBLP:conf/kdd/KriegelP05")
public class FDBSCAN extends GeneralizedDBSCAN {
  /**
   * Constructor that initialized GeneralizedDBSCAN.
   *
   * @param epsilon Epsilon radius
   * @param sampleSize Sample size
   * @param threshold Threshold
   * @param seed Random generator
   * @param minpts MinPts
   */
  public FDBSCAN(double epsilon, int sampleSize, double threshold, RandomFactory seed, int minpts) {
    super(new FDBSCANNeighborPredicate(epsilon, sampleSize, threshold, seed), new MinPtsCorePredicate(minpts), false);
  }

  /**
   * Parameterizer class.
   *
   * Redundant to {@link FDBSCANNeighborPredicate}.
   *
   * @author Alexander Koos
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
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

    /**
     * MinPts of DBSCAN.
     */
    protected int minPts;

    @Override
    public void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter epsilonP = new DoubleParameter(DBSCAN.Parameterizer.EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.doubleValue();
      }
      IntParameter minPtsP = new IntParameter(DBSCAN.Parameterizer.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minPtsP)) {
        minPts = minPtsP.intValue();
      }
      IntParameter sampleSizep = new IntParameter(FDBSCANNeighborPredicate.Parameterizer.SAMPLE_SIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(sampleSizep)) {
        sampleSize = sampleSizep.getValue();
      }
      DoubleParameter thresholdp = new DoubleParameter(FDBSCANNeighborPredicate.Parameterizer.THRESHOLD_ID, 0.5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(thresholdp)) {
        threshold = thresholdp.getValue();
      }
      RandomParameter seedp = new RandomParameter(FDBSCANNeighborPredicate.Parameterizer.SEED_ID);
      if(config.grab(seedp)) {
        seed = seedp.getValue();
      }
    }

    @Override
    protected FDBSCAN makeInstance() {
      return new FDBSCAN(epsilon, sampleSize, threshold, seed, minPts);
    }
  }
}
