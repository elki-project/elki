package de.lmu.ifi.dbs.elki.algorithm.outlier;

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

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.MaskedDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Outlier detection algorithm using a mixture model approach. The data is
 * modeled as a mixture of two distributions, a Gaussian distribution for
 * ordinary data and a uniform distribution for outliers. At first all Objects
 * are in the set of normal objects and the set of anomalous objects is empty.
 * An iterative procedure then transfers objects from the ordinary set to the
 * anomalous set if the transfer increases the overall likelihood of the data.
 *
 * Reference:
 * <p>
 * E. Eskin<br />
 * Anomaly detection over noisy data using learned probability distributions.<br />
 * In Proc. of the Seventeenth International Conference on Machine Learning
 * (ICML-2000).
 * </p>
 *
 * @author Lisa Reichert
 *
 * @param <V> Vector Type
 */
@Title("Gaussian-Uniform Mixture Model Outlier Detection")
@Description("Fits a mixture model consisting of a Gaussian and a uniform distribution to the data.")
@Reference(prefix = "Generalization using the likelihood gain as outlier score of", //
authors = "E. Eskin", //
title = "Anomaly detection over noisy data using learned probability distributions", //
booktitle = "Proc. of the Seventeenth International Conference on Machine Learning (ICML-2000)")
public class GaussianUniformMixture<V extends NumberVector> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(GaussianUniformMixture.class);

  /**
   * Small value to increment diagonally of a matrix in order to avoid
   * singularity before building the inverse.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

  /**
   * Holds the cutoff value.
   */
  private double c;

  /**
   * log(l) precomputed
   */
  private double logl;

  /**
   * log(1-l) precomputed
   */
  private double logml;

  /**
   * Constructor with parameters.
   *
   * @param l l value
   * @param c c value
   */
  public GaussianUniformMixture(double l, double c) {
    super();
    this.logl = Math.log(l);
    this.logml = Math.log(1 - l);
    this.c = c;
  }

  /**
   * Run the algorithm
   *
   * @param relation Data relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<V> relation) {
    // Use an array list of object IDs for fast random access by an offset
    ArrayDBIDs objids = DBIDUtil.ensureArray(relation.getDBIDs());
    // A bit set to flag objects as anomalous, none at the beginning
    long[] bits = BitsUtil.zero(objids.size());
    // Positive masked collection
    DBIDs normalObjs = new MaskedDBIDs(objids, bits, true);
    // Positive masked collection
    DBIDs anomalousObjs = new MaskedDBIDs(objids, bits, false);
    // resulting scores
    WritableDoubleDataStore oscores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    // compute loglikelihood
    double logLike = relation.size() * logml + loglikelihoodNormal(normalObjs, relation);
    // LOG.debugFine("normalsize   " + normalObjs.size() + " anormalsize  " +
    // anomalousObjs.size() + " all " + (anomalousObjs.size() +
    // normalObjs.size()));
    // LOG.debugFine(logLike + " loglike beginning" +
    // loglikelihoodNormal(normalObjs, database));
    DoubleMinMax minmax = new DoubleMinMax();

    DBIDIter iter = objids.iter();
    for(int i = 0; i < objids.size(); i++, iter.advance()) {
      // LOG.debugFine("i     " + i);
      // Change mask to make the current object anomalous
      BitsUtil.setI(bits, i);
      // Compute new likelihoods
      double currentLogLike = normalObjs.size() * logml + loglikelihoodNormal(normalObjs, relation) + anomalousObjs.size() * logl + loglikelihoodAnomalous(anomalousObjs);

      // if the loglike increases more than a threshold, object stays in
      // anomalous set and is flagged as outlier
      final double loglikeGain = currentLogLike - logLike;
      oscores.putDouble(iter, loglikeGain);
      minmax.put(loglikeGain);

      if(loglikeGain > c) {
        // flag as outlier
        // LOG.debugFine("Outlier: " + curid + " " + (currentLogLike -
        // logLike));
        // Update best logLike
        logLike = currentLogLike;
      }
      else {
        // LOG.debugFine("Inlier: " + curid + " " + (currentLogLike - logLike));
        // undo bit set
        BitsUtil.clearI(bits, i);
      }
    }

    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0);
    DoubleRelation res = new MaterializedDoubleRelation("Gaussian Mixture Outlier Score", "gaussian-mixture-outlier", oscores, relation.getDBIDs());
    return new OutlierResult(meta, res);
  }

  /**
   * Loglikelihood anomalous objects. Uniform distribution
   *
   * @param anomalousObjs
   * @return loglikelihood for anomalous objects
   */
  private double loglikelihoodAnomalous(DBIDs anomalousObjs) {
    int n = anomalousObjs.size();
    return n * Math.log(1.0 / n);
  }

  /**
   * Computes the loglikelihood of all normal objects. Gaussian model
   *
   * @param objids Object IDs for 'normal' objects.
   * @param database Database
   * @return loglikelihood for normal objects
   */
  private double loglikelihoodNormal(DBIDs objids, Relation<V> database) {
    if(objids.isEmpty()) {
      return 0;
    }
    CovarianceMatrix builder = CovarianceMatrix.make(database, objids);
    Vector mean = builder.getMeanVector();
    Matrix covarianceMatrix = builder.destroyToSampleMatrix();

    // test singulaere matrix
    Matrix covInv = covarianceMatrix.cheatToAvoidSingularity(SINGULARITY_CHEAT).inverse();

    double covarianceDet = covarianceMatrix.det();
    double fakt = 1.0 / Math.sqrt(MathUtil.powi(MathUtil.TWOPI, RelationUtil.dimensionality(database)) * covarianceDet);
    // for each object compute probability and sum
    double prob = 0;
    for(DBIDIter iter = objids.iter(); iter.valid(); iter.advance()) {
      Vector x = database.get(iter).getColumnVector().minusEquals(mean);
      double mDist = x.transposeTimesTimes(covInv, x);
      prob += Math.log(fakt * Math.exp(-mDist / 2.0));
    }
    return prob;
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
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter to specify the fraction of expected outliers.
     */
    public static final OptionID L_ID = new OptionID("mmo.l", "expected fraction of outliers");

    /**
     * Parameter to specify the cutoff.
     */
    public static final OptionID C_ID = new OptionID("mmo.c", "cutoff");

    protected double l = 1E-7;

    protected double c = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter lP = new DoubleParameter(C_ID, 1E-7);
      if(config.grab(lP)) {
        l = lP.getValue();
      }
      final DoubleParameter cP = new DoubleParameter(C_ID, 1E-7);
      if(config.grab(cP)) {
        c = cP.getValue();
      }
    }

    @Override
    protected GaussianUniformMixture<V> makeInstance() {
      return new GaussianUniformMixture<>(l, c);
    }
  }
}