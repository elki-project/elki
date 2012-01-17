package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * Fast Outlier Detection Using the "Local Correlation Integral".
 * 
 * Exact implementation only, not aLOCI.
 * 
 * TODO: add aLOCI
 * 
 * Outlier detection using multiple epsilon neighborhoods.
 * 
 * Based on: S. Papadimitriou, H. Kitagawa, P. B. Gibbons and C. Faloutsos:
 * LOCI: Fast Outlier Detection Using the Local Correlation Integral. In: Proc.
 * 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has RangeQuery
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("LOCI: Fast Outlier Detection Using the Local Correlation Integral")
@Description("Algorithm to compute outliers based on the Local Correlation Integral")
@Reference(authors = "S. Papadimitriou, H. Kitagawa, P. B. Gibbons, C. Faloutsos", title = "LOCI: Fast Outlier Detection Using the Local Correlation Integral", booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003", url = "http://dx.doi.org/10.1109/ICDE.2003.1260802")
public class LOCI<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(LOCI.class);

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   */
  public static final OptionID RMAX_ID = OptionID.getOrCreateOptionID("loci.rmax", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the minimum neighborhood size
   */
  public static final OptionID NMIN_ID = OptionID.getOrCreateOptionID("loci.nmin", "Minimum neighborhood size to be considered.");

  /**
   * Parameter to specify the averaging neighborhood scaling.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("loci.alpha", "Scaling factor for averaging neighborhood");

  /**
   * Holds the value of {@link #RMAX_ID}.
   */
  private D rmax;

  /**
   * Holds the value of {@link #NMIN_ID}.
   */
  private int nmin;

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param rmax Maximum radius
   * @param nmin Minimum neighborhood size
   * @param alpha Alpha value
   */
  public LOCI(DistanceFunction<? super O, D> distanceFunction, D rmax, int nmin, double alpha) {
    super(distanceFunction);
    this.rmax = rmax;
    this.nmin = nmin;
    this.alpha = alpha;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    Relation<O> relation = database.getRelation(getInputTypeRestriction()[0]);
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    RangeQuery<O, D> rangeQuery = database.getRangeQuery(distFunc);

    FiniteProgress progressPreproc = logger.isVerbose() ? new FiniteProgress("LOCI preprocessing", relation.size(), logger) : null;
    // LOCI preprocessing step
    WritableDataStore<ArrayList<DoubleIntPair>> interestingDistances = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_SORTED, ArrayList.class);
    for(DBID id : relation.iterDBIDs()) {
      List<DistanceResultPair<D>> neighbors = rangeQuery.getRangeForDBID(id, rmax);
      // build list of critical distances
      ArrayList<DoubleIntPair> cdist = new ArrayList<DoubleIntPair>(neighbors.size() * 2);
      {
        for(int i = 0; i < neighbors.size(); i++) {
          DistanceResultPair<D> r = neighbors.get(i);
          if(i + 1 < neighbors.size() && r.getDistance().compareTo(neighbors.get(i + 1).getDistance()) == 0) {
            continue;
          }
          cdist.add(new DoubleIntPair(r.getDistance().doubleValue(), i));
          final double ri = r.getDistance().doubleValue() / alpha;
          if(ri <= rmax.doubleValue()) {
            cdist.add(new DoubleIntPair(ri, Integer.MIN_VALUE));
          }
        }
      }
      Collections.sort(cdist);
      // fill the gaps to have fast lookups of number of neighbors at a given
      // distance.
      int lastk = 0;
      for(DoubleIntPair c : cdist) {
        if(c.second == Integer.MIN_VALUE) {
          c.second = lastk;
        }
        else {
          lastk = c.second;
        }
      }

      interestingDistances.put(id, cdist);
      if(progressPreproc != null) {
        progressPreproc.incrementProcessed(logger);
      }
    }
    if(progressPreproc != null) {
      progressPreproc.ensureCompleted(logger);
    }
    // LOCI main step
    FiniteProgress progressLOCI = logger.isVerbose() ? new FiniteProgress("LOCI scores", relation.size(), logger) : null;
    WritableDoubleDataStore mdef_norm = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore mdef_radius = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();

    for(DBID id : relation.iterDBIDs()) {
      final List<DoubleIntPair> cdist = interestingDistances.get(id);
      final double maxdist = cdist.get(cdist.size() - 1).first;
      final int maxneig = cdist.get(cdist.size() - 1).second;

      double maxmdefnorm = 0.0;
      double maxnormr = 0;
      if(maxneig >= nmin) {
        D range = distFunc.getDistanceFactory().fromDouble(maxdist);
        // Compute the largest neighborhood we will need.
        List<DistanceResultPair<D>> maxneighbors = rangeQuery.getRangeForDBID(id, range);
        // Ensure the set is sorted. Should be a no-op with most indexes.
        Collections.sort(maxneighbors);
        // For any critical distance, compute the normalized MDEF score.
        for(DoubleIntPair c : cdist) {
          // Only start when minimum size is fulfilled
          if (c.second < nmin) {
            continue;
          }
          final double r = c.first;
          final double alpha_r = alpha * r;
          // compute n(p_i, \alpha * r) from list (note: alpha_r is different from c!)
          final int n_alphar = elementsAtRadius(cdist, alpha_r);
          // compute \hat{n}(p_i, r, \alpha) and the corresponding \simga_{MDEF}
          MeanVariance mv_n_r_alpha = new MeanVariance();
          for(DistanceResultPair<D> ne : maxneighbors) {
            // Stop at radius r
            if(ne.getDistance().doubleValue() > r) {
              break;
            }
            int rn_alphar = elementsAtRadius(interestingDistances.get(ne.getDBID()), alpha_r);
            mv_n_r_alpha.put(rn_alphar);
          }
          // We only use the average and standard deviation
          final double nhat_r_alpha = mv_n_r_alpha.getMean();
          final double sigma_nhat_r_alpha = mv_n_r_alpha.getNaiveStddev();

          // Redundant divisions removed.
          final double mdef = (nhat_r_alpha - n_alphar); // / nhat_r_alpha;
          final double sigmamdef = sigma_nhat_r_alpha; // / nhat_r_alpha;
          final double mdefnorm = mdef / sigmamdef;

          if(mdefnorm > maxmdefnorm) {
            maxmdefnorm = mdefnorm;
            maxnormr = r;
          }
        }
      }
      else {
        // FIXME: when nmin was not fulfilled - what is the proper value then?
        maxmdefnorm = 1.0;
        maxnormr = maxdist;
      }
      mdef_norm.putDouble(id, maxmdefnorm);
      mdef_radius.putDouble(id, maxnormr);
      minmax.put(maxmdefnorm);
      if(progressLOCI != null) {
        progressLOCI.incrementProcessed(logger);
      }
    }
    if(progressLOCI != null) {
      progressLOCI.ensureCompleted(logger);
    }
    Relation<Double> scoreResult = new MaterializedRelation<Double>("LOCI normalized MDEF", "loci-mdef-outlier", TypeUtil.DOUBLE, mdef_norm, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.POSITIVE_INFINITY, 0.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    result.addChildResult(new MaterializedRelation<Double>("LOCI MDEF Radius", "loci-critical-radius", TypeUtil.DOUBLE, mdef_radius, relation.getDBIDs()));
    return result;
  }

  /**
   * Get the number of objects for a given radius, from the list of critical
   * distances, storing (radius, count) pairs.
   * 
   * @param criticalDistances
   * @param radius
   * @return Number of elements at the given radius
   */
  protected int elementsAtRadius(List<DoubleIntPair> criticalDistances, final double radius) {
    int n_r = 0;
    for(DoubleIntPair c2 : criticalDistances) {
      if(c2.first > radius) {
        break;
      }
      if(c2.second != Integer.MIN_VALUE) {
        // Update
        n_r = c2.second;
      }
    }
    return n_r;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected D rmax = null;

    protected int nmin = 0;

    protected double alpha = 0.5;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final D distanceFactory = (distanceFunction != null) ? distanceFunction.getDistanceFactory() : null;
      final DistanceParameter<D> rmaxP = new DistanceParameter<D>(RMAX_ID, distanceFactory);
      if(config.grab(rmaxP)) {
        rmax = rmaxP.getValue();
      }

      final IntParameter nminP = new IntParameter(NMIN_ID, 20);
      if(config.grab(nminP)) {
        nmin = nminP.getValue();
      }

      final DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 0.5);
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }
    }

    @Override
    protected LOCI<O, D> makeInstance() {
      return new LOCI<O, D>(distanceFunction, rmax, nmin, alpha);
    }
  }
}