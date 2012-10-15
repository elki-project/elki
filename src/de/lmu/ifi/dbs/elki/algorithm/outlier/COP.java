package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ChiSquaredDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Correlation outlier probability: Outlier Detection in Arbitrarily Oriented
 * Subspaces
 * 
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br />
 * Outlier Detection in Arbitrarily Oriented Subspaces<br />
 * in: Proc. IEEE International Conference on Data Mining (ICDM 2012)
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 * @param <D> Distance type
 */
@Title("COP: Correlation Outlier Probability")
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", title = "Outlier Detection in Arbitrarily Oriented Subspaces", booktitle = "Proc. IEEE International Conference on Data Mining (ICDM 2012)")
public class COP<V extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(COP.class);

  /**
   * Number of neighbors to be considered.
   */
  int k;

  /**
   * Holds the PCA runner.
   */
  private PCARunner<V> pca;

  /**
   * Expected amount of outliers.
   */
  double expect = 0.0001;

  /**
   * Score type.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public enum DistanceDist {
    /**
     * Use chi^2 for score normalization.
     */
    CHISQUARED,
    /**
     * Use gamma distributions for score normalization.
     */
    GAMMA
  }

  /**
   * Type of distribution to assume for distances.
   */
  DistanceDist dist = DistanceDist.CHISQUARED;

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k number of neighbors
   * @param pca PCA computation method
   * @param expect Expected fraction of outliers (for score normalization)
   * @param dist Distance distribution model (ChiSquared, Gamma)
   */
  public COP(DistanceFunction<? super V, D> distanceFunction, int k, PCARunner<V> pca, double expect, DistanceDist dist) {
    super(distanceFunction);
    this.k = k;
    this.pca = pca;
    this.expect = expect;
    this.dist = dist;
  }

  /**
   * Process a single relation.
   * 
   * @param relation Relation to process
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<V> relation) {
    final DBIDs ids = relation.getDBIDs();
    KNNQuery<V, D> knnQuery = QueryUtil.getKNNQuery(relation, getDistanceFunction(), k + 1);

    final int dim = RelationUtil.dimensionality(relation);
    if (k <= dim + 1) {
      LOG.warning("PCA is underspecified with a too low k! k should be at much larger than " + dim);
    }

    WritableDoubleDataStore cop_score = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDataStore<Vector> cop_err_v = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Vector.class);
    WritableIntegerDataStore cop_dim = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, -1);
    // compute neighbors of each db object
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Correlation Outlier Probabilities", relation.size(), LOG) : null;

    for (DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      KNNResult<D> neighbors = knnQuery.getKNNForDBID(id, k + 1);
      ModifiableDBIDs nids = DBIDUtil.newHashSet(neighbors);
      nids.remove(id); // Do not use query object

      Vector centroid = Centroid.make(relation, nids).toVector(relation).getColumnVector();
      Vector relative = relation.get(id).getColumnVector().minusEquals(centroid);

      PCAResult pcares = pca.processIds(nids, relation);
      Matrix evecs = pcares.getEigenvectors();
      Vector projected = evecs.transposeTimes(relative);
      double[] evs = pcares.getEigenvalues();

      double min = Double.POSITIVE_INFINITY;
      int vdim = dim;
      switch(dist) {
      case CHISQUARED: {
        double sqdevs = 0;
        for (int d = 0; d < dim; d++) {
          // Scale with Stddev
          double dev = projected.get(d);
          // Accumulate
          sqdevs += dev * dev / evs[d];
          // Evaluate
          double score = 1 - ChiSquaredDistribution.cdf(sqdevs, d + 1);
          if (score < min) {
            min = score;
            vdim = d + 1;
          }
        }
        break;
      }
      case GAMMA: {
        double[][] dists = new double[dim][nids.size()];
        int j = 0;
        Vector srel = new Vector(dim);
        for (DBIDIter s = nids.iter(); s.valid() && j < nids.size(); s.advance()) {
          V vec = relation.get(s);
          for (int d = 0; d < dim; d++) {
            srel.set(d, vec.doubleValue(d) - centroid.get(d));
          }
          Vector serr = evecs.transposeTimes(srel);
          double sqdist = 0.0;
          for (int d = 0; d < dim; d++) {
            sqdist += serr.get(d) * serr.get(d) / evs[d];
            dists[d][j] = sqdist;
          }
          j++;
        }
        double sqdevs = 0;
        for (int d = 0; d < dim; d++) {
          // Scale with Stddev
          final double dev = projected.get(d);
          // Accumulate
          sqdevs += dev * dev / evs[d];
          // Sort, so we can trim the top 15% below.
          Arrays.sort(dists[d]);
          // Evaluate
          double score = 1 - GammaDistribution.estimate(dists[d], (int) (.85 * dists[d].length)).cdf(sqdevs);
          if (score < min) {
            min = score;
            vdim = d + 1;
          }
        }
        break;
      }
      }
      // Normalize the value
      final double prob = expect * (1 - min) / (expect + min);
      // Construct the error vector:
      for (int d = vdim; d < dim; d++) {
        projected.set(d, 0.0);
      }
      Vector ev = evecs.times(projected).timesEquals(-1 * prob);

      cop_score.putDouble(id, prob);
      cop_err_v.put(id, ev);
      cop_dim.putInt(id, dim + 1 - vdim);

      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if (prog != null) {
      prog.ensureCompleted(LOG);
    }

    // combine results.
    Relation<Double> scoreResult = new MaterializedRelation<Double>("Correlation Outlier Probabilities", "cop-outlier", TypeUtil.DOUBLE, cop_score, ids);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    result.addChildResult(new MaterializedRelation<Integer>("Local Dimensionality", "cop-dim", TypeUtil.INTEGER, cop_dim, ids));
    result.addChildResult(new MaterializedRelation<Vector>("Error vectors", "cop-errorvec", TypeUtil.VECTOR, cop_err_v, ids));
    return result;
  }

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
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, D> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its COP_SCORE, must be an integer greater than
     * 0.
     * <p>
     * Key: {@code -cop.k}
     * </p>
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("cop.k", "The number of nearest neighbors of an object to be considered for computing its COP_SCORE.");

    /**
     * Distribution assumption for distances.
     * <p>
     * Key: {@code -cop.dist}
     * </p>
     */
    public static final OptionID DIST_ID = OptionID.getOrCreateOptionID("cop.dist", "The assumed distribution of squared distances. ChiSquared is faster, Gamma expected to be more accurate but could also overfit.");

    /**
     * Class to compute the PCA with.
     * <p>
     * Key: {@code -cop.pcarunner}
     * </p>
     */
    public static final OptionID PCARUNNER_ID = OptionID.getOrCreateOptionID("cop.pcarunner", "The class to compute (filtered) PCA.");

    /**
     * Expected share of outliers.
     * <p>
     * Key: {@code -cop.expect}
     * 
     * Default: 0.001
     * </p>
     */
    public static final OptionID EXPECT_ID = OptionID.getOrCreateOptionID("cop.expect", "Expected share of outliers. Only affect score normalization.");

    /**
     * Number of neighbors to be considered.
     */
    int k;

    /**
     * Holds the object performing the dependency derivation.
     */
    PCARunner<V> pca;

    /**
     * Distance distributution assumption.
     */
    DistanceDist dist;

    /**
     * Expected amount of outliers.
     */
    double expect;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID);
      kP.addConstraint(new GreaterConstraint(5));
      if (config.grab(kP)) {
        k = kP.intValue();
      }
      EnumParameter<DistanceDist> distP = new EnumParameter<DistanceDist>(DIST_ID, DistanceDist.class, DistanceDist.GAMMA);
      if (config.grab(distP)) {
        dist = distP.getValue();
      }
      DoubleParameter expectP = new DoubleParameter(EXPECT_ID, 0.001);
      expectP.addConstraint(new GreaterConstraint(0));
      expectP.addConstraint(new LessConstraint(1.0));
      if (config.grab(expectP)) {
        expect = expectP.doubleValue();
      }
      ObjectParameter<PCARunner<V>> pcaP = new ObjectParameter<PCARunner<V>>(PCARUNNER_ID, PCARunner.class, PCARunner.class);
      if (config.grab(pcaP)) {
        pca = pcaP.instantiateClass(config);
      }
    }

    @Override
    protected COP<V, D> makeInstance() {
      return new COP<V, D>(distanceFunction, k, pca, expect, dist);
    }
  }
}
