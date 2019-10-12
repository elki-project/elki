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

import java.util.Arrays;

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.math.MeanVariance;
import elki.outlier.OutlierAlgorithm;
import elki.result.Metadata;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.QuotientOutlierScoreMeta;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Fast Outlier Detection Using the "Local Correlation Integral".
 * <p>
 * Exact implementation only, not aLOCI. See {@link ALOCI}.
 * <p>
 * Outlier detection using multiple epsilon neighborhoods.
 * <p>
 * This implementation has O(n<sup>3</sup> log n) runtime complexity!
 * <p>
 * Reference:
 * <p>
 * S. Papadimitriou, H. Kitagawa, P. B. Gibbons, C. Faloutsos:<br>
 * LOCI: Fast Outlier Detection Using the Local Correlation Integral.<br>
 * In: Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03)
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @has - - - RangeQuery
 *
 * @param <O> Object type
 */
@Title("LOCI: Fast Outlier Detection Using the Local Correlation Integral")
@Description("Algorithm to compute outliers based on the Local Correlation Integral")
@Reference(authors = "S. Papadimitriou, H. Kitagawa, P. B. Gibbons, C. Faloutsos", //
    title = "LOCI: Fast Outlier Detection Using the Local Correlation Integral", //
    booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03)", //
    url = "https://doi.org/10.1109/ICDE.2003.1260802", //
    bibkey = "DBLP:conf/icde/PapadimitriouKGF03")
