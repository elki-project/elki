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
package elki.algorithm.outlier.lof;

import elki.algorithm.AbstractDistanceBasedAlgorithm;
import elki.algorithm.outlier.OutlierAlgorithm;
import elki.data.NumberVector;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.DatabaseUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.distancefunction.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.math.statistics.kernelfunctions.GaussianKernelDensityFunction;
import elki.math.statistics.kernelfunctions.KernelDensityFunction;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier Detection with Kernel Density Functions.
 * <p>
 * A variation of LOF which uses kernel density estimation, but in contrast to
 * {@link SimpleKernelDensityLOF} also uses the reachability concept of LOF.
 * <p>
 * Reference:
 * <p>
 * Outlier Detection with Kernel Density Functions<br>
 * L. J. Latecki, A. Lazarevic, D. Pokrajac<br>
 * Machine Learning and Data Mining in Pattern Recognition
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @has - - - KNNQuery
 * @has - - - KernelDensityFunction
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("LDF: Outlier Detection with Kernel Density Functions")
@Reference(authors = "L. J. Latecki, A. Lazarevic, D. Pokrajac", //
    title = "Outlier Detection with Kernel Density Functions", //
    booktitle = "Machine Learning and Data Mining in Pattern Recognition", //
    url = "https://doi.org/10.1007/978-3-540-73499-4_6", //
    bibkey = "DBLP:conf/mldm/LateckiLP07")
public class LDF<O extends NumberVector> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LDF.class);

  /**
   * Parameter k.
   */
  protected int k;

  /**
   * Bandwidth scaling factor.
   */
  protected double h = 1;

  /**
   * Scaling constant, to limit value range to 1/c
   */
  protected double c = 0.1;

  /**
   * Kernel density function
   */
  private KernelDensityFunction kernel;

  /**
   * Constructor.
   *
   * @param k the value of k
   * @param kernel Kernel function
   * @param h Kernel bandwidth scaling
   * @param c Score scaling parameter
   */
  public LDF(int k, Distance<? super O> distance, KernelDensityFunction kernel, double h, double c) {
    super(distance);
    this.k = k + 1;
    this.kernel = kernel;
    this.h = h;
    this.c = c;
  }

  /**
   * Run the naive kernel density LOF algorithm.
   *
   * @param database Database to query
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("LDF", 3) : null;
    final int dim = RelationUtil.dimensionality(relation);
    DBIDs ids = relation.getDBIDs();

    LOG.beginStep(stepprog, 1, "Materializing neighborhoods w.r.t. distance function.");
    KNNQuery<O> knnq = DatabaseUtil.precomputedKNNQuery(database, relation, getDistance(), k);

    // Compute LDEs
    LOG.beginStep(stepprog, 2, "Computing LDEs.");
    WritableDoubleDataStore ldes = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress densProgress = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final KNNList neighbors = knnq.getKNNForDBID(it, k);
      double sum = 0.0;
      int count = 0;
      // Fast version for double distances
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, it)) {
          continue;
        }
        final double nkdist = knnq.getKNNForDBID(neighbor, k).getKNNDistance();
        if(!(nkdist > 0.) || nkdist == Double.POSITIVE_INFINITY) {
          sum = Double.POSITIVE_INFINITY;
          count++;
          break;
        }
        final double v = MathUtil.max(nkdist, neighbor.doubleValue()) / (h * nkdist);
        sum += kernel.density(v) / MathUtil.powi(h * nkdist, dim);
        count++;
      }
      ldes.putDouble(it, sum / count);
      LOG.incrementProcessed(densProgress);
    }
    LOG.ensureCompleted(densProgress);

    // Compute local density factors.
    LOG.beginStep(stepprog, 3, "Computing LDFs.");
    WritableDoubleDataStore ldfs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();

    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("Local Density Factors", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final double lrdp = ldes.doubleValue(it);
      final KNNList neighbors = knnq.getKNNForDBID(it, k);
      double sum = 0.0;
      int count = 0;
      for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        // skip the point itself
        if(DBIDUtil.equal(neighbor, it)) {
          continue;
        }
        sum += ldes.doubleValue(neighbor);
        count++;
      }
      sum /= count;
      final double div = lrdp + c * sum;
      double ldf = div == Double.POSITIVE_INFINITY ? (sum < Double.POSITIVE_INFINITY ? 0. : 1) : (div > 0) ? sum / div : 0;
      ldfs.putDouble(it, ldf);
      // update minimum and maximum
      lofminmax.put(ldf);

      LOG.incrementProcessed(progressLOFs);
    }
    LOG.ensureCompleted(progressLOFs);

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Local Density Factor", ids, ldfs);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, 1. / c, 1 / (1 + c));
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);

    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new CombinedTypeInformation(getDistance().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD));
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
   * @hidden
   *
   * @param <O> vector type
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Option ID for kernel.
     */
    public static final OptionID KERNEL_ID = new OptionID("ldf.kernel", "Kernel to use for LDF.");

    /**
     * Option ID for k
     */
    public static final OptionID K_ID = new OptionID("ldf.k", "Number of neighbors to use for LDF.");

    /**
     * Option ID for h - kernel bandwidth scaling
     */
    public static final OptionID H_ID = new OptionID("ldf.h", "Kernel bandwidth multiplier for LDF.");

    /**
     * Option ID for c
     */
    public static final OptionID C_ID = new OptionID("ldf.c", "Score scaling parameter for LDF.");

    /**
     * The neighborhood size to use.
     */
    protected int k = 2;

    /**
     * Kernel density function parameter
     */
    KernelDensityFunction kernel;

    /**
     * Bandwidth scaling factor.
     */
    protected double h = 1;

    /**
     * Scaling constant, to limit value range to 1/c
     */
    protected double c = 0.1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(pK)) {
        k = pK.getValue();
      }

      ObjectParameter<KernelDensityFunction> kernelP = new ObjectParameter<>(KERNEL_ID, KernelDensityFunction.class, GaussianKernelDensityFunction.class);
      if(config.grab(kernelP)) {
        kernel = kernelP.instantiateClass(config);
      }

      DoubleParameter hP = new DoubleParameter(H_ID);
      if(config.grab(hP)) {
        h = hP.doubleValue();
      }

      DoubleParameter cP = new DoubleParameter(C_ID, 0.1);
      if(config.grab(cP)) {
        c = cP.doubleValue();
      }
    }

    @Override
    protected LDF<O> makeInstance() {
      return new LDF<>(k, distanceFunction, kernel, h, c);
    }
  }
}
