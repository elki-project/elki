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
package elki.outlier.lof;

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.NumberVector;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.DatabaseUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.math.MeanVariance;
import elki.math.statistics.distribution.NormalDistribution;
import elki.math.statistics.kernelfunctions.GaussianKernelDensityFunction;
import elki.math.statistics.kernelfunctions.KernelDensityFunction;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.ProbabilisticOutlierScore;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Generalized Outlier Detection with Flexible Kernel Density Estimates.
 * <p>
 * This is an outlier detection inspired by LOF, but using kernel density
 * estimation (KDE) from statistics. Unfortunately, for higher dimensional data,
 * kernel density estimation itself becomes difficult. At this point, the
 * <tt>kdeos.idim</tt> parameter can become useful, which allows to either
 * disable dimensionality adjustment completely (<tt>0</tt>) or to set it to a
 * lower dimensionality than the data representation. This may sound like a hack
 * at first, but real data is often of lower intrinsic dimensionality, and
 * embedded into a higher data representation. Adjusting the kernel to account
 * for the representation seems to yield worse results than using a lower,
 * intrinsic, dimensionality.
 * <p>
 * If your data set has many duplicates, the <tt>kdeos.kernel.minbw</tt>
 * parameter sets a minimum kernel bandwidth, which may improve results in these
 * cases, as it prevents kernels from degenerating to single points.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
 * Generalized Outlier Detection with Flexible Kernel Density Estimates<br>
 * Proc. 14th SIAM International Conference on Data Mining (SDM 2014)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - KNNQuery
 * @has - - - KernelDensityFunction
 *
 * @param <O> Object type
 */
@Title("KDEOS: Kernel Density Estimator Outlier Score")
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Generalized Outlier Detection with Flexible Kernel Density Estimates", //
    booktitle = "Proc. 14th SIAM International Conference on Data Mining (SDM 2014)", //
    url = "https://doi.org/10.1137/1.9781611973440.63", //
    bibkey = "DBLP:conf/sdm/SchubertZK14")