public class LOCI<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LOCI.class);

  /**
   * Maximum radius.
   */
  private double rmax;

  /**
   * Minimum neighborhood size.
   */
  private int nmin = 0;

  /**
   * Scaling of averaging neighborhood.
   */
  private double alpha = 0.5;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param rmax Maximum radius
   * @param nmin Minimum neighborhood size
   * @param alpha Alpha value
   */
  public LOCI(Distance<? super O> distance, double rmax, int nmin, double alpha) {
    super(distance);
    this.rmax = rmax;
    this.nmin = nmin;
    this.alpha = alpha;
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    RangeQuery<O> rangeQuery = new QueryBuilder<>(relation, distance).rangeQuery();
    DBIDs ids = relation.getDBIDs();

    // LOCI preprocessing step
    WritableDataStore<DoubleIntArrayList> interestingDistances = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_SORTED, DoubleIntArrayList.class);
    precomputeInterestingRadii(ids, rangeQuery, interestingDistances);
    // LOCI main step
    FiniteProgress progressLOCI = LOG.isVerbose() ? new FiniteProgress("LOCI scores", relation.size(), LOG) : null;
    WritableDoubleDataStore mdef_norm = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore mdef_radius = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();

    // Shared instance, to save allocations.
    MeanVariance mv_n_r_alpha = new MeanVariance();

    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      final DoubleIntArrayList cdist = interestingDistances.get(iditer);
      final double maxdist = cdist.getDouble(cdist.size() - 1);
      final int maxneig = cdist.getInt(cdist.size() - 1);

      double maxmdefnorm = 0.0;
      double maxnormr = 0;
      if(maxneig >= nmin) {
        // Compute the largest neighborhood we will need.
        DoubleDBIDList maxneighbors = rangeQuery.getRangeForDBID(iditer, maxdist);
        // TODO: Ensure the result is sorted. This is currently implied.

        // For any critical distance, compute the normalized MDEF score.
        for(int i = 0, size = cdist.size(); i < size; i++) {
          // Only start when minimum size is fulfilled
          if(cdist.getInt(i) < nmin) {
            continue;
          }
          final double r = cdist.getDouble(i);
          final double alpha_r = alpha * r;
          // compute n(p_i, \alpha * r) from list (note: alpha_r is not cdist!)
          final int n_alphar = cdist.getInt(cdist.find(alpha_r));
          // compute \hat{n}(p_i, r, \alpha) and the corresponding \simga_{MDEF}
          mv_n_r_alpha.reset();
          for(DoubleDBIDListIter neighbor = maxneighbors.iter(); neighbor.valid(); neighbor.advance()) {
            // Stop at radius r
            if(neighbor.doubleValue() > r) {
              break;
            }
            DoubleIntArrayList cdist2 = interestingDistances.get(neighbor);
            int rn_alphar = cdist2.getInt(cdist2.find(alpha_r));
            mv_n_r_alpha.put(rn_alphar);
          }
          // We only use the average and standard deviation
          final double nhat_r_alpha = mv_n_r_alpha.getMean();
          final double sigma_nhat_r_alpha = mv_n_r_alpha.getNaiveStddev();

          // Redundant divisions by nhat_r_alpha removed.
          final double mdef = nhat_r_alpha - n_alphar;
          final double sigmamdef = sigma_nhat_r_alpha;
          final double mdefnorm = mdef / sigmamdef;

          if(mdefnorm > maxmdefnorm) {
            maxmdefnorm = mdefnorm;
            maxnormr = r;
          }
        }
      }
      else {
        // FIXME: when nmin was not fulfilled - what is the proper value then?
        maxmdefnorm = Double.POSITIVE_INFINITY;
        maxnormr = maxdist;
      }
      mdef_norm.putDouble(iditer, maxmdefnorm);
      mdef_radius.putDouble(iditer, maxnormr);
      minmax.put(maxmdefnorm);
      LOG.incrementProcessed(progressLOCI);
    }
    LOG.ensureCompleted(progressLOCI);
    DoubleRelation scoreResult = new MaterializedDoubleRelation("LOCI normalized MDEF", relation.getDBIDs(), mdef_norm);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    Metadata.hierarchyOf(result).addChild(new MaterializedDoubleRelation("LOCI MDEF Radius", relation.getDBIDs(), mdef_radius));
    return result;
  }

  /**
   * Preprocessing step: determine the radii of interest for each point.
   *
   * @param ids IDs to process
   * @param rangeQuery Range query
   * @param interestingDistances Distances of interest
   */
  protected void precomputeInterestingRadii(DBIDs ids, RangeQuery<O> rangeQuery, WritableDataStore<DoubleIntArrayList> interestingDistances) {
    FiniteProgress progressPreproc = LOG.isVerbose() ? new FiniteProgress("LOCI preprocessing", ids.size(), LOG) : null;
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      DoubleDBIDList neighbors = rangeQuery.getRangeForDBID(iditer, rmax);
      // build list of critical distances
      DoubleIntArrayList cdist = new DoubleIntArrayList(neighbors.size() << 1);
      {
        int i = 0;
        DoubleDBIDListIter ni = neighbors.iter();
        while(ni.valid()) {
          final double curdist = ni.doubleValue();
          ++i;
          ni.advance();
          // Skip, if tied to the next object:
          if(ni.valid() && curdist == ni.doubleValue()) {
            continue;
          }
          cdist.append(curdist, i);
          // Scale radius, and reinsert
          if(alpha != 1.) {
            final double ri = curdist / alpha;
            if(ri <= rmax) {
              cdist.append(ri, Integer.MIN_VALUE);
            }
          }
        }
      }
      cdist.sort();

      // fill the gaps to have fast lookups of number of neighbors at a given
      // distance.
      int lastk = 0;
      for(int i = 0, size = cdist.size(); i < size; i++) {
        final int k = cdist.getInt(i);
        if(k == Integer.MIN_VALUE) {
          cdist.setValue(i, lastk);
        }
        else {
          lastk = k;
        }
      }
      // TODO: shrink the list, removing duplicate radii?

      interestingDistances.put(iditer, cdist);
      LOG.incrementProcessed(progressPreproc);
    }
    LOG.ensureCompleted(progressPreproc);
  }

  /**
   * Array of double-int values.
   *
   * @author Erich Schubert
   */
  private static class DoubleIntArrayList {
    /**
     * Double keys
     */
    double[] keys;

    /**
     * Integer values
     */
    int[] vals;

    /**
     * Used size
     */
    int size = 0;

    /**
     * Constructor.
     *
     * @param alloc Initial allocation.
     */
    public DoubleIntArrayList(int alloc) {
      keys = new double[alloc];
      vals = new int[alloc];
      size = 0;
    }

    /**
     * Collection size.
     *
     * @return Size
     */
    public int size() {
      return size;
    }

    /**
     * Get the key at the given position.
     *
     * @param i Position
     * @return Key
     */
    public double getDouble(int i) {
      return keys[i];
    }

    /**
     * Get the value at the given position.
     *
     * @param i Position
     * @return Value
     */
    public int getInt(int i) {
      return vals[i];
    }

    /**
     * Get the value at the given position.
     *
     * @param i Position
     * @param val New value
     */
    public void setValue(int i, int val) {
      vals[i] = val;
    }

    /**
     * Append a key-value pair.
     *
     * @param key Key to append
     * @param val Value to append.
     */
    public void append(double key, int val) {
      if(size == keys.length) {
        keys = Arrays.copyOf(keys, size << 1);
        vals = Arrays.copyOf(vals, size << 1);
      }
      keys[size] = key;
      vals[size] = val;
      ++size;
    }

    /**
     * Find the last position with a smaller or equal key.
     *
     * @param search Key
     * @return Position
     */
    public int find(final double search) {
      int a = 0, b = size - 1;
      while(a <= b) {
        final int mid = (a + b) >>> 1;
        final double cur = keys[mid];
        if(cur > search) {
          b = mid - 1;
        }
        else { // less or equal!
          a = mid + 1;
        }
      }
      return b;
    }

    /**
     * Sort the array list.
     */
    public void sort() {
      DoubleIntegerArrayQuickSort.sort(keys, vals, size);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
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
   * @param <O> Object type
   */
  public static class Par<O> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered, must be suitable to the distance function specified.
     */
    public static final OptionID RMAX_ID = new OptionID("loci.rmax", "The maximum radius of the neighborhood to be considered.");

    /**
     * Parameter to specify the minimum neighborhood size
     */
    public static final OptionID NMIN_ID = new OptionID("loci.nmin", "Minimum neighborhood size to be considered.");

    /**
     * Parameter to specify the averaging neighborhood scaling.
     */
    public static final OptionID ALPHA_ID = new OptionID("loci.alpha", "Scaling factor for averaging neighborhood");

    /**
     * Maximum radius.
     */
    protected double rmax;

    /**
     * Minimum neighborhood size.
     */
    protected int nmin = 0;

    /**
     * Scaling of averaging neighborhood.
     */
    protected double alpha = 0.5;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(RMAX_ID) //
          .grab(config, x -> rmax = x);
      new IntParameter(NMIN_ID, 20) //
          .grab(config, x -> nmin = x);
      new DoubleParameter(ALPHA_ID, 0.5) //
          .grab(config, x -> alpha = x);
    }

    @Override
    public LOCI<O> make() {
      return new LOCI<>(distance, rmax, nmin, alpha);
    }
  }
}
