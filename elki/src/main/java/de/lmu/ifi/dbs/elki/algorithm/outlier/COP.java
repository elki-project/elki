package de.lmu.ifi.dbs.elki.algorithm.outlier;

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
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ChiSquaredDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.GammaChoiWetteEstimator;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
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
 */
@Title("COP: Correlation Outlier Probability")
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
title = "Outlier Detection in Arbitrarily Oriented Subspaces", //
booktitle = "Proc. IEEE International Conference on Data Mining (ICDM 2012)", //
url = "http://dx.doi.org/10.1109/ICDM.2012.21")
public class COP<V extends NumberVector> extends AbstractDistanceBasedAlgorithm<V, OutlierResult>implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(COP.class);

  /**
   * Result name for the COP outlier scores.
   */
  public static final String COP_SCORES = "cop-outlier";

  /**
   * Result name for the dimensionality.
   */
  public static final String COP_DIM = "cop-dim";

  /**
   * Result name for the error vectors.
   */
  public static final String COP_ERRORVEC = "cop-errorvec";

  /**
   * A clone of
   * {@link de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter}
   * that only uses the first 85% of the array!
   */
  private static final NumberArrayAdapter<Double, double[]> SHORTENED_ARRAY = new NumberArrayAdapter<Double, double[]>() {
    @Override
    public int size(double[] array) {
      return (int) (.85 * array.length);
    }

    @Override
    public Double get(double[] array, int off) throws IndexOutOfBoundsException {
      return Double.valueOf(array[off]);
    }

    @Override
    public double getDouble(double[] array, int off) throws IndexOutOfBoundsException {
      return array[off];
    }

    @Override
    public float getFloat(double[] array, int off) throws IndexOutOfBoundsException {
      return (float) array[off];
    }

    @Override
    public int getInteger(double[] array, int off) throws IndexOutOfBoundsException {
      return (int) array[off];
    }

    @Override
    public short getShort(double[] array, int off) throws IndexOutOfBoundsException {
      return (short) array[off];
    }

    @Override
    public long getLong(double[] array, int off) throws IndexOutOfBoundsException {
      return (long) array[off];
    }

    @Override
    public byte getByte(double[] array, int off) throws IndexOutOfBoundsException {
      return (byte) array[off];
    }
  };

  /**
   * Number of neighbors to be considered.
   */
  int k;

  /**
   * Holds the PCA runner.
   */
  private PCARunner pca;

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
    CHISQUARED, /**
                 * Use gamma distributions for score normalization.
                 */
    GAMMA
  }

  /**
   * Type of distribution to assume for distances.
   */
  DistanceDist dist = DistanceDist.CHISQUARED;

  /**
   * Include models in output.
   */
  boolean models;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k number of neighbors
   * @param pca PCA computation method
   * @param expect Expected fraction of outliers (for score normalization)
   * @param dist Distance distribution model (ChiSquared, Gamma)
   * @param models Report models
   */
  public COP(DistanceFunction<? super V> distanceFunction, int k, PCARunner pca, double expect, DistanceDist dist, boolean models) {
    super(distanceFunction);
    this.k = k;
    this.pca = pca;
    this.expect = expect;
    this.dist = dist;
    this.models = models;
  }

  /**
   * Process a single relation.
   *
   * @param relation Relation to process
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<V> relation) {
    final DBIDs ids = relation.getDBIDs();
    KNNQuery<V> knnQuery = QueryUtil.getKNNQuery(relation, getDistanceFunction(), k + 1);

    final int dim = RelationUtil.dimensionality(relation);
    if(k <= dim + 1) {
      LOG.warning("PCA is underspecified with a too low k! k should be at much larger than " + dim);
    }

    WritableDoubleDataStore cop_score = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDataStore<Vector> cop_err_v = null;
    WritableIntegerDataStore cop_dim = null;
    if(models) {
      cop_err_v = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Vector.class);
      cop_dim = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, -1);
    }
    // compute neighbors of each db object
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Correlation Outlier Probabilities", relation.size(), LOG) : null;

    for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      KNNList neighbors = knnQuery.getKNNForDBID(id, k + 1);
      ModifiableDBIDs nids = DBIDUtil.newHashSet(neighbors);
      nids.remove(id); // Do not use query object

      Vector centroid = Centroid.make(relation, nids);
      Vector relative = relation.get(id).getColumnVector().minusEquals(centroid);

      PCAResult pcares = pca.processIds(nids, relation);
      Matrix evecs = pcares.getEigenvectors();
      Vector projected = evecs.transposeTimes(relative);
      double[] evs = pcares.getEigenvalues();

      double min = Double.POSITIVE_INFINITY;
      int vdim = dim;
      switch(dist){
      case CHISQUARED: {
        double sqdevs = 0;
        for(int d = 0; d < dim; d++) {
          // Scale with Stddev
          double dev = projected.get(d);
          // Accumulate
          sqdevs += dev * dev / evs[d];
          // Evaluate
          double score = 1 - ChiSquaredDistribution.cdf(sqdevs, d + 1);
          if(score < min) {
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
        for(DBIDIter s = nids.iter(); s.valid() && j < nids.size(); s.advance()) {
          V vec = relation.get(s);
          for(int d = 0; d < dim; d++) {
            srel.set(d, vec.doubleValue(d) - centroid.get(d));
          }
          Vector serr = evecs.transposeTimes(srel);
          double sqdist = 0.0;
          for(int d = 0; d < dim; d++) {
            sqdist += serr.get(d) * serr.get(d) / evs[d];
            dists[d][j] = sqdist;
          }
          j++;
        }
        double sqdevs = 0;
        for(int d = 0; d < dim; d++) {
          // Scale with Stddev
          final double dev = projected.get(d);
          // Accumulate
          sqdevs += dev * dev / evs[d];
          // Sort, so we can trim the top 15% below.
          Arrays.sort(dists[d]);
          // Evaluate
          double score = 1 - GammaChoiWetteEstimator.STATIC.estimate(dists[d], SHORTENED_ARRAY).cdf(sqdevs);
          if(score < min) {
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
      for(int d = vdim; d < dim; d++) {
        projected.set(d, 0.0);
      }
      Vector ev = evecs.times(projected).timesEquals(-1 * prob);

      cop_score.putDouble(id, prob);
      if(models) {
        cop_err_v.put(id, ev);
        cop_dim.putInt(id, dim + 1 - vdim);
      }

      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    // combine results.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Correlation Outlier Probabilities", COP_SCORES, cop_score, ids);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    if(models) {
      result.addChildResult(new MaterializedRelation<>("Local Dimensionality", COP_DIM, TypeUtil.INTEGER, cop_dim, ids));
      result.addChildResult(new MaterializedRelation<>("Error vectors", COP_ERRORVEC, TypeUtil.VECTOR, cop_err_v, ids));
    }
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
  public static class Parameterizer<V extends NumberVector> extends AbstractDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its COP_SCORE, must be an integer greater than
     * 0.
     * <p>
     * Key: {@code -cop.k}
     * </p>
     */
    public static final OptionID K_ID = new OptionID("cop.k", "The number of nearest neighbors of an object to be considered for computing its COP_SCORE.");

    /**
     * Distribution assumption for distances.
     * <p>
     * Key: {@code -cop.dist}
     * </p>
     */
    public static final OptionID DIST_ID = new OptionID("cop.dist", "The assumed distribution of squared distances. ChiSquared is faster, Gamma expected to be more accurate but could also overfit.");

    /**
     * Class to compute the PCA with.
     * <p>
     * Key: {@code -cop.pcarunner}
     * </p>
     */
    public static final OptionID PCARUNNER_ID = new OptionID("cop.pcarunner", "The class to compute (filtered) PCA.");

    /**
     * Expected share of outliers.
     * <p>
     * Key: {@code -cop.expect}
     *
     * Default: 0.001
     * </p>
     */
    public static final OptionID EXPECT_ID = new OptionID("cop.expect", "Expected share of outliers. Only affect score normalization.");

    /**
     * Include COP error vectors in output.
     * <p>
     * Key: {@code -cop.models}
     *
     * Default: off
     * </p>
     */
    public static final OptionID MODELS_ID = new OptionID("cop.models", "Include COP models (error vectors) in output. This needs more memory.");

    /**
     * Number of neighbors to be considered.
     */
    int k;

    /**
     * Holds the object performing the dependency derivation.
     */
    PCARunner pca;

    /**
     * Distance distributution assumption.
     */
    DistanceDist dist;

    /**
     * Expected amount of outliers.
     */
    double expect;

    /**
     * Include COP models
     */
    boolean models = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID);
      kP.addConstraint(new GreaterConstraint(5));
      if(config.grab(kP)) {
        k = kP.intValue();
      }
      EnumParameter<DistanceDist> distP = new EnumParameter<>(DIST_ID, DistanceDist.class, DistanceDist.GAMMA);
      if(config.grab(distP)) {
        dist = distP.getValue();
      }
      DoubleParameter expectP = new DoubleParameter(EXPECT_ID, 0.001);
      expectP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      expectP.addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      if(config.grab(expectP)) {
        expect = expectP.doubleValue();
      }
      ObjectParameter<PCARunner> pcaP = new ObjectParameter<>(PCARUNNER_ID, PCARunner.class, PCARunner.class);
      if(config.grab(pcaP)) {
        pca = pcaP.instantiateClass(config);
      }
      Flag modelsF = new Flag(MODELS_ID);
      if(config.grab(modelsF)) {
        models = modelsF.isTrue();
      }
    }

    @Override
    protected COP<V> makeInstance() {
      return new COP<>(distanceFunction, k, pca, expect, dist, models);
    }
  }
}
