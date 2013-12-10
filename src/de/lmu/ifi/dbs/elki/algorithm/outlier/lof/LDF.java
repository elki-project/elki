package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.GaussianKernelDensityFunction;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.KernelDensityFunction;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier Detection with Kernel Density Functions.
 * 
 * A variation of LOF which uses kernel density estimation, but in contrast to
 * {@link SimpleKernelDensityLOF} also uses the reachability concept of LOF.
 * 
 * Reference:
 * <p>
 * Outlier Detection with Kernel Density Functions.<br/>
 * L. J. Latecki, A. Lazarevic, D. Pokrajac<br />
 * Machine Learning and Data Mining in Pattern Recognition 2007
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has KNNQuery
 * @apiviz.has KernelDensityFunction
 * 
 * @param <O> the type of objects handled by this Algorithm
 * @param <D> Distance type
 */
@Reference(authors = "L. J. Latecki, A. Lazarevic, D. Pokrajac", title = "Outlier Detection with Kernel Density Functions", booktitle = "Machine Learning and Data Mining in Pattern Recognition", url = "http://dx.doi.org/10.1007/978-3-540-73499-4_6")
public class LDF<O extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
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
  public LDF(int k, DistanceFunction<? super O, D> distance, KernelDensityFunction kernel, double h, double c) {
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

    // "HEAVY" flag for KNN Query since it is used more than once
    KNNQuery<O, D> knnq = QueryUtil.getKNNQuery(relation, getDistanceFunction(), k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      if(stepprog != null) {
        stepprog.beginStep(1, "Materializing neighborhoods w.r.t. distance function.", LOG);
      }
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<>(relation, getDistanceFunction(), k);
      database.addIndex(preproc);
      DistanceQuery<O, D> rdq = database.getDistanceQuery(relation, getDistanceFunction());
      knnq = preproc.getKNNQuery(rdq, k);
    }

    // Compute LDEs
    if(stepprog != null) {
      stepprog.beginStep(2, "Computing LDEs.", LOG);
    }
    WritableDoubleDataStore ldes = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress densProgress = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final KNNList<D> neighbors = knnq.getKNNForDBID(it, k);
      double sum = 0.0;
      int count = 0;
      if(neighbors instanceof DoubleDistanceKNNList) {
        // Fast version for double distances
        for(DoubleDistanceDBIDListIter neighbor = ((DoubleDistanceKNNList) neighbors).iter(); neighbor.valid(); neighbor.advance()) {
          if(DBIDUtil.equal(neighbor, it)) {
            continue;
          }
          final double nkdist = ((DoubleDistanceKNNList) knnq.getKNNForDBID(neighbor, k)).doubleKNNDistance();
          if(nkdist > 0.) {
            final double v = Math.max(nkdist, neighbor.doubleDistance()) / (h * nkdist);
            sum += kernel.density(v) / MathUtil.powi(h * nkdist, dim);
            count++;
          }
          else {
            sum = Double.POSITIVE_INFINITY;
            count++;
            break;
          }
        }
      }
      else {
        for(DistanceDBIDListIter<D> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          if(DBIDUtil.equal(neighbor, it)) {
            continue;
          }
          final double nkdist = knnq.getKNNForDBID(neighbor, k).getKNNDistance().doubleValue();
          if(nkdist > 0.) {
            final double v = Math.max(nkdist, neighbor.getDistance().doubleValue()) / (h * nkdist);
            sum += kernel.density(v) / MathUtil.powi(h * nkdist, dim);
            count++;
          }
          else {
            sum = Double.POSITIVE_INFINITY;
            count++;
            break;
          }
        }
      }
      ldes.putDouble(it, sum / count);
      if(densProgress != null) {
        densProgress.incrementProcessed(LOG);
      }
    }
    if(densProgress != null) {
      densProgress.ensureCompleted(LOG);
    }

    // Compute local density factors.
    if(stepprog != null) {
      stepprog.beginStep(3, "Computing LDFs.", LOG);
    }
    WritableDoubleDataStore ldfs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();

    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("Local Density Factors", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final double lrdp = ldes.doubleValue(it);
      final KNNList<D> neighbors = knnq.getKNNForDBID(it, k);
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
      double ldf = (div > 0) ? sum / div : 0;
      ldfs.putDouble(it, ldf);
      // update minimum and maximum
      lofminmax.put(ldf);

      if(progressLOFs != null) {
        progressLOFs.incrementProcessed(LOG);
      }
    }
    if(progressLOFs != null) {
      progressLOFs.ensureCompleted(LOG);
    }

    if(stepprog != null) {
      stepprog.setCompleted(LOG);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<>("Local Density Factor", "ldf-outlier", TypeUtil.DOUBLE, ldfs, ids);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, 1. / c, 1 / (1 + c));
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
   * @apiviz.exclude
   * 
   * @param <O> vector type
   * @param <D> distance type
   */
  public static class Parameterizer<O extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
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

      final IntParameter pK = new IntParameter(K_ID);
      pK.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
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
    protected LDF<O, D> makeInstance() {
      return new LDF<>(k, distanceFunction, kernel, h, c);
    }
  }
}
