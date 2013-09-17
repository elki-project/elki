package experimentalcode.erich.diss;

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
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MeanVarianceMinMax;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.KernelDensityFunction;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;

/**
 * Kernel-based outlier detection.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class KernelOutlier<O extends FeatureVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KernelOutlier.class);

  KernelDensityFunction kernel;

  int kmin, kmax;

  double scale;

  double minBandwidth = 1e-6;

  final static double CUTOFF = 1e-20;

  boolean usemad = false;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param kmin Minimum number of neighbors
   * @param kmax Maximum number of neighbors
   * @param kernel Kernel function
   * @param scale Kernel scaling parameter
   * @param usemad Use MAD instead of mean and variance.
   */
  public KernelOutlier(DistanceFunction<? super O, D> distanceFunction, int kmin, int kmax, KernelDensityFunction kernel, double scale, boolean usemad) {
    super(distanceFunction);
    this.kmin = kmin;
    this.kmax = kmax;
    this.kernel = kernel;
    this.scale = scale;
    this.usemad = usemad;
  }

  public OutlierResult run(Database database, Relation<O> rel) {
    final DistanceQuery<O, D> dq = database.getDistanceQuery(rel, getDistanceFunction());
    final int dim = RelationUtil.dimensionality(rel);
    final DBIDs ids = rel.getDBIDs();

    KNNQuery<O, D> knnq;
    // We need each neighborhood twice - use "HEAVY" flag.
    knnq = QueryUtil.getKNNQuery(rel, getDistanceFunction(), kmax + 1, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if (knnq == null) {
      LOG.verbose("Running kNN preprocessor.");
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<>(rel, getDistanceFunction(), kmax + 1);
      database.addIndex(preproc);
      knnq = preproc.getKNNQuery(dq, kmax + 1, DatabaseQuery.HINT_HEAVY_USE);
    }

    // Initialize store for densities
    WritableDataStore<double[]> densities = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, double[].class);
    final int knum = kmax + 1 - kmin;
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      densities.put(iter, new double[knum]);
    }
    // Distribute densities:
    LOG.verbose("Computing densities.");
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      KNNList<D> neighbors = knnq.getKNNForDBID(iter, kmax + 1);
      if (neighbors instanceof DoubleDistanceKNNList) {
        final DoubleDistanceKNNList dneighbors = (DoubleDistanceKNNList) neighbors;
        int k = 1;
        double sum = 0.;
        for (DoubleDistanceDBIDListIter kneighbor = dneighbors.iter(); kneighbor.valid(); kneighbor.advance(), k++) {
          sum += kneighbor.doubleDistance();
          if (k < kmin) {
            continue;
          }
          final double ibw = k / (.5*sum * scale);
          final double sca = 1.; // Math.pow(ibw, dim);
          for (DoubleDistanceDBIDListIter neighbor = dneighbors.iter(); neighbor.valid(); neighbor.advance()) {
            double dens = sca * kernel.density(neighbor.doubleDistance() * ibw);
            densities.get(neighbor)[k - kmin] += dens;
            if (dens < CUTOFF) { break; }
          }
          if (k == kmax) {
            break;
          }
        }
        assert (k == kmax) : "K is " + k + " size: " + neighbors.size();
      } else {
        throw new NotImplementedException(ExceptionMessages.UNSUPPORTED_NOT_YET);
      }
    }
    // Compute scores:
    LOG.verbose("Computing scores.");
    WritableDoubleDataStore kofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DoubleMinMax minmax = new DoubleMinMax();

    double[][] scratch = new double[knum][kmax + 5];
    double[] meds = new double[knum], mads = new double[knum];
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double[] dens = densities.get(iter);
      KNNList<D> neighbors = knnq.getKNNForDBID(iter, kmax + 1);
      if (scratch[0].length < neighbors.size()) {
        // Resize! Add some extra margin again.
        scratch = new double[knum][neighbors.size() + 5];
      }
      if (neighbors instanceof DoubleDistanceKNNList) {
        {
          int i = 0;
          for (DoubleDistanceDBIDListIter neighbor = ((DoubleDistanceKNNList) neighbors).iter(); neighbor.valid(); neighbor.advance(), i++) {
            double[] ndens = densities.get(neighbor);
            for (int k = 0; k < knum; k++) {
              scratch[k][i] = ndens[k];
            }
          }
          assert (i == neighbors.size());
        }
        // Compute medians and MADs
        double score = 0.;
        if (usemad) {
          for (int k = 0; k < knum; k++) {
            meds[k] = QuickSelect.median(scratch[k], 0, neighbors.size());
            for (int j = 0; j < neighbors.size(); j++) {
              scratch[k][j] = Math.abs(scratch[k][j] - meds[k]);
            }
            mads[k] = NormalDistribution.ONEBYPHIINV075 * QuickSelect.median(scratch[k], 0, neighbors.size());
          }
          for (int k = 0; k < knum; k++) {
            if (mads[k] > 0) {
              meds[k] = -(dens[k] - meds[k]) / mads[k];
            } else {
              meds[k] = 0.;
            }
          }
          score = QuickSelect.quantile(meds, .5);
        } else {
          MeanVariance mv = new MeanVariance();
          for (int k = 0; k < knum; k++) {
            for (int j = 0; j < neighbors.size(); j++) {
              mv.put(scratch[k][j]);
            }
            meds[k] = mv.getMean();
            mads[k] = mv.getSampleStddev();
            mv.reset();
          }
          for (int k = 0; k < knum; k++) {
            final double s = (mads[k] > 0.) ? (-(dens[k] - meds[k]) / mads[k]) : 0.;
            score += s;
          }
          score /= knum;
        }
        // score = Math.max(0, Math.exp(score));
        score = NormalDistribution.standardNormalCDF(score);
        minmax.put(score);
        kofs.put(iter, score);
      } else {
        throw new AbortException(ExceptionMessages.UNSUPPORTED_NOT_YET);
      }
    }

    Relation<Double> scoreres = new MaterializedRelation<>("Kernel Outlier Factor", "kof-outlier", TypeUtil.DOUBLE, kofs, ids);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(meta, scoreres);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // FIXME: require vector field, too!
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
