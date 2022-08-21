/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.uncertain;

import elki.clustering.dbscan.DBSCAN;
import elki.clustering.dbscan.GeneralizedDBSCAN;
import elki.clustering.dbscan.predicates.MinPtsCorePredicate;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

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
  public static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new DoubleParameter(DBSCAN.Par.EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> epsilon = x);
      new IntParameter(DBSCAN.Par.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minPts = x);
      new IntParameter(FDBSCANNeighborPredicate.Par.SAMPLE_SIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> sampleSize = x);
      new DoubleParameter(FDBSCANNeighborPredicate.Par.THRESHOLD_ID, 0.5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> threshold = x);
      new RandomParameter(FDBSCANNeighborPredicate.Par.SEED_ID).grab(config, x -> seed = x);
    }

    @Override
    public FDBSCAN make() {
      return new FDBSCAN(epsilon, sampleSize, threshold, seed, minPts);
    }
  }
}
