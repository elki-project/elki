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
package elki.clustering.subspace;

import java.util.Random;

import elki.data.Cluster;
import elki.data.NumberVector;
import elki.data.model.SubspaceModel;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.random.RandomFactory;

/**
 * The heuristic variant of the DOC algorithm, FastDOC
 * <p>
 * Reference:
 * <p>
 * C. M. Procopiuc, M. Jones, P. K. Agarwal, T. M. Murali<br>
 * A Monte Carlo algorithm for fast projective clustering<br>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '02).
 * 
 * @author Florian Nuecke
 * @since 0.7.5
 * 
 * @has - - - SubspaceModel
 */
@Title("FastDOC: Density-based Optimal projective Clustering")
@Reference(authors = "C. M. Procopiuc, M. Jones, P. K. Agarwal, T. M. Murali", //
    title = "A Monte Carlo algorithm for fast projective clustering", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '02)", //
    url = "https://doi.org/10.1145/564691.564739", //
    bibkey = "DBLP:conf/sigmod/ProcopiucJAM02")
public class FastDOC extends DOC {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FastDOC.class);

  /**
   * Holds the value of {@link Par#D_ZERO_ID}.
   */
  private int d_zero;

  /**
   * Constructor.
   * 
   * @param alpha &alpha; relative density threshold.
   * @param beta &beta; balancing parameter for size vs. dimensionality.
   * @param w half width parameter.
   * @param random Random factory
   */
  public FastDOC(double alpha, double beta, double w, int d_zero, RandomFactory random) {
    super(alpha, beta, w, random);
    this.d_zero = d_zero;
  }

  /**
   * Performs a single run of FastDOC, finding a single cluster.
   * 
   * @param relation used to get actual values for DBIDs.
   * @param S The set of points we're working on.
   * @param d Dimensionality of the data set we're currently working on.
   * @param r Size of random samples.
   * @param m Number of inner iterations (per seed point).
   * @param n Number of outer iterations (seed points).
   * @param minClusterSize Minimum size a cluster must have to be accepted.
   * @return a cluster, if one is found, else <code>null</code>.
   */
  @Override
  protected Cluster<SubspaceModel> runDOC(Relation<? extends NumberVector> relation, ArrayModifiableDBIDs S, int d, int n, int m, int r, int minClusterSize) {
    // Relevant attributes of highest cardinality.
    long[] D = null;
    // The seed point for the best dimensions.
    DBIDVar dV = DBIDUtil.newVar();

    // Inform the user about the progress in the current iteration.
    FiniteProgress iprogress = LOG.isVerbose() ? new FiniteProgress("Iteration progress for current cluster", m * n, LOG) : null;

    Random random = rnd.getSingleThreadedRandom();

    DBIDArrayIter iter = S.iter();
    outer: for(int i = 0; i < n; ++i) {
      // Pick a random seed point.
      iter.seek(random.nextInt(S.size()));

      for(int j = 0; j < m; ++j) {
        // Choose a set of random points.
        DBIDs randomSet = DBIDUtil.randomSample(S, r, random);

        // Initialize cluster info.
        long[] nD = BitsUtil.zero(d);

        // Test each dimension.
        for(int k = 0; k < d; ++k) {
          if(dimensionIsRelevant(k, relation, randomSet)) {
            BitsUtil.setI(nD, k);
          }
        }

        if(D == null || BitsUtil.cardinality(nD) > BitsUtil.cardinality(D)) {
          D = nD;
          dV.set(iter);

          if(BitsUtil.cardinality(D) >= d_zero) {
            if(iprogress != null) {
              iprogress.setProcessed(iprogress.getTotal(), LOG);
            }
            break outer;
          }
        }
        LOG.incrementProcessed(iprogress);
      }
    }
    LOG.ensureCompleted(iprogress);

    // If no relevant dimensions were found, skip it.
    if(D == null || BitsUtil.cardinality(D) == 0) {
      return null;
    }

    // Get all points in the box.
    DBIDs C = findNeighbors(dV, D, S, relation);

    // If we have a non-empty cluster, return it.
    return (C.size() >= minClusterSize) ? makeCluster(relation, C, D) : null;
  }

  /**
   * Parameterization class.
   * 
   * @author Florian Nuecke
   */
  public static class Par extends DOC.Par {
    /**
     * Stopping threshold for FastDOC.
     */
    public static final OptionID D_ZERO_ID = new OptionID("fastdoc.d0", "Parameter for FastDOC, setting the number of relevant attributes which, when found for a cluster, are deemed enough to stop iterating.");

    /**
     * Stopping threshold for FastDOC.
     */
    protected int d_zero;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(D_ZERO_ID, 5) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> d_zero = x);
    }

    @Override
    public FastDOC make() {
      return new FastDOC(alpha, beta, w, d_zero, random);
    }
  }
}