public class KDEOS<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KDEOS.class);

  /**
   * Kernel function to use for density estimation.
   */
  KernelDensityFunction kernel;

  /**
   * Minimum and maximum number of neighbors to use.
   */
  int kmin, kmax;

  /**
   * Kernel scaling parameter.
   */
  double scale;

  /**
   * Kernel minimum bandwidth.
   */
  double minBandwidth = 1e-6;

  /**
   * Intrinsic dimensionality.
   */
  int idim = -1;

  /**
   * Significance cutoff when computing kernel density.
   */
  final static double CUTOFF = 1e-20;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param kmin Minimum number of neighbors
   * @param kmax Maximum number of neighbors
   * @param kernel Kernel function
   * @param minBandwidth Minimum bandwidth
   * @param scale Kernel scaling parameter
   * @param idim Intrinsic dimensionality (use 0 to use real dimensionality)
   */
  public KDEOS(Distance<? super O> distanceFunction, int kmin, int kmax, KernelDensityFunction kernel, double minBandwidth, double scale, int idim) {
    super(distanceFunction);
    this.kmin = kmin;
    this.kmax = kmax;
    this.kernel = kernel;
    this.minBandwidth = minBandwidth;
    this.scale = scale;
    this.idim = idim;
  }

  /**
   * Run the KDEOS outlier detection algorithm.
   *
   * @param rel Relation to process
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<O> rel) {
    final DBIDs ids = rel.getDBIDs();

    LOG.verbose("Running kNN preprocessor.");
    KNNQuery<O> knnq = DatabaseUtil.precomputedKNNQuery(rel, getDistance(), kmax + 1);

    // Initialize store for densities
    WritableDataStore<double[]> densities = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, double[].class);
    estimateDensities(rel, knnq, ids, densities);

    // Compute scores:
    WritableDoubleDataStore kofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DoubleMinMax minmax = new DoubleMinMax();
    computeOutlierScores(knnq, ids, densities, kofs, minmax);

    DoubleRelation scoreres = new MaterializedDoubleRelation("Kernel Density Estimation Outlier Scores", ids, kofs);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore(minmax.getMin(), minmax.getMax());
    return new OutlierResult(meta, scoreres);
  }

  /**
   * Perform the kernel density estimation step.
   *
   * @param rel Relation to query
   * @param knnq kNN query
   * @param ids IDs to process
   * @param densities Density storage
   */
  protected void estimateDensities(Relation<O> rel, KNNQuery<O> knnq, final DBIDs ids, WritableDataStore<double[]> densities) {
    final int dim = dimensionality(rel);
    final int knum = kmax + 1 - kmin;
    // Initialize storage:
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      densities.put(iter, new double[knum]);
    }
    // Distribute densities:
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing densities", ids.size(), LOG) : null;
    double iminbw = (minBandwidth > 0.) ? 1. / (minBandwidth * scale) : Double.POSITIVE_INFINITY;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      KNNList neighbors = knnq.getKNNForDBID(iter, kmax + 1);
      int k = 1, idx = 0;
      double sum = 0.;
      for(DoubleDBIDListIter kneighbor = neighbors.iter(); k <= kmax && kneighbor.valid(); kneighbor.advance(), k++) {
        sum += kneighbor.doubleValue();
        if(k < kmin) {
          continue;
        }
        final double ibw = Math.min(k / (sum * scale), iminbw);
        final double sca = MathUtil.powi(ibw, dim);
        for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          final double dens;
          if(sca < Double.POSITIVE_INFINITY) { // NaNs with duplicate points!
            dens = sca * kernel.density(neighbor.doubleValue() * ibw);
          }
          else {
            dens = neighbor.doubleValue() == 0. ? 1. : 0.;
          }
          densities.get(neighbor)[idx] += dens;
          if(dens < CUTOFF) {
            break;
          }
        }
        ++idx; // Only if k >= kmin
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Ugly hack to allow using this implementation without having a well-defined
   * dimensionality.
   *
   * @param rel Data relation
   * @return Dimensionality
   */
  private int dimensionality(Relation<O> rel) {
    // Explicit:
    if(idim >= 0) {
      return idim;
    }
    // Cast to vector field relation.
    @SuppressWarnings("unchecked")
    final Relation<NumberVector> frel = (Relation<NumberVector>) rel;
    int dim = RelationUtil.dimensionality(frel);
    if(dim < 1) {
      throw new AbortException("When using KDEOS with non-vectorspace data, the intrinsic dimensionality parameter must be set!");
    }
    return dim;
  }

  /**
   * Compute the final KDEOS scores.
   *
   * @param knnq kNN query
   * @param ids IDs to process
   * @param densities Density estimates
   * @param kdeos Score outputs
   * @param minmax Minimum and maximum scores
   */
  protected void computeOutlierScores(KNNQuery<O> knnq, final DBIDs ids, WritableDataStore<double[]> densities, WritableDoubleDataStore kdeos, DoubleMinMax minmax) {
    final int knum = kmax + 1 - kmin;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing KDEOS scores", ids.size(), LOG) : null;

    double[][] scratch = new double[knum][kmax + 5];
    MeanVariance mv = new MeanVariance();

    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double[] dens = densities.get(iter);
      KNNList neighbors = knnq.getKNNForDBID(iter, kmax + 1);
      if(scratch[0].length < neighbors.size()) {
        // Resize scratch. Add some extra margin again.
        scratch = new double[knum][neighbors.size() + 5];
      }
      { // Store density matrix of neighbors
        int i = 0;
        for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance(), i++) {
          double[] ndens = densities.get(neighbor);
          for(int k = 0; k < knum; k++) {
            scratch[k][i] = ndens[k];
          }
        }
        assert (i == neighbors.size());
      }
      // Compute means and stddevs for each k
      double score = 0.;
      for(int i = 0; i < knum; i++) {
        mv.reset();
        for(int j = 0; j < neighbors.size(); j++) {
          mv.put(scratch[i][j]);
        }
        final double mean = mv.getMean(), stddev = mv.getSampleStddev();
        if(stddev > 0.) {
          score += (mean - dens[i]) / stddev;
        }
      }
      score /= knum; // average
      score = NormalDistribution.standardNormalCDF(score);
      minmax.put(score);
      kdeos.put(iter, score);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    TypeInformation res = getDistance().getInputTypeRestriction();
    if(idim < 0) {
      res = new CombinedTypeInformation(TypeUtil.NUMBER_VECTOR_FIELD, res);
    }
    return TypeUtil.array(res);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    /**
     * Parameter to specify the kernel density function.
     */
    public static final OptionID KERNEL_ID = new OptionID("kdeos.kernel", "Kernel density function to use.");

    /**
     * Parameter to specify the minimum bandwidth.
     */
    public static final OptionID KERNEL_MIN_ID = new OptionID("kdeos.kernel.minbw", "Minimum bandwidth for kernel density estimation.");

    /**
     * Parameter to specify the kernel scaling factor.
     */
    public static final OptionID KERNEL_SCALE_ID = new OptionID("kdeos.kernel.scale", "Scaling factor for the kernel function.");

    /**
     * Minimum value of k to analyze.
     */
    public static final OptionID KMIN_ID = new OptionID("kdeos.k.min", "Minimum value of k to analyze.");

    /**
     * Maximum value of k to analyze.
     */
    public static final OptionID KMAX_ID = new OptionID("kdeos.k.max", "Maximum value of k to analyze.");

    /**
     * Intrinsic dimensionality.
     */
    public static final OptionID IDIM_ID = new OptionID("kdeos.idim", "Intrinsic dimensionality of this data set. Use -1 for using the true data dimensionality, but values such as 0-2 often offer better performance.");

    /**
     * Kernel function to use for density estimation.
     */
    KernelDensityFunction kernel;

    /**
     * Minimum and maximum number of neighbors to use.
     */
    int kmin;

    /**
     * Minimum and maximum number of neighbors to use.
     */
    int kmax;

    /**
     * Kernel scaling parameter.
     */
    double scale;

    /**
     * Kernel minimum bandwidth.
     */
    double minBandwidth = 0.;

    /**
     * Intrinsic dimensionality.
     */
    int idim = -1;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<KernelDensityFunction>(KERNEL_ID, KernelDensityFunction.class, GaussianKernelDensityFunction.class) //
          .grab(config, x -> kernel = x);
      IntParameter kminP = new IntParameter(KMIN_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      kminP.grab(config, x -> kmin = x);
      IntParameter kmaxP = new IntParameter(KMAX_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      kmaxP.grab(config, x -> kmax = x);
      // Non-formalized parameter constraint: k_min <= k_max
      if(kmin > kmax) {
        config.reportError(new WrongParameterValueException(kminP, "must be at most", kmaxP, ""));
      }
      new DoubleParameter(KERNEL_SCALE_ID)//
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setDefaultValue(.25) //
          // For simpler parameterization, scale kernels by their canonical
          // bandwidth, when the kernel is configured.
          .grab(config, x -> scale = x * ((kernel != null) ? kernel.canonicalBandwidth() : 1.));
      new DoubleParameter(KERNEL_MIN_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .setOptional(true) //
          .grab(config, x -> minBandwidth = x);
      new IntParameter(IDIM_ID, 1) //
          .grab(config, x -> idim = x);
    }

    @Override
    public KDEOS<O> make() {
      return new KDEOS<>(distanceFunction, kmin, kmax, kernel, minBandwidth, scale, idim);
    }
  }
}
