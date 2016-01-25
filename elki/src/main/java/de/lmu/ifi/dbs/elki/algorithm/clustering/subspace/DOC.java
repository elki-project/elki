package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

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

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceMaximumDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * <p>
 * The DOC algorithm, and it's heuristic variant, FastDOC. DOC is a sampling
 * based subspace clustering algorithm.
 * </p>
 * 
 * <p>
 * Reference: <br/>
 * C. M. Procopiuc, M. Jones, P. K. Agarwal, T. M. Murali<br />
 * A Monte Carlo algorithm for fast projective clustering. <br />
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '02).
 * </p>
 * 
 * @author Florian Nuecke
 * @since 0.6.0
 * 
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm.
 */
@Title("DOC: Density-based Optimal projective Clustering")
@Reference(authors = "C. M. Procopiuc, M. Jones, P. K. Agarwal, T. M. Murali", //
title = "A Monte Carlo algorithm for fast projective clustering", //
booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '02)", //
url = "http://dx.doi.org/10.1145/564691.564739")
public class DOC<V extends NumberVector> extends AbstractAlgorithm<Clustering<SubspaceModel>>implements SubspaceClusteringAlgorithm<SubspaceModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DOC.class);

  /**
   * Relative density threshold parameter alpha.
   */
  private double alpha;

  /**
   * Balancing parameter for importance of points vs. dimensions
   */
  private double beta;

  /**
   * Half width parameter.
   */
  private double w;

  /**
   * Holds the value of {@link Parameterizer#HEURISTICS_ID}.
   */
  private boolean heuristics;

  /**
   * Holds the value of {@link Parameterizer#D_ZERO_ID}.
   */
  private int d_zero;

  /**
   * Randomizer used internally for sampling points.
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param alpha &alpha; relative density threshold.
   * @param beta &beta; balancing parameter for size vs. dimensionality.
   * @param w <em>w</em> half width parameter.
   * @param heuristics whether to use heuristics (FastDOC) or not.
   * @param random Random factory
   */
  public DOC(double alpha, double beta, double w, boolean heuristics, int d_zero, RandomFactory random) {
    this.alpha = alpha;
    this.beta = beta;
    this.w = w;
    this.heuristics = heuristics;
    this.d_zero = d_zero;
    this.rnd = random;
  }

  /**
   * Performs the DOC or FastDOC (as configured) algorithm on the given
   * Database.
   * 
   * <p>
   * This will run exhaustively, i.e. run DOC until no clusters are found
   * anymore / the database size has shrunk below the threshold for minimum
   * cluster size.
   * </p>
   * 
   * @param database Database
   * @param relation Data relation
   */
  public Clustering<SubspaceModel> run(Database database, Relation<V> relation) {
    // Dimensionality of our set.
    final int d = RelationUtil.dimensionality(relation);

    // Get available DBIDs as a set we can remove items from.
    ArrayModifiableDBIDs S = DBIDUtil.newArray(relation.getDBIDs());

    // Precompute values as described in Figure 2.
    double r = Math.abs(Math.log(d + d) / Math.log(beta * .5));
    // Outer loop count.
    int n = (int) (2. / alpha);
    // Inner loop count.
    int m = (int) (Math.pow(2. / alpha, r) * Math.log(4));
    if(heuristics) {
      m = Math.min(m, Math.min(1000000, d * d));
    }

    // Minimum size for a cluster for it to be accepted.
    int minClusterSize = (int) (alpha * S.size());

    // List of all clusters we found.
    Clustering<SubspaceModel> result = new Clustering<>("DOC Clusters", "DOC");

    // Inform the user about the number of actual clusters found so far.
    IndefiniteProgress cprogress = LOG.isVerbose() ? new IndefiniteProgress("Number of clusters", LOG) : null;

    // To not only find a single cluster, we continue running until our set
    // of points is empty.
    while(S.size() > minClusterSize) {
      Cluster<SubspaceModel> C;
      if(heuristics) {
        C = runFastDOC(database, relation, S, d, n, m, (int) r);
      }
      else {
        C = runDOC(database, relation, S, d, n, m, (int) r, minClusterSize);
      }

      if(C == null) {
        // Stop trying if we couldn't find a cluster.
        break;
      }
      // Found a cluster, remember it, remove its points from the set.
      result.addToplevelCluster(C);

      // Remove all points of the cluster from the set and continue.
      S.removeDBIDs(C.getIDs());

      if(cprogress != null) {
        cprogress.setProcessed(result.getAllClusters().size(), LOG);
      }
    }

    // Add the remainder as noise.
    if(S.size() > 0) {
      long[] alldims = BitsUtil.ones(d);
      result.addToplevelCluster(new Cluster<>(S, true, new SubspaceModel(new Subspace(alldims), Centroid.make(relation, S))));
    }
    LOG.setCompleted(cprogress);
    return result;
  }

  /**
   * Performs a single run of DOC, finding a single cluster.
   * 
   * @param database Database context
   * @param relation used to get actual values for DBIDs.
   * @param S The set of points we're working on.
   * @param d Dimensionality of the data set we're currently working on.
   * @param r Size of random samples.
   * @param m Number of inner iterations (per seed point).
   * @param n Number of outer iterations (seed points).
   * @param minClusterSize Minimum size a cluster must have to be accepted.
   * @return a cluster, if one is found, else <code>null</code>.
   */
  private Cluster<SubspaceModel> runDOC(Database database, Relation<V> relation, ArrayModifiableDBIDs S, final int d, int n, int m, int r, int minClusterSize) {
    // Best cluster for the current run.
    DBIDs C = null;
    // Relevant attributes for the best cluster.
    long[] D = null;
    // Quality of the best cluster.
    double quality = Double.NEGATIVE_INFINITY;

    // Bounds for our cluster.
    // ModifiableHyperBoundingBox bounds = new ModifiableHyperBoundingBox(new
    // double[d], new double[d]);

    // Weights for distance (= rectangle query)
    SubspaceMaximumDistanceFunction df = new SubspaceMaximumDistanceFunction(BitsUtil.zero(d));
    DistanceQuery<V> dq = database.getDistanceQuery(relation, df);
    RangeQuery<V> rq = database.getRangeQuery(dq);

    // Inform the user about the progress in the current iteration.
    FiniteProgress iprogress = LOG.isVerbose() ? new FiniteProgress("Iteration progress for current cluster", m * n, LOG) : null;

    Random random = rnd.getSingleThreadedRandom();
    DBIDArrayIter iter = S.iter();

    for(int i = 0; i < n; ++i) {
      // Pick a random seed point.
      iter.seek(random.nextInt(S.size()));

      for(int j = 0; j < m; ++j) {
        // Choose a set of random points.
        DBIDs randomSet = DBIDUtil.randomSample(S, r, random);

        // Initialize cluster info.
        long[] nD = BitsUtil.zero(d);

        // Test each dimension and build bounding box.
        for(int k = 0; k < d; ++k) {
          if(dimensionIsRelevant(k, relation, randomSet)) {
            BitsUtil.setI(nD, k);
          }
        }
        if(BitsUtil.cardinality(nD) > 0) {
          // Get all points in the box.
          df.setSelectedDimensions(nD);
          // TODO: add filtering capabilities into query API!
          DBIDs nC = DBIDUtil.intersection(S, rq.getRangeForDBID(iter, w));

          if(LOG.isDebuggingFiner()) {
            LOG.finer("Testing a cluster candidate, |C| = " + nC.size() + ", |D| = " + BitsUtil.cardinality(nD));
          }

          // Is the cluster large enough?
          if(nC.size() < minClusterSize) {
            // Too small.
            if(LOG.isDebuggingFiner()) {
              LOG.finer("... but it's too small.");
            }
          }
          else {
            // Better cluster than before?
            double nQuality = computeClusterQuality(nC.size(), BitsUtil.cardinality(nD));
            if(nQuality > quality) {
              if(LOG.isDebuggingFiner()) {
                LOG.finer("... and it's the best so far: " + nQuality + " vs. " + quality);
              }
              C = nC;
              D = nD;
              quality = nQuality;
            }
            else {
              if(LOG.isDebuggingFiner()) {
                LOG.finer("... but we already have a better one.");
              }
            }
          }
        }
        LOG.incrementProcessed(iprogress);
      }
    }
    LOG.ensureCompleted(iprogress);

    return (C != null) ? makeCluster(relation, C, D) : null;
  }

  /**
   * Performs a single run of FastDOC, finding a single cluster.
   * 
   * @param database Database context
   * @param relation used to get actual values for DBIDs.
   * @param S The set of points we're working on.
   * @param d Dimensionality of the data set we're currently working on.
   * @param r Size of random samples.
   * @param m Number of inner iterations (per seed point).
   * @param n Number of outer iterations (seed points).
   * @return a cluster, if one is found, else <code>null</code>.
   */
  private Cluster<SubspaceModel> runFastDOC(Database database, Relation<V> relation, ArrayModifiableDBIDs S, int d, int n, int m, int r) {
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
    SubspaceMaximumDistanceFunction df = new SubspaceMaximumDistanceFunction(D);
    DistanceQuery<V> dq = database.getDistanceQuery(relation, df);
    RangeQuery<V> rq = database.getRangeQuery(dq, DatabaseQuery.HINT_SINGLE);

    // TODO: add filtering capabilities into query API!
    DBIDs C = DBIDUtil.intersection(S, rq.getRangeForDBID(dV, w));

    // If we have a non-empty cluster, return it.
    return (C.size() > 0) ? makeCluster(relation, C, D) : null;
  }

  /**
   * Utility method to test if a given dimension is relevant as determined via a
   * set of reference points (i.e. if the variance along the attribute is lower
   * than the threshold).
   * 
   * @param dimension the dimension to test.
   * @param relation used to get actual values for DBIDs.
   * @param points the points to test.
   * @return <code>true</code> if the dimension is relevant.
   */
  private boolean dimensionIsRelevant(int dimension, Relation<V> relation, DBIDs points) {
    double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
    for(DBIDIter iter = points.iter(); iter.valid(); iter.advance()) {
      double xV = relation.get(iter).doubleValue(dimension);
      min = (xV < min) ? xV : min;
      max = (xV > max) ? xV : max;
      if(max - min > w) {
        return false;
      }
    }
    return true;
  }

  /**
   * Utility method to create a subspace cluster from a list of DBIDs and the
   * relevant attributes.
   * 
   * @param relation to compute a centroid.
   * @param C the cluster points.
   * @param D the relevant dimensions.
   * @return an object representing the subspace cluster.
   */
  private Cluster<SubspaceModel> makeCluster(Relation<V> relation, DBIDs C, long[] D) {
    DBIDs ids = DBIDUtil.newHashSet(C); // copy, also to lose distance values!
    Cluster<SubspaceModel> cluster = new Cluster<>(ids);
    cluster.setModel(new SubspaceModel(new Subspace(D), Centroid.make(relation, ids)));
    return cluster;
  }

  /**
   * Computes the quality of a cluster based on its size and number of relevant
   * attributes, as described via the &mu;-function from the paper.
   * 
   * @param clusterSize the size of the cluster.
   * @param numRelevantDimensions the number of dimensions relevant to the
   *        cluster.
   * @return a quality measure (only use this to compare the quality to that
   *         other clusters).
   */
  private double computeClusterQuality(int clusterSize, int numRelevantDimensions) {
    return clusterSize * Math.pow(1. / beta, numRelevantDimensions);
  }

  // ---------------------------------------------------------------------- //

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Florian Nuecke
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Relative density threshold parameter Alpha.
     */
    public static final OptionID ALPHA_ID = new OptionID("doc.alpha", "Minimum relative density for a set of points to be considered a cluster (|C|>=doc.alpha*|S|).");

    /**
     * Balancing parameter for importance of points vs. dimensions
     */
    public static final OptionID BETA_ID = new OptionID("doc.beta", "Preference of cluster size versus number of relevant dimensions (higher value means higher priority on larger clusters).");

    /**
     * Half width parameter.
     */
    public static final OptionID W_ID = new OptionID("doc.w", "Maximum extent of scattering of points along a single attribute for the attribute to be considered relevant.");

    /**
     * Parameter to enable FastDOC heuristics.
     */
    public static final OptionID HEURISTICS_ID = new OptionID("doc.fastdoc", "Use heuristics as described, thus using the FastDOC algorithm (not yet implemented).");

    /**
     * Stopping threshold for FastDOC.
     */
    public static final OptionID D_ZERO_ID = new OptionID("doc.d0", "Parameter for FastDOC, setting the number of relevant attributes which, when found for a cluster, are deemed enough to stop iterating.");

    /**
     * Random seeding parameter.
     */
    public static final OptionID RANDOM_ID = new OptionID("doc.random-seed", "Random seed, for reproducible experiments.");

    /**
     * Relative density threshold parameter Alpha.
     */
    protected double alpha;

    /**
     * Balancing parameter for importance of points vs. dimensions
     */
    protected double beta;

    /**
     * Half width parameter.
     */
    protected double w;

    /**
     * Parameter to enable FastDOC heuristics.
     */
    protected boolean heuristics;

    /**
     * Stopping threshold for FastDOC.
     */
    protected int d_zero;

    /**
     * Random seeding factory.
     */
    protected RandomFactory random = RandomFactory.DEFAULT;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      {
        DoubleParameter param = new DoubleParameter(ALPHA_ID, 0.2) //
        .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
        .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
        if(config.grab(param)) {
          alpha = param.getValue();
        }
      }

      {
        DoubleParameter param = new DoubleParameter(BETA_ID, 0.8) //
        .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
        .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
        if(config.grab(param)) {
          beta = param.getValue();
        }
      }

      {
        DoubleParameter param = new DoubleParameter(W_ID, 0.05) //
        .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
        if(config.grab(param)) {
          w = param.getValue();
        }
      }

      {
        Flag param = new Flag(HEURISTICS_ID);
        if(config.grab(param)) {
          heuristics = param.getValue();
        }
      }

      if(heuristics) {
        IntParameter param = new IntParameter(D_ZERO_ID, 5) //
        .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(param)) {
          d_zero = param.getValue();
        }
      }

      {
        RandomParameter param = new RandomParameter(RANDOM_ID);
        if(config.grab(param)) {
          random = param.getValue();
        }
      }
    }

    @Override
    protected DOC<V> makeInstance() {
      return new DOC<>(alpha, beta, w, heuristics, d_zero, random);
    }
  }
}
