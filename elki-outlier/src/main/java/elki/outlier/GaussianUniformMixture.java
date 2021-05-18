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
package elki.outlier;

import static elki.math.linearalgebra.VMath.minusEquals;
import static elki.math.linearalgebra.VMath.transposeTimesTimes;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.math.linearalgebra.LUDecomposition;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Outlier detection algorithm using a mixture model approach. The data is
 * modeled as a mixture of two distributions, a Gaussian distribution for
 * ordinary data and a uniform distribution for outliers. At first all Objects
 * are in the set of normal objects and the set of anomalous objects is empty.
 * An iterative procedure then transfers objects from the ordinary set to the
 * anomalous set if the transfer increases the overall likelihood of the data.
 * <p>
 * Reference:
 * <p>
 * E. Eskin<br>
 * Anomaly detection over noisy data using learned probability distributions<br>
 * Proc. 17th Int. Conf. on Machine Learning (ICML-2000)
 *
 * @author Lisa Reichert
 * @since 0.3
 */
@Title("Gaussian-Uniform Mixture Model Outlier Detection")
@Description("Fits a mixture model consisting of a Gaussian and a uniform distribution to the data.")
@Reference(prefix = "Generalization using the likelihood gain as outlier score of", //
    authors = "E. Eskin", //
    title = "Anomaly detection over noisy data using learned probability distributions", //
    booktitle = "Proc. 17th Int. Conf. on Machine Learning (ICML-2000)", //
    url = "https://doi.org/10.7916/D8C53SKF", //
    bibkey = "DBLP:conf/icml/Eskin00")
public class GaussianUniformMixture implements OutlierAlgorithm {
  /**
   * Maximum number of iterations to do.
   */
  private static final int MAX_ITER = 10;

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
    this.logl = FastMath.log(l);
    this.logml = FastMath.log(1 - l);
    this.c = c;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Run the algorithm
   *
   * @param relation Data relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<? extends NumberVector> relation) {
    // Use an array list of object IDs for fast random access by an offset
    ArrayDBIDs objids = DBIDUtil.ensureArray(relation.getDBIDs());
    // Build entire covariance matrix:
    CovarianceMatrix builder = CovarianceMatrix.make(relation, objids);

    // Anomalous objects
    HashSetModifiableDBIDs anomalous = DBIDUtil.newHashSet();
    // resulting scores
    WritableDoubleDataStore oscores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    // compute loglikelihood
    double logLike = objids.size() * logml + loglikelihoodNormal(objids, anomalous, builder, relation);

    DoubleMinMax minmax = new DoubleMinMax();
    for(int loop = 0; loop < MAX_ITER; ++loop) {
      boolean changed = false;
      for(DBIDIter iter = objids.iter(); iter.valid(); iter.advance()) {
        // Change mask to make the current Fobject anomalous
        boolean wasadded = anomalous.add(iter) || !anomalous.remove(iter);
        NumberVector vec = relation.get(iter);
        builder.put(vec, wasadded ? -1 : +1); // Remove
        // Compute new likelihoods
        double currentLogLike = (objids.size() - anomalous.size()) * logml + loglikelihoodNormal(objids, anomalous, builder, relation) //
            + anomalous.size() * logl + loglikelihoodAnomalous(anomalous);

        // if the loglike increases more than a threshold, the object remains in
        // the assigned state:
        final double loglikeGain = currentLogLike - logLike;
        oscores.putDouble(iter, loglikeGain);
        minmax.put(loglikeGain);

        if(loglikeGain > c) {
          logLike = currentLogLike;
          changed = true;
        }
        else {
          // remove from anomalous again
          wasadded = wasadded ? anomalous.remove(iter) : !anomalous.add(iter);
          builder.put(vec, wasadded ? +1 : -1);
        }
      }
      assert anomalous.size() == Math.round(objids.size() - builder.getWeight());
      if(!changed) {
        break;
      }
    }

    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0);
    DoubleRelation res = new MaterializedDoubleRelation("Gaussian Mixture Outlier Score", relation.getDBIDs(), oscores);
    return new OutlierResult(meta, res);
  }

  /**
   * Loglikelihood anomalous objects. Uniform distribution.
   *
   * @param anomalousObjs
   * @return loglikelihood for anomalous objects
   */
  private double loglikelihoodAnomalous(DBIDs anomalousObjs) {
    return anomalousObjs.isEmpty() ? 0 : anomalousObjs.size() * -FastMath.log(anomalousObjs.size());
  }

  /**
   * Computes the loglikelihood of all normal objects. Gaussian model
   *
   * @param objids Object IDs for 'normal' objects.
   * @param builder Covariance matrix builder
   * @param relation Database
   * @return loglikelihood for normal objects
   */
  private double loglikelihoodNormal(DBIDs objids, SetDBIDs anomalous, CovarianceMatrix builder, Relation<? extends NumberVector> relation) {
    double[] mean = builder.getMeanVector();
    final LUDecomposition lu = new LUDecomposition(builder.makeSampleMatrix());
    double[][] covInv = lu.inverse();
    // for each object compute probability and sum
    double prob = (objids.size() - anomalous.size()) * -FastMath.log(Math.sqrt(MathUtil.powi(MathUtil.TWOPI, RelationUtil.dimensionality(relation)) * lu.det()));
    for(DBIDIter iter = objids.iter(); iter.valid(); iter.advance()) {
      if(!anomalous.contains(iter)) {
        double[] xcent = minusEquals(relation.get(iter).toArray(), mean);
        prob -= .5 * transposeTimesTimes(xcent, covInv, xcent);
      }
    }
    return prob;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the fraction of expected outliers.
     */
    public static final OptionID L_ID = new OptionID("mmo.l", "expected fraction of outliers");

    /**
     * Parameter to specify the cutoff.
     */
    public static final OptionID C_ID = new OptionID("mmo.c", "cutoff");

    /**
     * Expected outlier fraction
     */
    protected double l = 1E-7;

    /**
     * Cutoff parameter
     */
    protected double c = 1E-7;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(L_ID, 1E-7) //
          .grab(config, x -> l = x);
      new DoubleParameter(C_ID, 1E-7) //
          .grab(config, x -> c = x);
    }

    @Override
    public GaussianUniformMixture make() {
      return new GaussianUniformMixture(l, c);
    }
  }
}
