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
package elki.outlier.lof;

import elki.Algorithm;
import elki.data.NumberVector;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.math.statistics.kernelfunctions.EpanechnikovKernelDensityFunction;
import elki.math.statistics.kernelfunctions.KernelDensityFunction;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.QuotientOutlierScoreMeta;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A simple variant of the LOF algorithm, which uses a simple kernel density
 * estimation instead of the local reachability density.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @has - - - KNNSearcher
 * @has - - - KernelDensityFunction
 *
 * @param <O> the type of objects handled by this algorithm
 */
public class SimpleKernelDensityLOF<O extends NumberVector> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SimpleKernelDensityLOF.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Number of neighbors + the query point
   */
  protected int kplus;

  /**
   * Kernel density function
   */
  protected KernelDensityFunction kernel;

  /**
   * Constructor.
   *
   * @param k number of neighbors
   * @param kernel Kernel function
   */
  public SimpleKernelDensityLOF(int k, Distance<? super O> distance, KernelDensityFunction kernel) {
    super();
    this.distance = distance;
    this.kplus = k + 1; // + query point
    this.kernel = kernel;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // FIXME: it could be a non-numeric field, too.
    return TypeUtil.array(new CombinedTypeInformation(distance.getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD));
  }

  /**
   * Run the naive kernel density LOF algorithm.
   *
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("KernelDensityLOF", 3) : null;
    final int dim = RelationUtil.dimensionality(relation);
    DBIDs ids = relation.getDBIDs();

    LOG.beginStep(stepprog, 1, "Materializing neighborhoods w.r.t. distance function.");
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).precomputed().kNNByDBID(kplus);

    // Compute LRDs
    LOG.beginStep(stepprog, 2, "Computing densities.");
    WritableDoubleDataStore dens = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress densProgress = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final KNNList neighbors = knnq.getKNN(it, kplus);
      int count = 0;
      double sum = 0.0;
      // Fast version for double distances
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, it)) {
          continue;
        }
        double max = knnq.getKNN(neighbor, kplus).getKNNDistance();
        if(max == 0) {
          sum = Double.POSITIVE_INFINITY;
          break;
        }
        final double v = neighbor.doubleValue() / max;
        sum += kernel.density(v) / MathUtil.powi(max, dim);
        count++;
      }
      final double density = count > 0 ? sum / count : 0.;
      dens.putDouble(it, density);
      LOG.incrementProcessed(densProgress);
    }
    LOG.ensureCompleted(densProgress);

    // compute LOF_SCORE of each db object
    LOG.beginStep(stepprog, 3, "Computing KLOFs.");
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();

    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("KLOF_SCORE for objects", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final double lrdp = dens.doubleValue(it);
      final double lof;
      if(lrdp > 0) {
        final KNNList neighbors = knnq.getKNN(it, kplus);
        double sum = 0.0;
        int count = 0;
        for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          // skip the point itself
          if(DBIDUtil.equal(neighbor, it)) {
            continue;
          }
          sum += dens.doubleValue(neighbor);
          count++;
        }
        lof = (lrdp == Double.POSITIVE_INFINITY) ? (sum == Double.POSITIVE_INFINITY ? 1 : 0.) : sum / (count * lrdp);
      }
      else {
        lof = 1.0;
      }
      lofs.putDouble(it, lof);
      // update minimum and maximum
      lofminmax.put(lof);

      LOG.incrementProcessed(progressLOFs);
    }
    LOG.ensureCompleted(progressLOFs);

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Kernel Density Local Outlier Factor", ids, lofs);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);

    return result;
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
  public static class Par<O extends NumberVector> implements Parameterizer {
    /**
     * Option ID for kernel density LOF kernel.
     */
    public static final OptionID KERNEL_ID = new OptionID("kernellof.kernel", "Kernel to use for kernel density LOF.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * The neighborhood size to use.
     */
    protected int k;

    /**
     * Kernel density function parameter
     */
    protected KernelDensityFunction kernel;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(LOF.Par.K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> k = x);
      new ObjectParameter<KernelDensityFunction>(KERNEL_ID, KernelDensityFunction.class, EpanechnikovKernelDensityFunction.class) //
          .grab(config, x -> kernel = x);
    }

    @Override
    public SimpleKernelDensityLOF<O> make() {
      return new SimpleKernelDensityLOF<>(k, distance, kernel);
    }
  }
}
