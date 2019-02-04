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
package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.EpanechnikovKernelDensityFunction;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.KernelDensityFunction;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A simple variant of the LOF algorithm, which uses a simple kernel density
 * estimation instead of the local reachability density.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @has - - - KNNQuery
 * @has - - - KernelDensityFunction
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Alias("de.lmu.ifi.dbs.elki.algorithm.outlier.SimpleKernelDensityLOF")
public class SimpleKernelDensityLOF<O extends NumberVector> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SimpleKernelDensityLOF.class);

  /**
   * Parameter k.
   */
  protected int k;

  /**
   * Kernel density function
   */
  private KernelDensityFunction kernel;

  /**
   * Constructor.
   *
   * @param k the value of k
   * @param kernel Kernel function
   */
  public SimpleKernelDensityLOF(int k, DistanceFunction<? super O> distance, KernelDensityFunction kernel) {
    super(distance);
    this.k = k + 1; // + query point
    this.kernel = kernel;
  }

  /**
   * Run the naive kernel density LOF algorithm.
   *
   * @param database Database to query
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("KernelDensityLOF", 3) : null;
    final int dim = RelationUtil.dimensionality(relation);
    DBIDs ids = relation.getDBIDs();

    LOG.beginStep(stepprog, 1, "Materializing neighborhoods w.r.t. distance function.");
    KNNQuery<O> knnq = DatabaseUtil.precomputedKNNQuery(database, relation, getDistanceFunction(), k);

    // Compute LRDs
    LOG.beginStep(stepprog, 2, "Computing densities.");
    WritableDoubleDataStore dens = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress densProgress = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final KNNList neighbors = knnq.getKNNForDBID(it, k);
      int count = 0;
      double sum = 0.0;
      // Fast version for double distances
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, it)) {
          continue;
        }
        double max = knnq.getKNNForDBID(neighbor, k).getKNNDistance();
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
        final KNNList neighbors = knnq.getKNNForDBID(it, k);
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
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Kernel Density Local Outlier Factor", "kernel-density-slof-outlier", lofs, ids);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);

    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new CombinedTypeInformation(getDistanceFunction().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD));
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
     * Option ID for kernel density LOF kernel.
     */
    public static final OptionID KERNEL_ID = new OptionID("kernellof.kernel", "Kernel to use for kernel density LOF.");

    /**
     * The neighborhood size to use.
     */
    protected int k;

    /**
     * Kernel density function parameter
     */
    KernelDensityFunction kernel;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(LOF.Parameterizer.K_ID) //
      .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(pK)) {
        k = pK.getValue();
      }

      ObjectParameter<KernelDensityFunction> kernelP = new ObjectParameter<>(KERNEL_ID, KernelDensityFunction.class, EpanechnikovKernelDensityFunction.class);
      if(config.grab(kernelP)) {
        kernel = kernelP.instantiateClass(config);
      }
    }

    @Override
    protected SimpleKernelDensityLOF<O> makeInstance() {
      return new SimpleKernelDensityLOF<>(k, distanceFunction, kernel);
    }
  }
}
